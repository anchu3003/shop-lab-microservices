package com.shoplab.search.index;

import com.shoplab.search.config.SearchProperties;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Client đọc catalog cho <b>reconcile</b> (SL-3.6) — đường lùi bắt buộc của mọi index.
 *
 * <p><b>Vì sao chỉ dùng cho reconcile, không dùng cho luồng thường</b>: nếu mỗi event mà search gọi
 * lại catalog thì vừa thừa (event đã có dữ liệu) vừa tạo coupling runtime — và "dựng lại index khi
 * catalog sập" là chuyện không làm được.
 *
 * <p><b>Vì sao {@code java.net.http.HttpClient} chứ không {@code RestClient}</b> (bẫy thật gặp ở
 * SL-3.6): {@code RestClient.Builder} là bean auto-config <b>không</b> có mặt trong context của
 * service này (Boot 4 tách auto-config ra module riêng) ⇒ inject vào là context fail ngay. Dùng JDK
 * client + parse JSON tường minh thì chỉ phụ thuộc thứ chắc chắn có: JDK và Jackson.
 *
 * <p><b>Không có token</b>: `GET /v1/products` là endpoint public (guest xem sản phẩm) ⇒ service
 * token ở đây chỉ thêm phụ thuộc mà không thêm quyền.
 */
@Component
public class CatalogClient {

  private static final Logger LOG = LoggerFactory.getLogger(CatalogClient.class);

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final String baseUrl;
  private final HttpClient http;
  private final ObjectMapper mapper = new ObjectMapper();

  public CatalogClient(SearchProperties properties) {
    this.baseUrl = properties.catalogUrl();
    this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    LOG.info("catalog client: {}", baseUrl);
  }

  /**
   * Đọc một trang product <b>còn sống</b> của catalog (sort `updatedAt desc` để lần quét ổn định).
   *
   * @throws CatalogUnavailableException khi không gọi được catalog hoặc body không parse được
   */
  public CatalogPage page(int page, int size) {
    String url =
        baseUrl
            + "/v1/products?page="
            + page
            + "&size="
            + size
            // `createdAt` là field DUY NHẤT (cùng `price`) mà `ProductQuery` của catalog cho sort —
            // gửi `updatedAt` thì catalog trả 400 (bẫy gặp thật ở bước verify SL-3.6). Reindex chỉ
            // cần thứ tự ỔN ĐỊNH để lần quét lặp lại cho cùng kết quả, không cần mới-nhất-trước.
            + "&sort="
            + URLEncoder.encode("createdAt,desc", StandardCharsets.UTF_8);
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build();
    try {
      HttpResponse<String> response =
          http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() != 200) {
        throw new CatalogUnavailableException(
            "catalog trả HTTP " + response.statusCode() + " cho " + url);
      }
      CatalogPage body = mapper.readValue(response.body(), CatalogPage.class);
      if (body == null) {
        throw new CatalogUnavailableException("catalog trả body rỗng (page=" + page + ")");
      }
      return body;
    } catch (HttpTimeoutException ex) {
      throw new CatalogUnavailableException("catalog quá hạn " + TIMEOUT + ": " + url, ex);
    } catch (IOException ex) {
      throw new CatalogUnavailableException("không đọc được catalog: " + ex.getMessage(), ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new CatalogUnavailableException("bị ngắt khi gọi catalog", ex);
    }
  }

  /** Tổng số product còn sống — job so độ lệch chỉ cần 1 row. */
  public long totalActiveProducts() {
    return page(0, 1).totalElements();
  }

  /**
   * Một phần tử trong `PageResponse<ProductResponse>` của catalog.
   *
   * <p>`updatedAt` để dạng chuỗi rồi {@code Instant.parse} ở nơi dùng: bớt phụ thuộc vào việc
   * Jackson có đăng ký module java.time hay không (cấu hình đó đổi theo version ⇒ lỗi chỉ lộ ra ở
   * runtime).
   */
  public record CatalogProduct(
      UUID id,
      UUID categoryId,
      String name,
      String description,
      BigDecimal price,
      String status,
      String updatedAt) {}

  /** `PageResponse` của catalog — khai đủ field để không phụ thuộc cấu hình ignore-unknown. */
  public record CatalogPage(
      List<CatalogProduct> content, int page, int size, long totalElements, int totalPages) {}

  /** Catalog không đọc được ⇒ reconcile phải dừng, KHÔNG được archive gì. */
  public static class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(String message) {
      super(message);
    }

    public CatalogUnavailableException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
