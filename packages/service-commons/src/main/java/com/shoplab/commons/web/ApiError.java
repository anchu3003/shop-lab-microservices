package com.shoplab.commons.web;

import com.shoplab.commons.error.ErrorCode;
import java.time.Instant;
import java.util.List;

/**
 * Body lỗi thống nhất của mọi service ShopLab — client chỉ cần parse một format duy nhất (AGENTS.md
 * §6).
 *
 * <p>{@code traceId} là phần quan trọng nhất: user báo lỗi thì chỉ cần đưa id này, dev grep log ra
 * toàn bộ hành trình của request.
 */
public record ApiError(
    String code,
    String message,
    String traceId,
    Instant timestamp,
    String path,
    List<FieldViolation> details) {

  /** Lỗi ở mức từng field (dùng cho validation). */
  public record FieldViolation(String field, String message) {}

  /** Tạo body lỗi không có chi tiết field. */
  public static ApiError of(ErrorCode code, String message, String path) {
    return of(code, message, path, List.of());
  }

  /**
   * Tạo body lỗi.
   *
   * @param message message riêng; {@code null} thì dùng {@link ErrorCode#defaultMessage()}
   * @param details chi tiết theo field; {@code null} được coi như rỗng
   */
  public static ApiError of(
      ErrorCode code, String message, String path, List<FieldViolation> details) {
    return new ApiError(
        code.name(),
        message == null ? code.defaultMessage() : message,
        CorrelationId.current(),
        Instant.now(),
        path,
        details == null ? List.of() : List.copyOf(details));
  }
}
