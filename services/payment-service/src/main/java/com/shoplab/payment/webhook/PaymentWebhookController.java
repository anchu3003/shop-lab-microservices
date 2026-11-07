package com.shoplab.payment.webhook;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/**
 * Webhook provider — endpoint <b>public</b> duy nhất của hệ thống (không JWT).
 *
 * <p>Nhận <b>raw body</b> ({@code String}) chứ không bind thẳng vào record, vì chữ ký HMAC phải
 * tính trên đúng byte provider gửi. Verify <b>trước</b> khi parse: request không có chữ ký hợp lệ
 * không được chạm vào code nghiệp vụ.
 *
 * <p>Luôn trả 200 khi đã xử lý (kể cả trùng) — provider chỉ cần biết "đã nhận"; trả 4xx/5xx sẽ làm
 * nó retry vô ích (hoặc retry đúng khi 5xx, nên chỉ dùng cho lỗi thật).
 */
@Tag(name = "Payment webhook")
@RestController
@RequestMapping("/v1/webhooks")
public class PaymentWebhookController {

  private final WebhookSignatureVerifier verifier;
  private final PaymentWebhookService service;
  private final ObjectMapper objectMapper;

  public PaymentWebhookController(
      WebhookSignatureVerifier verifier, PaymentWebhookService service, ObjectMapper objectMapper) {
    this.verifier = verifier;
    this.service = service;
    this.objectMapper = objectMapper;
  }

  @Operation(
      summary = "Nhận webhook provider (idempotent theo event id)",
      description =
          "Xác thực bằng HMAC-SHA256 trên raw body + timestamp; gửi lại cùng eventId ⇒ 200 với"
              + " duplicate=true và không đổi dữ liệu.")
  @PostMapping("/payment")
  public ResponseEntity<PaymentWebhookResponse> receive(
      @RequestHeader(value = WebhookSignatureVerifier.SIGNATURE_HEADER, required = false)
          String signature,
      @RequestHeader(value = WebhookSignatureVerifier.TIMESTAMP_HEADER, required = false)
          String timestamp,
      @RequestBody String rawBody) {
    verifier.verify(signature, timestamp, rawBody);
    PaymentWebhookRequest request = parse(rawBody);
    return ResponseEntity.ok(service.apply(rawBody, request));
  }

  private PaymentWebhookRequest parse(String rawBody) {
    try {
      return objectMapper.readValue(rawBody, PaymentWebhookRequest.class);
    } catch (RuntimeException ex) {
      throw new com.shoplab.commons.error.AppException(
          com.shoplab.commons.error.ErrorCode.VALIDATION_ERROR,
          "Webhook body không phải JSON hợp lệ");
    }
  }
}
