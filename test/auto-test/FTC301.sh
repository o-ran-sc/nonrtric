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

TC_ONELINE_DESCR="Resync 10000 policies using OSC interface over REST+SNDC"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh

#### TEST BEGIN ####

#Local vars in test script
##########################
# Path to callback receiver
CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"

clean_containers

start_ric_simulators ricsim_g1 4 OSC_2.1.0

start_mr

start_cr

start_sdnc

start_consul_cbs

prepare_consul_config      SDNC  ".consul_config.json"
consul_config_app                  ".consul_config.json"

start_control_panel


start_policy_agent

use_agent_rest_http

api_get_status 200

sim_print ricsim_g1_1 interface

sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json

api_equal json:policy_types 1 120  #Wait for the agent to refresh types from the simulator

api_put_service 201 "rapp1" 3600 "$CR_PATH/callbacks/1"

api_put_policy 201 "rapp1" ricsim_g1_1 1 2000 testdata/OSC/pi1_template.json 10000

sim_equal ricsim_g1_1 num_instances 10000

sim_post_delete_instances 200 ricsim_g1_1

sim_equal ricsim_g1_1 num_instances 0

sim_equal ricsim_g1_1 num_instances 10000 300

api_delete_policy 204 2435

api_delete_policy 204 8693

sim_post_delete_instances 200 ricsim_g1_1

sim_post_delete_instances 200 ricsim_g1_1

sim_equal ricsim_g1_1 num_instances 9998 300


check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_containers