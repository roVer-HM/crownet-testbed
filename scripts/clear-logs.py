#!/usr/bin/env python3
"""
Clear beacon and message logs on all hosts via HTTP DELETE requests.

This script iterates over all hosts listed in a hosts file and deletes
logs from two endpoints:

    /api/v1/analytics/beacons
    /api/v1/analytics/messages

Features:
- Reads hosts from a text file (`hosts.txt` by default).
  Lines may contain plain IPs (e.g. 192.168.0.3:8080) or URLs (http://…).
- Issues HTTP DELETE requests to each host for both endpoints.
- Prints color-coded, emoji-enhanced CLI output (✅/❌/⚠️/🎉).
- Summarizes results across all hosts and endpoints.
- Exits with a non-zero status code if any requests fail.

Arguments:
    --hosts   Path to hosts file (default: hosts.txt).

Exit codes:
    0 if all DELETE requests succeeded,
    1 if one or more requests failed.

Example:
    ./clear_logs.py --hosts hosts.txt
"""
import argparse
import sys
import requests
from pathlib import Path

GREEN = "\033[92m"; RED = "\033[91m"; YELLOW = "\033[93m"
CYAN = "\033[96m"; RESET = "\033[0m"; BOLD = "\033[1m"

ENDPOINTS = [
    "/api/v1/analytics/beacons",
    "/api/v1/analytics/messages",
]

def read_hosts(path: Path):
    try:
        lines = [ln.strip() for ln in path.read_text(encoding="utf-8").splitlines()]
        return [ln if ln.startswith("http") else f"http://{ln}"
                for ln in lines if ln and not ln.startswith("#")]
    except FileNotFoundError:
        print(f"{RED}❌ ERROR:{RESET} hosts file {path} not found.", file=sys.stderr)
        sys.exit(1)

def delete_logs(hosts, endpoint):
    total = 0
    ok = 0
    fail = 0
    print(f"{BOLD}🧹 Deleting data at {CYAN}{endpoint}{RESET}{BOLD} on all hosts…{RESET}")

    for host in hosts:
        url = f"{host.rstrip('/')}{endpoint}"
        total += 1
        try:
            r = requests.delete(url, timeout=5)
            if r.status_code in (200, 204):
                print(f"  {GREEN}✅ {host}{RESET} — deleted ({r.status_code})")
                ok += 1
            else:
                print(f"  {RED}❌ {host}{RESET} — error ({r.status_code})")
                fail += 1
        except requests.exceptions.RequestException as e:
            print(f"  {RED}❌ {host}{RESET} — exception {e}")
            fail += 1

    print()
    return total, ok, fail

def main():
    ap = argparse.ArgumentParser(description="Delete beacon and message logs on all hosts.")
    ap.add_argument("--hosts", default="hosts.txt", help="Path to hosts.txt")
    args = ap.parse_args()

    hosts = read_hosts(Path(args.hosts))
    if not hosts:
        print(f"{RED}❌ ERROR:{RESET} no valid hosts found.", file=sys.stderr)
        sys.exit(1)

    grand_total = grand_ok = grand_fail = 0
    for ep in ENDPOINTS:
        t, o, f = delete_logs(hosts, ep)
        grand_total += t; grand_ok += o; grand_fail += f

    print("----------------------------------------------------")
    if grand_fail == 0:
        print(f"{GREEN}🎉 Success:{RESET} All {grand_ok} of {grand_total} requests completed.")
    else:
        print(f"{YELLOW}⚠️  Summary:{RESET} {GREEN}{grand_ok} ok{RESET}, {RED}{grand_fail} failed{RESET}")
        sys.exit(1)

if __name__ == "__main__":
    main()