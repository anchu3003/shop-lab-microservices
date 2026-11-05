package com.shoplab.gateway.testsupport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;

/**
 * Service đích giả cho test của gateway — dựng bằng {@code HttpServer} có sẵn trong JDK.
 *
 * <p><b>Vì sao không dùng WireMock/MockWebServer</b>: thêm 1 dependency chỉ để giả một HTTP server
 * là không cần thiết khi JDK đã có {@code com.sun.net.httpserver}. Đổi lại phải tự viết ~40 dòng xử
 * lý.
 *
 * <p>Stub này đóng vai identity-service ở mức tối thiểu cần cho gateway: công bố JWKS và <b>echo
 * lại header</b> để test chứng minh gateway có gắn {@code x-user-id}/{@code x-user-role} hay không.
 */
public final class UpstreamStub implements AutoCloseable {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final HttpServer server;

  public UpstreamStub(String jwksJson) throws IOException {
    this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> handle(exchange, jwksJson));
    server.start();
  }

  /** Base URL không có dấu {@code /} ở cuối — dùng để khai vào {@code shoplab.gateway.routes}. */
  public String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public int port() {
    return server.getAddress().getPort();
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private static void handle(HttpExchange exchange, String jwksJson) throws IOException {
    String path = exchange.getRequestURI().getPath();

    if ("/.well-known/jwks.json".equals(path)) {
      respond(exchange, 200, jwksJson, "application/json");
      return;
    }
    if (path.endsWith("/boom")) {
      // Service đích trả 5xx → gateway phải giữ nguyên status + body này.
      respond(
          exchange,
          500,
          "{\"code\":\"INTERNAL_ERROR\",\"message\":\"upstream exploded\"}",
          "application/json");
      return;
    }

    // Echo: mọi thứ gateway gửi xuống đều nhìn thấy được ở đây.
    Map<String, Object> echo = new LinkedHashMap<>();
    echo.put("method", exchange.getRequestMethod());
    echo.put("path", path);
    echo.put("query", exchange.getRequestURI().getQuery());
    echo.put("userId", header(exchange, "x-user-id"));
    echo.put("role", header(exchange, "x-user-role"));
    echo.put("correlationId", header(exchange, "x-correlation-id"));
    echo.put("authorization", header(exchange, "authorization"));
    echo.put("body", new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

    respond(exchange, 200, MAPPER.writeValueAsString(echo), "application/json");
  }

  private static String header(HttpExchange exchange, String name) {
    return exchange.getRequestHeaders().getFirst(name);
  }

  private static void respond(HttpExchange exchange, int status, String body, String contentType)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }
}
