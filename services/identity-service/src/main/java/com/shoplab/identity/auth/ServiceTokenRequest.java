package com.shoplab.identity.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Yêu cầu cấp service token (SL-3.11) — tương đương {@code client_credentials} của OAuth2, rút gọn.
 *
 * @param clientId định danh service gọi (vd {@code order-service})
 * @param clientSecret secret của service đó — so sánh hằng thời gian ở service
 * @param audience service **đích** mà token sẽ dùng được (vd {@code inventory-service})
 */
public record ServiceTokenRequest(
    @NotBlank(message = "clientId không được rỗng") String clientId,
    @NotBlank(message = "clientSecret không được rỗng") String clientSecret,
    @NotBlank(message = "audience không được rỗng") String audience) {}
