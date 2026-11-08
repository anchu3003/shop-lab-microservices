package com.shoplab.notification.template;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Render template kiểu placeholder `{{var}}` (SL-3.4 — mock provider, không SMTP, không engine
 * thật).
 *
 * <p><b>Vì sao không dùng Thymeleaf/Freemarker</b>: bài học của phase này là **event chạy async +
 * retry/DLQ**, không phải template engine. Thêm 1 dependency lớn chỉ để thay chuỗi là over-engineer
 * (`AGENTS.md` §3.4).
 *
 * <p><b>Biến thiếu thì để nguyên placeholder</b> thay vì ném lỗi: email thiếu 1 field vẫn hữu ích
 * hơn là message vào DLQ vì payload của phase trước thiếu field (và DLQ chỉ nên chứa lỗi **thật**).
 */
@Component
public class TemplateRenderer {

  public String render(String template, Map<String, Object> vars) {
    if (template == null) {
      return "";
    }
    String out = template;
    for (Map.Entry<String, Object> entry : vars.entrySet()) {
      String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
      out = out.replace("{{" + entry.getKey() + "}}", value);
    }
    return out;
  }
}
