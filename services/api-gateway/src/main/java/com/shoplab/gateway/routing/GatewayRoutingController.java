package com.shoplab.gateway.routing;

import com.shoplab.commons.error.AppException;
import com.shoplab.commons.error.ErrorCode;
import com.shoplab.gateway.config.GatewayProperties;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cửa vào của mọi request không phải endpoint hạ tầng của gateway.
 *
 * <p>Mapping {@code /**} là <b>catch-all</b>: các endpoint cụ thể ({@code /health}, {@code /ready},
 * {@code /version}, {@code /debug/*}) vẫn được Spring ưu tiên vì pattern cụ thể thắng pattern tổng
 * quát — nên chúng không bị route ra ngoài.
 *
 * <p>Body được đọc thẳng từ {@code InputStream} (không dùng {@code @RequestBody}): gateway không
 * cần hiểu body, chỉ cần chuyển tiếp nguyên byte — nhờ vậy mọi content-type đều đi qua được.
 */
@Hidden // proxy catch-all: không phải API nghiệp vụ, tài liệu ở spec của service đích (SL-1.8)
@RestController
public class GatewayRoutingController {

  private final RouteMatcher routeMatcher;
  private final RequestForwarder forwarder;

  public GatewayRoutingController(RouteMatcher routeMatcher, RequestForwarder forwarder) {
    this.routeMatcher = routeMatcher;
    this.forwarder = forwarder;
  }

  @RequestMapping("/**")
  public ResponseEntity<byte[]> route(HttpServletRequest request) throws IOException {
    String path = request.getRequestURI();
    GatewayProperties.Route route =
        routeMatcher
            .match(path)
            .orElseThrow(
                () -> new AppException(ErrorCode.NOT_FOUND, "Gateway không có route cho path này"));

    byte[] body = request.getInputStream().readAllBytes();
    return forwarder.forward(request, route, body);
  }
}
