package com.shoplab.commons.events;

/**
 * Không publish được event (broker nack / timeout / mất kết nối) — caller phải coi là "chưa gửi".
 */
public class EventPublishException extends RuntimeException {

  public EventPublishException(String message) {
    super(message);
  }

  public EventPublishException(String message, Throwable cause) {
    super(message, cause);
  }
}
