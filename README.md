```sh
docker build -t crownet-testbed .
```

```sh
sudo docker run --rm --network host \
  -e SERVER_ADDRESS=0.0.0.0 \
  -e EXPERIMENTAL_NODE_ID=3 \
  192.168.0.2:5000/crownet-testbed:latest
```

```shell
docker tag crownet-testbed:latest 192.168.0.2:5000/crownet-testbed:latest
```

```sh
docker push 192.168.0.2:5000/crownet-testbed:latest
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