#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${BACKUP_ENV_FILE:-$SCRIPT_DIR/.env}"
[[ -f "$ENV_FILE" ]] && { set -a; source "$ENV_FILE"; set +a; }

RUN_ID="${RUN_ID:-$(date +%Y%m%d-%H%M%S)}"
BATCH_DIR="${BATCH_DIR:-${BACKUP_ROOT:-/data/xuan-erp/backup}/runs/$RUN_ID}"
MANIFEST_FILE="${MANIFEST_FILE:-$BATCH_DIR/manifest.md}"
DRY_RUN="${DRY_RUN:-0}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --run-id) RUN_ID="$2"; shift ;;
    --batch-dir) BATCH_DIR="$2"; shift ;;
    --manifest) MANIFEST_FILE="$2"; shift ;;
    --dry-run) DRY_RUN=1 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
  shift
done

NACOS_BASE_URL="${NACOS_BASE_URL:-http://duaoyunxuan.com:9041}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-prod}"
NACOS_GROUP="${NACOS_GROUP:-XUAN_ERP_GROUP}"
NACOS_DATA_IDS="${NACOS_DATA_IDS:-}"
OUT_DIR="$BATCH_DIR/nacos"

append_manifest() {
  [[ "$DRY_RUN" == "1" ]] && return 0
  echo "- nacos: $1, dir=$OUT_DIR, time=$(date -Is)" >> "$MANIFEST_FILE"
}

if [[ "$DRY_RUN" == "1" ]]; then
  echo "Would export Nacos configs from $NACOS_BASE_URL namespace=$NACOS_NAMESPACE group=$NACOS_GROUP dataIds=${NACOS_DATA_IDS:-<not configured>}"
  exit 0
fi

mkdir -p "$OUT_DIR"

if [[ -z "$NACOS_DATA_IDS" ]]; then
  cat > "$OUT_DIR/README.txt" <<EOF
NACOS_DATA_IDS is not configured.
Set it in the server-side .env, for example:
NACOS_DATA_IDS=xuan-common.yaml,xuan-gateway.yaml,xuan-product.yaml
EOF
  append_manifest "skipped, NACOS_DATA_IDS not configured"
  exit 0
fi

if ! command -v curl >/dev/null 2>&1; then
  append_manifest "skipped, curl not found"
  exit 0
fi

TOKEN_QUERY=""
if [[ -n "${NACOS_PASSWORD:-}" ]]; then
  LOGIN_RESPONSE="$(curl -fsS -X POST "$NACOS_BASE_URL/nacos/v1/auth/users/login" \
    -d "username=$NACOS_USERNAME" \
    -d "password=$NACOS_PASSWORD" || true)"
  ACCESS_TOKEN="$(printf '%s' "$LOGIN_RESPONSE" | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
  [[ -n "$ACCESS_TOKEN" ]] && TOKEN_QUERY="&accessToken=$ACCESS_TOKEN"
fi

IFS=',' read -r -a DATA_IDS <<< "$NACOS_DATA_IDS"
EXPORTED=0
for raw_data_id in "${DATA_IDS[@]}"; do
  data_id="$(echo "$raw_data_id" | xargs)"
  [[ -z "$data_id" ]] && continue
  target="$OUT_DIR/$data_id"
  mkdir -p "$(dirname "$target")"
  if curl -fsS "$NACOS_BASE_URL/nacos/v1/cs/configs?dataId=$data_id&group=$NACOS_GROUP&tenant=$NACOS_NAMESPACE$TOKEN_QUERY" -o "$target"; then
    EXPORTED=$((EXPORTED + 1))
  else
    echo "Failed to export $data_id" > "$target.export-error.txt"
  fi
done

append_manifest "success, exported=$EXPORTED"
