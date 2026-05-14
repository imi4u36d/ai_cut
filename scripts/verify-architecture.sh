#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MAX_LINES="${ARCH_CHECK_MAX_LINES:-40}"
STRICT="${ARCH_CHECK_STRICT:-0}"
status=0

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1"
    exit 1
  fi
}

print_matches() {
  local title="$1"
  local matches="$2"

  echo "==> $title"
  if [[ -z "$matches" ]]; then
    echo "OK: 未发现命中"
    echo
    return
  fi

  local count
  count="$(printf '%s\n' "$matches" | sed '/^$/d' | wc -l | tr -d ' ')"
  echo "发现 $count 处命中，展示前 $MAX_LINES 行:"
  printf '%s\n' "$matches" | sed -n "1,${MAX_LINES}p"
  if (( count > MAX_LINES )); then
    echo "... 还有 $((count - MAX_LINES)) 行，设置 ARCH_CHECK_MAX_LINES 可调整展示数量。"
  fi
  echo
}

require_command rg

echo "JianDou 架构迁移守卫"
echo

backend_blocking="$(
  rg -n --glob 'apps/api-spring/**/src/main/java/**/*.java' \
    --glob '!apps/api-spring/api-boot/src/main/java/**' \
    'Map<String,\s*Object>' . || true
)"
backend_legacy="$(
  rg -n --glob 'apps/api-spring/api-boot/src/main/java/**/*.java' \
    'Map<String,\s*Object>' . || true
)"

print_matches "后端生产代码 Map<String, Object>（阻断：api-boot 之外）" "$backend_blocking"
if [[ -n "$backend_blocking" ]]; then
  echo "FAIL: 新模块生产代码不应继续引入 Map<String, Object>，请改为明确 DTO/value object。"
  echo
  status=1
fi

print_matches "后端生产代码 Map<String, Object>（历史白名单：api-boot）" "$backend_legacy"
if [[ -n "$backend_legacy" ]]; then
  echo "WARN: api-boot 当前作为历史迁移白名单，仅展示不阻断。"
  echo
fi

frontend_matches="$(
  rg -n --glob 'apps/web/src/**/*.{ts,vue}' --glob 'apps/admin/src/**/*.{ts,vue}' \
    -e '\bany\b' \
    -e 'Record<string,\s*unknown>' . || true
)"
print_matches "前端业务代码 any / Record<string, unknown>" "$frontend_matches"
if [[ -n "$frontend_matches" ]]; then
  echo "FAIL: 前端业务代码仍有宽类型，请逐步收敛到明确接口或窄化 helper。"
  echo
  status=1
fi

api_v2_matches="$(
  rg -n --glob '!**/node_modules/**' \
    --glob '!**/dist/**' \
    --glob '!**/target/**' \
    --glob '!**/.git/**' \
    --glob '!package-lock.json' \
    --glob '!npm-shrinkwrap.json' \
    --glob '!pnpm-lock.yaml' \
    --glob '!yarn.lock' \
    --glob '!scripts/verify-architecture.sh' \
    '/api/v2' . \
    | grep -Ev "https?://[^[:space:]\"']*/api/v2|/api/v2/apps/protocols/compatible-mode/v1" || true
)"
print_matches "仓库内非第三方 /api/v2 残留（第三方 http(s) URL 已忽略）" "$api_v2_matches"
if [[ -n "$api_v2_matches" ]]; then
  echo "FAIL: 仍存在 /api/v2 残留，请迁移到 /api/v3 或删除过期引用。"
  echo
  status=1
fi

if (( status == 0 )); then
  echo "架构检查通过。"
else
  echo "架构检查发现迁移遗留项。当前默认是报告模式，输出中的 FAIL 项需要后续清理。"
  if [[ "$STRICT" == "1" ]]; then
    echo "严格模式已启用，返回失败。"
    exit "$status"
  fi
fi

exit 0
