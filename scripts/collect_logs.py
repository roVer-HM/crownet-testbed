#!/usr/bin/env python3
"""
Collect logs from messages and beacons endpoints for all nodes.
Organizes logs into a structured folder hierarchy with separate beacon and message folders.

Usage:
  python collect_logs.py --output-dir ./logs --filename data.json
  python collect_logs.py --output-dir ./experiment_results --filename results.json --hosts hosts.txt
  python collect_logs.py --output-dir ./logs --filename logs.json --nodes 3 4 5 6
"""

import argparse
import json
import os
import sys
import requests
from typing import List, Dict, Any, Optional
from pathlib import Path
import time

def parse_args():
    parser = argparse.ArgumentParser(
        description="Collect logs from messages and beacons endpoints for all nodes"
    )
    parser.add_argument(
        "--output-dir", 
        type=str, 
        required=True,
        help="Main output directory where logs will be stored"
    )
    parser.add_argument(
        "--filename", 
        type=str, 
        required=True,
        help="Filename for the JSON log files (will be used for both beacon and message logs)"
    )
    parser.add_argument(
        "--hosts", 
        type=str, 
        default="hosts.txt",
        help="Path to hosts.txt file (default: hosts.txt)"
    )
    parser.add_argument(
        "--nodes", 
        type=int, 
        nargs="+",
        help="Specific node IDs to collect from (default: all nodes in hosts.txt)"
    )
    parser.add_argument(
        "--prefix", 
        type=str, 
        default="192.168.0.",
        help="IP prefix (default: 192.168.0.)"
    )
    parser.add_argument(
        "--port", 
        type=int, 
        default=8080, 
        help="HTTP port (default: 8080)"
    )
    parser.add_argument(
        "--timeout", 
        type=float, 
        default=10.0,
        help="Request timeout in seconds (default: 10.0)"
    )
    parser.add_argument(
        "--retries", 
        type=int, 
        default=3,
        help="Number of retries for failed requests (default: 3)"
    )
    return parser.parse_args()

def read_hosts(hosts_file: str) -> List[str]:
    """Read hosts from hosts.txt file."""
    try:
        with open(hosts_file, 'r') as f:
            hosts = [line.strip() for line in f if line.strip() and not line.startswith('#')]
        return hosts
    except FileNotFoundError:
        print(f"[ERROR] Hosts file '{hosts_file}' not found", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"[ERROR] Failed to read hosts file: {e}", file=sys.stderr)
        sys.exit(1)

def extract_node_id(host: str) -> int:
    """Extract node ID from host URL."""
    try:
        # Handle both http://192.168.0.X:8080 and 192.168.0.X:8080 formats
        if host.startswith('http://'):
            host = host[7:]  # Remove http://
        elif host.startswith('https://'):
            host = host[8:]  # Remove https://
        
        # Extract IP and port
        if ':' in host:
            ip_part = host.split(':')[0]
        else:
            ip_part = host
        
        # Extract last octet as node ID
        node_id = int(ip_part.split('.')[-1])
        return node_id
    except Exception as e:
        print(f"[WARN] Could not extract node ID from '{host}': {e}", file=sys.stderr)
        return None

def fetch_logs(url: str, timeout: float, retries: int) -> Optional[List[Dict[str, Any]]]:
    """Fetch logs from a given URL with retry logic."""
    for attempt in range(retries):
        try:
            response = requests.get(url, timeout=timeout)
            response.raise_for_status()
            data = response.json()
            
            if not isinstance(data, list):
                print(f"[WARN] Unexpected JSON type at {url}: {type(data)}", file=sys.stderr)
                return None
                
            return data
        except requests.exceptions.RequestException as e:
            if attempt < retries - 1:
                print(f"[WARN] Attempt {attempt + 1} failed for {url}: {e}. Retrying...", file=sys.stderr)
                time.sleep(1)  # Brief delay before retry
            else:
                print(f"[ERROR] Failed to fetch logs from {url} after {retries} attempts: {e}", file=sys.stderr)
                return None
        except json.JSONDecodeError as e:
            print(f"[ERROR] Invalid JSON response from {url}: {e}", file=sys.stderr)
            return None
        except Exception as e:
            print(f"[ERROR] Unexpected error fetching from {url}: {e}", file=sys.stderr)
            return None
    
    return None

def create_directory_structure(output_dir: str, node_id: int) -> tuple[Path, Path]:
    """Create directory structure for a node."""
    base_path = Path(output_dir)
    # Subtract 2 from node_id: 3->1, 4->2, 5->3, etc.
    adjusted_node_id = node_id - 2
    beacon_path = base_path / "beacon" / f"node-{adjusted_node_id}"
    message_path = base_path / "message" / f"node-{adjusted_node_id}"
    
    # Create directories
    beacon_path.mkdir(parents=True, exist_ok=True)
    message_path.mkdir(parents=True, exist_ok=True)
    
    return beacon_path, message_path

def save_logs_to_file(logs: List[Dict[str, Any]], file_path: Path, log_type: str):
    """Save logs to a JSON file."""
    try:
        with open(file_path, 'w') as f:
            json.dump(logs, f, indent=2, default=str)
        print(f"[OK] Saved {len(logs)} {log_type} logs to {file_path}")
    except Exception as e:
        print(f"[ERROR] Failed to save {log_type} logs to {file_path}: {e}", file=sys.stderr)

def collect_logs_from_node(host: str, node_id: int, filename: str, output_dir: str, 
                          timeout: float, retries: int) -> bool:
    """Collect both beacon and message logs from a single node."""
    print(f"\n[INFO] Collecting logs from node {node_id} ({host})")
    
    # Create directory structure
    beacon_path, message_path = create_directory_structure(output_dir, node_id)
    
    # Collect beacon logs
    beacon_url = f"{host}/api/v1/analytics/beacons"
    beacon_logs = fetch_logs(beacon_url, timeout, retries)
    
    if beacon_logs is not None:
        beacon_file = beacon_path / filename
        save_logs_to_file(beacon_logs, beacon_file, "beacon")
    else:
        print(f"[WARN] No beacon logs collected from node {node_id}")
    
    # Collect message logs
    message_url = f"{host}/api/v1/analytics/messages"
    message_logs = fetch_logs(message_url, timeout, retries)
    
    if message_logs is not None:
        message_file = message_path / filename
        save_logs_to_file(message_logs, message_file, "message")
    else:
        print(f"[WARN] No message logs collected from node {node_id}")
    
    return beacon_logs is not None or message_logs is not None

def main():
    args = parse_args()
    
    # Validate arguments
    if not args.filename.endswith('.json'):
        print("[WARN] Filename should end with .json extension", file=sys.stderr)
    
    # Read hosts
    if args.nodes:
        # Use specific nodes
        hosts = [f"http://{args.prefix}{node_id}:{args.port}" for node_id in args.nodes]
        node_ids = args.nodes
    else:
        # Read from hosts file
        hosts = read_hosts(args.hosts)
        node_ids = [extract_node_id(host) for host in hosts]
        # Filter out None values
        valid_hosts = [(host, node_id) for host, node_id in zip(hosts, node_ids) if node_id is not None]
        hosts = [host for host, _ in valid_hosts]
        node_ids = [node_id for _, node_id in valid_hosts]
    
    if not hosts:
        print("[ERROR] No valid hosts found", file=sys.stderr)
        sys.exit(1)
    
    print(f"[INFO] Collecting logs from {len(hosts)} nodes")
    print(f"[INFO] Output directory: {args.output_dir}")
    print(f"[INFO] Filename: {args.filename}")
    print(f"[INFO] Timeout: {args.timeout}s, Retries: {args.retries}")
    
    # Create main output directory
    output_path = Path(args.output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    
    # Collect logs from each node
    successful_collections = 0
    for host, node_id in zip(hosts, node_ids):
        try:
            if collect_logs_from_node(host, node_id, args.filename, args.output_dir, 
                                    args.timeout, args.retries):
                successful_collections += 1
        except Exception as e:
            print(f"[ERROR] Unexpected error collecting from node {node_id}: {e}", file=sys.stderr)
    
    print(f"\n[INFO] Collection complete!")
    print(f"[INFO] Successfully collected logs from {successful_collections}/{len(hosts)} nodes")
    print(f"[INFO] Logs saved to: {output_path.absolute()}")
    
    if successful_collections == 0:
        print("[ERROR] No logs were collected successfully", file=sys.stderr)
        sys.exit(1)
    elif successful_collections < len(hosts):
        print(f"[WARN] Some nodes failed. Check the output above for details.", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
