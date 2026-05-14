#!/usr/bin/env bash

set -euo pipefail

check() {
  local label="$1"
  shift

  echo
  echo "==> $label"
  "$@" 2>/dev/null | sed -n \
    -e '1p' \
    -e '/^[Xx]-[Jj]iandou-[Gg]ateway-[Rr]oute:/p' \
    -e '/^[Ll]ocation:/p' \
    -e '/^[Ss]erver:/p'
}

check_dns() {
  local domain="$1"
  local answer

  answer="$(
    curl -fsS --connect-timeout 5 --max-time 10 \
      "https://cloudflare-dns.com/dns-query?name=$domain&type=A" \
      -H "accept: application/dns-json" \
      | sed -n 's/.*"data":"\([0-9.]*\)".*/\1/p' \
      | tr '\n' ' '
  )"
  echo
  echo "==> dns $domain"
  echo "$answer"
  if echo "$answer" | grep -Eq '(^| )198\.18\.'; then
    echo "WARN: $domain resolves to 198.18.0.0/15, which is not a normal public origin or Cloudflare edge address."
  fi
}

check_dns "jiandou.org"
check_dns "admin.jiandou.org"

check "docker gateway web host" \
  curl -I --connect-timeout 5 --max-time 10 -H "Host: jiandou.org" http://127.0.0.1/

check "docker gateway admin host" \
  curl -I --connect-timeout 5 --max-time 10 -H "Host: admin.jiandou.org" http://127.0.0.1/

check "docker gateway admin port" \
  curl -I --connect-timeout 5 --max-time 10 http://127.0.0.1:5174/

check "docker gateway api v3 health" \
  curl -I --connect-timeout 5 --max-time 10 -H "Host: jiandou.org" http://127.0.0.1/api/v3/health

check "docker gateway openapi docs" \
  curl -I --connect-timeout 5 --max-time 10 -H "Host: jiandou.org" http://127.0.0.1/v3/api-docs

check "public web domain" \
  curl -I --connect-timeout 8 --max-time 15 https://jiandou.org/

check "public admin domain" \
  curl -I --connect-timeout 8 --max-time 15 https://admin.jiandou.org/

echo
echo "Expected routes:"
echo "  jiandou.org on 127.0.0.1:80        -> X-Jiandou-Gateway-Route: web-domain"
echo "  admin.jiandou.org on 127.0.0.1:80  -> X-Jiandou-Gateway-Route: admin-domain"
echo "  127.0.0.1:5174                     -> X-Jiandou-Gateway-Route: admin-port"
echo "  /api/v3/health                     -> proxied to api:8000/api/v3/health"
echo "  /v3/api-docs                       -> proxied to api:8000/v3/api-docs"
echo "  https://admin.jiandou.org          -> X-Jiandou-Gateway-Route: admin-domain when Cloudflare uses HTTP origin"
