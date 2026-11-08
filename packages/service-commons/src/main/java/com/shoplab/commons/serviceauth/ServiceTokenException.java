package com.shoplab.commons.serviceauth;

/** Xin service token thất bại (identity không tới được, secret sai, response lạ). */
public class ServiceTokenException extends RuntimeException {

  public ServiceTokenException(String message) {
    super(message);
  }

  public ServiceTokenException(String message, Throwable cause) {
    super(message, cause);
  }
}
