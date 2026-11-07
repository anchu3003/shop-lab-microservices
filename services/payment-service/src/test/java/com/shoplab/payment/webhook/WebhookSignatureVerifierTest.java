package com.shoplab.payment.webhook;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoplab.payment.config.PaymentProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test verify chữ ký webhook — 4 nhánh: hợp lệ, body bị sửa, timestamp cũ (replay), thiếu header.
 *
 * <p>Đây là test quan trọng nhất của service: webhook là endpoint public duy nhất, chữ ký là thứ
 * duy nhất phân biệt provider với kẻ lạ.
 */
class WebhookSignatureVerifierTest {

  private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
  private static final String BODY = "{\"eventId\":\"evt_1\",\"status\":\"succeeded\"}";

  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final WebhookSignatureVerifier verifier =
      new WebhookSignatureVerifier(
          new PaymentProperties(
              Duration.ofMinutes(15),
              "mock",
              "http://localhost:8080/mock-provider/pay",
              "top-secret",
              Duration.ofMinutes(5),
              true,
              Duration.ofMillis(1),
              Duration.ofHours(24),
              Duration.ofHours(1),
              200),
          clock);

  private String nowTimestamp() {
    return Long.toString(NOW.getEpochSecond());
  }

  @Test
  @DisplayName("chữ ký đúng → qua")
  void validSignaturePasses() {
    String ts = nowTimestamp();
    assertThatCode(() -> verifier.verify(verifier.sign(ts, BODY), ts, BODY))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("body bị sửa sau khi ký → 401 (HMAC trên raw body)")
  void tamperedBodyFails() {
    String ts = nowTimestamp();
    String signature = verifier.sign(ts, BODY);

    assertThatThrownBy(
            () -> verifier.verify(signature, ts, BODY.replace("succeeded", "succeeded ")))
        .isInstanceOf(WebhookExceptions.InvalidSignature.class);
  }

  @Test
  @DisplayName("timestamp lệch quá cửa sổ → 401 (chặn replay)")
  void staleTimestampFails() {
    String oldTs = Long.toString(NOW.minus(Duration.ofMinutes(30)).getEpochSecond());
    String signature = verifier.sign(oldTs, BODY);

    assertThatThrownBy(() -> verifier.verify(signature, oldTs, BODY))
        .isInstanceOf(WebhookExceptions.StaleTimestamp.class);
  }

  @Test
  @DisplayName("thiếu chữ ký → 401")
  void missingSignatureFails() {
    assertThatThrownBy(() -> verifier.verify(null, nowTimestamp(), BODY))
        .isInstanceOf(WebhookExceptions.InvalidSignature.class);
  }

  @Test
  @DisplayName("timestamp không phải số → 401 (không nổ 500)")
  void nonNumericTimestampFails() {
    assertThatThrownBy(() -> verifier.verify("sha256=abc", "hom-nay", BODY))
        .isInstanceOf(WebhookExceptions.InvalidSignature.class);
  }
}
