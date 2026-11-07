package com.shoplab.payment.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.commons.config.RequiredConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test danh sách env bắt buộc — đặc biệt {@code PAYMENT_WEBHOOK_SECRET}: thiếu nó thì webhook
 * public không còn cách nào phân biệt provider với kẻ lạ, nên phải nằm trong danh sách validate lúc
 * boot.
 */
class RequiredEnvTest {

  @Test
  @DisplayName("có đủ 3 env bắt buộc, gồm webhook secret")
  void allContainsWebhookSecret() {
    List<String> names = RequiredEnv.ALL.stream().map(RequiredConfig::key).toList();

    assertThat(names).containsExactly("APP_ENV", "POSTGRES_PASSWORD", "PAYMENT_WEBHOOK_SECRET");
  }

  @Test
  @DisplayName("mỗi env đều có mô tả để message lỗi tự giải thích")
  void everyEnvHasDescription() {
    assertThat(RequiredEnv.ALL).allSatisfy(config -> assertThat(config.description()).isNotBlank());
  }
}
