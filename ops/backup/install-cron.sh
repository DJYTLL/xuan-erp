#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${BACKUP_ENV_FILE:-$SCRIPT_DIR/.env}"
[[ -f "$ENV_FILE" ]] && { set -a; source "$ENV_FILE"; set +a; }

BACKUP_ROOT="${BACKUP_ROOT:-/data/xuan-erp/backup}"
SCRIPT_TARGET="${BACKUP_SCRIPT_TARGET:-$BACKUP_ROOT/scripts}"
CRON_LOG_DIR="$BACKUP_ROOT/logs"
CRON_LINE="0 2 * * * /bin/bash $SCRIPT_TARGET/backup-all.sh >> $CRON_LOG_DIR/cron.log 2>&1"
MARKER="# xuan-erp-backup"

usage() {
  cat <<'USAGE'
Usage: install-cron.sh

Installs the daily Xuan ERP backup cron job at 02:00.
Run this script on the Linux server after copying ops/backup scripts to the server.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

mkdir -p "$SCRIPT_TARGET" "$CRON_LOG_DIR"

if [[ "$SCRIPT_DIR" != "$SCRIPT_TARGET" ]]; then
  cp -a "$SCRIPT_DIR/." "$SCRIPT_TARGET/"
fi

TMP_CRON="$(mktemp)"
trap 'rm -f "$TMP_CRON"' EXIT

crontab -l 2>/dev/null | grep -v "$MARKER" > "$TMP_CRON" || true
{
  echo "$MARKER"
  echo "$CRON_LINE $MARKER"
} >> "$TMP_CRON"

crontab "$TMP_CRON"

echo "Installed cron:"
echo "$CRON_LINE"
