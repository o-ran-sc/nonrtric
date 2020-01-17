#!/usr/bin/env bash

TC_ONELINE_DESCR="Auto test for policy agent refreshing configurations from consul/cbs"

. ../common/testcase_common.sh $1 $2

#### TEST BEGIN ####

clean_containers

start_simulators

consul_config_app         "../simulator-group/consul_cbs/config.json"

start_policy_agent

check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END

