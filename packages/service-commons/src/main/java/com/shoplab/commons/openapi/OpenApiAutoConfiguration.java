package com.shoplab.commons.openapi;

import com.shoplab.commons.web.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Cấu hình OpenAPI dùng chung cho mọi service ShopLab (SL-1.8).
 *
 * <p><b>Tại sao ở commons</b>: ba service đều cần cùng một khung spec — tên, security scheme
 * `bearerAuth`, và bộ response lỗi chuẩn `ApiError` cho 400/401/403/404/409. Viết một lần ở đây thì
 * service mới chỉ cần khai dependency springdoc là có spec đúng chuẩn, không phải copy 60 dòng.
 *
 * <p><b>Tại sao dùng customizer thay vì rải annotation</b>: hành vi lỗi là <i>của toàn hệ</i>
 * (`GlobalExceptionHandler` + security filter chain), không phải của từng endpoint. Khai bằng
 * customizer đảm bảo endpoint thêm sau này <b>tự</b> có tài liệu lỗi — không phụ thuộc việc dev nhớ
 * annotation.
 *
 * <p>Chỉ active khi service có springdoc trên classpath ({@code @ConditionalOnClass}) ⇒ không ảnh
 * hưởng service không dùng OpenAPI.
 */
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OpenApiAutoConfiguration {

  /** Mã lỗi chuẩn mà MỌI endpoint có thể trả (xem {@code ErrorCode} + security filter chain). */
  private static final Map<String, String> ERROR_RESPONSES = standardErrorResponses();

  /**
   * Literal thay cho {@code org.springframework.http.MediaType} — trùng tên với swagger MediaType.
   */
  private static final String JSON = "application/json";

  private static Map<String, String> standardErrorResponses() {
    Map<String, String> responses = new LinkedHashMap<>();
    responses.put("400", "Input không hợp lệ (xem `details` theo từng field)");
    responses.put("401", "Chưa xác thực: thiếu/sai/hết hạn token");
    responses.put("403", "Đã xác thực nhưng thiếu quyền");
    responses.put("404", "Không tìm thấy tài nguyên");
    responses.put("409", "Xung đột dữ liệu (trùng mã, version lệch...)");
    // LinkedHashMap (không Map.copyOf) để giữ THỨ TỰ: spec phải deterministic giữa các lần
    // export, nếu không mỗi lần chạy sẽ đổi thứ tự mã lỗi ⇒ git diff rác.
    return Collections.unmodifiableMap(responses);
  }

  /**
   * Khung spec: thông tin service + security scheme bearer.
   *
   * <p>Không đặt security toàn cục: endpoint public (guest đọc catalog) phải hiện là public.
   * Endpoint cần token được đánh dấu bằng {@code @SecurityRequirement(name = "bearerAuth")} ở
   * method.
   */
  @Bean
  @ConditionalOnMissingBean
  public OpenAPI shoplabOpenApi(@Value("${spring.application.name:unknown}") String serviceName) {
    return new OpenAPI()
        .info(
            new Info()
                .title("ShopLab · " + serviceName)
                .version("v1")
                .description(
                    "API Phase 1. Mọi lỗi dùng schema `ApiError` (có `traceId` để grep log). "
                        + "401 = chưa xác thực · 403 = thiếu quyền · 503 = dependency chết (retry được)."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token lấy từ POST /v1/auth/login")));
  }

  /** Đưa schema {@code ApiError} (và các record con) vào {@code components.schemas} của spec. */
  @Bean
  public OpenApiCustomizer shoplabApiErrorSchemas() {
    return openApi -> {
      if (openApi.getComponents() == null) {
        openApi.setComponents(new Components());
      }
      ModelConverters.getInstance()
          .readAll(ApiError.class)
          .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
    };
  }

  /** Gắn bộ response lỗi chuẩn vào <b>mọi</b> operation (kể cả endpoint thêm sau này). */
  @Bean
  public OperationCustomizer shoplabErrorResponsesCustomizer() {
    return (operation, handlerMethod) -> {
      if (operation.getResponses() == null) {
        operation.setResponses(new ApiResponses());
      }
      addMissingErrorResponses(operation);
      return operation;
    };
  }

  private static void addMissingErrorResponses(Operation operation) {
    ERROR_RESPONSES.forEach(
        (code, description) ->
            operation
                .getResponses()
                .computeIfAbsent(
                    code,
                    missing ->
                        new ApiResponse()
                            .description(description)
                            .content(
                                new Content()
                                    .addMediaType(
                                        JSON,
                                        new MediaType()
                                            .schema(
                                                new Schema<>()
                                                    .$ref("#/components/schemas/ApiError"))))));
  }
}
