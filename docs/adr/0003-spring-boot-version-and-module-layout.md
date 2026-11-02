# ADR-0003 — Spring Boot 4.1.2 và layout Maven multi-module

| | |
|---|---|
| **Trạng thái** | Accepted |
| **Ngày** | 2026-11-02 |
| **Liên quan** | `Backlog.md` → `SL-0.4`; ADR-0001 (tech stack); `architecture.md` §11.1 |

---

## Context

ADR-0001 đã chốt **Java 21 + Spring Boot** là ngôn ngữ chính nhưng **không pin version** Spring Boot.
`SL-0.4` là ticket code Java đầu tiên → phải chốt version ngay, vì mọi service Phase 1 sẽ thừa hưởng.

Ràng buộc:

- Repo **mới hoàn toàn** — không có code/service nào phải migrate → chi phí đổi version bây giờ gần bằng 0,
  nhưng sẽ tăng theo số service.
- Parent POM hiện tại (`SL-0.2`) đang giữ convention riêng: Spotless + google-java-format, Checkstyle,
  Lefthook. Không muốn mất những thứ đó.
- Cần **shared library** dùng chung giữa nhiều service (`packages/service-commons`) → bắt buộc phải là
  Maven multi-module, không thể để mỗi service một POM rời.
- Mục tiêu repo là **học architecture**, không phải chạy đua công nghệ mới nhất.

## Options

### 1. Version Spring Boot

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **4.1.2** (latest stable) | Repo mới, không có gì phải migrate; còn được hỗ trợ; không phải nâng cấp lớn giữa Phase 1 | Ít tutorial hơn 3.x; một số artifact đã đổi tên (`spring-boot-starter-web` → `-webmvc`, `TestRestTemplate` bị chuyển khỏi `spring-boot-test`) → dễ mất thời gian tra cứu | Repo mới, ưu tiên không phải migrate sau |
| **3.5.16** (dòng 3.5.x cuối) | Tài liệu/plugin phong phú nhất, ít friction nhất khi tra Google | Là dòng cũ; sẽ phải nâng lên 4.x giữa roadmap → đúng lúc đang bận học pattern | Có code cũ cần giữ, hoặc plugin bên thứ ba chưa hỗ trợ 4.x |
| **4.0.9** (dòng 4.0.x) | Trung dung: 4.x nhưng ổn định lâu hơn 4.1 | Vẫn là 4.x (cùng mức độ ít tutorial) mà lại cũ hơn 4.1 → không có lý do rõ ràng | Khi cần né bug mới của 4.1 |

### 2. Cách thừa hưởng cấu hình Spring Boot

| Phương án | Ưu | Nhược | Khi nào dùng |
|---|---|---|---|
| **Đổi parent sang `spring-boot-starter-parent`** | Được cấu hình sẵn: `-parameters`, surefire, resource filtering, `spring-boot-maven-plugin` executions | Mất quyền kiểm soát parent; convention riêng (Spotless/Checkstyle của `SL-0.2`) phải nhét vào parent khác hoặc lặp lại | Repo mà mọi thứ đều là Spring Boot, không có convention riêng |
| **Giữ parent riêng + import `spring-boot-dependencies` BOM** | Giữ được convention đang có; BOM quản version dependency tập trung; đổi version = sửa **1 property** | **Phải tự bật lại những gì starter-parent làm sẵn** — đã vấp 2 lần ngay ở `SL-0.4` (xem Consequences) | Repo multi-module có convention riêng (trường hợp này) |

## Decision

**1. Spring Boot 4.1.2.** Repo mới hoàn toàn nên không có chi phí migrate, và bắt đầu trên dòng đang được
hỗ trợ tốt hơn là cố ý chọn dòng cũ rồi phải nâng cấp giữa roadmap.

**Lý do loại phương án khác:**

- **3.5.16** — loại: friction khi tra cứu thấp hơn là lợi ích ngắn hạn, nhưng cái giá là một lần nâng major
  version vào giữa Phase 1–2, đúng lúc đang tập trung vào pattern. Đổi bây giờ (0 service) rẻ hơn nhiều.
- **4.0.9** — loại: cùng mức độ ít tutorial như 4.1 nhưng lại cũ hơn, không có ưu điểm bù lại.

**2. Giữ parent POM riêng, import BOM.**

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>${spring-boot.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

**Lý do loại `spring-boot-starter-parent`:** parent hiện tại đang giữ Spotless + Checkstyle + cấu hình
compiler; đổi parent sẽ phải di chuyển toàn bộ convention đó. Import BOM cho đúng thứ cần (quản version
dependency) mà không mất gì.

**3. Maven multi-module.**

```
pom.xml                        (parent: modules, BOM, pluginManagement)
├── packages/service-commons/  shoplab-service-commons  (jar — hạ tầng dùng chung)
└── services/service-template/ shoplab-service-template (jar — service mẫu)
```

Version nội bộ dùng `${project.version}` trong `dependencyManagement` → không bao giờ lệch version giữa
các module.

## Consequences

- `architecture.md` §11.1 pin **Spring Boot 4.1.2** (trước đó chỉ ghi "Java 21 + Spring Boot").
- **Đổi version sau này = sửa 1 property** `<spring-boot.version>` trong parent POM.
- ⚠️ **Không dùng `spring-boot-starter-parent` nghĩa là phải tự bật những gì nó làm sẵn.** Hai cái đã vấp
  ngay ở `SL-0.4`, cả hai đều **không lộ ra lúc compile**:
  - **`-parameters` của javac**: thiếu → `@RequestParam long ms` chết lúc runtime với
    `Name for argument of type [long] not specified`. Đã bật trong `maven-compiler-plugin`.
  - **`maven-surefire-plugin` version**: super-POM của Maven để mặc định bản **2.17**, không hỗ trợ JUnit
    Platform → **`Tests run: 0` mà vẫn `BUILD SUCCESS`**. Đã pin `3.6.1`. Đây là kiểu lỗi nguy hiểm nhất:
    pipeline xanh nhưng không test gì.
- **Artifact đã đổi tên ở Spring Boot 4** — tốn thời gian tra cứu nếu không biết:
  - `spring-boot-starter-web` → **deprecated**, dùng `spring-boot-starter-webmvc`.
  - `TestRestTemplate` **không còn** trong `spring-boot-test`; `@AutoConfigureMockMvc` cũng không còn ở
    `spring-boot-test-autoconfigure`. `SL-0.4` né hẳn bằng `@SpringBootTest(RANDOM_PORT)` + `@LocalServerPort`
    + `java.net.http.HttpClient` thuần, và `MockMvcBuilders.standaloneSetup` cho unit test.
  - File auto-configuration vẫn là `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- **Thêm module mới** = thêm `<module>` vào parent + thêm `<dependency>` cho `shoplab-service-commons` nếu cần.
- Mỗi service Java thêm ~150 MiB RAM khi chạy (JVM idle) — cộng vào baseline hạ tầng ~150 MiB.
- `pom.xml` của parent phình thêm ~60 dòng cấu hình build tự viết (compiler, surefire, spotless, checkstyle,
  jacoco, spring-boot-maven-plugin) — đây là **cái giá đã biết** của việc giữ parent riêng.

## Revisit

- Revisit version Spring Boot khi **plugin bên thứ ba bắt buộc** (đã dự kiến: Testcontainers ở Phase 1–2,
  OpenTelemetry agent ở Phase 6) **chưa hỗ trợ 4.x** — điều kiện: có ít nhất 1 plugin không có bản tương thích
  4.x và không có workaround trong vòng 1 ngày.
- Revisit việc dùng parent riêng khi **số dòng cấu hình build tự viết vượt 100 dòng** (hiện ~60 dòng) —
  lúc đó chi phí bảo trì parent riêng có thể đã lớn hơn lợi ích.
- Revisit layout multi-module khi **số module > 10** hoặc build toàn repo vượt **2 phút** trên máy dev →
  chuyển mặc định sang build có chọn lọc (`-pl ... -am`) trong tài liệu + CI.
