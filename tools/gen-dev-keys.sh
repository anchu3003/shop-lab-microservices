#!/usr/bin/env bash
# ==============================================================================
# gen-dev-keys.sh — sinh cặp khoá RSA dùng cho DEV (JWT RS256) — ticket SL-1.1
#
# Vì sao phải là script chứ không commit sẵn khoá:
#   Private key KHÔNG được nằm trong git (AGENTS.md §9) — kể cả khoá dev, vì commit
#   nó lên là tạo thói quen sai và rất dễ bị copy sang môi trường thật.
#
# Kết quả:
#   infra/keys/dev-private.pem  (PKCS#8)  ← CHỈ identity-service đọc
#   infra/keys/dev-public.pem   (SPKI)    ← công bố qua JWKS cho gateway/catalog (SL-1.2+)
#
# Dùng:
#   bash tools/gen-dev-keys.sh            # sinh nếu chưa có (idempotent)
#   bash tools/gen-dev-keys.sh --force    # sinh lại (mọi token cũ hết hiệu lực)
# ==============================================================================

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
keys_dir="${repo_root}/infra/keys"
private_key="${keys_dir}/dev-private.pem"
public_key="${keys_dir}/dev-public.pem"

force="false"
if [ "${1:-}" = "--force" ]; then
  force="true"
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "gen-dev-keys: thiếu 'openssl' — cài rồi chạy lại (WSL/Ubuntu có sẵn)." >&2
  exit 1
fi

if [ -f "$private_key" ] && [ -f "$public_key" ] && [ "$force" != "true" ]; then
  echo "gen-dev-keys: đã có khoá dev, không ghi đè."
  echo "  private: ${private_key}"
  echo "  public : ${public_key}"
  echo "  Sinh lại (token cũ hết hiệu lực): bash tools/gen-dev-keys.sh --force"
  exit 0
fi

mkdir -p "$keys_dir"

# RSA 2048, PKCS#8 (Java đọc trực tiếp, không cần convert).
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$private_key"
# Public key dạng SPKI — đúng dạng mà JWKS công bố (n, e).
openssl rsa -in "$private_key" -pubout -out "$public_key" 2>/dev/null

# Kiểm tra thật sự sinh được, thay vì tin vào exit code.
for f in "$private_key" "$public_key"; do
  if [ ! -s "$f" ] || ! head -n 1 "$f" | grep -q 'BEGIN'; then
    echo "gen-dev-keys: file '$f' không hợp lệ." >&2
    exit 1
  fi
done

# Private key chỉ chủ sở hữu đọc được.
chmod 600 "$private_key"
chmod 644 "$public_key"

echo "gen-dev-keys: xong."
echo "  private (KHÔNG commit): ${private_key}"
echo "  public                 : ${public_key}"
echo
# Container chạy non-root; private key mode 600 nên container PHẢI chạy cùng uid/gid với user
# này, nếu không sẽ bị AccessDeniedException. In ra sẵn 2 dòng để copy vào .env.
echo "Thêm vào .env (để container đọc được private key mode 600):"
echo "  SHOPLAB_UID=$(id -u)"
echo "  SHOPLAB_GID=$(id -g)"
