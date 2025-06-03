import requests
import csv
import time
from datetime import datetime
from collections import defaultdict

NODES = [f"192.168.0.{i}" for i in range(2, 4)]
PORT = 8080
ENDPOINT_PATH = "/api/v1/analytics/log"
CSV_FILE = "beacon_throughput_per_second.csv"

MONITOR_DURATION = 60     # Laufzeit in Sekunden
FETCH_INTERVAL = 10       # Alle X Sekunden holen
WINDOW_SIZE = 1           # Sekündlich aggregieren

print("Starte Beacon-Durchsatzanalyse...")

def fetch_logs(ip, since_ms):
    try:
        url = f"http://{ip}:{PORT}{ENDPOINT_PATH}"
        response = requests.get(url, params={"since": since_ms}, timeout=2)
        if response.status_code == 200:
            return response.json()
    except requests.RequestException:
        pass
    return []

# CSV vorbereiten
with open(CSV_FILE, mode="w", newline="") as file:
    writer = csv.writer(file)
    writer.writerow(["timestamp", "total_bytes", "beacon_count", "nodes", "bytes_per_sec"])

start_time = time.time()
since_timestamp = int((start_time - FETCH_INTERVAL) * 1000)  # initialer Zeitraum

while time.time() - start_time < MONITOR_DURATION:
    all_beacons = []

    for ip in NODES:
        all_beacons.extend(fetch_logs(ip, since_timestamp))

    since_timestamp = int(time.time() * 1000)

    # Gruppieren nach Sekunden (timestamp → Sekunde → Beacons)
    buckets = defaultdict(list)
    for b in all_beacons:
        ts_sec = int(b["timestamp"] / 1000)
        buckets[ts_sec].append(b)

    with open(CSV_FILE, mode="a", newline="") as file:
        writer = csv.writer(file)

        for ts_sec in sorted(buckets.keys()):
            group = buckets[ts_sec]
            total_bytes = sum(b["sendThroughput"] for b in group)
            beacon_count = len(group)
            node_ids = {b["sourceId"] for b in group}
            timestamp_str = datetime.fromtimestamp(ts_sec).isoformat()
            bytes_per_sec = total_bytes / WINDOW_SIZE

            writer.writerow([timestamp_str, total_bytes, beacon_count, ",".join(sorted(node_ids)), bytes_per_sec])
            print(f"[{timestamp_str}] {bytes_per_sec:.1f} B/s from {len(node_ids)} nodes ({beacon_count} beacons)")

    time.sleep(FETCH_INTERVAL)

print("Analyse abgeschlossen.")