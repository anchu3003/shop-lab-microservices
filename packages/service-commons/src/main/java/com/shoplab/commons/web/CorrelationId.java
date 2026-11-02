package com.shoplab.commons.web;

import com.shoplab.commons.log.MdcKeys;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

/**
 * Correlation id — sợi chỉ nối mọi log của một request, kể cả khi request đi qua nhiều service.
 *
 * <p>Ở Phase 0 chỉ có một khái niệm duy nhất ({@code traceId} = {@code x-correlation-id}) để tránh
 * over-engineer. Phase 6 (OpenTelemetry) sẽ tách {@code traceId} (do OTel sinh) khỏi {@code
 * correlationId} (do client/gateway sinh) — lúc đó {@code CorrelationIdFilter} sẽ set thêm field
 * {@code traceId} từ context OTel.
 */
public final class CorrelationId {

  /** Header chuẩn để propagate correlation id giữa các service. */
  public static final String HEADER = "x-correlation-id";

  /** Giới hạn độ dài để client không nhét rác vào log/header. */
  public static final int MAX_LENGTH = 128;

  /**
   * Chỉ nhận ký tự an toàn. Id do client gửi là input không tin được: nếu cho tự do, nó có thể phá
   * format log JSON (newline, dấu ngoặc) hoặc làm phình log.
   */
  private static final Pattern SAFE_PATTERN = Pattern.compile("[A-Za-z0-9._:-]+");

  /** Correlation id của request đang xử lý (lấy từ MDC) — {@code null} nếu ngoài request. */
  public static String current() {
    return MDC.get(MdcKeys.TRACE_ID);
  }

  /**
   * Chuẩn hoá correlation id nhận từ client: dùng lại nếu hợp lệ, ngược lại sinh mới.
   *
   * @param candidate giá trị header client gửi (có thể {@code null})
   * @return id an toàn, không bao giờ {@code null}
   */
  public static String resolve(String candidate) {
    if (candidate == null) {
      return newId();
    }
    String trimmed = candidate.trim();
    if (trimmed.isEmpty()
        || trimmed.length() > MAX_LENGTH
        || !SAFE_PATTERN.matcher(trimmed).matches()) {
      return newId();
    }
    return trimmed;
  }

  /** Sinh correlation id mới. */
  public static String newId() {
    return UUID.randomUUID().toString();
  }

  private CorrelationId() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
