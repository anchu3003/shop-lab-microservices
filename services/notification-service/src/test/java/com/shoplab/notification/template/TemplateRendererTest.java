package com.shoplab.notification.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit test renderer placeholder của notification-service (SL-3.4). */
class TemplateRendererTest {

  private final TemplateRenderer renderer = new TemplateRenderer();

  @Test
  @DisplayName("thay mọi placeholder có trong vars, giữ nguyên chữ xung quanh")
  void shouldReplaceAllPlaceholders() {
    String out =
        renderer.render(
            "Đơn {{orderId}} trị giá {{total}} {{currency}}",
            Map.of("orderId", "abc", "total", 70000, "currency", "VND"));

    assertThat(out).isEqualTo("Đơn abc trị giá 70000 VND");
  }

  @Test
  @DisplayName(
      "placeholder KHÔNG có trong payload giữ nguyên — không ném lỗi (email thiếu field "
          + "vẫn hơn là message vào DLQ)")
  void shouldKeepUnknownPlaceholder() {
    String out = renderer.render("Đơn {{orderId}} huỷ vì {{reason}}", Map.of("orderId", "abc"));

    assertThat(out).isEqualTo("Đơn abc huỷ vì {{reason}}");
  }

  @Test
  @DisplayName("giá trị null render thành chuỗi rỗng, không in 'null'")
  void shouldRenderNullAsEmpty() {
    Map<String, Object> vars = new LinkedHashMap<>();
    vars.put("reason", null);

    assertThat(renderer.render("Lý do: {{reason}}.", vars)).isEqualTo("Lý do: .");
  }

  @Test
  @DisplayName("template null → chuỗi rỗng (template thiếu do cấu hình, không nổ NPE)")
  void shouldReturnEmptyForNullTemplate() {
    assertThat(renderer.render(null, Map.of())).isEmpty();
  }

  @Test
  @DisplayName("không escape HTML: body là text thuần, mock provider không render HTML")
  void shouldNotEscapeHtml() {
    assertThat(renderer.render("{{name}}", Map.of("name", "<b>A</b>"))).isEqualTo("<b>A</b>");
  }
}
