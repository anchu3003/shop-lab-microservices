package com.shoplab.gateway.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.shoplab.gateway.config.GatewayProperties;
import com.shoplab.gateway.testsupport.JwtFixture;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Unit test cho {@link JwtVerifier} — mọi nhánh từ chối token đều phải được test. */
class JwtVerifierTest {

  private static final String JWKS_URI = "http://identity.test/.well-known/jwks.json";

  private JwtFixture fixture;
  private MockRestServiceServer server;
  private JwtVerifier verifier;

  @BeforeEach
  void setUp() {
    fixture = new JwtFixture();
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient = builder.build();

    GatewayProperties properties =
        new GatewayProperties(
            List.of(new GatewayProperties.Route("/v1/auth", "http://identity.test:8081")),
            List.of(),
            new GatewayProperties.Jwt(
                JWKS_URI,
                JwtFixture.ISSUER,
                Duration.ofMinutes(5),
                Duration.ofSeconds(10),
                Duration.ofSeconds(30)),
            null);

    verifier =
        new JwtVerifier(
            new JwksKeyStore(restClient, properties, Clock.systemUTC()),
            properties,
            Clock.systemUTC());
  }

  private void expectJwks() {
    server
        .expect(requestTo(JWKS_URI))
        .andRespond(withSuccess(fixture.jwksJson(), MediaType.APPLICATION_JSON));
  }

  @Test
  @DisplayName("token hợp lệ → lấy được userId (sub) và role")
  void shouldVerifyValidToken() {
    expectJwks();
    String userId = UUID.randomUUID().toString();

    AuthenticatedUser user = verifier.verify(fixture.validToken(userId, "BUYER"));

    assertThat(user.userId()).isEqualTo(userId);
    assertThat(user.role()).isEqualTo("BUYER");
  }

  @Test
  @DisplayName("token hết hạn → từ chối")
  void shouldRejectExpiredToken() {
    expectJwks();

    assertThatThrownBy(() -> verifier.verify(fixture.expiredToken("u1", "BUYER")))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("hết hạn");
  }

  @Test
  @DisplayName("iss khác → từ chối (token của hệ thống khác không dùng được ở đây)")
  void shouldRejectWrongIssuer() {
    expectJwks();

    assertThatThrownBy(() -> verifier.verify(fixture.wrongIssuerToken("u1", "BUYER")))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("iss");
  }

  @Test
  @DisplayName("chữ ký bằng khoá khác → từ chối")
  void shouldRejectTokenSignedByAnotherKey() {
    expectJwks();

    assertThatThrownBy(() -> verifier.verify(fixture.tokenSignedByAnotherKey("u1", "BUYER")))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("chữ ký");
  }

  @Test
  @DisplayName("alg=HS256 (algorithm confusion) → từ chối TRƯỚC khi tra khoá")
  void shouldRejectHs256Token() {
    // Không khai expectation JWKS nào: token phải bị chặn ngay ở bước kiểm tra thuật toán.
    assertThatThrownBy(() -> verifier.verify(fixture.hs256Token("u1", "ADMIN")))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("thuật toán");

    server.verify();
  }

  @Test
  @DisplayName("token không có kid → từ chối")
  void shouldRejectTokenWithoutKid() {
    assertThatThrownBy(() -> verifier.verify(fixture.tokenWithoutKid("u1", "BUYER")))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("kid");
  }

  @Test
  @DisplayName("token thiếu claim role → từ chối (không để downstream tự đoán quyền)")
  void shouldRejectTokenWithoutRole() {
    expectJwks();

    assertThatThrownBy(() -> verifier.verify(fixture.tokenWithoutRole("u1")))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("role");
  }

  @Test
  @DisplayName("token rác → từ chối, không ném lỗi kỹ thuật ra ngoài")
  void shouldRejectGarbageToken() {
    assertThatThrownBy(() -> verifier.verify("khong-phai-token"))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("không đọc được");
  }

  @Test
  @DisplayName("không lấy được JWKS → 503 (JwksUnavailableException), KHÔNG phải 401")
  void shouldPropagateJwksUnavailable() {
    server.expect(requestTo(JWKS_URI)).andRespond(withServerError());

    assertThatThrownBy(() -> verifier.verify(fixture.validToken("u1", "BUYER")))
        .isInstanceOf(JwksUnavailableException.class);
  }
}
