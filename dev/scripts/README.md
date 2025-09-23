# Experiment Utilities for ARC-DSA

This folder contains helper tools to orchestrate, deploy, reset, and collect ARC-DSA testbed runs.  
They are part of my bachelor thesis on *Adaptive Rate Control for Decentralized Sensing Applications (ARC-DSA)*.

All scripts are stored on the **master node** of the testbed, which controls all host nodes.

---

## Contents

- `schedule.py` — schedule scenarios (`always`, `ramp`, `bursts`) on all nodes via HTTP
- `collect.py` — collect generated logs (beacons + messages) from all nodes via HTTP
- `clear.py` — delete beacon and message logs on all nodes via HTTP
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

`schedule.py` sends start/stop windows to each node's endpoint `POST /api/v1/nodes/schedule`.

Patterns:

- **always** — all nodes active for the whole run
- **ramp** — nodes join sequentially, run together, then leave sequentially
- **bursts** — nodes act in groups to create spikes of load

### Example

```bash
# Always pattern, no rate adaption, start in 60s, run 56s
./schedule.py --pattern always --hosts hosts.txt --time-limit 56 --start-offset 60

# Ramp pattern, with rate adaption, fixed start time
./schedule.py --pattern ramp --hosts hosts.txt --time-limit 560 --start 2025-08-31T13:00:00 --use-rate-adaption
```

---

## Resetting logs

`clear.py` deletes:

- `DELETE /api/v1/analytics/beacons`
- `DELETE /api/v1/analytics/messages`

Usage:

```bash
./clear.py --hosts hosts.txt
```

---

## Collecting logs

`collect.py` fetches beacon and message logs from all nodes and stores them in structured folders.

```bash
./collect.py --output-dir ./logs --filename data.json --hosts hosts.txt
```

---

## Suggested Workflow

1. **Clear logs**
   ```bash
   ./clear.py --hosts hosts.txt
   ```

2. **Schedule scenario**
   ```bash
   ./schedule.py --pattern always --hosts hosts.txt --time-limit 56 --start-offset 60
   ```

3. **Wait until finished** (`--time-limit`)

4. **Collect logs**
   ```bash
   ./collect.py --output-dir ./logs --filename run.json --hosts hosts.txt
   ```
