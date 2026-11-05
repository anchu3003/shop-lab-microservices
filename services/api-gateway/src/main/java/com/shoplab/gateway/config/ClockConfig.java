package com.shoplab.gateway.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link Clock} tiêm được để test kiểm soát thời gian (token hết hạn, TTL cache JWKS) — thay vì gọi
 * {@code Instant.now()} rải rác trong code, thứ không thể test tất định.
 */
@Configuration
public class ClockConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
