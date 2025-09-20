# Experiment Utilities for ARC-DSA

This folder contains helper tools to orchestrate, deploy, reset, and collect ARC-DSA testbed runs.  
They are part of my bachelor thesis on *Adaptive Rate Control for Decentralized Sensing Applications (ARC-DSA)*.

All scripts are stored on the **master node** of the testbed, which controls all host nodes.

---

## Contents

- `orchestrate.py` — schedule scenarios (`always`, `ramp`, `bursts`) on all nodes via HTTP
- `deploy.py` — push new Docker image to all nodes via SSH
- `sync_time.py` — restart chrony and synchronize node clocks with the master node
- `collect-logs.py` — collect generated logs (beacons + messages) from all nodes via HTTP
- `clear-logs.py` — delete beacon and message logs on all nodes via HTTP
- `hosts.txt` — list of node base URLs (one per line)

---

## Prerequisites

### Python
- Python ≥ 3.9
- `requests` and `paramiko` packages installed

---

## hosts.txt

Each line contains a host:

```text
http://192.168.0.3:8080
http://192.168.0.4:8080
...
http://192.168.0.16:8080
```

---

## Orchestrating runs

`orchestrate.py` sends start/stop windows to each node’s endpoint `POST /api/v1/nodes/schedule`.

Patterns:

- **always** — all nodes active for the whole run
- **ramp** — nodes join sequentially, run together, then leave sequentially
- **bursts** — nodes act in groups to create spikes of load

### Example

```bash
# Always pattern, no rate adaption, start in 60s, run 56s
./orchestrate.py --pattern always --hosts hosts.txt --time-limit 56 --start-offset 60

# Ramp pattern, with rate adaption, fixed start time
./orchestrate.py --pattern ramp --hosts hosts.txt --time-limit 560 --start 2025-08-31T13:00:00 --use-rate-adaption
```

---

## Resetting logs

`clear-logs.py` deletes:

- `DELETE /api/v1/analytics/beacons`
- `DELETE /api/v1/analytics/messages`

Usage:

```bash
./clear-logs.py --hosts hosts.txt
```

---

## Deploying new Docker image

`deploy.py` connects via SSH to all nodes and executes:

```bash
sudo docker pull 192.168.0.2:5000/crownet-testbed:latest
```

Example:

```bash
./deploy.py --hosts hosts.txt --user ubuntu --password pwdUbuntu --image 192.168.0.2:5000/crownet-testbed:latest
```

---

## Time synchronization

`sync_time.py` restarts chrony on each node and forces a manual time sync with the master.

```bash
./sync_time.py --hosts hosts.txt --user ubuntu --password pwdUbuntu
```

---

## Collecting logs

`collect-logs.py` fetches beacon and message logs from all nodes and stores them in structured folders.

```bash
./collect-logs.py --output-dir ./logs --filename data.json --hosts hosts.txt
```

---

## Suggested Workflow

1. **Clear logs**
   ```bash
   ./clear-logs.py --hosts hosts.txt
   ```

2. **Schedule scenario**
   ```bash
   ./orchestrate.py --pattern always --hosts hosts.txt --time-limit 56 --start-offset 60
   ```

3. **Wait until finished** (`--time-limit`)

4. **Collect logs**
   ```bash
   ./collect-logs.py --output-dir ./logs --filename run.json --hosts hosts.txt
   ```

5. **Deploy new version** (if needed)
   ```bash
   ./deploy.py --hosts hosts.txt --user ubuntu --password pwdUbuntu --image 192.168.0.2:5000/crownet-testbed:latest
   ```

6. **Sync time** (optional)
   ```bash
   ./sync_time.py --hosts hosts.txt --user ubuntu --password pwdUbuntu
   ```
