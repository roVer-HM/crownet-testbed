# ARC-DSA Experiments – Jupyter Notebooks

This folder contains the Jupyter notebook used to analyze experimental data for my bachelor thesis on **Adaptive Rate Control for Decentralized Sensing Applications (ARC-DSA)**. The notebooks document the evaluation of different network load scenarios and serve as a digital **lab journal**. 

The script `scripts/schedule.py` was used to orchestrate the scenarios on the physical testbed.

---

## 📂 Folder Structure
```
laboratory/
│
├── data.zip                     # Compressed experiment data
│   └── Contains my results from the testbed during the bachelor thesis:
│       ├── 03-09-2025/          # Experiment data from September 3rd, 2025
│       │   └── scenarios/       # Raw experiment data (JSON logs per node)
│       │       ├── always/      # Scenario with constant activity
│       │       │   ├── beacon/  # Beacon transmissions
│       │       │   └── message/ # Application messages
│       │       ├── burst/       # Scenario with bursty activity
│       │       │   ├── beacon/  # Beacon transmissions
│       │       │   └── message/ # Application messages
│       │       └── ramp/        # Gradual activation/deactivation of nodes
│       │           ├── beacon/  # Beacon transmissions
│       │           └── message/ # Application messages
│       │
│       └── 05-09-2025/          # Experiment data from September 5th, 2025
│           └── scenarios/       # Raw experiment data with ARC-DSA rate adaptation
│               ├── always_rate/ # Always scenario with ARC-DSA rate adaptation
│               │   ├── beacon/  # Beacon transmissions
│               │   └── message/ # Application messages
│               ├── burst_rate/  # Bursty activity with ARC-DSA rate adaptation
│               │   ├── beacon/  # Beacon transmissions
│               │   └── message/ # Application messages
│               └── ramp_rate/   # Ramp scenario with ARC-DSA rate adaptation
│                   ├── beacon/  # Beacon transmissions
│                   └── message/ # Application messages
│
├── analysis.ipynb               # Lab notebook for data analysis
└── README.md                    # This file
```

---

## 🧪 Scenarios

### Without ARC-DSA Rate Adaptation (03-09-2025)
- **Always** – All nodes are active throughout the experiment (constant network load).
- **Ramp** – Nodes are activated/deactivated step by step, producing a ramp-shaped load profile.
- **Burst** – Groups of nodes are active only for short time intervals (bursty load).

### With ARC-DSA Rate Adaptation (05-09-2025)
- **Always Rate** – Always scenario with ARC-DSA rate adaptation enabled.
- **Ramp Rate** – Ramp scenario with ARC-DSA rate adaptation enabled.
- **Burst Rate** – Burst scenario with ARC-DSA rate adaptation enabled.

Each scenario includes both **beacon** and **message** data, organized by node (node-1 to node-14).

---

## 📊 Contents of the Notebooks

Each notebook documents:
1. **Data extraction** from `data.zip` containing all experiment scenarios
2. **Data loading** from the extracted date-specific `scenarios/` folders (e.g., `03-09-2025/scenarios/` or `05-09-2025/scenarios/`)
3. **Aggregation** into 1-second bins (Bytes/s per node)
4. **Rolling average computation** (default: 5s window)
5. **Plots** of total throughput and active node count

The generated plots are exported to the `plots/` folder.

---

## ⚙️ Requirements

- Python 3.10+
- Jupyter Notebook / JupyterLab
- Check out the defined imports in the notebooks and install the required packages