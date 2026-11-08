package com.shoplab.order.saga;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy cập {@code checkout_sagas} + {@code checkout_saga_steps}.
 *
 * <p><b>Vì sao update từng bước (nhiều câu UPDATE nhỏ) thay vì ghi một lần khi xong</b>: mỗi câu là
 * một "điểm lưu". Process chết sau bước reserve ⇒ row đã có `reservation_id` ⇒ lần chạy sau (hoặc
 * sweeper `SL-2.6b`) biết phải trả chỗ. Ghi một lần thì crash để lại state rỗng và hàng bị treo tới
 * khi hết TTL.
 */
public interface CheckoutSagaRepository {

  /** Mở saga (`STARTED`). Phải chạy **trước** call ra ngoài đầu tiên. */
  void start(CheckoutSagaRecord saga);

  void markLock(UUID orderId, UUID lockId, Instant now);

  void markReservation(UUID orderId, UUID reservationId, Instant now);

  void markCompletedStart(UUID orderId, UUID paymentIntentId, String paymentUrl, Instant now);

  /**
   * Cập nhật "đơn này có cần thanh toán không" — biết được sau bước lock voucher (đơn 0 đồng khi
   * voucher giảm 100%). Tách khỏi lúc insert vì lúc đó chưa tính xong tiền.
   */
  void markPaymentRequired(UUID orderId, boolean paymentRequired, Instant now);

  void markFailed(UUID orderId, CheckoutSagaState state, String failureCode, Instant now);

  /**
   * Chuyển saga sang trạng thái kết thúc (`CONFIRMED` / `CANCELLED` / `EXPIRED`).
   *
   * <p>Không dùng CAS ở đây vì chốt chặn thật nằm ở {@code orders.markStatus} (chỉ 1 lần chuyển, 1
   * dòng history); saga chỉ phản ánh kết quả đó.
   */
  void markState(UUID orderId, CheckoutSagaState state, String failureCode, Instant now);

  /**
   * Saga còn `COMPLETED_START` + order còn `PENDING_PAYMENT` + tạo trước {@code createdBefore}
   * (`SL-2.6b` sweeper).
   *
   * <p>Join sang {@code orders} được vì **cùng một database** của service này (không phải join
   * xuyên service — điều bị cấm ở `AGENTS.md` §6).
   */
  List<CheckoutSagaRecord> findExpirable(Instant createdBefore, int limit);

  Optional<CheckoutSagaRecord> findById(UUID orderId);

  void addStep(SagaStepRecord step);

  /** Các bước đã chạy, theo thứ tự thời gian — dùng cho test và khi điều tra sự cố. */
  List<SagaStepRecord> findSteps(UUID orderId);
}
