package com.shoplab.gateway.routing;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.commons.web.CorrelationId;
import com.shoplab.gateway.config.GatewayProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Chuyển tiếp request tới service đích và trả <b>nguyên</b> response về cho client.
 *
 * <p>Nguyên tắc:
 *
 * <ul>
 *   <li><b>Không</b> bọc lại body của service đích (tránh double-encode, tránh làm mất field lạ).
 *   <li>Status của service đích được giữ nguyên (kể cả 4xx/5xx) — gateway không "diễn giải lại" lỗi
 *       của người khác. Chỉ lỗi <b>do chính gateway</b> sinh ra mới dùng {@code ApiError}.
 *   <li>Không tới được service đích → <b>503</b> {@code ApiError} (khác 401: đây là lỗi hạ tầng,
 *       client retry được).
 * </ul>
 */
@Component
public class RequestForwarder {

  /**
   * Header chỉ có ý nghĩa cho <b>một</b> kết nối HTTP, không được chuyển tiếp (RFC 7230 §6.1).
   * {@code content-length} do chính client HTTP của gateway tự tính lại từ body.
   */
  private static final Set<String> HOP_BY_HOP =
      Set.of(
          "connection",
          "keep-alive",
          "transfer-encoding",
          "te",
          "trailer",
          "upgrade",
          "proxy-authorization",
          "proxy-authenticate",
          "host",
          "content-length",
          // Nén giữa gateway và service nội bộ chỉ làm phức tạp thêm (phải xử lý content-encoding),
          // trong khi lợi ích gần bằng 0 trên network Docker.
          "accept-encoding");

  private final RestClient restClient;

  public RequestForwarder(RestClient gatewayRestClient) {
    this.restClient = gatewayRestClient;
  }

  public ResponseEntity<byte[]> forward(
      HttpServletRequest request, GatewayProperties.Route route, byte[] body) {

    URI target = targetUri(route, request);
    try {
      RestClient.RequestBodySpec spec =
          restClient
              .method(HttpMethod.valueOf(request.getMethod()))
              .uri(target)
              .headers(headers -> copyRequestHeaders(request, headers));

      RestClient.RequestBodySpec withBody = body.length == 0 ? spec : spec.body(body);

      return withBody.exchange(
          (clientRequest, clientResponse) ->
              new ResponseEntity<>(
                  clientResponse.getBody().readAllBytes(),
                  copyResponseHeaders(clientResponse.getHeaders()),
                  HttpStatusCode.valueOf(clientResponse.getStatusCode().value())));
    } catch (ResourceAccessException e) {
      // Connect fail / read timeout: service đích chết hoặc treo.
      throw new AppException(
          ErrorCode.UNAVAILABLE, "Service đích không tới được: " + route.baseUrl());
    }
  }

  private static void copyRequestHeaders(HttpServletRequest request, HttpHeaders target) {
    for (String name : Collections.list(request.getHeaderNames())) {
      if (isForwardable(name)) {
        for (String value : Collections.list(request.getHeaders(name))) {
          target.add(name, value);
        }
      }
    }
    // Correlation id là sợi chỉ nối log 2 đầu: ghi lại lần cuối để chắc chắn giá trị client thấy ở
    // response = giá trị service đích ghi vào log (kể cả khi client không gửi gì).
    target.set(CorrelationId.HEADER, CorrelationId.current());
  }

  private static HttpHeaders copyResponseHeaders(HttpHeaders upstream) {
    HttpHeaders headers = new HttpHeaders();
    upstream.forEach(
        (name, values) -> {
          if (isForwardable(name) && !CorrelationId.HEADER.equalsIgnoreCase(name)) {
            headers.put(name, values);
          }
        });
    return headers;
  }

  private static boolean isForwardable(String name) {
    return name != null && !HOP_BY_HOP.contains(name.toLowerCase());
  }

  /** Ghép base URL của service đích với path + query của request (giữ nguyên encoding gốc). */
  private static URI targetUri(GatewayProperties.Route route, HttpServletRequest request) {
    String base = route.baseUrl();
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    String query = request.getQueryString();
    return URI.create(base + request.getRequestURI() + (query == null ? "" : "?" + query));
  }
}
