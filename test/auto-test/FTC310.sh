#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2020 Nordix Foundation. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================
#


TC_ONELINE_DESCR="Resync of RIC via changes in the consul config"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh
. ../common/controller_api_functions.sh

#### TEST BEGIN ####


# Clean container and start all needed containers #
clean_containers

# Start one RIC of each type
start_ric_simulators ricsim_g1 1  OSC_2.1.0
start_ric_simulators ricsim_g2 1  STD_1.1.3

start_mr

start_cr

start_consul_cbs

prepare_consul_config      NOSDNC  ".consul_config.json"

consul_config_app                  ".consul_config.json"

start_control_panel

start_policy_agent

api_equal json:rics 2 120


# Add an OSC RIC and check
start_ric_simulators ricsim_g2 2  STD_1.1.3

prepare_consul_config      NOSDNC  ".consul_config.json"

consul_config_app                  ".consul_config.json"

api_equal json:rics 3 120

check_policy_agent_logs
check_control_panel_logs

# Remove one OSC RIC and check
start_ric_simulators ricsim_g2 1  STD_1.1.3

prepare_consul_config      NOSDNC  ".consul_config.json"

consul_config_app                  ".consul_config.json"

api_equal json:rics 2 120

check_policy_agent_logs
check_control_panel_logs

store_logs          END


#### TEST COMPLETE ####


print_result

auto_clean_containers
