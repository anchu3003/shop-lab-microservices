package com.shoplab.order.saga;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.commons.serviceauth.ServiceTokenClient;
import com.shoplab.order.order.OrderRecord;
import com.shoplab.order.order.OrderRepository;
import com.shoplab.order.order.OrderStatus;
import com.shoplab.order.order.OrderWriter;
import com.shoplab.order.outbox.CheckoutOutbox;
import com.shoplab.order.saga.downstream.DownstreamGateway;
import com.shoplab.order.saga.downstream.DownstreamResult;
import com.shoplab.order.saga.downstream.PaymentStatusResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Bước **hoàn tất** của checkout saga (`SL-2.6b`): chốt đơn, huỷ đơn, và để lô hết hạn cho sweeper.
 *
 * <p><b>Ba nguyên tắc</b>:
 *
 * <ol>
 *   <li><b>Không tin client</b>: "đã thanh toán" phải đọc từ `payment-service` — nếu không, ai cũng
 *       bấm confirm được đơn mà không trả đồng nào.
 *   <li><b>Không rollback commit</b>: chốt hàng rồi chốt voucher; nếu voucher fail thì để stock đã
 *       chốt và trả lỗi — retry confirm an toàn vì `commit` của inventory/promotion **idempotent**
 *       (gọi lại khi đã COMMITTED ⇒ 200). "Un-commit" không có ý nghĩa nghiệp vụ.
 *   <li><b>Chốt chặn ở order, không ở saga</b>: mọi chuyển trạng thái đi qua {@code
 *       OrderWriter.*Pending} (compare-and-set `WHERE status = 'PENDING_PAYMENT'`) ⇒ 1 lần chuyển,
 *       1 dòng history, kể cả khi 2 request đồng thời.
 * </ol>
 */
@Service
public class CheckoutFinishService {

  private static final Logger LOG = LoggerFactory.getLogger(CheckoutFinishService.class);

  private final DownstreamGateway downstream;
  private final CheckoutSagaRepository sagas;
  private final OrderRepository orders;
  private final OrderWriter writer;
  private final CheckoutOutbox finalizer;
  private final ServiceTokenClient serviceTokens;
  private final Clock clock;

  public CheckoutFinishService(
      DownstreamGateway downstream,
      CheckoutSagaRepository sagas,
      OrderRepository orders,
      OrderWriter writer,
      CheckoutOutbox finalizer,
      ServiceTokenClient serviceTokens,
      Clock clock) {
    this.downstream = downstream;
    this.sagas = sagas;
    this.orders = orders;
    this.writer = writer;
    this.finalizer = finalizer;
    this.serviceTokens = serviceTokens;
    this.clock = clock;
  }

  /**
   * Chốt đơn sau khi thanh toán (hoặc ngay với đơn 0 đồng).
   *
   * @throws AppException 404 saga không tồn tại/không phải của mình · 409 sai trạng thái (đã huỷ,
   *     hết hạn, payment chưa `SUCCEEDED`) · 503 không tới được service đích
   */
  public SagaStatusResponse confirm(UUID orderId, UUID actorId, boolean admin, String bearer) {
    CheckoutSagaRecord saga = requireReadable(orderId, actorId, admin);
    if (saga.state() == CheckoutSagaState.CONFIRMED) {
      // Idempotent: khách bấm 2 lần / app retry đều nhận kết quả cũ, không commit lần 2.
      return SagaStatusResponse.from(saga);
    }
    requireInFlight(saga, "confirm");
    requireOrderPending(orderId);

    verifyPayment(saga, bearer);
    commitStock(saga, bearer);
    commitVoucher(saga, bearer);

    Instant now = Instant.now(clock);
    // SL-3.3: chốt đơn + đánh saga + GHI OUTBOX trong MỘT transaction (bean riêng
    // `CheckoutOutbox` — self-invocation sẽ bỏ qua @Transactional).
    finalizer.confirm(orderId, "thanh toán đã xác nhận", now);
    LOG.info("checkout confirmed: order={} paymentRequired={}", orderId, saga.paymentRequired());
    return reloaded(orderId);
  }

  /**
   * Huỷ đơn còn chờ thanh toán → trả hàng + trả lượt voucher.
   *
   * <p>Idempotent: huỷ lần 2 trả trạng thái hiện tại; huỷ đơn **đã confirm** → 409 (tiền đã về, huỷ
   * là nghiệp vụ hoàn tiền — thuộc Phase 6).
   */
  public SagaStatusResponse cancel(
      UUID orderId, UUID actorId, boolean admin, String bearer, String reason) {
    CheckoutSagaRecord saga = requireReadable(orderId, actorId, admin);
    switch (saga.state()) {
      case CANCELLED, EXPIRED -> {
        return SagaStatusResponse.from(saga);
      }
      case CONFIRMED -> throw conflict("Đơn đã CONFIRMED — huỷ cần quy trình hoàn tiền (Phase 6)");
      case FAILED_STOCK, FAILED_ORDER, FAILED_PAYMENT -> {
        return SagaStatusResponse.from(saga);
      }
      case STARTED -> throw conflict("Saga chưa xong bước start — chưa có gì để huỷ");
      case COMPLETED_START -> {
        // Đi tiếp xuống dưới.
      }
      default -> throw new IllegalStateException("Trạng thái saga lạ: " + saga.state());
    }

    releaseStock(saga, bearer);
    releaseVoucher(saga, bearer);

    Instant now = Instant.now(clock);
    String why = reason == null || reason.isBlank() ? "khách huỷ" : reason;
    // SL-3.3: huỷ + ghi outbox cùng transaction (như confirm).
    finalizer.cancel(orderId, why, now);
    LOG.info("checkout cancelled: order={} reason={}", orderId, why);
    return reloaded(orderId);
  }

  // __FINISH_PART2__

  /**
   * Audience của 2 service đích — trùng `spring.application.name` của chúng (điều kiện của `aud`).
   */
  private static final String INVENTORY_AUDIENCE = "inventory-service";

  private static final String PROMOTION_AUDIENCE = "promotion-service";

  /**
   * Chốt đơn do **event** `payment.succeeded.v1` (SL-3.5, trả `TD-17`) — đường thứ hai, không có
   * token người dùng.
   *
   * <p><b>Vì sao thứ tự kiểm tra quan trọng</b>: đây là chỗ dễ chốt nhầm nhất trong hệ. Mỗi nhánh
   * "không chốt được" phải **ack** (ném ra là message vào DLQ rồi quay lại vô ích ở lần replay sau)
   * và **log đủ dữ kiện** để người trực xử lý tay — vì những ca đó là chuyện tiền, không phải
   * chuyện message.
   *
   * <p><b>Vì sao không gọi {@code verifyPayment}</b>: event được phát ra từ chính `payment-service`
   * ngay sau khi nó CAS intent sang `SUCCEEDED` — gọi lại API payment chỉ tốn round-trip và không
   * có token user để gọi. Bằng chứng thanh toán ở đây là **envelope** (khoá `eventId` do outbox giữ
   * ổn định), cộng thêm 2 chốt: `paymentIntentId` phải khớp saga + consumer dedupe theo `eventId`.
   *
   * @param eventId id envelope — chỉ dùng cho log/audit (dedupe đã do consumer base lo)
   * @param payload payload đã decode (`orderId`, `paymentIntentId`, …)
   */
  public void confirmByEvent(String eventId, Map<String, Object> payload) {
    UUID orderId = uuidOrNull(payload, "orderId");
    UUID intentId = uuidOrNull(payload, "paymentIntentId");
    if (orderId == null || intentId == null) {
      LOG.warn("payment.succeeded thiếu orderId/paymentIntentId (event={}) — bỏ qua", eventId);
      return;
    }

    Optional<CheckoutSagaRecord> found = sagas.findById(orderId);
    if (found.isEmpty()) {
      LOG.warn(
          "payment.succeeded cho đơn không có saga (order={} event={}) — bỏ qua", orderId, eventId);
      return;
    }
    CheckoutSagaRecord saga = found.get();
    if (saga.state() == CheckoutSagaState.CONFIRMED) {
      // Hai đường confirm (client + event) là chấp nhận có chủ ý ⇒ gặp nhau ở đây là chuyện bình
      // thường, không phải lỗi.
      LOG.info("đơn {} đã CONFIRMED trước đó — bỏ qua event {}", orderId, eventId);
      return;
    }
    if (saga.state() != CheckoutSagaState.COMPLETED_START) {
      LOG.warn(
          "đơn {} ở saga {} — không chốt theo event {} (cần xử lý tay)",
          orderId,
          saga.state(),
          eventId);
      return;
    }
    if (saga.paymentIntentId() == null || !saga.paymentIntentId().equals(intentId)) {
      // Chốt nhầm đơn đã trả tiền là lỗi không sửa được bằng code ⇒ từ chối + log to.
      LOG.warn(
          "payment.succeeded KHÔNG khớp intent: đơn {} cần {} nhưng event {} báo {} — không chốt",
          orderId,
          saga.paymentIntentId(),
          eventId,
          intentId);
      return;
    }
    Optional<OrderRecord> order = orders.findById(orderId);
    if (order.isEmpty() || order.get().status() != OrderStatus.PENDING_PAYMENT) {
      // Ca nghiệp vụ thật: tiền đã về nhưng đơn đã EXPIRED/CANCELLED ⇒ phải hoàn tiền (Phase 6).
      LOG.error(
          "TIỀN ĐÃ THU nhưng đơn {} đang {} (event={}, paymentIntent={}) — cần hoàn tiền tay; xem TD-31",
          orderId,
          order.map(o -> o.status().name()).orElse("KHÔNG TỒN TẠI"),
          eventId,
          intentId);
      return;
    }

    addStep(
        orderId,
        CheckoutStep.PAYMENT_VERIFY,
        SagaStepRecord.SagaStepStatus.SKIPPED,
        null,
        System.nanoTime(),
        "đã có payment.succeeded.v1 (event=" + eventId + ")");

    commitStock(saga, serviceTokens.tokenFor(INVENTORY_AUDIENCE));
    commitVoucher(saga, serviceTokens.tokenFor(PROMOTION_AUDIENCE));

    long start = System.nanoTime();
    Instant now = Instant.now(clock);
    finalizer.confirm(orderId, "thanh toán xác nhận qua payment.succeeded.v1", now);
    addStep(
        orderId,
        CheckoutStep.PAYMENT_EVENT_CONFIRM,
        SagaStepRecord.SagaStepStatus.OK,
        null,
        start,
        "event=" + eventId + " intent=" + intentId);
    LOG.info(
        "checkout tự confirm qua event: order={} intent={} event={}", orderId, intentId, eventId);
  }

  /**
   * Sweeper gọi cho từng saga quá `payment-window`: **chỉ ghi DB** (order + saga `EXPIRED`).
   *
   * <p><b>Vì sao không gọi release</b>: sweeper chạy nền, không có JWT của user, mà endpoint
   * `release` của inventory/promotion yêu cầu token (defense in depth) ⇒ gọi sẽ 401. Hàng/voucher
   * được **TTL sweeper của chính inventory/promotion** trả lại (cùng mốc 15′, đã verify ở `SL-2.3`/
   * `SL-2.4`); đánh đổi: hàng về kho trễ tối đa ~1 nhịp quét. Khi Phase 3 có service-to-service
   * auth thì sweeper gọi release ngay (xem `TD-25`).
   *
   * @return {@code true} nếu lần này thực sự chuyển đơn sang `EXPIRED`
   */
  public boolean expire(UUID orderId) {
    Optional<CheckoutSagaRecord> found = sagas.findById(orderId);
    if (found.isEmpty() || found.get().state() != CheckoutSagaState.COMPLETED_START) {
      return false;
    }
    CheckoutSagaRecord saga = found.get();

    // Chỉ trả hàng khi đơn THẬT SỰ còn chờ thanh toán: nếu đơn vừa được event confirm ở nơi khác
    // thì
    // release ở đây sẽ trả hàng của một đơn đã bán (bán quá tồn kho) — kiểm trước khi làm là rẻ
    // nhất.
    Optional<OrderRecord> order = orders.findById(orderId);
    if (order.isEmpty() || order.get().status() != OrderStatus.PENDING_PAYMENT) {
      return false;
    }

    // TD-25 (trả ở SL-3.5): sweeper giờ có service token nên gọi release TRỰC TIẾP thay vì chờ TTL
    // của inventory/promotion (trước đây hàng về kho trễ ≤ 1 nhịp). Lỗi release KHÔNG chặn việc
    // expire: đơn đã hết hạn thì phải rời khỏi trạng thái chờ, còn hàng thì TTL của service kia vẫn
    // là lưới an toàn cuối (log WARN để biết lưới đó có phải dùng tới hay không).
    releaseQuietly(saga, INVENTORY_AUDIENCE, CheckoutStep.STOCK_RELEASE);
    releaseQuietly(saga, PROMOTION_AUDIENCE, CheckoutStep.VOUCHER_RELEASE);

    long start = System.nanoTime();
    Instant now = Instant.now(clock);
    boolean changed = writer.expirePending(orderId, "quá hạn thanh toán", now);
    sagas.markState(orderId, CheckoutSagaState.EXPIRED, "PAYMENT_WINDOW_EXCEEDED", now);
    if (changed) {
      addStep(
          orderId,
          CheckoutStep.EXPIRE,
          SagaStepRecord.SagaStepStatus.OK,
          null,
          start,
          "quá payment-window; hàng/voucher trả trực tiếp bằng service token");
      LOG.info("checkout expired: order={} (đã gọi release)", orderId);
    } else {
      // Release đã chạy nhưng CAS không đổi được trạng thái ⇒ có request confirm chen vào giữa.
      LOG.warn(
          "expire: đơn {} không còn PENDING_PAYMENT khi CAS — đã release hàng/voucher, cần kiểm tay",
          orderId);
    }
    return changed;
  }

  /**
   * Gọi release bằng service token, **nuốt lỗi có log** — dùng cho sweeper (không có ngữ cảnh người
   * dùng để báo lỗi, và một lần release fail không được làm hỏng cả nhịp quét).
   */
  private void releaseQuietly(CheckoutSagaRecord saga, String audience, CheckoutStep step) {
    boolean hasTarget =
        step == CheckoutStep.STOCK_RELEASE ? saga.reservationId() != null : saga.lockId() != null;
    if (!hasTarget) {
      return;
    }
    try {
      String token = serviceTokens.tokenFor(audience);
      if (step == CheckoutStep.STOCK_RELEASE) {
        releaseStock(saga, token);
      } else {
        releaseVoucher(saga, token);
      }
    } catch (RuntimeException ex) {
      LOG.warn(
          "expire: release {} thất bại cho đơn {} ({}): {} — vẫn expire, TTL của {} sẽ trả sau",
          step,
          saga.orderId(),
          audience,
          ex.getMessage(),
          audience);
    }
  }

  private void verifyPayment(CheckoutSagaRecord saga, String bearer) {
    if (!saga.paymentRequired()) {
      // Đơn 0 đồng (voucher 100%, ADR-0006 D6): không có intent để verify.
      addStep(
          saga.orderId(),
          CheckoutStep.PAYMENT_VERIFY,
          SagaStepRecord.SagaStepStatus.SKIPPED,
          null,
          System.nanoTime(),
          "đơn 0 đồng — không cần thanh toán");
      return;
    }
    long start = System.nanoTime();
    if (saga.paymentIntentId() == null) {
      throw conflict("Saga cần thanh toán nhưng không có paymentIntentId");
    }
    DownstreamResult<PaymentStatusResponse> result =
        downstream.getPaymentIntent(bearer, saga.paymentIntentId());
    PaymentStatusResponse payment = result.body();
    String status = payment == null ? "UNKNOWN" : payment.status();
    if (payment == null || !payment.succeeded()) {
      addStep(
          saga.orderId(),
          CheckoutStep.PAYMENT_VERIFY,
          SagaStepRecord.SagaStepStatus.FAILED,
          result.status(),
          start,
          "payment status=" + status);
      throw conflict("Payment chưa SUCCEEDED (đang là " + status + ") — chưa chốt được đơn");
    }
    addStep(
        saga.orderId(),
        CheckoutStep.PAYMENT_VERIFY,
        SagaStepRecord.SagaStepStatus.OK,
        result.status(),
        start,
        "payment SUCCEEDED");
  }

  private void commitStock(CheckoutSagaRecord saga, String bearer) {
    if (saga.reservationId() == null) {
      throw conflict("Saga không có reservationId — không chốt được hàng");
    }
    long start = System.nanoTime();
    DownstreamResult<Void> result = downstream.commitStock(bearer, saga.reservationId());
    addStep(
        saga.orderId(),
        CheckoutStep.STOCK_COMMIT,
        SagaStepRecord.SagaStepStatus.OK,
        result.status(),
        start,
        "reservation=" + saga.reservationId());
  }

  private void commitVoucher(CheckoutSagaRecord saga, String bearer) {
    if (saga.lockId() == null) {
      addStep(
          saga.orderId(),
          CheckoutStep.VOUCHER_COMMIT,
          SagaStepRecord.SagaStepStatus.SKIPPED,
          null,
          System.nanoTime(),
          "không dùng voucher");
      return;
    }
    long start = System.nanoTime();
    DownstreamResult<Void> result = downstream.commitVoucher(bearer, saga.lockId());
    addStep(
        saga.orderId(),
        CheckoutStep.VOUCHER_COMMIT,
        SagaStepRecord.SagaStepStatus.OK,
        result.status(),
        start,
        "lock=" + saga.lockId());
  }

  private void releaseStock(CheckoutSagaRecord saga, String bearer) {
    if (saga.reservationId() == null) {
      return;
    }
    long start = System.nanoTime();
    downstream.releaseStock(bearer, saga.reservationId());
    addStep(
        saga.orderId(),
        CheckoutStep.STOCK_RELEASE,
        SagaStepRecord.SagaStepStatus.OK,
        null,
        start,
        "reservation=" + saga.reservationId());
  }

  private void releaseVoucher(CheckoutSagaRecord saga, String bearer) {
    if (saga.lockId() == null) {
      return;
    }
    long start = System.nanoTime();
    downstream.releaseVoucher(bearer, saga.lockId());
    addStep(
        saga.orderId(),
        CheckoutStep.VOUCHER_RELEASE,
        SagaStepRecord.SagaStepStatus.OK,
        null,
        start,
        "lock=" + saga.lockId());
  }

  // __FINISH_PART3__

  /** 404 cho người ngoài — không lộ sự tồn tại của saga (cùng quy ước `SL-2.5`). */
  private CheckoutSagaRecord requireReadable(UUID orderId, UUID actorId, boolean admin) {
    CheckoutSagaRecord saga =
        sagas
            .findById(orderId)
            .orElseThrow(() -> new NotFoundException("Checkout saga không tồn tại: " + orderId));
    if (!admin && !saga.userId().equals(actorId)) {
      throw new NotFoundException("Checkout saga không tồn tại: " + orderId);
    }
    return saga;
  }

  private static void requireInFlight(CheckoutSagaRecord saga, String action) {
    if (saga.state() != CheckoutSagaState.COMPLETED_START) {
      throw conflict(
          "Không " + action + " được: saga đang ở " + saga.state() + " (cần COMPLETED_START)");
    }
  }

  private void requireOrderPending(UUID orderId) {
    OrderRecord order =
        orders
            .findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order không tồn tại: " + orderId));
    if (order.status() != OrderStatus.PENDING_PAYMENT) {
      throw conflict("Đơn đang ở " + order.status() + " — không confirm được");
    }
  }

  private SagaStatusResponse reloaded(UUID orderId) {
    return sagas
        .findById(orderId)
        .map(SagaStatusResponse::from)
        .orElseThrow(() -> new IllegalStateException("Saga biến mất sau khi cập nhật: " + orderId));
  }

  private void addStep(
      UUID orderId,
      CheckoutStep step,
      SagaStepRecord.SagaStepStatus status,
      Integer httpStatus,
      long startNanos,
      String detail) {
    sagas.addStep(
        new SagaStepRecord(
            orderId,
            step,
            status,
            httpStatus,
            (int) Duration.ofNanos(System.nanoTime() - startNanos).toMillis(),
            truncate(detail),
            Instant.now(clock)));
  }

  private static String truncate(String detail) {
    if (detail == null) {
      return null;
    }
    return detail.length() <= 200 ? detail : detail.substring(0, 197) + "…";
  }

  private static UUID uuidOrNull(Map<String, Object> payload, String field) {
    Object value = payload == null ? null : payload.get(field);
    if (value == null || String.valueOf(value).isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(String.valueOf(value));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static AppException conflict(String message) {
    return new AppException(ErrorCode.CONFLICT, message);
  }
}
