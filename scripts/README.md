# 🧪 Experiment Utilities for ARC-DSA

This folder contains helper tools to **orchestrate** and **reset** ARC-DSA testbed runs. They are part of my bachelor thesis on *Adaptive Rate Control for Decentralized Sensing Applications (ARC-DSA)*.

---

## 📂 Contents

- ⚙️ `orchestrate.py` — schedule scenarios (`always`, `ramp`, `bursts`) on all nodes via HTTP.
- 🧹 `clear-logs.py` — delete beacon **and** message logs on all nodes.
- 📝 `hosts.txt` — list of node base URLs (one per line).

---

## 🔧 Prerequisites

### Python
- Python ≥ 3.9

---

## 🌍 hosts.txt

Each line contains ae host/IP:

```text
http://192.168.0.3:8080
http://192.168.0.4:8080
...
http://192.168.0.16:8080
```

---

## 🚀 Orchestrating runs

`scripts/orchestrate.py` sends start/stop windows to each node’s endpoint `POST /api/v1/nodes/schedule`.

Patterns:

- 🟢 **always** — all nodes active for the whole run
- 📈 **ramp** — nodes join sequentially, run together, then leave sequentially
- 🔄 **bursts** — nodes act in groups to create spikes of load

### Example

```bash
# Always pattern, no rate adaption, start in 60s, run 56s
./orchestrate.py   --pattern always   --hosts hosts.txt   --time-limit 56   --start-offset 60

# Ramp pattern, with rate adaption, fixed start time
./orchestrate.py   --pattern ramp   --hosts hosts.txt   --time-limit 560   --start 2025-08-31T13:00:00   --use-rate-adaption
```

---

## 🧹 Resetting logs

`clear-logs.py` deletes:

- `DELETE /api/v1/analytics/beacons`
- `DELETE /api/v1/analytics/messages`

Usage:

```bash
./clear-logs.py
```

---

## 📌 Suggested Workflow

1. 🧹 **Clear logs**
   ```bash
   ./clear-logs.sh hosts.txt
   ```
2. ⚙️ **Schedule scenario**
   ```bash
   ./orchestrate.py --pattern ramp ...
   ```
3. ⏳ **Wait until finished** (`--time-limit`)
4. 📦 **Archive** plots + raw JSONs. Each node provides endpoints to fetch the collected logs. 

✨ Happy experimenting! ✨
