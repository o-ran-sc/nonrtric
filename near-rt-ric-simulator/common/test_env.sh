#!/usr/bin/env bash

# Set the images for the Policy Agent app to use for the auto tests. Do not add the image tag.
#
# Local Policy Agent image and tag, shall point to locally built image (non-nexus path)
export POLICY_AGENT_LOCAL_IMAGE=o-ran-sc/nonrtric-policy-agent
# Remote image
export POLICY_AGENT_REMOTE_IMAGE=nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-policy-agent
# SDNC A1 Adapter remote image
export SDNC_A1_ADAPTER_IMAGE=nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-a1-controller:1.7.4
# Dashboard remote image
export DASHBOARD_IMAGE=nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-dashboard:1.0.1

# Common env var for auto-test.

POLICY_AGENT_PORT=8081
POLICY_AGENT_LOGPATH="/var/log/policy-agent/application.log"  #Path the application log in the Policy Agent container
DOCKER_SIM_NWNAME="nonrtric-docker-net"                       #Name of docker private network
CONSUL_HOST="consul-server"                                   #Host name of consul
CONSUL_PORT=8500                                              #Port number of consul
CONFIG_BINDING_SERVICE="config-binding-service"               #Host name of CBS
PA_APP_BASE="policy-agent"                                    #Base name for Policy Agent container
