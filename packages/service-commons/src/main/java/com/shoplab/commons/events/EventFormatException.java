package com.shoplab.commons.events;

/** Message/format event không hợp lệ (JSON hỏng · thiếu field · version lạ). */
public class EventFormatException extends RuntimeException {

  public EventFormatException(String message, Throwable cause) {
    super(message, cause);
  }
}
