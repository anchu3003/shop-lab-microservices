#!/usr/bin/env bash
# ==============================================================================
# export-openapi.sh — gom spec OpenAPI của các service → docs/api/openapi.yaml (SL-1.8)
#
# Chạy (stack local đang chạy, các service healthy):
#   bash tools/export-openapi.sh
#
# Vì sao export ra 1 file thay vì để mỗi service tự host spec:
#   - `docs/api/openapi.yaml` nằm trong git ⇒ **diff được theo phase** (Phase 2 nhìn vào thấy API
#     mới thêm gì), review PR thấy thay đổi contract, không cần dựng stack mới đọc được.
#   - `servers` trỏ về **gateway** (cửa vào duy nhất) — spec mô tả cách client thật gọi API.
#
# Vì sao KHÔNG merge spec của gateway: gateway là proxy catch-all (`@Hidden`), không sở hữu API
# nghiệp vụ nào ⇒ merge vào chỉ gây nhiễu. Script vẫn kiểm `/v3/api-docs` của gateway trả 200
# (điều kiện AC: cả 3 service expose endpoint này).
#
# Định dạng: `python3` + `pyyaml` (đã kiểm: 6.0.3). KHÔNG dùng `jq` (máy không có).
# ==============================================================================
set -euo pipefail

IDENTITY="${IDENTITY:-http://localhost:8081}"
CATALOG="${CATALOG:-http://localhost:8082}"
GATEWAY="${GATEWAY:-http://localhost:8080}"
OUT="docs/api/openapi.yaml"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

fetch() {
  local name="$1" url="$2" code
  code="$(curl -s -o "$TMP_DIR/$name.json" -w '%{http_code}' "$url" || true)"
  if [ "$code" != "200" ]; then
    echo "LỖI: $url trả $code — service chưa chạy hoặc đã tắt OpenAPI (APP_ENV != local?)" >&2
    echo "     kiểm tra: curl -s -o /dev/null -w '%{http_code}\\n' $url · docker ps" >&2
    exit 1
  fi
  echo "  OK $name ← $url"
}

echo "== Lấy spec của từng service"
fetch identity "$IDENTITY/v3/api-docs"
fetch catalog "$CATALOG/v3/api-docs"
fetch gateway "$GATEWAY/v3/api-docs"

echo "== Merge + validate → $OUT"
mkdir -p docs/api
python3 - "$TMP_DIR" "$OUT" <<'PY'
import json
import pathlib
import sys

import yaml

tmp = pathlib.Path(sys.argv[1])
out = pathlib.Path(sys.argv[2])

identity = json.loads((tmp / "identity.json").read_text(encoding="utf-8"))
catalog = json.loads((tmp / "catalog.json").read_text(encoding="utf-8"))

merged = {
    "openapi": "3.0.1",
    "info": {
        "title": "ShopLab API — Phase 1",
        "version": "v1",
        "description": (
            "API của walking skeleton: identity (auth/RBAC) + catalog (product/SKU).\n\n"
            "Sinh tự động bằng `bash tools/export-openapi.sh` — KHÔNG sửa file này bằng tay.\n\n"
            "Mọi lỗi dùng schema `ApiError` (có `traceId` để grep log): "
            "401 = chưa xác thực · 403 = thiếu quyền · 503 = dependency chết (retry được)."
        ),
    },
    "servers": [
        {
            "url": "http://localhost:8080",
            "description": "api-gateway — cửa vào duy nhất (verify JWT, route tới service)",
        }
    ],
    "tags": [],
    "paths": {},
    "components": {"schemas": {}, "securitySchemes": {}},
}

for spec in (identity, catalog):
    merged["paths"].update(spec.get("paths", {}))
    components = spec.get("components", {})
    for name, schema in components.get("schemas", {}).items():
        existing = merged["components"]["schemas"].get(name)
        if existing is not None and existing != schema:
            raise SystemExit(f"LỖI: schema '{name}' khác nhau giữa 2 service — thống nhất ở commons")
        merged["components"]["schemas"][name] = schema
    merged["components"]["securitySchemes"].update(components.get("securitySchemes", {}))
    merged["tags"].extend(tag for tag in spec.get("tags", []) if tag not in merged["tags"])

# Sắp xếp để file **ổn định** giữa các lần export (git diff chỉ hiện thay đổi thật).
def sorted_operation(operation):
    """Sort cả `responses` — thứ tự mã lỗi không được phụ thuộc thứ tự map của service."""
    responses = operation.get("responses")
    if responses:
        operation["responses"] = dict(sorted(responses.items()))
    return operation


merged["paths"] = {
    path: {
        method: sorted_operation(merged["paths"][path][method])
        for method in sorted(merged["paths"][path])
    }
    for path in sorted(merged["paths"])
}
merged["components"]["schemas"] = dict(sorted(merged["components"]["schemas"].items()))
merged["tags"] = sorted(merged["tags"], key=lambda tag: tag.get("name", ""))

out.write_text(
    yaml.safe_dump(merged, sort_keys=False, allow_unicode=True, width=100), encoding="utf-8"
)
PY

# ----------------------------- validate (AC) -----------------------------
# Cách validate: `python3` + `pyyaml` (máy không có spectral/swagger-cli và không cài thêm được
# offline). Kiểm: khoá bắt buộc, security scheme bearer, schema ApiError cho 400/401/403/404/409.
python3 - "$OUT" <<'PY'
import pathlib
import sys

import yaml

spec = yaml.safe_load(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8"))
errors = []

if not str(spec.get("openapi", "")).startswith("3."):
    errors.append("thiếu `openapi` (phải 3.x)")
if "title" not in spec.get("info", {}):
    errors.append("thiếu `info.title`")
if not spec.get("servers"):
    errors.append("thiếu `servers` (phải trỏ gateway)")

schemes = spec.get("components", {}).get("securitySchemes", {})
if schemes.get("bearerAuth", {}).get("scheme") != "bearer":
    errors.append("thiếu security scheme `bearerAuth` kiểu http/bearer")

schemas = spec.get("components", {}).get("schemas", {})
if "ApiError" not in schemas:
    errors.append("thiếu schema `ApiError`")

operations = 0
protected = 0
for path, item in spec.get("paths", {}).items():
    if not path.startswith("/"):
        errors.append(f"path không hợp lệ: {path}")
    for method, operation in item.items():
        operations += 1
        if operation.get("security"):
            protected += 1
        responses = operation.get("responses") or {}
        for code in ("400", "401", "403", "404", "409"):
            if code not in responses:
                errors.append(f"{method.upper()} {path} thiếu response {code}")
                continue
            ref = (
                responses[code]
                .get("content", {})
                .get("application/json", {})
                .get("schema", {})
                .get("$ref", "")
            )
            if not ref.endswith("/ApiError"):
                errors.append(f"{method.upper()} {path} → {code} không dùng schema ApiError")

if errors:
    print("VALIDATE FAIL:", file=sys.stderr)
    for error in errors:
        print("  -", error, file=sys.stderr)
    raise SystemExit(1)

print(
    f"VALIDATE PASS · {len(spec.get('paths', {}))} path · {operations} operation"
    f" · {protected} operation cần token · {len(schemas)} schema"
)
PY

echo "== Xong: $OUT"
git --no-pager diff --stat -- "$OUT" || true
