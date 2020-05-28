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

TC_ONELINE_DESCR="Testing of the health check app"

#App names to exclude checking pulling images for, space separated list
EXCLUDED_IMAGES="SDNC_ONAP"

. ../common/testcase_common.sh $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh

#### TEST BEGIN ####

#Local vars in test script
##########################
# Path to callback receiver
CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"

clean_containers

start_ric_simulators  ricsim_g1 6 OSC_2.1.0

start_ric_simulators  ricsim_g2 5 STD_1.1.3



start_consul_cbs

prepare_consul_config      NOSDNC  ".consul_config.json"
consul_config_app                  ".consul_config.json"


start_policy_agent


use_agent_rest_http

api_get_status 200

sim_print ricsim_g1_1 interface
sim_print ricsim_g1_2 interface
sim_print ricsim_g1_3 interface
sim_print ricsim_g1_4 interface
sim_print ricsim_g1_5 interface
sim_print ricsim_g1_6 interface

sim_print ricsim_g2_1 interface
sim_print ricsim_g2_2 interface
sim_print ricsim_g2_3 interface
sim_print ricsim_g2_4 interface
sim_print ricsim_g2_5 interface

sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json
sim_put_policy_type 201 ricsim_g1_2 1 testdata/OSC/sim_1.json
sim_put_policy_type 201 ricsim_g1_3 1 testdata/OSC/sim_1.json
sim_put_policy_type 201 ricsim_g1_4 1 testdata/OSC/sim_1.json
sim_put_policy_type 201 ricsim_g1_5 1 testdata/OSC/sim_1.json
sim_put_policy_type 201 ricsim_g1_6 1 testdata/OSC/sim_1.json

api_equal json:policy_types 2 120

sleep_wait 30 "Give the agent some extra time...."

# Create policies
use_agent_rest_http

api_put_service 201 "rapp1" 3600 "$CR_PATH/1"

api_put_policy 201 "rapp1" ricsim_g1_1 1 2010 NOTRANSIENT testdata/OSC/pi1_template.json 1
api_put_policy 201 "rapp1" ricsim_g1_2 1 2020 NOTRANSIENT testdata/OSC/pi1_template.json 1
api_put_policy 201 "rapp1" ricsim_g1_3 1 2030 NOTRANSIENT testdata/OSC/pi1_template.json 1
api_put_policy 201 "rapp1" ricsim_g1_4 1 2040 NOTRANSIENT testdata/OSC/pi1_template.json 1
api_put_policy 201 "rapp1" ricsim_g1_5 1 2050 NOTRANSIENT testdata/OSC/pi1_template.json 1
api_put_policy 201 "rapp1" ricsim_g1_6 1 2060 NOTRANSIENT testdata/OSC/pi1_template.json 1

sim_equal ricsim_g1_1 num_instances 1
sim_equal ricsim_g1_2 num_instances 1
sim_equal ricsim_g1_3 num_instances 1
sim_equal ricsim_g1_4 num_instances 1
sim_equal ricsim_g1_5 num_instances 1
sim_equal ricsim_g1_6 num_instances 1

api_put_policy 201 "rapp1" ricsim_g2_1 NOTYPE 2110 NOTRANSIENT testdata/STD/pi1_template.json 1
api_put_policy 201 "rapp1" ricsim_g2_2 NOTYPE 2120 NOTRANSIENT testdata/STD/pi1_template.json 1
api_put_policy 201 "rapp1" ricsim_g2_3 NOTYPE 2130 NOTRANSIENT testdata/STD/pi1_template.json 1
api_put_policy 201 "rapp1" ricsim_g2_4 NOTYPE 2140 NOTRANSIENT testdata/STD/pi1_template.json 1
api_put_policy 201 "rapp1" ricsim_g2_5 NOTYPE 2150 NOTRANSIENT testdata/STD/pi1_template.json 1

sim_equal ricsim_g2_1 num_instances 1
sim_equal ricsim_g2_2 num_instances 1
sim_equal ricsim_g2_3 num_instances 1
sim_equal ricsim_g2_4 num_instances 1
sim_equal ricsim_g2_5 num_instances 1


check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END

print_result