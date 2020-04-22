#!/usr/bin/env bash

TC_ONELINE_DESCR="dockerirze the test, setup docker container for Policy Agent, cbs, consul, Near-RT RIC simulator"

. ../common/testcase_common.sh $1 $2 $3

#### TEST BEGIN ####

clean_containers

start_ric_simulator

prepare_consul_config

start_simulators

consul_config_app         "../simulator-group/consul_cbs/config.json"

start_control_panel

start_sdnc

start_policy_agent

check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END
