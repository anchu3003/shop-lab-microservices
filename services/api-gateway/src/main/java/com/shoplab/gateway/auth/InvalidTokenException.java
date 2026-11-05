package com.shoplab.gateway.auth;

/**
 * Token không dùng được: thiếu, sai định dạng, sai chữ ký, hết hạn, sai {@code iss}/{@code kid}.
 *
 * <p>Message đưa ra ngoài phải <b>ngắn và không lộ chi tiết nội bộ</b> (AGENTS.md §9) — lý do đầy
 * đủ chỉ ghi vào log.
 */
public class InvalidTokenException extends RuntimeException {

  public InvalidTokenException(String message) {
    super(message);
  }

  public InvalidTokenException(String message, Throwable cause) {
    super(message, cause);
  }
}
