#!/usr/bin/env bash

# Local image and tag, shall point to locally built image (non-nexus path)
export POLICY_AGENT_LOCAL_IMAGE=oransc/policy-agent


# Common env var for auto-test.

POLICY_AGENT_PORT=8081
POLICY_AGENT_LOGPATH="/var/log/policy-agent/application.log"  #Path the application log in the dfc container
DOCKER_SIM_NWNAME="nonrtric-docker-net"             #Name of docker private network
CONSUL_HOST="consul-server"            #Host name of consul
CONSUL_PORT=8500                       #Port number of consul
CONFIG_BINDING_SERVICE="config-binding-service"  #Host name of CBS
PA_APP_BASE="policy-agent"
