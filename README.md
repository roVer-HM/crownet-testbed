```sh
docker build -t crownet-testbed .
```



```shell
docker tag crownet-testbed:latest 192.168.0.2:5000/crownet-testbed:latest
```

```sh
docker push 192.168.0.2:5000/crownet-testbed:latest
```

```shell
sudo docker pull 192.168.0.2:5000/crownet-testbed:latest
```
```sh
sudo docker run --rm --network host \
  -e SERVER_ADDRESS=0.0.0.0 \
  -e EXPERIMENTAL_NODE_ID=4 \
  192.168.0.2:5000/crownet-testbed:latest
```

python3 push_schedule.py \
--pattern always \
--hosts ./hosts.txt \
--time-limit 120 \
--start 2025-08-14T12:00:00

python3 push_schedule.py \
--pattern ramp \
--hosts ./hosts.txt \
--time-limit 56 \
--start-offset 90 \
--use-rate-adaption \
--dry-run

python3 push_schedule.py \
--pattern bursts \
--hosts ./hosts.txt \
--time-limit 56 \
--start 2025-08-14T12:05:00



# Always
# 1) Always ohne Adaption (CHECK - 4)
python3 orchestrate.py --pattern always --hosts hosts.txt \
--time-limit 560 --start-offset 60

# 2) Always mit Adaption + Randomisierung (CHECK - 4)
python3 orchestrate.py --pattern always --hosts hosts.txt \
--time-limit 560 --start-offset 60 --use-rate-adaption

# Ramp
# 1) Ramp ohne Adaption (CHECK)
python3 orchestrate.py --pattern ramp --hosts hosts.txt \
--time-limit 560 --start-offset 60

# 2) Ramp mit Adaption, ohne Randomisierung 
python3 orchestrate.py --pattern ramp --hosts hosts.txt \
--time-limit 560 --start-offset 60 --use-rate-adaption

# 3) Ramp mit Adaption + Randomisierung
python3 orchestrate.py --pattern ramp --hosts hosts.txt \
--time-limit 560 --start-offset 60 --use-rate-adaption

# Bursts
# 1) Bursts ohne Adaption
python3 orchestrate.py --pattern bursts --hosts hosts.txt \
--time-limit 560 --start-offset 60

# 2) Bursts mit Adaption, ohne Randomisierung
python3 orchestrate.py --pattern bursts --hosts hosts.txt \
--time-limit 560 --start-offset 60 --use-rate-adaption

# 3) Bursts mit Adaption + Randomisierung
python3 orchestrate.py --pattern bursts --hosts hosts.txt \
--time-limit 560 --start-offset 60 --use-rate-adaption

python3 live_beacon_analysis.py --bmax 2000 --window 5
python3 live_message_analysis.py --bmax 62500 --window 5