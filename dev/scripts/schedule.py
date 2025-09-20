#!/usr/bin/env python3
"""
Schedule ARC-DSA scenarios on all testbed nodes via HTTP POST.

This script sends start and end times for experiments to each node’s
`/api/v1/nodes/schedule` endpoint. It supports different traffic patterns
(always, ramp, bursts) and provides emoji-enhanced output.

Features:
- Reads hosts from a file (default: hosts.txt).
- Supports experiment patterns:
  * always  → all nodes active for the full duration
  * ramp    → nodes start/stop gradually across the experiment
  * bursts  → groups of nodes active in overlapping time windows
- Base start time can be given as ISO string or relative offset in seconds.
- Configurable total duration (`--time-limit`), concurrency, retries, and timeouts.
- Dry-run mode (`--dry-run`) prints the planned schedule without sending.
"""

import argparse, concurrent.futures as cf, datetime as dt, json, sys, time
from pathlib import Path
import requests

GREEN="\033[92m"; RED="\033[91m"; YELLOW="\033[93m"; CYAN="\033[96m"; RESET="\033[0m"; BOLD="\033[1m"

DENOM = 56.0

def parse_args():
    p = argparse.ArgumentParser(description="Send start/end times for ARC-DSA patterns to all hosts via HTTP POST.")
    p.add_argument("--pattern", required=True, choices=["always", "ramp", "bursts"],
                   help="Scenario pattern: always | ramp | bursts")
    p.add_argument("--hosts", required=True, help="Path to hosts.txt (one URL per line)")
    p.add_argument("--time-limit", type=float, default=56.0, help="Total scenario duration in seconds")
    p.add_argument("--start", default=None, help="Start time ISO 'YYYY-MM-DDTHH:MM:SS'")
    p.add_argument("--start-offset", type=float, default=60.0, help="Offset in seconds from now if --start not set")
    p.add_argument("--use-rate-adaption", action="store_true", help="Enable adaptive rate control")
    p.add_argument("--timeout", type=float, default=5.0, help="HTTP request timeout per host")
    p.add_argument("--retries", type=int, default=2, help="Retries per host in case of failure")
    p.add_argument("--concurrency", type=int, default=16, help="Max number of parallel requests")
    p.add_argument("--dry-run", action="store_true", help="Only print schedule, don’t send")
    return p.parse_args()

def read_hosts(path: Path):
    lines = [ln.strip() for ln in path.read_text(encoding="utf-8").splitlines()
             if ln.strip() and not ln.strip().startswith("#")]
    return [ln.strip('"').strip("'") for ln in lines]

def base_start_epoch(start_iso, start_offset):
    if start_iso:
        try:
            dt_local = dt.datetime.strptime(start_iso, "%Y-%m-%dT%H:%M:%S")
        except ValueError:
            sys.exit(f"{RED}❌ ERROR:{RESET} --start must use format YYYY-MM-DDTHH:MM:SS")
        return dt_local.timestamp()
    return time.time() + start_offset

def iso_no_tz(epoch: float) -> str:
    return dt.datetime.fromtimestamp(epoch).strftime("%Y-%m-%dT%H:%M:%S")

def offsets_for(pattern: str, i: int, time_limit: float, total_hosts: int):
    if pattern == "always":
        return 0.0, time_limit
    if pattern in ("ramp", "bursts") and total_hosts != 14:
        sys.exit(f"{RED}❌ ERROR:{RESET} Pattern '{pattern}' requires exactly 14 hosts. Found: {total_hosts}.")
    if pattern == "ramp":
        return (i / DENOM) * time_limit, ((43.0 + i) / DENOM) * time_limit
    if i <= 1:
        return (0.0/ DENOM)*time_limit, (56.0/ DENOM)*time_limit
    elif i <= 7:
        return (7.0/ DENOM)*time_limit, (49.0/ DENOM)*time_limit
    else:
        return (14.0/ DENOM)*time_limit, (42.0/ DENOM)*time_limit

def send(url: str, payload: dict, timeout: float, retries: int):
    last_exc=None
    for attempt in range(retries+1):
        try:
            r=requests.post(url,json=payload,timeout=timeout)
            return url,r.status_code,r.text.strip()
        except Exception as e:
            last_exc=e
            time.sleep(min(1.0*(attempt+1),3.0))
    return url,-1,f"ERROR: {last_exc}"

def main():
    args=parse_args()
    hosts=read_hosts(Path(args.hosts))
    if not hosts:
        sys.exit(f"{RED}❌ ERROR:{RESET} hosts.txt is empty.")

    base_epoch=base_start_epoch(args.start,args.start_offset)
    SCHEDULE_PATH="/api/v1/nodes/schedule"
    schedule_hosts=[f"{h.rstrip('/')}{SCHEDULE_PATH}" for h in hosts]

    print(f"{BOLD}🎬 Scheduling experiments on {len(schedule_hosts)} hosts{RESET}")
    print(f"   Pattern: {CYAN}{args.pattern}{RESET}")
    print(f"   timeLimit: {args.time_limit:.1f}s")
    print(f"   base start: {iso_no_tz(base_epoch)} (epoch={int(base_epoch)})")
    print(f"   useRateAdaption: {args.use_rate_adaption}")
    print(f"   dry-run: {args.dry_run}\n")

    jobs=[]
    for i,url in enumerate(schedule_hosts):
        start_off,stop_off=offsets_for(args.pattern,i,args.time_limit,len(schedule_hosts))
        payload={"startTime": iso_no_tz(base_epoch+start_off),
                 "endTime": iso_no_tz(base_epoch+stop_off),
                 "useRateAdaption": bool(args.use_rate_adaption)}
        jobs.append((i,url,payload))

    if args.dry_run:
        for i,url,payload in jobs:
            print(f"{YELLOW}🔎 DRY-RUN UE {i:02d}{RESET} -> {url}")
            print("     "+json.dumps(payload,ensure_ascii=False))
        return

    results=[]
    with cf.ThreadPoolExecutor(max_workers=args.concurrency) as ex:
        futs=[ex.submit(send,url,payload,args.timeout,args.retries) for _,url,payload in jobs]
        for fut in cf.as_completed(futs):
            results.append(fut.result())

    ok=0
    print(f"\n{BOLD}📊 Results:{RESET}")
    for url,status,text in results:
        if status in (200,201,202,204):
            ok+=1
            print(f"{GREEN}✅ {url}{RESET} -> {status}")
        else:
            print(f"{RED}❌ {url}{RESET} -> {status} {text[:120]}")

    fail=len(results)-ok
    print("\n"+"-"*55)
    if fail==0:
        print(f"{GREEN}🎉 All {ok} hosts successfully scheduled.{RESET}")
    else:
        print(f"{YELLOW}⚠️  Summary:{RESET} {GREEN}{ok} ok{RESET}, {RED}{fail} failed{RESET}")

if __name__=="__main__":
    main()