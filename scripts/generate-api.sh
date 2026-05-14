#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_DOCS_URL="${API_DOCS_URL:-http://127.0.0.1:8000/v3/api-docs}"
OPENAPI_JSON="${OPENAPI_OUTPUT:-$ROOT_DIR/docs/openapi.json}"
OPENAPI_TYPES="${OPENAPI_TYPES_OUTPUT:-$ROOT_DIR/packages/api-client/src/generated/schema.ts}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1"
    exit 1
  fi
}

require_command curl

mkdir -p "$(dirname "$OPENAPI_JSON")" "$(dirname "$OPENAPI_TYPES")"

tmp_file="$(mktemp)"
trap 'rm -f "$tmp_file"' EXIT

echo "生成 OpenAPI:"
echo "  source: $API_DOCS_URL"
echo "  json:   $OPENAPI_JSON"
echo "  types:  $OPENAPI_TYPES"

if ! curl -fsS "$API_DOCS_URL" -o "$tmp_file"; then
  echo "无法读取 OpenAPI 文档，请确认 Spring Boot 已启动且 $API_DOCS_URL 可访问。"
  exit 1
fi

if ! grep -q '"openapi"' "$tmp_file"; then
  echo "OpenAPI 响应中未找到 openapi 字段，请确认 Spring Boot 已启动且 /v3/api-docs 可访问。"
  exit 1
fi

mv "$tmp_file" "$OPENAPI_JSON"
echo "OpenAPI JSON 已写入 $OPENAPI_JSON"

if command -v npx >/dev/null 2>&1; then
  npx --yes openapi-typescript@7.13.0 "$OPENAPI_JSON" -o "$OPENAPI_TYPES"
  echo "OpenAPI TypeScript 类型已写入 $OPENAPI_TYPES"
else
  echo "WARN: 未找到 npx，已跳过 TypeScript 类型生成。"
fi
