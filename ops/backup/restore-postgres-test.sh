#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${BACKUP_ENV_FILE:-$SCRIPT_DIR/.env}"
[[ -f "$ENV_FILE" ]] && { set -a; source "$ENV_FILE"; set +a; }

BACKUP_ROOT="${BACKUP_ROOT:-/data/xuan-erp/backup}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-xuan-pgsql}"
POSTGRES_HOST="${POSTGRES_HOST:-127.0.0.1}"
POSTGRES_PORT="${POSTGRES_PORT:-9042}"
POSTGRES_USER="${POSTGRES_USER:-xuan}"
POSTGRES_RESTORE_TEST_DB="${POSTGRES_RESTORE_TEST_DB:-xuan_erp_restore_test}"
DUMP_FILE=""

usage() {
  cat <<'USAGE'
Usage: restore-postgres-test.sh [--dump /path/to/db.dump.gz]

Restores a PostgreSQL dump into a temporary test database to verify backup usability.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dump) DUMP_FILE="$2"; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 2 ;;
  esac
  shift
done

if [[ -z "$DUMP_FILE" ]]; then
  DUMP_FILE="$(find "$BACKUP_ROOT/runs" -path '*/postgres/*.dump.gz' -type f -printf '%T@ %p\n' 2>/dev/null | sort -nr | awk 'NR==1 {print $2}')"
fi

if [[ -z "$DUMP_FILE" || ! -f "$DUMP_FILE" ]]; then
  echo "PostgreSQL dump not found. Use --dump /path/to/file.dump.gz" >&2
  exit 1
fi

TMP_DUMP="/tmp/xuan-erp-restore-${RANDOM}.dump"
gzip -dc "$DUMP_FILE" > "$TMP_DUMP"
trap 'rm -f "$TMP_DUMP"' EXIT

if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -Fxq "$POSTGRES_CONTAINER"; then
  docker cp "$TMP_DUMP" "$POSTGRES_CONTAINER:/tmp/xuan-erp-restore.dump"
  docker exec "$POSTGRES_CONTAINER" dropdb -U "$POSTGRES_USER" --if-exists "$POSTGRES_RESTORE_TEST_DB"
  docker exec "$POSTGRES_CONTAINER" createdb -U "$POSTGRES_USER" "$POSTGRES_RESTORE_TEST_DB"
  docker exec "$POSTGRES_CONTAINER" pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_RESTORE_TEST_DB" --clean --if-exists /tmp/xuan-erp-restore.dump
  docker exec "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_RESTORE_TEST_DB" -c 'select 1;'
elif command -v pg_restore >/dev/null 2>&1; then
  export PGPASSWORD="${POSTGRES_PASSWORD:-}"
  dropdb -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" --if-exists "$POSTGRES_RESTORE_TEST_DB"
  createdb -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" "$POSTGRES_RESTORE_TEST_DB"
  pg_restore -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_RESTORE_TEST_DB" --clean --if-exists "$TMP_DUMP"
  psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_RESTORE_TEST_DB" -c 'select 1;'
else
  echo "Neither docker PostgreSQL container nor local pg_restore is available." >&2
  exit 1
fi

echo "PostgreSQL restore test passed: $POSTGRES_RESTORE_TEST_DB"
