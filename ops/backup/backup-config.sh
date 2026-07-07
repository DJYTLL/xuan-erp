#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
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

COMPOSE_ROOT="${COMPOSE_ROOT:-/data/xuan-erp/compose}"
NGINX_CONFIG_DIR="${NGINX_CONFIG_DIR:-/etc/nginx}"
CADDY_CONFIG_DIR="${CADDY_CONFIG_DIR:-/etc/caddy}"
OUT_DIR="$BATCH_DIR/config"
OUT_FILE="$OUT_DIR/config-${RUN_ID}.tar.gz"

append_manifest() {
  [[ "$DRY_RUN" == "1" ]] && return 0
  echo "- config: $1, file=$OUT_FILE, time=$(date -Is)" >> "$MANIFEST_FILE"
}

if [[ "$DRY_RUN" == "1" ]]; then
  echo "Would archive compose/config files from $COMPOSE_ROOT, $NGINX_CONFIG_DIR, $CADDY_CONFIG_DIR and $SCRIPT_DIR"
  exit 0
fi

mkdir -p "$OUT_DIR"
TMP_LIST="$(mktemp)"
trap 'rm -f "$TMP_LIST"' EXIT

for path in "$COMPOSE_ROOT" "$NGINX_CONFIG_DIR" "$CADDY_CONFIG_DIR" "$SCRIPT_DIR"; do
  if [[ -e "$path" ]]; then
    printf '%s\n' "$path" >> "$TMP_LIST"
  fi
done

if [[ ! -s "$TMP_LIST" ]]; then
  echo "No config paths found." > "$OUT_DIR/README.txt"
  append_manifest "skipped, no config paths found"
  exit 0
fi

tar -czf "$OUT_FILE" --absolute-names --files-from "$TMP_LIST"
append_manifest "success"
