package com.shoplab.notification.log;

import com.shoplab.notification.config.NotificationProperties;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * `GET /v1/notifications/me` (SL-3.4) — xem thông báo của **chính mình**, mới nhất trước.
 *
 * <p><b>Vì sao cần endpoint này</b>: nếu chỉ có `notification_logs` trong DB thì verify async phải
 * mở psql — người không quen DB không kiểm được "email đã gửi chưa". Một endpoint nhỏ biến việc
 * verify thành 1 lệnh `curl`, và cũng là API thật mà client sẽ dùng.
 *
 * <p><b>Không có endpoint xem của người khác</b> (kể cả admin) ở phase này: đọc log thông báo của
 * người khác là quyết định về quyền riêng tư, không phải thứ nên mở khi chưa có yêu cầu
 * (`AGENTS.md` §3.4).
 */
@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {

  private final NotificationLogRepository logs;
  private final NotificationProperties properties;

  public NotificationController(NotificationLogRepository logs, NotificationProperties properties) {
    this.logs = logs;
    this.properties = properties;
  }

  /**
   * Danh sách thông báo của user trong token (userId lấy từ claim `sub`, không tin query param).
   */
  @GetMapping("/me")
  public List<NotificationLogRepository.NotificationLog> mine(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(name = "limit", required = false) Integer limit) {
    UUID userId = UUID.fromString(jwt.getSubject());
    int effective =
        limit == null ? properties.listLimit() : Math.min(limit, properties.listLimit());
    return logs.findByUser(userId, effective);
  }
}
