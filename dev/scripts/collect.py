#!/usr/bin/env python3
"""
Collect beacon and message logs from all nodes in the testbed.

This script queries the following endpoints on each host:

    /api/v1/analytics/beacons
    /api/v1/analytics/messages

and saves the responses as JSON files into a structured directory tree.

Features:
- Reads hosts from a hosts file (default: hosts.txt) or generates them from node IDs.
- Creates separate folders for each node under `beacon/` and `message/`.
- Supports retries and request timeouts.
- Provides a final summary of successful and failed collections.

Arguments:
    --output-dir   Target directory for logs (created if missing).
    --filename     Filename for saved JSON logs (used for both beacon and message).
    --hosts        Path to hosts file (default: hosts.txt).
    --nodes        Optional list of node IDs to collect from instead of hosts.txt.
    --prefix       IP prefix for nodes when using --nodes (default: 192.168.0.).
    --port         HTTP port for node endpoints (default: 8080).
    --timeout      Request timeout in seconds (default: 10.0).
    --retries      Number of retries per failed request (default: 3).

Exit codes:
    0 if logs were collected from all nodes,
    1 if some or all nodes failed.

Example:
    ./collect_logs.py --output-dir ./logs --filename data.json --hosts hosts.txt
    ./collect_logs.py --output-dir ./logs --filename data.json --nodes 3 4 5 --prefix 192.168.0. --port 8080
"""
import argparse
import json
import sys
import requests
from typing import List, Dict, Any, Optional
from pathlib import Path
import time

GREEN = "\033[92m"; RED = "\033[91m"; YELLOW = "\033[93m"; CYAN = "\033[96m"; RESET = "\033[0m"; BOLD = "\033[1m"

def parse_args():
    parser = argparse.ArgumentParser(
        description="Collect logs from messages and beacons endpoints for all nodes"
    )
    parser.add_argument(
        "--output-dir", type=str, required=True,
        help="Main output directory where logs will be stored"
    )
    parser.add_argument(
        "--filename", type=str, required=True,
        help="Filename for the JSON log files (will be used for both beacon and message logs)"
    )
    parser.add_argument(
        "--hosts", type=str, default="hosts.txt",
        help="Path to hosts.txt file (default: hosts.txt)"
    )
    parser.add_argument(
        "--nodes", type=int, nargs="+",
        help="Specific node IDs to collect from (default: all nodes in hosts.txt)"
    )
    parser.add_argument(
        "--prefix", type=str, default="192.168.0.",
        help="IP prefix (default: 192.168.0.)"
    )
    parser.add_argument(
        "--port", type=int, default=8080,
        help="HTTP port (default: 8080)"
    )
    parser.add_argument(
        "--timeout", type=float, default=10.0,
        help="Request timeout in seconds (default: 10.0)"
    )
    parser.add_argument(
        "--retries", type=int, default=3,
        help="Number of retries for failed requests (default: 3)"
    )
    return parser.parse_args()

def read_hosts(hosts_file: str) -> List[str]:
    """Read hosts from hosts.txt file."""
    try:
        with open(hosts_file, 'r', encoding="utf-8") as f:
            hosts = [line.strip() for line in f if line.strip() and not line.startswith('#')]
        return hosts
    except FileNotFoundError:
        print(f"{RED}❌ ERROR:{RESET} Hosts file '{hosts_file}' not found", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"{RED}❌ ERROR:{RESET} Failed to read hosts file: {e}", file=sys.stderr)
        sys.exit(1)

def extract_node_id(host: str) -> Optional[int]:
    """Extract node ID from host URL."""
    try:
        h = host
        if h.startswith('http://'):
            h = h[7:]
        elif h.startswith('https://'):
            h = h[8:]
        if ':' in h:
            ip_part = h.split(':')[0]
        else:
            ip_part = h
        node_id = int(ip_part.split('.')[-1])
        return node_id
    except Exception as e:
        print(f"{YELLOW}⚠️ WARN:{RESET} Could not extract node ID from '{host}': {e}", file=sys.stderr)
        return None

def fetch_logs(url: str, timeout: float, retries: int) -> Optional[List[Dict[str, Any]]]:
    """Fetch logs from a given URL with retry logic."""
    for attempt in range(retries):
        try:
            r = requests.get(url, timeout=timeout)
            r.raise_for_status()
            data = r.json()
            if not isinstance(data, list):
                print(f"{YELLOW}⚠️ WARN:{RESET} Unexpected JSON type at {url}: {type(data)}", file=sys.stderr)
                return None
            return data
        except requests.exceptions.RequestException as e:
            if attempt < retries - 1:
                print(f"{YELLOW}⚠️ WARN:{RESET} Attempt {attempt + 1} failed for {url}: {e}. Retrying...", file=sys.stderr)
                time.sleep(1)
            else:
                print(f"{RED}❌ ERROR:{RESET} Failed to fetch logs from {url} after {retries} attempts: {e}", file=sys.stderr)
                return None
        except json.JSONDecodeError as e:
            print(f"{RED}❌ ERROR:{RESET} Invalid JSON response from {url}: {e}", file=sys.stderr)
            return None
        except Exception as e:
            print(f"{RED}❌ ERROR:{RESET} Unexpected error fetching from {url}: {e}", file=sys.stderr)
            return None
    return None

def create_directory_structure(output_dir: str, node_id: int) -> tuple[Path, Path]:
    """Create directory structure for a node."""
    base_path = Path(output_dir)
    # Subtract 2 from node_id: 3->1, 4->2, 5->3, etc.
    adjusted_node_id = node_id - 2
    beacon_path = base_path / "beacon" / f"node-{adjusted_node_id}"
    message_path = base_path / "message" / f"node-{adjusted_node_id}"
    beacon_path.mkdir(parents=True, exist_ok=True)
    message_path.mkdir(parents=True, exist_ok=True)
    return beacon_path, message_path

def save_logs_to_file(logs: List[Dict[str, Any]], file_path: Path, log_type: str):
    """Save logs to a JSON file."""
    try:
        with open(file_path, 'w', encoding="utf-8") as f:
            json.dump(logs, f, indent=2, ensure_ascii=False, default=str)
        print(f"  {GREEN}✅ Saved {len(logs):4d} {log_type} logs → {file_path}{RESET}")
    except Exception as e:
        print(f"  {RED}❌ ERROR:{RESET} Failed to save {log_type} logs to {file_path}: {e}", file=sys.stderr)

def collect_logs_from_node(host: str, node_id: int, filename: str, output_dir: str,
                           timeout: float, retries: int) -> bool:
    """Collect both beacon and message logs from a single node."""
    print(f"{BOLD}📥 Node {CYAN}{node_id:02d}{RESET}{BOLD} ({host}){RESET}")

    beacon_path, message_path = create_directory_structure(output_dir, node_id)

    # Beacon
    beacon_url = f"{host.rstrip('/')}/api/v1/analytics/beacons"
    beacon_logs = fetch_logs(beacon_url, timeout, retries)
    if beacon_logs is not None:
        beacon_file = beacon_path / filename
        save_logs_to_file(beacon_logs, beacon_file, "beacon")
    else:
        print(f"  {YELLOW}⚠️ WARN:{RESET} No beacon logs collected")

    # Message
    message_url = f"{host.rstrip('/')}/api/v1/analytics/messages"
    message_logs = fetch_logs(message_url, timeout, retries)
    if message_logs is not None:
        message_file = message_path / filename
        save_logs_to_file(message_logs, message_file, "message")
    else:
        print(f"  {YELLOW}⚠️ WARN:{RESET} No message logs collected")

    return (beacon_logs is not None) or (message_logs is not None)

def main():
    args = parse_args()

    if not args.filename.endswith('.json'):
        print(f"{YELLOW}⚠️ WARN:{RESET} Filename should end with .json", file=sys.stderr)

    # Hosts bestimmen
    if args.nodes:
        hosts = [f"http://{args.prefix}{node_id}:{args.port}" for node_id in args.nodes]
        node_ids = args.nodes
    else:
        hosts = read_hosts(args.hosts)
        node_ids = [extract_node_id(host) for host in hosts]
        valid = [(h, nid) for h, nid in zip(hosts, node_ids) if nid is not None]
        hosts  = [h for h, _ in valid]
        node_ids = [nid for _, nid in valid]

    if not hosts:
        print(f"{RED}❌ ERROR:{RESET} No valid hosts found", file=sys.stderr)
        sys.exit(1)

    output_path = Path(args.output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    print(f"{BOLD}🧾 Collecting logs from {len(hosts)} nodes{RESET}")
    print(f"   📁 Output:  {CYAN}{output_path.absolute()}{RESET}")
    print(f"   📝 File:    {CYAN}{args.filename}{RESET}")
    print(f"   ⏱️ Timeout: {args.timeout}s   🔁 Retries: {args.retries}\n")

    successful = 0
    for host, node_id in zip(hosts, node_ids):
        try:
            ok = collect_logs_from_node(host, node_id, args.filename, args.output_dir,
                                        args.timeout, args.retries)
            if ok:
                successful += 1
                print(f"{GREEN}✅ Done{RESET}\n")
            else:
                print(f"{YELLOW}⚠️ Partial/empty{RESET}\n")
        except Exception as e:
            print(f"{RED}❌ ERROR:{RESET} Unexpected error collecting from node {node_id}: {e}\n", file=sys.stderr)

    print(f"{BOLD}" + "-"*56 + f"{RESET}")
    if successful == len(hosts):
        print(f"{GREEN}🎉 Successfully collected logs from all {successful}/{len(hosts)} nodes.{RESET}")
    else:
        print(f"{YELLOW}⚠️ Summary:{RESET} {GREEN}{successful} ok{RESET}, {RED}{len(hosts)-successful} failed{RESET}")
        print(f"   🔎 Check output above for details.")
    if successful == 0:
        sys.exit(1)
    elif successful < len(hosts):
        sys.exit(1)

if __name__ == "__main__":
    main()