package com.shoplab.commons.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validate biến môi trường bắt buộc, chạy <b>trước</b> khi Spring khởi động.
 *
 * <p>Vì sao không để Spring {@code @ConfigurationProperties} + Bean Validation làm việc này: cái đó
 * chỉ báo lỗi khi bean được bind, message bị bọc trong {@code ConfigurationPropertiesBindException}
 * rất khó đọc, và app đã đi được một đoạn khởi động. Validate ở {@code main()} thì chết trước khi
 * có gì được tạo.
 *
 * <p>Cố ý là class thuần (không phụ thuộc Spring) để test được bằng JUnit thường, không cần
 * {@code @SpringBootTest} — test nhanh hơn và không cần context.
 */
public final class EnvValidator {

  /** Ném {@link MissingConfigException} nếu có biến bắt buộc bị thiếu/rỗng. */
  public static void validate(Map<String, String> env, List<RequiredConfig> required) {
    List<RequiredConfig> missing = findMissing(env, required);
    if (!missing.isEmpty()) {
      throw new MissingConfigException(missing);
    }
  }

  /** Trả về danh sách biến bắt buộc bị thiếu/rỗng; rỗng nghĩa là OK. */
  public static List<RequiredConfig> findMissing(
      Map<String, String> env, List<RequiredConfig> required) {
    List<RequiredConfig> missing = new ArrayList<>();
    for (RequiredConfig item : required) {
      String value = env.get(item.key());
      if (value == null || value.isBlank()) {
        missing.add(item);
      }
    }
    return List.copyOf(missing);
  }

  private EnvValidator() {
    throw new AssertionError("utility class — không khởi tạo");
  }
}
