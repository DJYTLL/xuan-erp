#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${BACKUP_ENV_FILE:-$SCRIPT_DIR/.env}"
DRY_RUN=0

usage() {
  cat <<'USAGE'
Usage: backup-all.sh [--dry-run]

Runs PostgreSQL, Nacos, Redis, Elasticsearch, and config backups.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 2 ;;
  esac
  shift
done

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a; source "$ENV_FILE"; set +a
else
  echo "WARN: env file not found: $ENV_FILE. Using built-in defaults where possible." >&2
fi

BACKUP_ROOT="${BACKUP_ROOT:-/data/xuan-erp/backup}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d-%H%M%S)}"
BATCH_DIR="${BATCH_DIR:-$BACKUP_ROOT/runs/$RUN_ID}"
LOG_DIR="$BACKUP_ROOT/logs"
MANIFEST_FILE="$BATCH_DIR/manifest.md"

run_step() {
  local name="$1"
  local script="$2"

  echo "==> [$name] start"
  if [[ "$DRY_RUN" == "1" ]]; then
    "$script" --dry-run --run-id "$RUN_ID" --batch-dir "$BATCH_DIR" --manifest "$MANIFEST_FILE"
    echo "==> [$name] dry-run ok"
    return 0
  fi

  if "$script" --run-id "$RUN_ID" --batch-dir "$BATCH_DIR" --manifest "$MANIFEST_FILE"; then
    echo "==> [$name] ok"
  else
    local exit_code=$?
    echo "- ${name}: failed, exit=${exit_code}, time=$(date -Is)" >> "$MANIFEST_FILE"
    echo "==> [$name] failed, continue next step" >&2
  fi
}

if [[ "$DRY_RUN" == "1" ]]; then
  cat <<EOF
Dry run backup plan:
- env file: $ENV_FILE
- backup root: $BACKUP_ROOT
- batch dir: $BATCH_DIR
- logs dir: $LOG_DIR
- retention days: $RETENTION_DAYS
- steps: postgres, nacos, redis, elasticsearch, config
EOF
else
  mkdir -p "$BATCH_DIR" "$LOG_DIR"
  {
    echo "# Xuan ERP Backup Manifest"
    echo
    echo "- run_id: $RUN_ID"
    echo "- started_at: $(date -Is)"
    echo "- backup_root: $BACKUP_ROOT"
    echo
    echo "## Steps"
  } > "$MANIFEST_FILE"
fi

export BACKUP_ROOT RUN_ID BATCH_DIR MANIFEST_FILE DRY_RUN

run_step "postgres" "$SCRIPT_DIR/backup-postgres.sh"
run_step "nacos" "$SCRIPT_DIR/backup-nacos.sh"
run_step "redis" "$SCRIPT_DIR/backup-redis.sh"
run_step "elasticsearch" "$SCRIPT_DIR/backup-elasticsearch.sh"
run_step "config" "$SCRIPT_DIR/backup-config.sh"

if [[ "$DRY_RUN" == "0" ]]; then
  {
    echo
    echo "## Finish"
    echo
    echo "- finished_at: $(date -Is)"
    echo "- retention_days: $RETENTION_DAYS"
  } >> "$MANIFEST_FILE"

  if [[ "$RETENTION_DAYS" =~ ^[0-9]+$ ]] && [[ "$RETENTION_DAYS" -gt 0 ]]; then
    find "$BACKUP_ROOT/runs" -mindepth 1 -maxdepth 1 -type d -mtime +"$RETENTION_DAYS" -print -exec rm -rf {} \;
  fi

  echo "Backup finished: $BATCH_DIR"
else
  echo "Dry run finished. No backup files were written."
fi
