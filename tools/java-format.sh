#!/usr/bin/env bash
# java-format.sh — Spotless apply trên các file .java đã staged
#
# Dùng bởi lefthook (hook pre-commit).
# Tự bỏ qua (exit 0) nếu chưa có JDK/Maven trong WSL để repo vẫn dùng được
# trước khi cài tooling (xem README.md → Yêu cầu môi trường).

set -euo pipefail

if ! command -v java >/dev/null 2>&1; then
  echo "java-format: bỏ qua — chưa có 'java' trên PATH (xem README.md)." >&2
  exit 0
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "java-format: bỏ qua — chưa có 'mvn' trên PATH (xem README.md)." >&2
  exit 0
fi

if [ ! -f pom.xml ]; then
  echo "java-format: bỏ qua — chưa có pom.xml." >&2
  exit 0
fi

if [ "$#" -eq 0 ]; then
  echo "java-format: không có file .java nào staged." >&2
  exit 0
fi

# Spotless chỉ format các file được chỉ định (tránh format cả repo mỗi lần commit)
mvn -q -DspotlessFiles="$(printf '%s' "$1")" spotless:apply
