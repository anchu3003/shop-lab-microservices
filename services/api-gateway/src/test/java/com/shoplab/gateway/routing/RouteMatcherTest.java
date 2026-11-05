package com.shoplab.gateway.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoplab.gateway.config.GatewayProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit test cho {@link RouteMatcher} — logic thuần, không cần Spring. */
class RouteMatcherTest {

  private static RouteMatcher matcher(GatewayProperties.Route... routes) {
    return new RouteMatcher(new GatewayProperties(List.of(routes), List.of(), null, null));
  }

  private static GatewayProperties.Route route(String prefix, String baseUrl) {
    return new GatewayProperties.Route(prefix, baseUrl);
  }

  @Test
  @DisplayName("khớp prefix và trả base URL tương ứng")
  void shouldMatchByPrefix() {
    RouteMatcher matcher =
        matcher(
            route("/v1/auth", "http://identity:8081"), route("/v1/users", "http://identity:8081"));

    assertThat(matcher.match("/v1/auth/login")).isPresent();
    assertThat(matcher.match("/v1/auth/login").orElseThrow().baseUrl())
        .isEqualTo("http://identity:8081");
    assertThat(matcher.match("/v1/users/me")).isPresent();
  }

  @Test
  @DisplayName("prefix dài nhất thắng — khai thêm route cụ thể hơn không phụ thuộc thứ tự config")
  void longestPrefixShouldWin() {
    // Route cụ thể khai TRƯỚC route tổng quát, và ngược lại — cả hai đều phải cho cùng kết quả.
    RouteMatcher specificFirst =
        matcher(
            route("/v1/auth/admin", "http://admin:9000"),
            route("/v1/auth", "http://identity:8081"));
    RouteMatcher genericFirst =
        matcher(
            route("/v1/auth", "http://identity:8081"),
            route("/v1/auth/admin", "http://admin:9000"));

    assertThat(specificFirst.match("/v1/auth/admin/users").orElseThrow().baseUrl())
        .isEqualTo("http://admin:9000");
    assertThat(genericFirst.match("/v1/auth/admin/users").orElseThrow().baseUrl())
        .isEqualTo("http://admin:9000");
    assertThat(genericFirst.match("/v1/auth/login").orElseThrow().baseUrl())
        .isEqualTo("http://identity:8081");
  }

  @Test
  @DisplayName(
      "khớp theo RANH GIỚI segment: /v1/auth KHÔNG khớp /v1/authz (bug kinh điển của gateway)")
  void shouldNotMatchPartialSegment() {
    RouteMatcher matcher = matcher(route("/v1/auth", "http://identity:8081"));

    assertThat(matcher.match("/v1/authz")).isEmpty();
    assertThat(matcher.match("/v1/authentication")).isEmpty();
  }

  @Test
  @DisplayName("khớp đúng path bằng prefix (không có dấu / ở sau)")
  void shouldMatchExactPath() {
    RouteMatcher matcher = matcher(route("/v1/auth", "http://identity:8081"));

    assertThat(matcher.match("/v1/auth")).isPresent();
  }

  @Test
  @DisplayName("prefix có dấu / ở cuối vẫn khớp bình thường")
  void shouldTolerateTrailingSlashInPrefix() {
    RouteMatcher matcher = matcher(route("/v1/auth/", "http://identity:8081"));

    assertThat(matcher.match("/v1/auth/login")).isPresent();
  }

  @Test
  @DisplayName("không route nào khớp hoặc path null → rỗng (gateway trả 404)")
  void shouldReturnEmptyWhenNothingMatches() {
    RouteMatcher matcher = matcher(route("/v1/auth", "http://identity:8081"));

    assertThat(matcher.match("/v2/khong-co")).isEmpty();
    assertThat(matcher.match(null)).isEmpty();
    assertThat(matcher.match("/v1/auth")).isPresent();
  }
}
