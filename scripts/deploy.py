#!/usr/bin/env python3
"""
Deploy a Docker image to all nodes in the testbed via SSH.

This script connects to each node using SSH, runs a `docker pull`
for the given image, and reports the result with emoji-enhanced output.

Features:
- Reads hosts from a hosts file (default: hosts.txt).
  Hosts may be plain IPs, hostnames, or full URLs (http://…).
- Connects with username and password (no keys required).
- Executes `sudo docker pull <image>` on each node.
- Runs operations in parallel with configurable concurrency.
- Provides a summary of successes and failures.

Arguments:
    --hosts            Path to hosts file (default: hosts.txt).
    --user             SSH username (default: ubuntu).
    --password         SSH password (required).
    --image            Full Docker image reference to pull (required).
    --ssh-port         SSH port (default: 22).
    --concurrency      Number of parallel SSH sessions (default: 6).
    --connect-timeout  SSH connect timeout in seconds (default: 8).
    --cmd-timeout      Command timeout in seconds (default: 300).

Exit codes:
    0 if all nodes pulled the image successfully,
    2 if one or more nodes failed,
    1 on invalid configuration (e.g., no hosts found).

Example:
    ./deploy.py --hosts hosts.txt \
                --user ubuntu \
                --password pwdUbuntu \
                --image 192.168.0.2:5000/crownet-testbed:latest \
                --concurrency 6
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
            host=re.split(r'[:/]', netloc)[0]  # IP/Hostname ohne :port, /pfad
            hosts.append(host)
    return hosts

def pull_on_host(host, user, password, image, ssh_port=22, connect_timeout=8, cmd_timeout=300):
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
        # sudo mit Passwort über stdin: -S, ohne Prompt: -p ''
        cmd=f"sudo -S -p '' docker pull {image}"
        stdin, stdout, stderr = client.exec_command(cmd, timeout=cmd_timeout)
        # Passwort in sudo einspeisen
        stdin.write(password + "\n")
        stdin.flush()
        out = stdout.read().decode(errors="replace")
        err = stderr.read().decode(errors="replace")
        rc = stdout.channel.recv_exit_status()
        dt = time.time()-t0
        client.close()
        ok = (rc==0)
        # gelegentlich schreibt docker pull viel nach stdout; wir kürzen für die Anzeige
        hint=""
        if not ok:
            low=(out+"\n"+err).lower()
            if "incorrect password" in low or "authentication failure" in low:
                hint="falsches sudo/ssh Passwort"
            elif "permission denied" in low:
                hint="keine Berechtigung / Permission denied"
            elif "connection refused" in low:
                hint="Docker-Dienst nicht erreichbar (läuft docker?)"
            else:
                # letzte nicht-leere Zeile als kompakter Hinweis
                lines=[l for l in (out+"\n"+err).splitlines() if l.strip()]
                hint = lines[-1][:120] if lines else "unbekannter Fehler"
        return {"host":host,"ok":ok,"rc":rc,"dt":dt,"hint":hint}
    except (socket.timeout, paramiko.ssh_exception.SSHException, OSError) as e:
        try:
            client.close()
        except Exception:
            pass
        return {"host":host,"ok":False,"rc":-1,"dt":time.time()-t0,"hint":str(e)[:120]}

def main():
    ap=argparse.ArgumentParser(description="Pull eines Docker-Images auf allen Nodes via SSH (mit Emoji-Output).")
    ap.add_argument("--hosts", default="hosts.txt")
    ap.add_argument("--user", default="ubuntu")
    ap.add_argument("--password", required=True)
    ap.add_argument("--image", required=True, help="z.B. 192.168.0.2:5000/crownet-testbed:latest")
    ap.add_argument("--ssh-port", type=int, default=22)
    ap.add_argument("--concurrency", type=int, default=6)
    ap.add_argument("--connect-timeout", type=int, default=8)
    ap.add_argument("--cmd-timeout", type=int, default=300)
    args=ap.parse_args()

    hosts=parse_hosts(args.hosts)
    if not hosts:
        print(f"{RED}❌ Keine Hosts in {args.hosts} gefunden.{RESET}")
        return 1

    print(f"{BOLD}📦 Pull Image auf {len(hosts)} Nodes:{RESET} {CYAN}{args.image}{RESET}\n")

    results=[]
    with cf.ThreadPoolExecutor(max_workers=args.concurrency) as ex:
        futs=[ex.submit(pull_on_host, h, args.user, args.password, args.image, args.ssh_port, args.connect_timeout, args.cmd_timeout) for h in hosts]
        for fut in cf.as_completed(futs):
            r=fut.result()
            if r["ok"]:
                print(f"{GREEN}✅ {r['host']}{RESET} — pull ok ({r['dt']:.1f}s)")
            else:
                print(f"{RED}❌ {r['host']}{RESET} — {r['hint']} ({r['dt']:.1f}s)")
            results.append(r)

    ok=sum(1 for r in results if r["ok"])
    fail=len(results)-ok
    print("\n" + "-"*52)
    if fail==0:
        print(f"{GREEN}🎉 Alle {ok} Nodes erfolgreich aktualisiert.{RESET}")
    else:
        print(f"{YELLOW}⚠️  Zusammenfassung:{RESET} {GREEN}{ok} ok{RESET}, {RED}{fail} fehlgeschlagen{RESET}")
        failed=[r["host"] for r in results if not r["ok"]]
        if failed:
            print("   🔁 Erneut versuchen:", ", ".join(failed))
    return 0 if fail==0 else 2

if __name__=="__main__":
    raise SystemExit(main())