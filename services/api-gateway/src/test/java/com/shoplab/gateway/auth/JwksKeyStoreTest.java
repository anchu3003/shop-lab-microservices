package com.shoplab.gateway.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.shoplab.gateway.config.GatewayProperties;
import com.shoplab.gateway.testsupport.JwtFixture;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Unit test cho {@link JwksKeyStore}: cache, TTL, refetch khi gặp {@code kid} lạ, và cách phân loại
 * lỗi (401 vs 503) — đây là phần quyết định gateway có "sập" khi identity chết hay không.
 */
class JwksKeyStoreTest {

  private static final String JWKS_URI = "http://identity.test/.well-known/jwks.json";

  private JwtFixture fixture;
  private MockRestServiceServer server;
  private RestClient restClient;

  @BeforeEach
  void setUp() {
    fixture = new JwtFixture();
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    restClient = builder.build();
  }

  private JwksKeyStore keyStore(Duration cacheTtl, Duration minRefreshInterval) {
    GatewayProperties properties =
        new GatewayProperties(
            List.of(new GatewayProperties.Route("/v1/auth", "http://identity.test:8081")),
            List.of(),
            new GatewayProperties.Jwt(
                JWKS_URI, JwtFixture.ISSUER, cacheTtl, minRefreshInterval, Duration.ofSeconds(30)),
            null);
    return new JwksKeyStore(restClient, properties, Clock.systemUTC());
  }

  /** minRefreshInterval = 0 → cho phép nạp lại ngay (mô phỏng "khoá vừa rotate"). */
  private JwksKeyStore keyStore(Duration cacheTtl) {
    return keyStore(cacheTtl, Duration.ZERO);
  }

  private void expectJwks() {
    server
        .expect(requestTo(JWKS_URI))
        .andRespond(withSuccess(fixture.jwksJson(), MediaType.APPLICATION_JSON));
  }

  @Test
  @DisplayName("lấy khoá theo kid từ JWKS")
  void shouldResolveKeyByKid() {
    expectJwks();

    RSAPublicKey key = keyStore(Duration.ofMinutes(5)).keyFor(JwtFixture.KID);

    assertThat(key.getModulus()).isEqualTo(fixture.publicKey().getModulus());
    server.verify();
  }

  @Test
  @DisplayName(
      "cache còn hạn → KHÔNG gọi identity lần thứ hai (1 request token không được thành 2)")
  void shouldCacheUntilTtlExpires() {
    expectJwks();
    JwksKeyStore store = keyStore(Duration.ofMinutes(5));

    store.keyFor(JwtFixture.KID);
    store.keyFor(JwtFixture.KID);
    store.keyFor(JwtFixture.KID);

    // Chỉ 1 expectation được khai báo → verify() qua nghĩa là đã không gọi thêm lần nào.
    server.verify();
    assertThat(store.cachedKeyCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("cache hết hạn (TTL=0) → nạp lại JWKS")
  void shouldReloadWhenTtlExpired() {
    expectJwks();
    expectJwks();
    JwksKeyStore store = keyStore(Duration.ZERO);

    store.keyFor(JwtFixture.KID);
    store.keyFor(JwtFixture.KID);

    server.verify();
  }

  @Test
  @DisplayName("gặp kid lạ → refetch 1 lần (khoá vừa rotate), vẫn không có thì 401")
  void shouldRefetchWhenKidIsUnknown() {
    expectJwks();
    expectJwks();

    assertThatThrownBy(() -> keyStore(Duration.ofMinutes(5)).keyFor("kid-la-hoac"))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("kid");

    server.verify();
  }

  @Test
  @DisplayName("kid lạ liên tục KHÔNG được biến mỗi request thành 1 lần gọi identity (chặn dội)")
  void shouldThrottleForcedRefresh() {
    // Chỉ khai 1 expectation: nếu gateway nạp lại lần thứ hai thì test đỏ ngay.
    expectJwks();
    JwksKeyStore store = keyStore(Duration.ofMinutes(5), Duration.ofMinutes(1));

    assertThatThrownBy(() -> store.keyFor("kid-la-1")).isInstanceOf(InvalidTokenException.class);
    assertThatThrownBy(() -> store.keyFor("kid-la-2")).isInstanceOf(InvalidTokenException.class);
    assertThatThrownBy(() -> store.keyFor("kid-la-3")).isInstanceOf(InvalidTokenException.class);

    server.verify();
  }

  @Test
  @DisplayName("token không có kid → 401 luôn, không cần gọi JWKS")
  void shouldRejectTokenWithoutKid() {
    assertThatThrownBy(() -> keyStore(Duration.ofMinutes(5)).keyFor(" "))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("kid");

    server.verify();
  }

  @Test
  @DisplayName("identity chết / JWKS lỗi → 503 (lỗi hạ tầng, KHÁC 401 của token sai)")
  void shouldReportUnavailableWhenJwksFails() {
    server.expect(requestTo(JWKS_URI)).andRespond(withServerError());

    assertThatThrownBy(() -> keyStore(Duration.ofMinutes(5)).keyFor(JwtFixture.KID))
        .isInstanceOf(JwksUnavailableException.class);
  }

  @Test
  @DisplayName("JWKS hỏng/rỗng → 503 chứ không phải 401")
  void shouldReportUnavailableWhenJwksMalformed() {
    server
        .expect(requestTo(JWKS_URI))
        .andRespond(withSuccess("{khong-phai-json}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> keyStore(Duration.ofMinutes(5)).keyFor(JwtFixture.KID))
        .isInstanceOf(JwksUnavailableException.class);
  }

  @Test
  @DisplayName("JWKS không có khoá RSA nào → 503")
  void shouldReportUnavailableWhenNoRsaKey() {
    server
        .expect(requestTo(JWKS_URI))
        .andRespond(withSuccess("{\"keys\":[]}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> keyStore(Duration.ofMinutes(5)).keyFor(JwtFixture.KID))
        .isInstanceOf(JwksUnavailableException.class);
  }
}
