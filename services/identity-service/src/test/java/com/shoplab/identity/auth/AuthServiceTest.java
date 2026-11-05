package com.shoplab.identity.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoplab.commons.error.ErrorCode;
import com.shoplab.identity.auth.jwt.IssuedToken;
import com.shoplab.identity.auth.jwt.JwtProperties;
import com.shoplab.identity.auth.jwt.JwtService;
import com.shoplab.identity.testsupport.JwtTestKeys;
import com.shoplab.identity.user.Role;
import com.shoplab.identity.user.User;
import com.shoplab.identity.user.UserRepository;
import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Unit test cho {@link AuthService} — repository được mock, không cần DB. */
class AuthServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

  @TempDir Path tempDir;

  private UserRepository users;
  private PasswordEncoder passwordEncoder;
  private AuthService authService;
  private JwtTestKeys keys;

  @BeforeEach
  void setUp() {
    users = mock(UserRepository.class);
    passwordEncoder = new BCryptPasswordEncoder();
    keys = JwtTestKeys.generateIn(tempDir);
    JwtService jwtService =
        new JwtService(
            new JwtProperties(
                "https://identity.shoplab.local",
                Duration.ofMinutes(15),
                keys.privateKeyPath().toString(),
                keys.publicKeyPath().toString()));
    authService =
        new AuthService(users, passwordEncoder, jwtService, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private void existingUser(String email, String rawPassword) {
    User user = User.register(email, passwordEncoder.encode(rawPassword), "An", Role.BUYER, NOW);
    when(users.findByEmail(email)).thenReturn(Optional.of(user));
  }

  @Test
  @DisplayName("đăng ký: chuẩn hoá email, hash mật khẩu, luôn tạo role BUYER")
  void registerShouldNormalizeHashAndAssignBuyer() {
    when(users.existsByEmail(any())).thenReturn(false);
    when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    authService.register("  An@Example.COM ", "matkhau123", "  An  ");

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users).save(saved.capture());
    User user = saved.getValue();

    assertThat(user.email()).isEqualTo("an@example.com");
    assertThat(user.displayName()).isEqualTo("An");
    assertThat(user.role()).isEqualTo(Role.BUYER);
    assertThat(user.passwordHash()).isNotEqualTo("matkhau123").startsWith("$2");
    assertThat(passwordEncoder.matches("matkhau123", user.passwordHash())).isTrue();
    assertThat(user.createdAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("đăng ký email đã tồn tại → 409 CONFLICT, KHÔNG ghi DB")
  void registerShouldRejectDuplicateEmail() {
    when(users.existsByEmail("an@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register("AN@example.com", "matkhau123", "An"))
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessageContaining("an@example.com")
        .extracting(ex -> ((EmailAlreadyExistsException) ex).errorCode())
        .isEqualTo(ErrorCode.CONFLICT);

    verify(users, never()).save(any(User.class));
  }

  @Test
  @DisplayName("đăng nhập đúng → token verify được bằng public key, sub = id user vừa đăng ký")
  void loginShouldIssueTokenForUser() {
    when(users.existsByEmail(any())).thenReturn(false);
    when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    User registered = authService.register("an@example.com", "matkhau123", "An");
    when(users.findByEmail("an@example.com")).thenReturn(Optional.of(registered));

    IssuedToken token = authService.login("AN@example.com", "matkhau123");

    Jwt jwt = decode(token.value());
    assertThat(jwt.getSubject()).isEqualTo(registered.id().toString());
    assertThat(jwt.getClaimAsString(JwtService.ROLE_CLAIM)).isEqualTo("BUYER");
    assertThat(token.expiresInSeconds()).isEqualTo(Duration.ofMinutes(15).toSeconds());
  }

  /**
   * Token được phát với {@code Clock} cố định ({@link #NOW}), nên validator khi decode cũng phải
   * dùng đúng clock đó — nếu để mặc định, nó so với thời gian thật của máy và kết luận token hết
   * hạn (test "đỏ vì đồng hồ", không phải vì logic sai).
   */
  private Jwt decode(String token) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.keyPair().getPublic()).build();
    JwtTimestampValidator validator = new JwtTimestampValidator();
    validator.setClock(Clock.fixed(NOW, ZoneOffset.UTC));
    decoder.setJwtValidator(validator);
    return decoder.decode(token);
  }

  @Test
  @DisplayName("sai mật khẩu và email không tồn tại → CÙNG một lỗi 401 (không lộ email đã đăng ký)")
  void loginShouldNotRevealWhetherEmailExists() {
    existingUser("an@example.com", "matkhau123");
    when(users.findByEmail("khong-ton-tai@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login("an@example.com", "sai-mat-khau"))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Email hoặc mật khẩu không đúng");

    assertThatThrownBy(() -> authService.login("khong-ton-tai@example.com", "matkhau123"))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Email hoặc mật khẩu không đúng");
  }

  @Test
  @DisplayName("normalizeEmail: trim + lowercase, null thành chuỗi rỗng (không NPE)")
  void shouldNormalizeEmail() {
    assertThat(AuthService.normalizeEmail("  A@B.COM  ")).isEqualTo("a@b.com");
    assertThat(AuthService.normalizeEmail(null)).isEmpty();
  }

  /** User id sinh ngẫu nhiên trong factory; test cần id cố định để assert claim {@code sub}. */
}
