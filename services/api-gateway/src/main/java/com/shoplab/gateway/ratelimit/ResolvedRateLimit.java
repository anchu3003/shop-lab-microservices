package com.shoplab.gateway.ratelimit;

/**
 * Rule đã resolve cho một request: dùng bucket nào, hạn mức bao nhiêu.
 *
 * @param routeKey phần giữa của key Redis ({@code rl:<routeKey>:u:<sub>}); đã bỏ {@code /} để key
 *     không có ký tự phân cách dễ gây nhầm
 * @param perUser hạn mức theo user (bỏ qua khi request không có danh tính)
 * @param perIp hạn mức theo IP — <b>luôn</b> áp dụng
 */
public record ResolvedRateLimit(
    String routeKey, RateLimitProperties.Limit perUser, RateLimitProperties.Limit perIp) {}
