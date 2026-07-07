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

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-xuan-pgsql}"
POSTGRES_HOST="${POSTGRES_HOST:-127.0.0.1}"
POSTGRES_PORT="${POSTGRES_PORT:-9042}"
POSTGRES_USER="${POSTGRES_USER:-xuan}"
POSTGRES_DB="${POSTGRES_DB:-xuan_erp}"
OUT_DIR="$BATCH_DIR/postgres"
OUT_FILE="$OUT_DIR/${POSTGRES_DB}-${RUN_ID}.dump"
GZIP_FILE="$OUT_FILE.gz"

append_manifest() {
  [[ "$DRY_RUN" == "1" ]] && return 0
  echo "- postgres: $1, file=$GZIP_FILE, time=$(date -Is)" >> "$MANIFEST_FILE"
}

if [[ "$DRY_RUN" == "1" ]]; then
  echo "Would run pg_dump -F c for database $POSTGRES_DB into $OUT_FILE"
  exit 0
fi

mkdir -p "$OUT_DIR"

if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -Fxq "$POSTGRES_CONTAINER"; then
  docker exec "$POSTGRES_CONTAINER" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -F c > "$OUT_FILE"
elif command -v pg_dump >/dev/null 2>&1; then
  export PGPASSWORD="${POSTGRES_PASSWORD:-}"
  pg_dump -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -F c -f "$OUT_FILE"
else
  append_manifest "skipped, pg_dump not found and container unavailable"
  exit 0
fi

gzip -f "$OUT_FILE"
append_manifest "success"
