#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import concurrent.futures as cf
import datetime as dt
import json
import sys
import time
from pathlib import Path

import requests

DENOM = 56.0  # wie in der Simulationsdatei

def parse_args():
    p = argparse.ArgumentParser(
        description="Sende Start/End-Zeiten für ARC-DSA-Muster an alle Hosts per HTTP POST."
    )
    p.add_argument("--pattern", required=True, choices=["always", "ramp", "bursts"],
                   help="Muster: always | ramp | bursts (wie in der Simulationsdatei)")
    p.add_argument("--hosts", required=True, help="Pfad zu hosts.txt (eine URL pro Zeile)")
    p.add_argument("--time-limit", type=float, default=56.0,
                   help="Gesamtdauer (Sek.) für das Szenario, default 56.0")
    p.add_argument("--start", default=None,
                   help="Basis-Startzeit als ISO 'YYYY-MM-DDTHH:MM:SS'. Wenn nicht gesetzt, wird --start-offset benutzt.")
    p.add_argument("--start-offset", type=float, default=60.0,
                   help="Offset in Sekunden ab jetzt, falls --start nicht gesetzt ist (Default 60)")
    p.add_argument("--use-rate-adaption", action="store_true",
                   help="Setzt useRateAdaption auf true (Default false)")
    p.add_argument("--timeout", type=float, default=5.0, help="HTTP Timeout pro Request (s)")
    p.add_argument("--retries", type=int, default=2, help="Retries pro Host bei Fehlern")
    p.add_argument("--concurrency", type=int, default=16, help="Parallelität")
    p.add_argument("--dry-run", action="store_true", help="Nur anzeigen, nichts senden")
    return p.parse_args()

def read_hosts(path: Path):
    lines = [ln.strip() for ln in path.read_text(encoding="utf-8").splitlines() if ln.strip()]
    return [ln.strip('"').strip("'") for ln in lines]

def base_start_epoch(start_iso: str | None, start_offset: float) -> float:
    if start_iso:
        try:
            dt_local = dt.datetime.strptime(start_iso, "%Y-%m-%dT%H:%M:%S")
        except ValueError:
            sys.exit("ERROR: --start muss Format YYYY-MM-DDTHH:MM:SS haben (z.B. 2025-08-13T12:21:00)")
        return dt_local.timestamp()
    return time.time() + start_offset

def iso_no_tz(epoch: float) -> str:
    # ISO ohne Zeitzone (wie in deinem Beispiel)
    return dt.datetime.fromtimestamp(epoch).strftime("%Y-%m-%dT%H:%M:%S")

def offsets_for(pattern: str, i: int, time_limit: float, total_hosts: int):
    """
    Gibt (start_off, stop_off) in Sekunden relativ zur Basis zurück.
    Entspricht exakt der Ini-Konfiguration.
    """
    if pattern == "always":
        return 0.0, time_limit

    if pattern in ("ramp", "bursts") and total_hosts != 14:
        sys.exit(f"ERROR: Muster '{pattern}' erwartet genau 14 Hosts (wie 0..13 in der Ini). Gefunden: {total_hosts}.")

    if pattern == "ramp":
        start_off = (i / DENOM) * time_limit
        stop_off  = ((43.0 + i) / DENOM) * time_limit
        return start_off, stop_off

    # pattern == "bursts"
    if i <= 1:
        start_off = (0.0  / DENOM) * time_limit
        stop_off  = (56.0 / DENOM) * time_limit
    elif i <= 7:
        start_off = (7.0  / DENOM) * time_limit
        stop_off  = (49.0 / DENOM) * time_limit
    else:
        start_off = (14.0 / DENOM) * time_limit
        stop_off  = (42.0 / DENOM) * time_limit
    return start_off, stop_off

def send(url: str, payload: dict, timeout: float, retries: int):
    last_exc = None
    for attempt in range(retries + 1):
        try:
            r = requests.post(url, json=payload, timeout=timeout)
            return url, r.status_code, r.text.strip()
        except Exception as e:
            last_exc = e
            time.sleep(min(1.0 * (attempt + 1), 3.0))
    return url, -1, f"ERROR: {last_exc}"

def main():
    args = parse_args()
    hosts = read_hosts(Path(args.hosts))
    if not hosts:
        sys.exit("ERROR: hosts.txt ist leer.")

    base_epoch = base_start_epoch(args.start, args.start_offset)

    print(f"[INFO] Hosts: {len(hosts)}")
    print(f"[INFO] Pattern: {args.pattern}")
    print(f"[INFO] timeLimit: {args.time_limit:.3f}s")
    print(f"[INFO] base start: {iso_no_tz(base_epoch)} (epoch={int(base_epoch)})")
    print(f"[INFO] useRateAdaption: {args.use_rate_adaption}")
    print(f"[INFO] dry-run: {args.dry_run}")
    print()

    jobs = []
    for i, url in enumerate(hosts):
        start_off, stop_off = offsets_for(args.pattern, i, args.time_limit, len(hosts))
        payload = {
            "startTime": iso_no_tz(base_epoch + start_off),
            "endTime":   iso_no_tz(base_epoch + stop_off),
            "useRateAdaption": bool(args.use_rate_adaption),
        }
        jobs.append((i, url, payload))

    if args.dry_run:
        for i, url, payload in jobs:
            print(f"[DRY] UE {i:02d} -> {url}")
            print("      " + json.dumps(payload, ensure_ascii=False))
        return

    results = []
    with cf.ThreadPoolExecutor(max_workers=args.concurrency) as ex:
        futs = [ex.submit(send, url, payload, args.timeout, args.retries) for _, url, payload in jobs]
        for fut in cf.as_completed(futs):
            results.append(fut.result())

    ok = 0
    print("\n[RESULTS]")
    for url, status, text in results:
        if status in (200, 201, 202, 204):
            ok += 1
            print(f"[OK]   {url} -> {status}")
        else:
            print(f"[FAIL] {url} -> {status} {text[:200]}")
    print(f"\n[SUMMARY] {ok}/{len(results)} erfolgreich.")

if __name__ == "__main__":
    main()