#!/usr/bin/env python3
"""
Fetch beacon logs from nodes, compute 5s averaged throughput (Bytes/s),
and plot per-node curves with a horizontal B_max line.

Usage example:
  python plot_beacon_rates.py --bmax 1000
  python plot_beacon_rates.py --nodes 3 4 5 --prefix 192.168.0. --port 8080 \
      --path /api/v1/analytics/beacons --window 5 --bmax 1000 --out beacon_rates.png
"""

import argparse
import sys
from typing import List, Dict, Any
import requests
import pandas as pd
import matplotlib.pyplot as plt

def parse_args():
    p = argparse.ArgumentParser(description="Plot 5s-averaged beacon throughput (Bytes/s) from nodes.")
    p.add_argument("--nodes", type=int, nargs="+", default=[3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16],
                   help="Node IDs (= last octet in 192.168.0.X). Default: 3 4 5")
    p.add_argument("--prefix", type=str, default="192.168.0.",
                   help="IP prefix (everything before node id). Default: 192.168.0.")
    p.add_argument("--port", type=int, default=8080, help="HTTP port. Default: 8080")
    p.add_argument("--path", type=str, default="/api/v1/analytics/beacons",
                   help="Endpoint path. Default: /api/v1/analytics/beacons")
    p.add_argument("--window", type=int, default=5,
                   help="Averaging window in seconds. Default: 5")
    p.add_argument("--bmax", type=float, default=1000.0,
                   help="Max bandwidth in Bytes/s for reference line. Default: 1000")
    p.add_argument("--out", type=str, default=None,
                   help="Output image file (e.g., plot.png). If omitted, just shows the plot.")
    return p.parse_args()

def fetch_logs(base_url: str, timeout: float = 5.0) -> List[Dict[str, Any]]:
    """Fetch JSON array of BeaconLog from a node."""
    r = requests.get(base_url, timeout=timeout)
    r.raise_for_status()
    data = r.json()  # Expecting list of {timestamp, sourceId, sequenceNo, sizeBytes}
    if not isinstance(data, list):
        raise ValueError(f"Unexpected JSON type at {base_url}: {type(data)}")
    return data

def parse_timestamp(ts_val):
    """
    Accept both epoch millis (int) and ISO-8601 strings.
    Returns pandas.Timestamp (ns precision).
    """
    if isinstance(ts_val, (int, float)):
        # epoch millis
        return pd.to_datetime(int(ts_val), unit="ms", utc=True)
    # else: assume string
    try:
        return pd.to_datetime(ts_val, utc=True)
    except Exception as e:
        raise ValueError(f"Unrecognized timestamp format: {ts_val!r}") from e

def logs_to_df(logs: List[Dict[str, Any]], node_id_hint: int = None) -> pd.DataFrame:
    """
    Convert raw logs to a DataFrame with columns:
    ts(datetime UTC), sizeBytes(int), sourceId(int), node(int)
    """
    if not logs:
        return pd.DataFrame(columns=["ts", "sizeBytes", "sourceId", "node"]).set_index("ts")

    rows = []
    for entry in logs:
        ts = parse_timestamp(entry.get("timestamp"))
        source_id = int(entry.get("sourceId"))
        size = int(entry.get("sizeBytes"))
        # node = source_id or node_id_hint; prefer explicit source_id
        node = source_id if source_id is not None else node_id_hint
        rows.append({"ts": ts, "sizeBytes": size, "sourceId": source_id, "node": node})
    df = pd.DataFrame(rows)
    df = df.sort_values("ts").set_index("ts")
    return df

def compute_rolling_bytes_per_sec(df: pd.DataFrame, window_s: int) -> pd.DataFrame:
    """
    For each node, resample to 1-second bins, sum bytes, then rolling-sum over 'window_s' seconds,
    and divide by window length to get Bytes/s averaged over the window.
    Returns DataFrame with index=second timestamps, one column per node (Bytes/s).
    """
    if df.empty:
        return pd.DataFrame()

    # 1-second resample of bytes per second (sum of sizes in that second)
    # For safety, ensure tz-aware index (already UTC from parse)
    per_sec = df.groupby("node")["sizeBytes"].resample("1S").sum().unstack(0).fillna(0.0)
    # Rolling window sum over last 'window_s' seconds, then divide by window
    rolled = per_sec.rolling(window=window_s, min_periods=1).sum() / float(window_s)
    rolled.columns.name = None  # nicer legend
    return rolled

def main():
    args = parse_args()

    # Fetch and build DF for all nodes
    all_df = []
    for n in args.nodes:
        url = f"http://{args.prefix}{n}:{args.port}{args.path}"
        try:
            logs = fetch_logs(url)
            df = logs_to_df(logs, node_id_hint=n)
            if df.empty:
                print(f"[WARN] No logs from node {n} at {url}", file=sys.stderr)
            all_df.append(df)
            print(f"[OK] Fetched {len(df)} rows from node {n}")
        except Exception as e:
            print(f"[ERROR] Node {n} ({url}): {e}", file=sys.stderr)

    if not all_df:
        print("No data fetched; abort.", file=sys.stderr)
        sys.exit(1)

    df_all = pd.concat(all_df).sort_index()

    # Compute rolling bytes/sec per node over window
    rolled = compute_rolling_bytes_per_sec(df_all, args.window)
    if rolled.empty:
        print("No time-indexed data to plot; abort.", file=sys.stderr)
        sys.exit(1)

    # Also compute total (sum across nodes)
    rolled["TOTAL"] = rolled.sum(axis=1)

    # Plot
    plt.figure(figsize=(10, 6))
    # plot per-node
    for col in rolled.columns:
        if col == "TOTAL":
            continue
        plt.plot(rolled.index, rolled[col], label=f"Node {col}")
    # plot total
    plt.plot(rolled.index, rolled["TOTAL"], linestyle="--", linewidth=2, label="TOTAL")

    # horizontal B_max line
    plt.axhline(args.bmax, linestyle=":", linewidth=2, label=f"B_max = {args.bmax:.0f} B/s")

    plt.title(f"5s-averaged throughput (Bytes/s), nodes={args.nodes}, window={args.window}s")
    plt.xlabel("Time (UTC)")
    plt.ylabel("Bytes/s (averaged)")
    plt.legend(loc="best")
    plt.grid(True, which="both", linestyle="--", alpha=0.4)
    plt.tight_layout()

    if args.out:
        plt.savefig(args.out, dpi=120)
        print(f"[OK] Saved plot to {args.out}")
    else:
        plt.show()

if __name__ == "__main__":
    main()