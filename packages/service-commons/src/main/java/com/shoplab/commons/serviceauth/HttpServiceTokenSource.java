package com.shoplab.commons.serviceauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link ServiceTokenClient.TokenSource} thật: gọi HTTP tới identity-service (SL-3.11).
 *
 * <p><b>Vì sao dùng {@link HttpClient} của JDK chứ không {@code RestClient}/{@code
 * RestTemplate}</b>: commons không phụ thuộc `spring-web` (service không-web như worker vẫn dùng
 * được), và đây là một POST JSON cố định — kéo thêm abstraction của Spring chỉ để gọi 1 endpoint là
 * trả giá vô ích ({@code AGENTS.md} §3.4). Cùng lựa chọn với `DownstreamGateway` ở order-service
 * (đã ghi bài học Boot 4 tách module http-client).
 *
 * <p><b>Lỗi phải nêu rõ trạng thái HTTP</b>: "401" (sai secret) khác "503" (identity chết) — gộp
 * thành "không lấy được token" sẽ khiến người trực sửa sai chỗ.
 */
public class HttpServiceTokenSource implements ServiceTokenClient.TokenSource {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

  private final ServiceTokenProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient http;

  public HttpServiceTokenSource(ServiceTokenProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
  }

  @Override
  public ServiceTokenClient.CachedToken fetch(String audience) {
    Map<String, String> body = new LinkedHashMap<>();
    body.put("clientId", properties.clientId());
    body.put("clientSecret", properties.clientSecret());
    body.put("audience", audience);

    HttpResponse<String> response;
    try {
      String json = objectMapper.writeValueAsString(body);
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(properties.tokenUrl()))
              .timeout(READ_TIMEOUT)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .build();
      response = http.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ServiceTokenException("bị ngắt khi xin service token cho " + audience, e);
    } catch (Exception e) {
      throw new ServiceTokenException(
          "không gọi được identity-service (" + properties.tokenUrl() + "): " + e.getMessage(), e);
    }

    if (response.statusCode() != 200) {
      // Không log body: response lỗi có thể chứa thông tin nội bộ; status + audience đủ để chẩn
      // đoán.
      throw new ServiceTokenException(
          "identity trả HTTP "
              + response.statusCode()
              + " khi cấp service token cho audience="
              + audience
              + " (401 = sai clientSecret, 400 = audience không được phép)");
    }
    return parse(response.body(), audience);
  }

  private ServiceTokenClient.CachedToken parse(String json, String audience) {
    try {
      JsonNode node = objectMapper.readTree(json);
      String token = node.path("accessToken").asText(null);
      long expiresIn = node.path("expiresIn").asLong(0);
      if (token == null || token.isBlank() || expiresIn <= 0) {
        throw new ServiceTokenException(
            "response cấp token thiếu accessToken/expiresIn (audience=" + audience + ")");
      }
      return new ServiceTokenClient.CachedToken(token, Instant.now().plusSeconds(expiresIn));
    } catch (ServiceTokenException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceTokenException("không parse được response cấp token", e);
    }
  }
}
