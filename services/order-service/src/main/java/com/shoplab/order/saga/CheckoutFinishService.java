package com.shoplab.order.saga;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.error.NotFoundException;
import com.shoplab.order.order.OrderRecord;
import com.shoplab.order.order.OrderRepository;
import com.shoplab.order.order.OrderStatus;
import com.shoplab.order.order.OrderWriter;
import com.shoplab.order.saga.downstream.DownstreamGateway;
import com.shoplab.order.saga.downstream.DownstreamResult;
import com.shoplab.order.saga.downstream.PaymentStatusResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
  private final Clock clock;

  public CheckoutFinishService(
      DownstreamGateway downstream,
      CheckoutSagaRepository sagas,
      OrderRepository orders,
      OrderWriter writer,
      Clock clock) {
    this.downstream = downstream;
    this.sagas = sagas;
    this.orders = orders;
    this.writer = writer;
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
    writer.confirmPending(orderId, "thanh toán đã xác nhận", now);
    sagas.markState(orderId, CheckoutSagaState.CONFIRMED, null, now);
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
    writer.cancelPending(orderId, why, now);
    sagas.markState(orderId, CheckoutSagaState.CANCELLED, null, now);
    LOG.info("checkout cancelled: order={} reason={}", orderId, why);
    return reloaded(orderId);
  }

  // __FINISH_PART2__

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
          "quá payment-window, hàng/voucher do TTL sweeper của inventory/promotion trả");
      LOG.info("checkout expired: order={} (hàng/voucher trả theo TTL)", orderId);
    }
    return changed;
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

  private static AppException conflict(String message) {
    return new AppException(ErrorCode.CONFLICT, message);
  }
}
