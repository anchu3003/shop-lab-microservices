package com.shoplab.search.index;

import com.shoplab.search.config.SearchProperties;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job phát hiện <b>index lệch</b> (SL-3.6): so số row ACTIVE của index với số product còn sống của
 * catalog, lệch quá ngưỡng (mặc định 1%) thì log WARN.
 *
 * <p><b>Phát hiện, không tự sửa.</b> Tự reindex theo lịch nghe hấp dẫn nhưng: (a) nó che mất nguyên
 * nhân lệch — consumer chết 3 ngày mà ai cũng thấy "search vẫn ổn"; (b) tải đọc toàn catalog theo
 * lịch là thứ phải đo trước khi bật. Ở Phase 3 này, một dòng WARN + lệnh reindex tay là đủ và trung
 * thực hơn.
 *
 * <p>Broker/catalog chết ⇒ job chỉ log WARN rồi thôi: đây là job vận hành, không được làm sập app.
 */
@Component
public class IndexDriftChecker {

  private static final Logger LOG = LoggerFactory.getLogger(IndexDriftChecker.class);

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final CatalogClient catalog;
  private final SearchRepository index;
  private final SearchProperties properties;

  public IndexDriftChecker(
      CatalogClient catalog, SearchRepository index, SearchProperties properties) {
    this.catalog = catalog;
    this.index = index;
    this.properties = properties;
  }

  @Scheduled(
      fixedDelayString = "${shoplab.search.drift-check-interval:10m}",
      initialDelayString = "${shoplab.search.drift-check-initial-delay:60s}")
  public void check() {
    try {
      checkOnce();
    } catch (RuntimeException ex) {
      // Không để exception lọt ra khỏi @Scheduled: nhịp sau sẽ không chạy và log đầy stack trace
      // (bài học SL-2.3).
      LOG.warn("so index/catalog: bỏ qua nhịp này — {}", ex.getMessage());
    }
  }

  /**
   * @return độ lệch phần trăm (0 = khớp) — tách ra để test được mà không cần `@Scheduled`
   */
  BigDecimal checkOnce() {
    long indexed = index.countByStatus(SearchProduct.ACTIVE);
    long inCatalog = catalog.totalActiveProducts();
    BigDecimal drift = driftPercent(indexed, inCatalog);

    if (drift.compareTo(properties.driftThresholdPercent()) > 0) {
      LOG.warn(
          "INDEX LỆCH: index={} catalog={} lệch={}% (ngưỡng {}%) ⇒ chạy"
              + " `POST /v1/admin/search/reindex`",
          indexed, inCatalog, drift, properties.driftThresholdPercent());
    } else {
      LOG.info("index khớp catalog: index={} catalog={} lệch={}%", indexed, inCatalog, drift);
    }
    return drift;
  }

  /** Lệch tương đối theo catalog — catalog là mẫu số vì nó là nguồn sự thật. */
  static BigDecimal driftPercent(long indexed, long inCatalog) {
    if (inCatalog == 0) {
      return indexed == 0 ? BigDecimal.ZERO : HUNDRED;
    }
    return BigDecimal.valueOf(Math.abs(indexed - inCatalog))
        .multiply(HUNDRED)
        .divide(BigDecimal.valueOf(inCatalog), 2, java.math.RoundingMode.HALF_UP);
  }
}
