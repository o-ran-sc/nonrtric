#!/usr/bin/env bash

DOCKER_SIM_NWNAME="nonrtric-docker-net"
echo "Creating docker network $DOCKER_SIM_NWNAME, if needed"
docker network ls| grep $DOCKER_SIM_NWNAME > /dev/null || docker network create $DOCKER_SIM_NWNAME

docker-compose -f consul_cbs/docker-compose-template.yml config > docker-compose.yml

docker-compose up -d