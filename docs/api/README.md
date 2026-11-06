# API spec — Phase 1

`openapi.yaml` trong thư mục này là **contract** của API Phase 1 (identity + catalog).

## Đọc spec ở đâu

- **Nhanh trong repo**: mở [`openapi.yaml`](openapi.yaml) — 1 file, `paths` sắp xếp theo alphabet nên
  diff theo phase rất dễ đọc.
- **UI tương tác** (khi muốn thử request): mở spec bằng Swagger Editor / Postman / Insomnia (import
  file YAML), hoặc chạy service local rồi mở `http://localhost:8081/swagger-ui` nếu bật UI.
- `servers[0].url = http://localhost:8080` — **gateway**, không phải port service. Client thật luôn
  gọi qua cửa vào duy nhất.

## Sinh lại spec (sau khi thêm/sửa endpoint)

```bash
docker compose -f infra/docker-compose.yml --env-file .env up -d   # 3 service phải healthy
bash tools/export-openapi.sh                                       # gom → docs/api/openapi.yaml
git add docs/api/openapi.yaml && git commit -m "docs(api): cap nhat OpenAPI spec"
```

Script lấy `/v3/api-docs` của **identity + catalog** rồi merge (gateway chỉ được kiểm 200, không merge
— nó là proxy catch-all `@Hidden`). Cuối script **tự validate**: `openapi` 3.x · `info.title` ·
`servers` · security scheme `bearerAuth` · schema `ApiError` · mọi operation có 400/401/403/404/409
trỏ về `ApiError`. Fail là exit code 1.

## Vì sao spec sinh từ code (không viết tay YAML)

Sinh từ code (springdoc) ⇒ spec **không thể lệch** với hành vi thật: endpoint mới tự xuất hiện, kiểu
Dữ liệu lấy từ chính DTO. Trade-off: spec là *hệ quả* của code, nên nếu sau này có client bên ngoài /
nhiều team thì nên chuyển **contract-first** (viết YAML trước, code theo) — ghi ở `Backlog.md → SL-1.8`
(revisit).

Bộ response lỗi (400/401/403/404/409) được gắn **tự động cho mọi operation** bằng `OperationCustomizer`
trong `packages/service-commons/.../openapi/OpenApiAutoConfiguration.java` — vì lỗi là hành vi của
*toàn hệ* (`GlobalExceptionHandler` + security filter chain), không phải của từng endpoint.

## Bảo mật

`/v3/api-docs` **chỉ bật ở local**. Cơ chế: `OpenApiEnvironmentPostProcessor` (commons) ép
`springdoc.api-docs.enabled=false` khi `APP_ENV != local`, trừ khi set tường minh
`SHOPLAB_OPENAPI_ENABLED=true`. Spec là bản đồ chi tiết hệ thống nên mặc định phải **tắt** ở môi
trường thật.
