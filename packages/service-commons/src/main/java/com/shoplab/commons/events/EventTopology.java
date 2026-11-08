package com.shoplab.commons.events;

import java.util.List;

/**
 * Tên exchange / queue / routing key của ShopLab — <b>nguồn sự thật duy nhất</b> trong code.
 *
 * <p>Mọi giá trị ở đây lấy từ <a
 * href="../../../../../../../docs/adr/0007-event-contract.md">ADR-0007</a> (§ Decision, D2). Đổi
 * tên trong file này là đổi hợp đồng với broker đang chạy: queue cũ còn message sẽ thành "mồ côi"
 * (không consumer nào bind) — phải có kế hoạch migrate, không phải sửa rồi deploy.
 *
 * <p><b>Vì sao là constant chứ không phải string rải rác</b>: một routing key sai một ký tự thì
 * RabbitMQ <i>không</i> báo lỗi — message rơi vào hư không và chỉ phát hiện khi thiếu email/index.
 * Dùng chung constant thì lỗi đó thành lỗi compile.
 */
public final class EventTopology {

  /** Exchange chính (topic) — producer publish với {@code routingKey = eventType}. */
  public static final String EXCHANGE = "shoplab.events";

  /** Exchange chết (direct) — đích của mọi DLQ. */
  public static final String DEAD_LETTER_EXCHANGE = "shoplab.dlx";

  /** Queue của từng consumer (mỗi consumer **một** queue — ADR-0007 D2). */
  public static final String QUEUE_NOTIFICATION = "q.notification";

  /** Consumer `order-service` — nhận {@link #ROUTING_PAYMENT_SUCCEEDED}. */
  public static final String QUEUE_ORDER = "q.order";

  /** Consumer `search-service` — nhận event của catalog. */
  public static final String QUEUE_SEARCH = "q.search";

  /** Hậu tố DLQ: {@code <queue>.dlq}. */
  public static final String DLQ_SUFFIX = ".dlq";

  /** `order-service` phát khi order → `CONFIRMED` (SL-3.3). */
  public static final String ROUTING_ORDER_CONFIRMED = "order.confirmed.v1";

  /** `order-service` phát khi order → `CANCELLED`. */
  public static final String ROUTING_ORDER_CANCELLED = "order.cancelled.v1";

  /** `payment-service` phát khi webhook chuyển intent sang `SUCCEEDED` (SL-3.5). */
  public static final String ROUTING_PAYMENT_SUCCEEDED = "payment.succeeded.v1";

  /** `catalog-service` phát khi product tạo/sửa (SL-3.6). */
  public static final String ROUTING_PRODUCT_UPDATED = "product.updated.v1";

  /** `catalog-service` phát khi product bị soft delete (SL-3.6). */
  public static final String ROUTING_PRODUCT_ARCHIVED = "product.archived.v1";

  /** Queue của mọi consumer — dùng để khai topology và để tool vận hành duyệt qua. */
  public static final List<String> QUEUES = List.of(QUEUE_NOTIFICATION, QUEUE_ORDER, QUEUE_SEARCH);

  /** Routing key mà một queue nhận — chiều ngược lại của bảng ở ADR-0007. */
  public static List<String> routingKeysFor(String queue) {
    return switch (queue) {
      case QUEUE_NOTIFICATION -> List.of(ROUTING_ORDER_CONFIRMED, ROUTING_ORDER_CANCELLED);
      case QUEUE_ORDER -> List.of(ROUTING_PAYMENT_SUCCEEDED);
      case QUEUE_SEARCH -> List.of(ROUTING_PRODUCT_UPDATED, ROUTING_PRODUCT_ARCHIVED);
      default -> throw new IllegalArgumentException("queue không thuộc topology ShopLab: " + queue);
    };
  }

  /** Tên DLQ của một queue. */
  public static String dlqOf(String queue) {
    return queue + DLQ_SUFFIX;
  }

  private EventTopology() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
