# ARC-DSA Experiments – Jupyter Notebooks

This folder contains the Jupyter notebooks used to analyze experimental data for my bachelor thesis on **Adaptive Rate Control for Decentralized Sensing Applications (ARC-DSA)**. The notebooks document the evaluation of different network load scenarios and serve as a digital **lab journal**. 

The script `scripts/orchestrate.py` was used to orchestrate the scenarios on the physical testbed.

---

## 📂 Folder Structure
```
laboratory/
│
├── plots/                       # Generated plots (PDF/PNG) for thesis
│
├── scenarios/                   # Raw experiment data (JSON logs per node)
│   ├── always/                  # Scenario with constant activity
│   │   ├── beacon/              # Beacon transmissions
│   │   └── message/             # Application messages
│   │       └── node-*/          # Node-wise JSON logs (01.json, 02.json, …)
│   │
│   ├── always_adaption/         # Same scenario, with ARC-DSA rate adaptation
│   ├── burst/                   # Scenario with bursty activity
│   ├── burst_adaption/          # Bursty activity with ARC-DSA
│   ├── ramp/                    # Gradual activation/deactivation of nodes
│   ├── ramp_adaption_no-rand/   # Ramp with ARC-DSA, deterministic
│   └── ramp_adaption_rand/      # Ramp with ARC-DSA, randomized
│
├── Auricchio_Laborbuch_Szenario-Always.ipynb   # Lab notebook for “Always” scenario
├── Auricchio_Laborbuch_Szenario-Ramp.ipynb     # Lab notebook for “Ramp” scenario
├── Auricchio_Laborbuch_Szenario-Burst.ipynb    # Lab notebook for “Burst” scenario
└── README.md                                   # This file
```

---

## 🧪 Scenarios

- **Always** – All nodes are active throughout the experiment (constant network load).
- **Ramp** – Nodes are activated/deactivated step by step, producing a ramp-shaped load profile.
- **Burst** – Groups of nodes are active only for short time intervals (bursty load).

Each scenario exists both **with** and **without** ARC-DSA rate adaptation,  
and in some cases with **randomization enabled**.

---

## 📊 Contents of the Notebooks

Each notebook documents:
1. **Data loading** from `scenarios/`
2. **Aggregation** into 1-second bins (Bytes/s per node)
3. **Rolling average computation** (default: 5s window)
4. **Plots** of total throughput and active node count

The generated plots are exported to the `plots/` folder.

---

## ⚙️ Requirements

- Python 3.10+
- Jupyter Notebook / JupyterLab
- Check out the defined imports in the notebooks and install the required packages