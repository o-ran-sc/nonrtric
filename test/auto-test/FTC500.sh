#!/usr/bin/env bash

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


TC_ONELINE_DESCR="Sample tests of the SDNC A1 controller restconf API"

. ../common/testcase_common.sh  $@
. ../common/controller_api_functions.sh
. ../common/ricsimulator_api_functions.sh

#### TEST BEGIN ####

# Clean container and start all needed containers #
clean_containers

start_ric_simulators ricsim_g1 1  OSC_2.1.0
start_ric_simulators ricsim_g2 1  STD_1.1.3

start_sdnc

# API tests
echo -e $YELLOW"TR13"$EYELLOW
controller_api_get_A1_policy_type 404 OSC ricsim_g1_1 1

sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json


controller_api_get_A1_policy_ids 200 OSC ricsim_g1_1 1
controller_api_get_A1_policy_ids 200 STD ricsim_g2_1

controller_api_get_A1_policy_type 200 OSC ricsim_g1_1 1
controller_api_get_A1_policy_type 200 OSC ricsim_g1_1 1 testdata/OSC/sim_1.json

controller_api_put_A1_policy 202 OSC ricsim_g1_1 1 4000 testdata/OSC/pi1_template.json
controller_api_put_A1_policy 201 STD ricsim_g2_1 5000   testdata/STD/pi1_template.json

controller_api_get_A1_policy_ids 200 OSC ricsim_g1_1 1 4000
controller_api_get_A1_policy_ids 200 STD ricsim_g2_1 5000

controller_api_get_A1_policy_status 200 OSC ricsim_g1_1 1 4000
controller_api_get_A1_policy_status 200 STD ricsim_g2_1 5000

VAL='NOT IN EFFECT'
controller_api_get_A1_policy_status 200 OSC ricsim_g1_1 1 4000 "$VAL" "false"
controller_api_get_A1_policy_status 200 STD ricsim_g2_1 5000 "UNDEFINED"

controller_api_delete_A1_policy 202 OSC ricsim_g1_1 1 4000
controller_api_delete_A1_policy 204 STD ricsim_g2_1 5000



store_logs          END

#### TEST COMPLETE ####

print_result

auto_clean_containers