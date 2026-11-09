package com.shoplab.search;

import com.shoplab.commons.config.EnvValidator;
import com.shoplab.commons.config.MissingConfigException;
import com.shoplab.search.config.RequiredEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * search-service — <b>index</b> sản phẩm cho tìm kiếm (SL-3.6).
 *
 * <p>Trách nhiệm: consume {@code product.updated.v1} / {@code product.archived.v1} từ {@code
 * q.search} để upsert/archive index, phục vụ {@code GET /v1/search} (FTS), và <b>dựng lại được toàn
 * bộ index</b> từ catalog bằng một lệnh ({@code POST /v1/admin/search/reindex}).
 *
 * <p><b>Đây KHÔNG phải nguồn sự thật.</b> Index lệch là chuyện bình thường của hệ event-driven
 * (at-least-once, retry, hai hệ không cùng transaction) — điều bắt buộc là <b>phát hiện</b> được
 * lệch (job so số row) và <b>sửa</b> được (reindex idempotent). Phase 3 dùng PostgreSQL FTS; Phase
 * 8 đổi sang OpenSearch bằng cách thay storage, không sửa contract event.
 *
 * <p>Database riêng ({@code search}) — database per service (AGENTS.md §6).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class SearchServiceApplication {

  public static void main(String[] args) {
    // Validate TRƯỚC SpringApplication.run: thiếu env bắt buộc thì chết ngay (xem SL-0.4).
    try {
      EnvValidator.validate(System.getenv(), RequiredEnv.ALL);
    } catch (MissingConfigException ex) {
      System.err.println();
      System.err.println("[shoplab] KHỞI ĐỘNG THẤT BẠI — cấu hình không hợp lệ");
      System.err.println(ex.getMessage());
      System.err.println();
      System.exit(1);
    }
    SpringApplication.run(SearchServiceApplication.class, args);
  }
}
