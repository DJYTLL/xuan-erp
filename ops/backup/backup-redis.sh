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

REDIS_CONTAINER="${REDIS_CONTAINER:-xuan-erp-redis}"
REDIS_DATA_DIR="${REDIS_DATA_DIR:-/data/xuan-erp/data/redis}"
REDIS_RUN_SAVE="${REDIS_RUN_SAVE:-false}"
OUT_DIR="$BATCH_DIR/redis"
OUT_FILE="$OUT_DIR/redis-persistence-${RUN_ID}.tar.gz"

append_manifest() {
  [[ "$DRY_RUN" == "1" ]] && return 0
  echo "- redis: $1, file=$OUT_FILE, time=$(date -Is)" >> "$MANIFEST_FILE"
}

if [[ "$DRY_RUN" == "1" ]]; then
  echo "Would archive Redis persistence files from $REDIS_DATA_DIR into $OUT_FILE"
  exit 0
fi

mkdir -p "$OUT_DIR"

if [[ "$REDIS_RUN_SAVE" == "true" ]] && command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -Fxq "$REDIS_CONTAINER"; then
  docker exec "$REDIS_CONTAINER" redis-cli SAVE >/dev/null
fi

if [[ -d "$REDIS_DATA_DIR" ]]; then
  tar -czf "$OUT_FILE" -C "$REDIS_DATA_DIR" .
  append_manifest "success"
else
  echo "Redis data dir not found: $REDIS_DATA_DIR" > "$OUT_DIR/README.txt"
  append_manifest "skipped, redis data dir not found"
fi
