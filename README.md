```sh
docker build -t crownet-testbed .
```

```sh
sudo docker run --rm --network host \
  -e "SERVER_ADDRESS=0.0.0.0" \
  -e "EXPERIMENTAL_WIFI_IP=192.168.1.3" \
  -e "EXPERIMENTAL_BROADCAST_IP=192.168.1.255" \
  -e "EXPERIMENTAL_WIFI_SENDING_PORT=5005" \
  -e "EXPERIMENTAL_WIFI_RECEIVING_PORT=5006" \
  -e "EXPERIMENTAL_PAYLOAD_SENDING_PORT=5007" \
  -e "EXPERIMENTAL_PAYLOAD_RECEIVING_PORT=5008" \
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