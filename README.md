# Crownet Testbed - ARC-DSA

A testbed for evaluating **Adaptive Rate Control for Decentralized Sensing Applications (ARC-DSA)**. This project implements a Spring Boot-based testbed that runs on multiple Raspberry Pi nodes to conduct network load experiments and evaluate rate adaptation algorithms.

## 🎯 Overview

This testbed system is designed for my bachelor thesis research on adaptive rate control in decentralized sensing applications. It provides:

- **Rate Adaptation**: ARC-DSA algorithm implementation for dynamic bandwidth control
- **Experiment Orchestration**: Automated scheduling and execution of different network load scenarios
- **Real-time Analytics**: Live monitoring and data collection from all nodes
- **Docker Deployment**: Containerized deployment for easy scaling and management

## 🏗️ Architecture

The system consists of multiple Spring Boot modules:

```
crownet-testbed/
├── application/         # Main Spring Boot application
├── beacon/              # Beacon transmission module
├── message/             # Application message handling
├── rate-control/        # ARC-DSA rate adaptation algorithm
├── scheduler/           # Experiment scheduling
├── analytics/           # Data collection and analysis
├── client/              # Client communication
├── resource/            # REST API endpoints
└── dev/                 # Development and analysis tools
    ├── ansible/         # Archived Ansible scripts (master node)
    ├── scripts/         # Archived orchestration scripts (master node)
    └── laboratory/      # Data analysis notebooks
```

**Note**: The files in `dev/ansible/` and `dev/scripts/` are archived copies of the actual deployment and orchestration tools that run on the master node of the physical testbed.

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Docker

### Building the Application

```bash
# Build the project
./gradlew build

# Build Docker image
docker build -t crownet-testbed .
```

### Running a Single Node

```bash
# Run with default configuration
docker run --rm --network host \
  -e SERVER_ADDRESS=0.0.0.0 \
  -e EXPERIMENTAL_NODE_ID=1 \
  crownet-testbed:latest
```

## 🧪 Experiment Scenarios

The testbed supports three main experiment patterns:

### Always Pattern
- All nodes active throughout the experiment
- Constant network load baseline
- Tests: `always` (without adaptation), `always_rate` (with ARC-DSA)

### Ramp Pattern  
- Nodes activated/deactivated sequentially
- Gradual load increase/decrease
- Tests: `ramp` (without adaptation), `ramp_rate` (with ARC-DSA)

### Burst Pattern
- Groups of nodes active in short intervals
- Bursty, intermittent load
- Tests: `bursts` (without adaptation), `burst_rate` (with ARC-DSA)

## 📊 Configuration

Key configuration parameters (via environment variables):

```properties
# Node identification
EXPERIMENTAL_NODE_ID=1

# Network settings
SERVER_ADDRESS=0.0.0.0
EXPERIMENTAL_BROADCAST_IP=192.168.1.255

# Beacon settings
ADHOC_BEACON_BANDWIDTH=2000        # bytes/s (50 kbit/s)
ADHOC_SEND_PORT=8888
ADHOC_RECEIVE_PORT=8889

# Message settings  
ADHOC_MESSAGE_BANDWIDTH=62500      # bytes/s (500 kbit/s)
ADHOC_SEND_PORT=9000
ADHOC_RECEIVE_PORT=9001
```

## 🔧 Development

### Project Structure

- **Multi-module Gradle project** with Spring Boot
- **Modular architecture** separating concerns (beacon, message, rate-control, etc.)
- **REST API** for external control and monitoring
- **Docker containerization** for deployment

### Key Modules

- **`beacon/`**: Handles beacon transmission and reception
- **`message/`**: Manages application-level message exchange
- **`rate-control/`**: Implements ARC-DSA rate adaptation algorithm
- **`scheduler/`**: Controls experiment timing and node activation
- **`analytics/`**: Collects and processes experimental data
- **`resource/`**: Provides REST API endpoints for external control

### API Endpoints

- `POST /api/v1/nodes/schedule` - Schedule experiment runs
- `GET /api/v1/analytics/beacons` - Retrieve beacon data
- `GET /api/v1/analytics/messages` - Retrieve message data
- `DELETE /api/v1/analytics/*` - Clear collected data

## 📈 Data Analysis

Experimental data is collected and analyzed using Jupyter notebooks in `dev/laboratory/`:

- **Raw data**: JSON logs per node (beacon and message data)
- **Analysis**: Throughput calculation, rolling averages, visualization
- **Results**: Plots and statistics for thesis evaluation

See `dev/laboratory/README.md` for detailed analysis workflow.

## 🛠️ Orchestration Scripts (Archived)

The `dev/scripts/` folder contains archived copies of the orchestration tools that run on the master node:

- **`schedule.py`**: Schedule and run experiments across all nodes
- **`collect.py`**: Collect experimental data from all nodes
- **`clear.py`**: Reset logs on all nodes

**Note**: These are reference copies. The actual scripts run on the master node of the physical testbed.

## 📚 Documentation

- **`dev/ansible/README.md`**: Archived Ansible automation documentation
- **`dev/scripts/README.md`**: Archived orchestration scripts documentation  
- **`dev/laboratory/README.md`**: Data analysis workflow

## 🎓 Thesis Context

This testbed was developed for my bachelor thesis on **Experimental Analysis of Adaptive Transmission Rate Control in a Wireless Sensor Network**. It enables:

- Evaluation of rate adaptation algorithms under different network conditions
- Comparison of fixed vs. adaptive bandwidth allocation
- Analysis of network performance under various load patterns
- Real-world validation of theoretical approaches

## 📄 License

This project is part of academic research. Please cite appropriately if used in your own work.

---

**Note**: This testbed is designed for research purposes and requires proper network infrastructure and Raspberry Pi cluster setup for full functionality.