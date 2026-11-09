package com.shoplab.search.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình verify JWT của notification-service.
 *
 * <p>Khác identity-service: catalog là <b>resource server</b> — nó KHÔNG giữ private key. Nó tải
 * public key qua <b>JWKS</b> của identity, nên xoay khoá ở identity không phải build lại catalog.
 *
 * @param jwksUri địa chỉ JWKS nội bộ (không đi qua gateway — tránh phụ thuộc vòng)
 * @param issuer giá trị {@code iss} bắt buộc trong token; phải khớp identity, nếu không mọi token
 *     đều 401
 */
@Validated
@ConfigurationProperties(prefix = "shoplab.jwt")
public record JwtProperties(
    @DefaultValue("http://localhost:8081/.well-known/jwks.json") @NotBlank String jwksUri,
    @DefaultValue("https://identity.shoplab.local") @NotBlank String issuer) {}
