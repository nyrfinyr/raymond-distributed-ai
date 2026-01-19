#!/bin/bash

docker compose build;
docker stack rm raymond;
docker stack up -c compose.yaml raymond
docker service logs -f raymond_node


