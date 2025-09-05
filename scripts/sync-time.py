#!/usr/bin/env python3
"""
Synchronize system time on multiple Raspberry Pi nodes via SSH using chrony.

This script connects to each host listed in a hosts file, restarts the chrony
service, and performs a manual time synchronization step (`chronyc -a makestep`).
The master node is assumed to be configured as the NTP/chrony server.

Features:
- Connects via SSH using username/password authentication.
- Executes commands with sudo by passing the password via stdin.
- Runs tasks in parallel across nodes with configurable concurrency.
- Provides a final summary of success/failure across all nodes.

Arguments:
    --hosts        Path to a file containing hostnames or URLs (default: hosts.txt).
                   Lines may be raw IPs or http(s):// URLs; ports are stripped.
    --user         SSH username (default: ubuntu).
    --password     SSH password (required).
    --ssh-port     SSH port (default: 22).
    --concurrency  Maximum number of concurrent SSH sessions (default: 6).

Exit codes:
    0 if all synchronizations succeeded,
    2 if one or more hosts failed.
"""
import argparse, concurrent.futures as cf, time, re, socket
import paramiko
from urllib.parse import urlparse

GREEN="\033[92m"; RED="\033[91m"; YELLOW="\033[93m"; CYAN="\033[96m"; RESET="\033[0m"; BOLD="\033[1m"

def parse_hosts(path):
    hosts=[]
    with open(path,"r",encoding="utf-8") as f:
        for line in f:
            s=line.strip()
            if not s or s.startswith("#"): continue
            if s.startswith("http://") or s.startswith("https://"):
                netloc=urlparse(s).netloc
            else:
                netloc=s
            host=re.split(r'[:/]', netloc)[0]
            hosts.append(host)
    return hosts

def sync_time_on_host(host, user, password, ssh_port=22, connect_timeout=8, cmd_timeout=60):
    t0=time.time()
    client=paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        client.connect(
            hostname=host,
            port=ssh_port,
            username=user,
            password=password,
            timeout=connect_timeout,
            banner_timeout=connect_timeout,
            auth_timeout=connect_timeout,
            allow_agent=False,
            look_for_keys=False,
        )
        cmds=[
            "sudo -S -p '' systemctl restart chrony",
            "sudo -S -p '' chronyc -a makestep"
        ]
        ok=True; msgs=[]
        for cmd in cmds:
            stdin, stdout, stderr = client.exec_command(cmd, timeout=cmd_timeout)
            stdin.write(password+"\n"); stdin.flush()
            out=stdout.read().decode(errors="replace").strip()
            err=stderr.read().decode(errors="replace").strip()
            rc=stdout.channel.recv_exit_status()
            if rc!=0: ok=False
            msgs.append((cmd, rc, out, err))
        client.close()
        dt=time.time()-t0
        return {"host":host,"ok":ok,"dt":dt,"msgs":msgs}
    except (socket.timeout, paramiko.ssh_exception.SSHException, OSError) as e:
        try: client.close()
        except Exception: pass
        return {"host":host,"ok":False,"dt":time.time()-t0,"msgs":[("connect",-1,"",str(e))]}

def main():
    ap=argparse.ArgumentParser(description="Chrony-Zeitsync auf allen Nodes via SSH.")
    ap.add_argument("--hosts", default="hosts.txt")
    ap.add_argument("--user", default="ubuntu")
    ap.add_argument("--password", required=True)
    ap.add_argument("--ssh-port", type=int, default=22)
    ap.add_argument("--concurrency", type=int, default=6)
    args=ap.parse_args()

    hosts=parse_hosts(args.hosts)
    if not hosts:
        print(f"{RED}❌ Keine Hosts in {args.hosts} gefunden.{RESET}")
        return 1

    print(f"{BOLD}⏱️ Zeitsynchronisation auf {len(hosts)} Nodes (chrony){RESET}\n")

    results=[]
    with cf.ThreadPoolExecutor(max_workers=args.concurrency) as ex:
        futs=[ex.submit(sync_time_on_host,h,args.user,args.password,args.ssh_port) for h in hosts]
        for fut in cf.as_completed(futs):
            r=fut.result()
            if r["ok"]:
                print(f"{GREEN}✅ {r['host']}{RESET} — Zeit erfolgreich synchronisiert ({r['dt']:.1f}s)")
            else:
                err=""
                for (_,rc,_,stderr) in r["msgs"]:
                    if rc!=0 and stderr: err=stderr; break
                if not err: err="Fehler bei chrony"
                print(f"{RED}❌ {r['host']}{RESET} — {err} ({r['dt']:.1f}s)")
            results.append(r)

    ok=sum(1 for r in results if r["ok"])
    fail=len(results)-ok
    print("\n" + "-"*52)
    if fail==0:
        print(f"{GREEN}🎉 Alle {ok} Nodes erfolgreich synchronisiert.{RESET}")
    else:
        print(f"{YELLOW}⚠️  Zusammenfassung:{RESET} {GREEN}{ok} ok{RESET}, {RED}{fail} fehlgeschlagen{RESET}")
        failed=[r["host"] for r in results if not r["ok"]]
        if failed:
            print("   🔁 Erneut versuchen:", ", ".join(failed))
    return 0 if fail==0 else 2

if __name__=="__main__":
    raise SystemExit(main())