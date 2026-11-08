package com.shoplab.commons.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Buộc khai topology <b>ngay lúc khởi động</b> (SL-3.1).
 *
 * <p><b>Vì sao cần class này</b> (bẫy thật gặp ở SL-3.1): {@code RabbitAdmin} của Spring AMQP khai
 * {@code Declarables} một cách <i>lazy</i> — nó nghe {@code ConnectionListener} và chỉ khai khi có
 * connection đầu tiên. Service <b>chưa</b> publish/consume (đúng trạng thái của SL-3.1) thì không
 * ai mở connection ⇒ exchange/queue **không tồn tại**, dù code "đã khai". Triệu chứng: log sạch,
 * app healthy, mà {@code tools/rabbit-topology.sh} rỗng.
 *
 * <p>Gọi {@link AmqpAdmin#initialize()} ở {@link SmartInitializingSingleton} (sau khi mọi bean
 * xong, trước khi app nhận traffic) để chắc chắn topology có mặt.
 *
 * <p><b>Fail mềm có chủ ý</b>: broker chết lúc service start **không** được làm service chết —
 * async không nằm trên đường critical của checkout (ADR-0007). Lỗi được log {@code WARN} và lần
 * khai sau (khi connection được mở) sẽ tự sửa.
 */
public class EventTopologyDeclarer implements SmartInitializingSingleton {

  private static final Logger LOG = LoggerFactory.getLogger(EventTopologyDeclarer.class);

  private final AmqpAdmin amqpAdmin;

  public EventTopologyDeclarer(AmqpAdmin amqpAdmin) {
    this.amqpAdmin = amqpAdmin;
  }

  @Override
  public void afterSingletonsInstantiated() {
    try {
      amqpAdmin.initialize();
      LOG.info(
          "khai topology event xong ({} · {} queue + DLQ)",
          EventTopology.EXCHANGE,
          EventTopology.QUEUES.size());
    } catch (RuntimeException ex) {
      // Broker chưa sẵn sàng: không chặn startup. Topology sẽ được khai khi connection đầu tiên mở.
      LOG.warn("không khai được topology event lúc khởi động: {}", ex.getMessage());
    }
  }
}
