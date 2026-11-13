package com.shoplab.gateway.ratelimit;

/** Không nói chuyện được với Redis khi đang kiểm rate limit (để filter quyết fail-open). */
public class RateLimitUnavailableException extends RuntimeException {

  public RateLimitUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
