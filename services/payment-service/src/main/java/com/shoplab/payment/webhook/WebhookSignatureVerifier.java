package com.shoplab.payment.webhook;

import com.shoplab.payment.config.PaymentProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Verify chữ ký webhook: {@code HMAC-SHA256(secret, timestamp + "\n" + rawBody)}.
 *
 * <p>Ba điểm bắt buộc, thiếu cái nào cũng thành lỗ hổng:
 *
 * <ol>
 *   <li><b>Ký trên raw body</b> — không phải JSON đã parse rồi serialize lại (thứ tự key, khoảng
 *       trắng, unicode đều đổi ⇒ hash khác).
 *   <li><b>Timestamp nằm trong chuỗi được ký</b> + kiểm cửa sổ thời gian ⇒ chặn replay.
 *   <li><b>So sánh constant-time</b> ({@link MessageDigest#isEqual}) ⇒ không rò thông tin qua thời
 *       gian phản hồi.
 * </ol>
 */
@Component
public class WebhookSignatureVerifier {

  /** Header chữ ký, dạng {@code sha256=<hex>}. */
  public static final String SIGNATURE_HEADER = "X-Provider-Signature";

  /** Header timestamp (epoch giây) — phải nằm trong chuỗi được ký. */
  public static final String TIMESTAMP_HEADER = "X-Provider-Timestamp";

  private static final String ALGORITHM = "HmacSHA256";

  private final PaymentProperties properties;
  private final Clock clock;

  public WebhookSignatureVerifier(PaymentProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * @throws WebhookExceptions.InvalidSignature khi thiếu header / sai chữ ký
   * @throws WebhookExceptions.StaleTimestamp khi timestamp lệch quá {@code webhook-tolerance}
   */
  public void verify(String signature, String timestamp, String rawBody) {
    if (signature == null || signature.isBlank()) {
      throw new WebhookExceptions.InvalidSignature("thiếu header " + SIGNATURE_HEADER);
    }
    if (timestamp == null || timestamp.isBlank()) {
      throw new WebhookExceptions.InvalidSignature("thiếu header " + TIMESTAMP_HEADER);
    }
    long epochSeconds;
    try {
      epochSeconds = Long.parseLong(timestamp.trim());
    } catch (NumberFormatException ex) {
      throw new WebhookExceptions.InvalidSignature(TIMESTAMP_HEADER + " không phải số nguyên");
    }
    Duration skew = Duration.between(Instant.ofEpochSecond(epochSeconds), Instant.now(clock)).abs();
    if (skew.compareTo(properties.webhookTolerance()) > 0) {
      throw new WebhookExceptions.StaleTimestamp(skew.toSeconds());
    }

    String expected = sign(timestamp.trim(), rawBody);
    boolean matches =
        MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            signature.trim().getBytes(StandardCharsets.UTF_8));
    if (!matches) {
      throw new WebhookExceptions.InvalidSignature("chữ ký không khớp");
    }
  }

  /** Sinh chữ ký — mock provider dùng để "đóng vai" provider thật. */
  public String sign(String timestamp, String rawBody) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(
          new SecretKeySpec(
              properties.webhookSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM));
      byte[] digest = mac.doFinal((timestamp + "\n" + rawBody).getBytes(StandardCharsets.UTF_8));
      return "sha256=" + HexFormat.of().formatHex(digest);
    } catch (java.security.GeneralSecurityException ex) {
      throw new IllegalStateException("JVM không hỗ trợ " + ALGORITHM, ex);
    }
  }
}
