package com.shoplab.commons.log;

/**
 * Tên field MDC dùng xuyên suốt ShopLab.
 *
 * <p>Phải khớp với cấu hình JSON trong {@code logback-shoplab.xml} — đổi ở đây thì đổi cả đó, nếu
 * không field sẽ không xuất hiện trong log.
 */
public final class MdcKeys {

  /** Correlation id của request: nhận từ header {@code x-correlation-id}, thiếu thì sinh mới. */
  public static final String TRACE_ID = "traceId";

  private MdcKeys() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
