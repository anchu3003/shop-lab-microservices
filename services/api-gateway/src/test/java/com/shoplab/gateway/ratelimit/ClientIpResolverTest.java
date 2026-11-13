package com.shoplab.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Lấy IP làm khoá bucket — chỗ dễ tự bắn vào chân nhất của rate limit.
 *
 * <p>Nếu tin {@code X-Forwarded-For} vô điều kiện thì kẻ tấn công đổi header mỗi request là có vô
 * hạn bucket ⇒ cả ticket này thành vô nghĩa. Vì vậy test phải chứng minh đúng chiều ngược lại:
 * <b>không</b> tin header đó khi peer không phải proxy của mình.
 */
class ClientIpResolverTest {

  private static ClientIpResolver resolver(List<String> trustedProxies) {
    RateLimitProperties properties =
        new RateLimitProperties(
            true, true, "rl", null, null, List.of("/health"), trustedProxies, List.of());
    return new ClientIpResolver(properties);
  }

  private static MockHttpServletRequest request(String peer, String forwardedFor) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/products");
    request.setRemoteAddr(peer);
    if (forwardedFor != null) {
      request.addHeader("X-Forwarded-For", forwardedFor);
    }
    return request;
  }

  @Test
  @DisplayName("không có proxy tin cậy ⇒ dùng IP peer TCP, BỎ QUA X-Forwarded-For của client")
  void shouldIgnoreForwardedForFromUntrustedPeer() {
    ClientIpResolver untrusting = resolver(List.of());

    String ip = untrusting.resolve(request("10.0.0.9", "1.2.3.4, 5.6.7.8"));

    assertThat(ip).isEqualTo("10.0.0.9");
  }

  @Test
  @DisplayName(
      "peer là proxy tin cậy ⇒ lấy hop KHÔNG tin cậy phải nhất (IP thật, không phải đầu danh sách)")
  void shouldTakeRightmostUntrustedHop() {
    ClientIpResolver trusting = resolver(List.of("172.18.0.1"));

    String ip = trusting.resolve(request("172.18.0.1", "9.9.9.9, 1.2.3.4, 172.18.0.1"));

    // 9.9.9.9 là IP client tự chèn vào đầu danh sách — tin nó là để client tự chọn bucket.
    assertThat(ip).isEqualTo("1.2.3.4");
  }

  @Test
  @DisplayName("proxy tin cậy nhưng không có header ⇒ vẫn dùng IP peer (không rơi vào 'unknown')")
  void shouldFallBackToPeerWithoutHeader() {
    ClientIpResolver trusting = resolver(List.of("172.18.0.1"));

    assertThat(trusting.resolve(request("172.18.0.1", null))).isEqualTo("172.18.0.1");
    assertThat(trusting.resolve(request("172.18.0.1", "   "))).isEqualTo("172.18.0.1");
  }

  @Test
  @DisplayName("toàn bộ chuỗi hop đều là proxy tin cậy ⇒ dùng peer (không trả chuỗi rỗng)")
  void shouldFallBackToPeerWhenAllHopsTrusted() {
    ClientIpResolver trusting = resolver(List.of("172.18.0.1", "172.18.0.2"));

    assertThat(trusting.resolve(request("172.18.0.1", "172.18.0.2, 172.18.0.1")))
        .isEqualTo("172.18.0.1");
  }

  @Test
  @DisplayName("không có remote address ⇒ 'unknown' (một xô chung, không phải NPE)")
  void shouldUseUnknownWhenPeerMissing() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/products");
    request.setRemoteAddr(null);

    assertThat(resolver(List.of()).resolve(request)).isEqualTo("unknown");
  }
}
