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

TC_ONELINE_DESCR="Testing of the Control Panel and the Health Check app - populating with types and instances"

#App names to exclude checking pulling images for, space separated list
EXCLUDED_IMAGES="SDNC SDNC_ONAP"

. ../common/testcase_common.sh $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh

#### TEST BEGIN ####

#Local vars in test script
##########################
# Path to callback receiver
CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"

clean_containers

OSC_NUM_RICS=6
STD_NUM_RICS=5

start_ric_simulators  ricsim_g1 $OSC_NUM_RICS OSC_2.1.0

start_ric_simulators  ricsim_g2 $STD_NUM_RICS STD_1.1.3

start_mr #Just to prevent errors in the agent log...

start_control_panel

start_consul_cbs

prepare_consul_config      NOSDNC  ".consul_config.json"
consul_config_app                  ".consul_config.json"

start_policy_agent

use_agent_rest_http

api_get_status 200

# Print the A1 version for OSC
for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    sim_print ricsim_g1_$i interface
done


# Print the A1 version for STD
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_print ricsim_g2_$i interface
done


# Load the polictypes in osc
for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    sim_put_policy_type 201 ricsim_g1_$i 2 testdata/OSC/sim_hw.json
    sim_put_policy_type 201 ricsim_g1_$i 20008 testdata/OSC/sim_tsa.json
done


#Check the number of schemas and the individual schemas in OSC
api_equal json:policy_types 3 120

for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    api_equal json:policy_types?ric=ricsim_g1_$i 2 120
done

# Check the schemas in OSC
for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    api_get_policy_schema 200 2 testdata/OSC/hw-agent-modified.json
    api_get_policy_schema 200 20008 testdata/OSC/tsa-agent-modified.json
done


# Create policies
use_agent_rest_http

api_put_service 201 "rapp1" 0 "$CR_PATH/1"

# Create policies in OSC
for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    api_put_policy 201 "rapp1" ricsim_g1_$i 2 $((2000+$i)) NOTRANSIENT testdata/OSC/pihw_template.json 1
    api_put_policy 201 "rapp1" ricsim_g1_$i 20008 $((2050+$i*10)) NOTRANSIENT testdata/OSC/pitsa_template.json 1
done


# Check the number of policies in OSC
for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    sim_equal ricsim_g1_$i num_instances 2
done


# Create policies in STD
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    api_put_policy 201 "rapp1" ricsim_g2_$i NOTYPE $((2100+$i)) NOTRANSIENT testdata/STD/pi1_template.json 1
done


# Check the number of policies in STD
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_equal ricsim_g2_$i num_instances 1
done

check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END

print_result