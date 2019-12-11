#!/bin/bash

DOCKER_SIM_NWNAME="nonrtric-docker-net"
echo "Creating docker network $DOCKER_SIM_NWNAME, if needed"
docker network ls| grep $DOCKER_SIM_NWNAME > /dev/null || docker network create $DOCKER_SIM_NWNAME

docker-compose -f docker-compose-template.yml config > docker-compose.yml

docker-compose up -d

CONSUL_PORT=8500

APP="policy-agent"
JSON_FILE="config.json"

curl -s -v  http://127.0.0.1:${CONSUL_PORT}/v1/kv/${APP}?dc=dc1 -X PUT -H 'Accept: application/json' -H 'Content-Type: application/json' -H 'X-Requested-With: XMLHttpRequest' --data-binary "@"$JSON_FILE