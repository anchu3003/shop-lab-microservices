package com.shoplab.commons.serviceauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.commons.serviceauth.ServiceTokenClient.CachedToken;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test cache service token (SL-3.11).
 *
 * <p>Ba thứ dễ sai và đều gây sự cố thật: xin token mỗi request (identity thành điểm chết), dùng
 * token đã gần hết hạn (401 giữa đường bay), và cache cả lỗi (hỏng cho tới khi restart).
 */
class ServiceTokenClientTest {

  private static final Instant T0 = Instant.parse("2026-11-08T14:00:00Z");
  private static final String AUDIENCE = "inventory-service";

  private final AtomicInteger fetches = new AtomicInteger();
  private final AtomicReference<Instant> now = new AtomicReference<>(T0);

  private ServiceTokenClient client(Duration ttl, Duration skew) {
    ServiceTokenClient.TokenSource source =
        audience -> {
          int n = fetches.incrementAndGet();
          assertThat(audience).isEqualTo(AUDIENCE);
          return new CachedToken("token-" + n, now.get().plus(ttl));
        };
    return new ServiceTokenClient(source, skew, now::get);
  }

  @Test
  @DisplayName("gọi 2 lần liên tiếp chỉ xin token 1 lần (cache)")
  void shouldCacheToken() {
    ServiceTokenClient client = client(Duration.ofMinutes(5), Duration.ofSeconds(30));

    assertThat(client.tokenFor(AUDIENCE)).isEqualTo("token-1");
    assertThat(client.tokenFor(AUDIENCE)).isEqualTo("token-1");
    assertThat(fetches).hasValue(1);
  }

  @Test
  @DisplayName("token còn hạn nhưng đã vào cửa sổ skew ⇒ xin token mới (tránh 401 giữa request)")
  void shouldRefreshBeforeExpiry() {
    ServiceTokenClient client = client(Duration.ofMinutes(5), Duration.ofSeconds(30));

    assertThat(client.tokenFor(AUDIENCE)).isEqualTo("token-1");
    now.set(T0.plus(Duration.ofMinutes(5)).minus(Duration.ofSeconds(10))); // còn 10s < skew 30s
    assertThat(client.tokenFor(AUDIENCE)).isEqualTo("token-2");
    assertThat(fetches).hasValue(2);
  }

  @Test
  @DisplayName("token hết hạn hẳn ⇒ xin mới")
  void shouldRefreshWhenExpired() {
    ServiceTokenClient client = client(Duration.ofMinutes(1), Duration.ofSeconds(30));

    client.tokenFor(AUDIENCE);
    now.set(T0.plus(Duration.ofMinutes(2)));

    assertThat(client.tokenFor(AUDIENCE)).isEqualTo("token-2");
  }

  @Test
  @DisplayName("lỗi xin token KHÔNG bị cache — lần sau thử lại, không giữ lỗi tới khi restart")
  void shouldNotCacheFailure() {
    AtomicInteger calls = new AtomicInteger();
    ServiceTokenClient failing =
        new ServiceTokenClient(
            audience -> {
              if (calls.incrementAndGet() == 1) {
                throw new ServiceTokenException("identity trả HTTP 503");
              }
              return new CachedToken("token-ok", Instant.now().plusSeconds(300));
            },
            Duration.ofSeconds(30));

    assertThatThrownBy(() -> failing.tokenFor(AUDIENCE))
        .isInstanceOf(ServiceTokenException.class)
        .hasMessageContaining("503");

    assertThat(failing.tokenFor(AUDIENCE)).isEqualTo("token-ok");
  }

  @Test
  @DisplayName("invalidate ⇒ lần sau xin lại (dùng khi service đích trả 401)")
  void shouldRefetchAfterInvalidate() {
    ServiceTokenClient client = client(Duration.ofMinutes(5), Duration.ofSeconds(30));

    client.tokenFor(AUDIENCE);
    client.invalidate(AUDIENCE);

    assertThat(client.tokenFor(AUDIENCE)).isEqualTo("token-2");
  }
}
