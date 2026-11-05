package com.shoplab.gateway.config;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * HTTP client dùng để gọi service đích.
 *
 * <p><b>Timeout là bắt buộc, không phải "nice to have"</b>: client không timeout thì một service
 * đích bị treo sẽ giữ thread của gateway cho tới khi hết thread pool → gateway chết theo service
 * chết. Đây là mầm của bài toán resilience ở Phase 5 (retry/circuit breaker), nhưng timeout phải có
 * ngay.
 *
 * <p>Dùng {@code JdkClientHttpRequestFactory} (java.net.http) thay vì {@code HttpURLConnection}:
 * HTTP/2 + connection pool sẵn có, và API timeout rõ ràng (connect ở client, read ở factory).
 */
@Configuration
public class GatewayClientConfig {

  @Bean
  public RestClient gatewayRestClient(GatewayProperties properties) {
    GatewayProperties.Timeouts timeouts = properties.timeouts();

    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeouts.connect()).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(timeouts.read());

    // KHÔNG cấu hình error handler: dùng `exchange(...)` để nhận nguyên response của service đích
    // (kể cả 4xx/5xx) rồi trả lại cho client — gateway không được "diễn giải lại" lỗi của service.
    return RestClient.builder().requestFactory(requestFactory).build();
  }
}
