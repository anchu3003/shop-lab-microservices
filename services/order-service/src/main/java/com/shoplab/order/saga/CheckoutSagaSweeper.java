package com.shoplab.order.saga;

import com.shoplab.order.config.SagaProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sweeper của checkout saga (`SL-2.6b`): đơn nào quá `payment-window` mà vẫn `PENDING_PAYMENT` thì
 * đánh dấu `EXPIRED`.
 *
 * <p><b>Vì sao cần</b>: Phase 2 chưa có event từ payment (`TD-17`) ⇒ nếu khách bỏ ngang thì không
 * ai chuyển trạng thái đơn, và nó treo `PENDING_PAYMENT` **vĩnh viễn**.
 *
 * <p><b>Vì sao chỉ ghi DB</b>: sweeper không có JWT của user nên không gọi được `release` (service
 * đích yêu cầu token). Hàng/voucher do TTL sweeper của inventory/promotion trả — xem javadoc {@link
 * CheckoutFinishService#expire} + `TD-25`.
 *
 * <p><b>Bất biến</b>: mỗi đơn chỉ chuyển trạng thái 1 lần (compare-and-set ở {@code orders}) ⇒ chạy
 * 2 vòng liên tiếp, hoặc 2 instance cùng chạy, cũng không tạo dữ liệu sai; lỗi ở 1 đơn không làm
 * chết cả nhịp quét.
 */
@Component
public class CheckoutSagaSweeper {

  private static final Logger LOG = LoggerFactory.getLogger(CheckoutSagaSweeper.class);

  private final CheckoutSagaRepository sagas;
  private final CheckoutFinishService finish;
  private final SagaProperties properties;
  private final Clock clock;

  public CheckoutSagaSweeper(
      CheckoutSagaRepository sagas,
      CheckoutFinishService finish,
      SagaProperties properties,
      Clock clock) {
    this.sagas = sagas;
    this.finish = finish;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(
      fixedDelayString = "${shoplab.saga.sweep-interval:60s}",
      initialDelayString = "${shoplab.saga.sweep-initial-delay:30s}")
  public void sweep() {
    try {
      expireDue();
    } catch (RuntimeException ex) {
      // Exception lọt ra khỏi @Scheduled làm nhịp đó chết và log đầy stack trace (bài học SL-2.3).
      LOG.warn("checkout sweep: nhịp quét lỗi — bỏ qua nhịp này: {}", ex.getMessage());
    }
  }

  /**
   * Một nhịp quét (tách khỏi {@link #sweep()} để test gọi trực tiếp, không phải chờ scheduler).
   *
   * @return số đơn đã chuyển sang `EXPIRED` trong nhịp này
   */
  public int expireDue() {
    Instant cutoff = Instant.now(clock).minus(properties.paymentWindow());
    List<CheckoutSagaRecord> due = sagas.findExpirable(cutoff, properties.sweepBatchSize());
    int expired = 0;
    for (CheckoutSagaRecord saga : due) {
      try {
        if (finish.expire(saga.orderId())) {
          expired++;
        }
      } catch (RuntimeException ex) {
        LOG.warn("checkout sweep: không expire được order={}: {}", saga.orderId(), ex.getMessage());
      }
    }
    if (expired > 0) {
      LOG.info(
          "checkout sweep: {} đơn quá hạn thanh toán → EXPIRED (hàng/voucher trả theo TTL)",
          expired);
    }
    return expired;
  }
}
