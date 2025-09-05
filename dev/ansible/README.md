# Ansible Control for Raspberry Pi Testbed

This folder contains the **documentation** and configuration for using Ansible to manage the Raspberry Pi testbed.  
Ansible is installed on the **master node** and is used to centrally provision, orchestrate experiments, and clean up across all worker nodes.

## Structure

```
ansible/
├── ansible.cfg             # Global Ansible configuration 
├── inventory.ini           # Inventory: list of all Pis + variables 
├── playbooks/              
│   ├── chrony_sync.yml     # Playbook to restart Chrony and force a time sync on all Pis
│   ├── pull_image.yml      # Playbook to pull a given Docker image on all Pis
│   ├── run_container.yml   # Playbook to start experiment containers on all Pis (with node-specific IDs)
│   ├── stop_container.yml  # Playbook to stop all running Docker containers on all Pis
├── files/                  # Place for static files/scripts/configs 
└── README.md               # this file
```

## Setup

1. Activate the Ansible virtual environment on the master node:
   ```bash
   source ~/venvs/ansible/bin/activate
   cd ~/ansible
   ```

2. Check the version:
   ```bash
   ansible --version
   ```

3. Verify connectivity:
   ```bash
   ansible pis -m ping -k -K
   ```

## Inventory

Example `inventory.ini`:

```ini
[pis]
pi01 ansible_host=192.168.0.3 ansible_user=ubuntu ansible_connection=paramiko experimental_node_id=1
pi02 ansible_host=192.168.0.4 ansible_user=ubuntu ansible_connection=paramiko experimental_node_id=2
pi03 ansible_host=192.168.0.5 ansible_user=ubuntu ansible_connection=paramiko experimental_node_id=3
...

[pis:vars]
ansible_python_interpreter=/usr/bin/python3
```

Each node has an `experimental_node_id` which is passed into the container runtime.

## Playbooks

- **chrony_sync.yml**  
  Restart chrony on all nodes and force a time synchronization.

- **pull_image.yml**  
  Pull the given Docker image on all nodes.

- **run_container.yml**  
  Start the experimental container on each node, using the node-specific ID.

- **stop_container.yml**  
  Stop all running containers on all nodes.

## Typical Workflow

```bash
# 1. Sync time across all nodes
ansible-playbook playbooks/chrony_sync.yml

# 2. Pull the latest image
ansible-playbook playbooks/pull_image.yml

# 3. Run experiment containers
ansible-playbook playbooks/run_container.yml

# 4. Stop all containers after the experiment
ansible-playbook playbooks/stop_container.yml
```

## Notes

- Use `-k` to provide the SSH password, `-K` to provide the sudo password (if required).
- If the `ubuntu` user has passwordless sudo, `-K` can be omitted.
- This setup is offline-capable, all dependencies are pre-installed in the Ansible virtual environment on the master node.
