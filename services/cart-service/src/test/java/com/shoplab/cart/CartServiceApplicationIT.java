package com.shoplab.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.cart.testsupport.TokenMinter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration test của cart-service — <b>Redis thật</b> (Testcontainers).
 *
 * <p>Vì sao bắt buộc có Redis thật: phần khó nhất của ticket là <b>Lua</b> (read-modify-write
 * nguyên tử + bất biến qty). Mock `StringRedisTemplate` chỉ chứng minh "code gọi đúng hàm", không
 * chứng minh script chạy đúng — mà lỗi ở đây là lỗi mất dữ liệu, không phải lỗi cú pháp.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "shoplab.env=local",
      "shoplab.debug-endpoints=true",
      // max-lines nhỏ để kiểm nhánh 422 mà không phải tạo 50 dòng.
      "shoplab.cart.max-lines=3"
    })
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CartServiceApplicationIT {

  private static final String REDIS_PASSWORD = "it-redis-pass";
  private static final String ISSUER = "https://identity.shoplab.local";
  private static final TokenMinter MINTER = TokenMinter.generate(ISSUER);
  private static final UUID BUYER = UUID.fromString("77777777-7777-4777-8777-000000000001");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
          .withExposedPorts(6379)
          .withCommand("redis-server", "--requirepass", REDIS_PASSWORD)
          .waitingFor(
              Wait.forLogMessage(".*Ready to accept connections.*\\n", 1)
                  .withStartupTimeout(Duration.ofSeconds(30)));

  @LocalServerPort private int port;

  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @TestConfiguration
  static class TestJwtConfig {

    @Bean
    @Primary
    JwtDecoder testJwtDecoder() {
      return MINTER.decoder();
    }
  }

  @DynamicPropertySource
  static void redis(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
  }

  // ==========================================================================
  // Guest flow
  // ==========================================================================

  @Test
  @DisplayName("guest: cấp token → thêm/sửa/xoá/xem giỏ; TTL luôn > 0")
  void guestFlow() throws Exception {
    String token = newGuestCart();

    JsonNode empty = body(send(HttpMethod.GET, "/v1/carts", null, guest(token)));
    assertThat(empty.get("items")).isEmpty();
    assertThat(empty.get("owner").asString()).isEqualTo("g:" + token);

    UUID sku = UUID.randomUUID();
    JsonNode added = body(putItem(token, sku, 2));
    assertThat(added.get("totalQty").asInt()).isEqualTo(2);
    assertThat(added.get("ttlSeconds").asLong()).isPositive();

    // PUT là set tuyệt đối ⇒ gọi lại cùng qty không nhân đôi (idempotent)
    JsonNode again = body(putItem(token, sku, 2));
    assertThat(again.get("totalQty").asInt()).isEqualTo(2);

    assertThat(body(putItem(token, sku, 7)).get("totalQty").asInt()).isEqualTo(7);

    JsonNode removed = body(send(HttpMethod.DELETE, "/v1/carts/items/" + sku, null, guest(token)));
    assertThat(removed.get("items")).isEmpty();

    JsonNode cleared = body(send(HttpMethod.DELETE, "/v1/carts", null, guest(token)));
    assertThat(cleared.get("totalQty").asInt()).isZero();
  }

  @Test
  @DisplayName("không JWT + không token → 401; token bịa → 401; có JWT → giỏ user")
  void authRules() throws Exception {
    assertThat(send(HttpMethod.GET, "/v1/carts", null, Map.of()).statusCode()).isEqualTo(401);

    HttpResponse<String> forged =
        send(HttpMethod.GET, "/v1/carts", null, Map.of("X-Cart-Token", "token-tu-bia"));
    assertThat(forged.statusCode()).isEqualTo(401);

    UUID sku = UUID.randomUUID();
    HttpResponse<String> userPut =
        send(HttpMethod.PUT, "/v1/carts/items/" + sku, qtyBody(1), withJwt(BUYER));
    assertThat(userPut.statusCode()).isEqualTo(200);
    assertThat(body(userPut).get("owner").asString()).isEqualTo("u:" + BUYER);
  }

  @Test
  @DisplayName("biên: qty 0 / 1000 → 422; quá maxLines (3) → 422")
  void limits() throws Exception {
    String token = newGuestCart();
    UUID sku = UUID.randomUUID();

    assertThat(
            send(HttpMethod.PUT, "/v1/carts/items/" + sku, qtyBody(0), guest(token)).statusCode())
        .isEqualTo(422);
    assertThat(
            send(HttpMethod.PUT, "/v1/carts/items/" + sku, qtyBody(1000), guest(token))
                .statusCode())
        .isEqualTo(422);

    for (int i = 0; i < 3; i++) {
      assertThat(
              send(HttpMethod.PUT, "/v1/carts/items/" + UUID.randomUUID(), qtyBody(1), guest(token))
                  .statusCode())
          .isEqualTo(200);
    }
    assertThat(
            send(HttpMethod.PUT, "/v1/carts/items/" + UUID.randomUUID(), qtyBody(1), guest(token))
                .statusCode())
        .isEqualTo(422);
  }

  // ==========================================================================
  // Merge + đồng thời (chỗ chứng minh Lua)
  // ==========================================================================

  @Test
  @DisplayName("merge: cộng dồn vào giỏ user (1+5=6), giỏ guest biến mất, token bị thu hồi")
  void mergeMovesGuestLines() throws Exception {
    String token = newGuestCart();
    UUID sku = UUID.randomUUID();
    putItem(token, sku, 5);
    send(HttpMethod.PUT, "/v1/carts/items/" + sku, qtyBody(1), withJwt(BUYER));

    HttpResponse<String> merged =
        send(HttpMethod.POST, "/v1/carts/merge", null, mergeHeaders(token));
    assertThat(merged.statusCode()).isEqualTo(200);
    JsonNode cart = body(merged);
    int qty = qtyOf(cart, sku);
    assertThat(qty).isGreaterThanOrEqualTo(6);

    // token đã bị thu hồi sau merge ⇒ không đọc lại được giỏ guest cũ
    assertThat(send(HttpMethod.GET, "/v1/carts", null, guest(token)).statusCode()).isEqualTo(401);
  }

  @Test
  @DisplayName(
      "20 merge SONG SONG cùng guest token → giỏ user chỉ cộng 1 lần (Lua + DEL nguyên tử)")
  void concurrentMergeAddsOnlyOnce() throws Exception {
    UUID buyer = UUID.randomUUID();
    String token = newGuestCart();
    UUID sku = UUID.randomUUID();
    putItem(token, sku, 5);

    Map<String, String> headers = new HashMap<>(withJwt(buyer));
    headers.put("X-Cart-Token", token);

    List<Integer> codes =
        runConcurrently(
            20, () -> send(HttpMethod.POST, "/v1/carts/merge", null, headers).statusCode());

    // Request thua có thể 401 (token đã bị thu hồi) hoặc 200 (đã merge xong) — cả hai đều đúng.
    assertThat(codes).allMatch(code -> code == 200 || code == 401);

    JsonNode cart = body(send(HttpMethod.GET, "/v1/carts", null, withJwt(buyer)));
    assertThat(qtyOf(cart, sku)).isEqualTo(5);
  }

  @Test
  @DisplayName(
      "20 PUT SONG SONG khác qty cùng SKU → dữ liệu không hỏng, qty cuối là một giá trị đã gửi")
  void concurrentPutsKeepDataConsistent() throws Exception {
    String token = newGuestCart();
    UUID sku = UUID.randomUUID();

    List<Integer> codes = new ArrayList<>();
    ExecutorService pool = Executors.newFixedThreadPool(20);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>();
    for (int i = 1; i <= 20; i++) {
      int qty = i;
      futures.add(
          pool.submit(
              () -> {
                start.await();
                return send(HttpMethod.PUT, "/v1/carts/items/" + sku, qtyBody(qty), guest(token))
                    .statusCode();
              }));
    }
    start.countDown();
    for (Future<Integer> future : futures) {
      codes.add(future.get(30, TimeUnit.SECONDS));
    }
    pool.shutdown();

    assertThat(codes).allMatch(code -> code == 200);
    JsonNode cart = body(send(HttpMethod.GET, "/v1/carts", null, guest(token)));
    int qty = qtyOf(cart, sku);
    assertThat(qty).isBetween(1, 20);
    assertThat(cart.get("items")).hasSize(1);
  }

  // ==========================================================================
  // helpers
  // ==========================================================================

  private List<Integer> runConcurrently(int threads, ThrowingSupplier<Integer> action)
      throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      futures.add(
          pool.submit(
              () -> {
                start.await();
                return action.get();
              }));
    }
    start.countDown();
    List<Integer> codes = new ArrayList<>();
    for (Future<Integer> future : futures) {
      codes.add(future.get(30, TimeUnit.SECONDS));
    }
    pool.shutdown();
    return codes;
  }

  @FunctionalInterface
  private interface ThrowingSupplier<T> {
    T get() throws Exception;
  }

  private String newGuestCart() throws Exception {
    HttpResponse<String> response = send(HttpMethod.POST, "/v1/carts/guest", null, Map.of());
    assertThat(response.statusCode()).isEqualTo(201);
    return body(response).get("cartToken").asString();
  }

  private HttpResponse<String> putItem(String token, UUID sku, int qty) throws Exception {
    return send(HttpMethod.PUT, "/v1/carts/items/" + sku, qtyBody(qty), guest(token));
  }

  private static Map<String, String> guest(String token) {
    return Map.of("X-Cart-Token", token);
  }

  private static Map<String, String> withJwt(UUID userId) {
    return Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + MINTER.token(userId, "BUYER"));
  }

  private static Map<String, String> mergeHeaders(String token) {
    Map<String, String> headers = new HashMap<>(withJwt(BUYER));
    headers.put("X-Cart-Token", token);
    return headers;
  }

  private static String qtyBody(int qty) {
    return "{\"qty\":" + qty + "}";
  }

  private static int qtyOf(JsonNode cart, UUID sku) {
    for (JsonNode item : cart.get("items")) {
      if (item.get("skuId").asString().equals(sku.toString())) {
        return item.get("qty").asInt();
      }
    }
    return 0;
  }

  private JsonNode body(HttpResponse<String> response) {
    return objectMapper.readTree(response.body());
  }

  private HttpResponse<String> send(
      HttpMethod method, String path, String body, Map<String, String> headers) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(10));
    headers.forEach(builder::header);
    if (body == null) {
      builder.method(method.name(), HttpRequest.BodyPublishers.noBody());
    } else {
      builder
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .method(method.name(), HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
    }
    return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }
}
