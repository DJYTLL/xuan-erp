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

ES_BASE_URL="${ES_BASE_URL:-http://127.0.0.1:9040}"
ES_SNAPSHOT_REPOSITORY="${ES_SNAPSHOT_REPOSITORY:-xuan_backup}"
SNAPSHOT_NAME="xuan-erp-${RUN_ID}"
OUT_DIR="$BATCH_DIR/elasticsearch"
OUT_FILE="$OUT_DIR/snapshot-${SNAPSHOT_NAME}.json"

append_manifest() {
  [[ "$DRY_RUN" == "1" ]] && return 0
  echo "- elasticsearch: $1, snapshot=$ES_SNAPSHOT_REPOSITORY/$SNAPSHOT_NAME, time=$(date -Is)" >> "$MANIFEST_FILE"
}

curl_auth=()
if [[ -n "${ES_USERNAME:-}" && -n "${ES_PASSWORD:-}" ]]; then
  curl_auth=(-u "$ES_USERNAME:$ES_PASSWORD")
fi

if [[ "$DRY_RUN" == "1" ]]; then
  echo "Would trigger Elasticsearch snapshot $ES_SNAPSHOT_REPOSITORY/$SNAPSHOT_NAME at $ES_BASE_URL"
  exit 0
fi

mkdir -p "$OUT_DIR"

if ! command -v curl >/dev/null 2>&1; then
  append_manifest "skipped, curl not found"
  exit 0
fi

if ! curl -fsS "${curl_auth[@]}" "$ES_BASE_URL/_snapshot/$ES_SNAPSHOT_REPOSITORY" > "$OUT_DIR/repository.json"; then
  cat > "$OUT_DIR/README.txt" <<EOF
Elasticsearch snapshot repository is not available: $ES_SNAPSHOT_REPOSITORY
Create it on the server first, then rerun this script.
EOF
  append_manifest "skipped, snapshot repository unavailable"
  exit 0
fi

curl -fsS "${curl_auth[@]}" -X PUT \
  "$ES_BASE_URL/_snapshot/$ES_SNAPSHOT_REPOSITORY/$SNAPSHOT_NAME?wait_for_completion=true" \
  -H 'Content-Type: application/json' \
  -d '{"indices":"*","ignore_unavailable":true,"include_global_state":false}' \
  -o "$OUT_FILE"

append_manifest "success"
