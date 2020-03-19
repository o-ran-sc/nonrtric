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

TC_ONELINE_DESCR="Repeatedly create and delete 10000 policies in each RICs for 24h"

. ../common/testcase_common.sh  $@

#### TEST BEGIN ####

#Local vars in test script
##########################
# Path to callback receiver
CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"

clean_containers

start_ric_simulators ricsim_g1 4 OSC_2.1.0

start_ric_simulators ricsim_ricsim_g2 4 STD_1.1.3

start_mr

start_cr

start_consul_cbs

prepare_consul_config      NOSDNC  ".consul_config.json"
consul_config_app                  ".consul_config.json"

start_control_panel

start_policy_agent

use_agent_rest

api_get_status 200

sim_print ricsim_g1_1 interface
sim_print ricsim_g1_2 interface
sim_print ricsim_g1_3 interface
sim_print ricsim_g1_4 interface

sim_print ricsim_g2_1 interface
sim_print ricsim_g2_2 interface
sim_print ricsim_g2_3 interface
sim_print ricsim_g2_4 interface

sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json
sim_put_policy_type 201 ricsim_g1_2 1 testdata/OSC/sim_1.json
sim_put_policy_type 201 ricsim_g1_3 1 testdata/OSC/sim_1.json
sim_put_policy_type 201 ricsim_g1_4 1 testdata/OSC/sim_1.json

sim_equal ricsim_g1_1 num_instances 0
sim_equal ricsim_g1_2 num_instances 0
sim_equal ricsim_g1_3 num_instances 0
sim_equal ricsim_g1_4 num_instances 0

sim_equal ricsim_g2_1 num_instances 0
sim_equal ricsim_g2_2 num_instances 0
sim_equal ricsim_g2_3 num_instances 0
sim_equal ricsim_g2_4 num_instances 0

api_equal json:policy_types 2 120  #Wait for the agent to refresh types from the simulator

api_put_service 201 "rapp1" 0 "$CR_PATH/callbacks/1"

TEST_DURATION=$((24*3600))
TEST_START=$SECONDS

while [ $(($SECONDS-$TEST_START)) -lt $TEST_DURATION ]; do

    echo ""
    echo "#########################################################################################################"
    echo -e $BOLD"INFO: Test executed for: "$(($SECONDS-$TEST_START)) "seconds. Target is: "$TEST_DURATION" seconds (24h)."$EBOLD
    echo "#########################################################################################################"
    echo ""


# Create 10000 instances in each RIC
    api_put_policy 201 "rapp1" ricsim_g1_1 1 200000 testdata/OSC/pi1_template.json 10000
    sim_equal ricsim_g1_1 num_instances 10000

    api_put_policy 201 "rapp1" ricsim_g1_2 1 220000 testdata/OSC/pi1_template.json 10000
    sim_equal ricsim_g1_2 num_instances 10000

    api_put_policy 201 "rapp1" ricsim_g1_3 1 240000 testdata/OSC/pi1_template.json 10000
    sim_equal ricsim_g1_3 num_instances 10000

    api_put_policy 201 "rapp1" ricsim_g1_4 1 260000 testdata/OSC/pi1_template.json 10000
    sim_equal ricsim_g1_4 num_instances 10000

    api_equal json:policy_ids 40000

    api_put_policy 201 "rapp1" ricsim_g2_1 NOTYPE 300000 testdata/OSC/pi1_template.json 10000
    sim_equal ricsim_g1_1 num_instances 10000

    api_put_policy 201 "rapp1" ricsim_g2_2 NOTYPE 320000 testdata/OSC/pi1_template.json 10000
    sim_equal ricsim_g1_2 num_instances 10000

    api_put_policy 201 "rapp1" ricsim_g2_3 NOTYPE 340000 testdata/OSC/pi1_template.json 10000
    sim_equal ricsim_g1_3 num_instances 10000

    api_put_policy 201 "rapp1" ricsim_g2_4 NOTYPE 360000 testdata/OSC/pi1_template.json 10000
    sim_equal ricsim_g1_4 num_instances 10000

    api_equal json:policy_ids 80000


# Delete all instances in each ric

    api_delete_policy 204 200000 10000
    sim_equal ricsim_g1_1 num_instances 0

    api_delete_policy 204 220000 10000
    sim_equal ricsim_g1_2 num_instances 0

    api_delete_policy 204 240000 10000
    sim_equal ricsim_g1_3 num_instances 0

    api_delete_policy 204 260000 10000
    sim_equal ricsim_g1_4 num_instances 0

    api_equal json:policy_ids 40000

    api_delete_policy 204 300000 10000
    sim_equal ricsim_g1_1 num_instances 0

    api_delete_policy 204 320000 10000
    sim_equal ricsim_g1_2 num_instances 0

    api_delete_policy 204 340000 10000
    sim_equal ricsim_g1_3 num_instances 0

    api_delete_policy 204 360000 10000
    sim_equal ricsim_g1_4 num_instances 0

    api_equal json:policy_ids 0
done


check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END

print_result