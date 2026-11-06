package com.shoplab.identity.auth;

import com.shoplab.identity.auth.jwt.IssuedToken;
import com.shoplab.identity.auth.jwt.JwtService;
import com.shoplab.identity.auth.refresh.RefreshTokenService;
import com.shoplab.identity.user.Role;
import com.shoplab.identity.user.User;
import com.shoplab.identity.user.UserRepository;
import java.time.Clock;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Luồng xác thực: đăng ký và đăng nhập.
 *
 * <p>Quy ước: entity và JWT <b>không</b> rời khỏi tầng này ở dạng thô ra ngoài — controller chỉ
 * nhận DTO đã map (xem {@link UserResponse}).
 */
@Service
public class AuthService {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokens;
  private final Clock clock;

  public AuthService(
      UserRepository users,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      RefreshTokenService refreshTokens,
      Clock clock) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokens = refreshTokens;
    this.clock = clock;
  }

  /**
   * Đăng ký user mới. Luôn tạo {@link Role#BUYER} — không cho client tự chọn role (SL-1.4 mới có
   * seller/admin, tạo qua seed).
   */
  @Transactional
  public User register(String rawEmail, String rawPassword, String displayName) {
    String email = normalizeEmail(rawEmail);
    if (users.existsByEmail(email)) {
      // Kiểm tra trước để trả 409 rõ ràng. Unique index trong DB vẫn là chốt chặn cuối khi có
      // hai request đăng ký cùng email chạy song song.
      throw new EmailAlreadyExistsException(email);
    }
    User user =
        User.register(
            email,
            passwordEncoder.encode(rawPassword),
            displayName.trim(),
            Role.BUYER,
            clock.instant());
    return users.save(user);
  }

  /**
   * Đăng nhập, trả cặp access + refresh token (SL-1.4).
   *
   * <p>Hai trường hợp "email không tồn tại" và "sai mật khẩu" trả <b>cùng một</b> lỗi 401 (xem
   * {@link InvalidCredentialsException}) để không lộ email nào đã đăng ký.
   */
  @Transactional
  public AuthSession login(String rawEmail, String rawPassword) {
    String email = normalizeEmail(rawEmail);
    User user = users.findByEmail(email).orElse(null);
    if (user == null || !passwordEncoder.matches(rawPassword, user.passwordHash())) {
      throw new InvalidCredentialsException();
    }
    return issueSession(user);
  }

  /**
   * Đổi refresh token lấy cặp token <b>mới</b> (rotate). Token cũ bị vô hiệu hoá ngay.
   *
   * <p><b>{@code noRollbackFor}</b>: khi phát hiện reuse, tầng dưới (<code>RefreshTokenService
   * </code>) vừa revoke cả chuỗi vừa throw. Nếu transaction ở <b>đây</b> rollback thì các lệnh
   * revoke bị huỷ theo (exception đi qua method này) ⇒ kẻ tấn công vẫn dùng được token. Đã gặp thật
   * ở IT SL-1.4 — transaction ngoài cùng mới là cái quyết định commit/rollback.
   *
   * @throws InvalidRefreshTokenException token sai/hết hạn/đã dùng (kèm revoke cả chuỗi nếu là
   *     reuse)
   */
  @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
  public AuthSession refresh(String rawRefreshToken) {
    RefreshTokenService.RotatedRefresh rotated = refreshTokens.rotate(rawRefreshToken);
    User user = users.findById(rotated.userId()).orElseThrow(InvalidRefreshTokenException::new);
    IssuedToken accessToken = jwtService.issueAccessToken(user.id(), user.role(), clock.instant());
    return new AuthSession(accessToken, rotated.rawToken());
  }

  /** Logout: thu hồi refresh token hiện tại. Idempotent — gọi lại không gây lỗi. */
  @Transactional
  public void logout(String rawRefreshToken) {
    refreshTokens.revoke(rawRefreshToken);
  }

  /** Cấp access token (JWT ngắn hạn) + refresh token (opaque, lưu hash) cho user. */
  private AuthSession issueSession(User user) {
    IssuedToken accessToken = jwtService.issueAccessToken(user.id(), user.role(), clock.instant());
    String rawRefreshToken = refreshTokens.issue(user.id()).rawToken();
    return new AuthSession(accessToken, rawRefreshToken);
  }

  /**
   * Chuẩn hoá email thành định danh đăng nhập.
   *
   * <p>Bắt buộc phải làm: nếu không, {@code A@x.com} và {@code a@x.com} là hai tài khoản khác nhau
   * trên cùng một người — vừa khó dùng vừa mở đường cho việc tạo tài khoản trùng.
   */
  static String normalizeEmail(String rawEmail) {
    return rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
  }
}
