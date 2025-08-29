#!/usr/bin/env bash
set -euo pipefail

HOSTS_FILE="${1:-hosts.txt}"

for ENDPOINT in "/api/v1/analytics/beacons" "/api/v1/analytics/messages"; do
  echo "[INFO] Lösche $ENDPOINT auf allen Hosts…"

  while IFS= read -r host; do
    # Leere Zeilen / Kommentare überspringen
    [[ -z "$host" || "$host" =~ ^# ]] && continue

    # http:// ergänzen, falls nicht vorhanden
    [[ "$host" != http* ]] && host="http://$host"

    # Trailing Slash entfernen und Endpoint anhängen
    url="${host%/}${ENDPOINT}"

    echo "DELETE $url"
    curl -sS -X DELETE "$url" -o /dev/null -w " -> %{http_code}\n"
  done < "$HOSTS_FILE"

  echo
done