package com.shoplab.commons.events;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.AmqpAdmin;

/**
 * {@link EventTopologyDeclarer} — chốt hai hành vi dễ mất khi refactor (SL-3.1).
 *
 * <p>Bài học gốc: {@code RabbitAdmin} khai topology **lazy** (chỉ khi có connection đầu tiên).
 * Service chưa publish/consume ⇒ không có connection ⇒ topology không tồn tại. Test này giữ cho
 * việc "khai ngay lúc khởi động" không bị xoá âm thầm.
 */
class EventTopologyDeclarerTest {

  @Test
  @DisplayName("gọi initialize() đúng 1 lần ngay sau khi mọi bean sẵn sàng")
  void shouldInitializeTopologyOnce() {
    AmqpAdmin admin = mock(AmqpAdmin.class);

    new EventTopologyDeclarer(admin).afterSingletonsInstantiated();

    verify(admin, times(1)).initialize();
  }

  @Test
  @DisplayName("broker chết lúc start KHÔNG làm app chết (async không nằm trên đường critical)")
  void shouldSwallowBrokerFailure() {
    AmqpAdmin admin = mock(AmqpAdmin.class);
    // initialize() trả void ⇒ phải dùng doThrow(...).when(...).initialize(), không dùng when(...).
    doThrow(new AmqpConnectException(new RuntimeException("refused"))).when(admin).initialize();

    // Không ném ra ngoài: nếu ném, Spring sẽ fail khởi động cả service vì broker — sai với
    // ADR-0007.
    EventTopologyDeclarer declarer = new EventTopologyDeclarer(admin);
    assertThatCode(declarer::afterSingletonsInstantiated).doesNotThrowAnyException();
  }
}
