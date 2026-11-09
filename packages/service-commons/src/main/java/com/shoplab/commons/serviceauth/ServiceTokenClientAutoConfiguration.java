package com.shoplab.commons.serviceauth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Cắm {@link ServiceTokenClient} cho service cần gọi service khác (SL-3.11).
 *
 * <p>Điều kiện bật: có {@code shoplab.service-auth.client-id} — nghĩa là service **đã khai** nó là
 * client. Không khai ⇒ không bean, không HTTP client nào được dựng ⇒ service chỉ-nhận-request không
 * phải nuôi cấu hình không dùng.
 *
 * <p><b>Fail-closed</b>: thiếu {@code client-secret} thì {@link
 * ServiceTokenProperties#configured()} false nhưng bean vẫn tạo — lỗi chỉ nổ khi thực sự gọi (kèm
 * message rõ). Lý do không chặn ở khởi động: service có thể chỉ dùng client cho một nhánh hiếm (vd
 * chỉ khi replay), và làm chết cả service vì thiếu 1 secret của nhánh đó là phản tác dụng.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "shoplab.service-auth.client-id")
@EnableConfigurationProperties(ServiceTokenProperties.class)
public class ServiceTokenClientAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ServiceTokenClient serviceTokenClient(ServiceTokenProperties properties) {
    return new ServiceTokenClient(new HttpServiceTokenSource(properties), properties.ttlSkew());
  }
}
