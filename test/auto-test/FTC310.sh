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

TC_ONELINE_DESCR="Repeatedly create and delete 10000 policies in each RICs for 24h. Via agent REST"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh

#### TEST BEGIN ####

#Local vars in test script
##########################
# Path to callback receiver
CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"
# Number of RICs per interface type (OSC and STD)
NUM_RICS=4
# Number of policy instances per RIC
NUM_INSTANCES=10000

clean_containers

start_ric_simulators ricsim_g1 4 OSC_2.1.0

start_ric_simulators ricsim_g2 4 STD_1.1.3

start_mr

start_cr

start_consul_cbs

prepare_consul_config      NOSDNC  ".consul_config.json"
consul_config_app                  ".consul_config.json"

start_control_panel

start_policy_agent

use_agent_rest_http

api_get_status 200

echo "Print the interface for group 1 simulators, shall be OSC"
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_print ricsim_g1_$i interface
done

echo "Print the interface for group 2 simulators, shall be STD"
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_print ricsim_g2_$i interface
done

echo "Load policy type in group 1 simulators"
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_put_policy_type 201 ricsim_g1_$i 1 testdata/OSC/sim_1.json
done

echo "Check the number of instances in  group 1 simulators, shall be 0"
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_equal ricsim_g1_$i num_instances 0
done

echo "Check the number of instances in  group 2 simulators, shall be 0"
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_equal ricsim_g2_$i num_instances 0
done

echo "Wait for the agent to refresh types from the simulator"
api_equal json:policy_types 2 120

echo "Check the number of types in the agent for each ric is 1"
for ((i=1; i<=$NUM_RICS; i++))
do
   api_equal json:policy_types?ric=ricsim_g1_$i 1 120
done

echo "Register a service"
api_put_service 201 "rapp1" 0 "$CR_PATH/1"

TEST_DURATION=$((24*3600))
TEST_START=$SECONDS

while [ $(($SECONDS-$TEST_START)) -lt $TEST_DURATION ]; do

    echo ""
    echo "#########################################################################################################"
    echo -e $BOLD"INFO: Test executed for: "$(($SECONDS-$TEST_START)) "seconds. Target is: "$TEST_DURATION" seconds (24h)."$EBOLD
    echo "#########################################################################################################"
    echo ""


    echo "Create 10000 instances in each OSC RIC"
    INSTANCE_ID=200000
    INSTANCES=0
    for ((i=1; i<=$NUM_RICS; i++))
    do
        api_put_policy 201 "rapp1" ricsim_g1_$i 1 $INSTANCE_ID testdata/OSC/pi1_template.json $NUM_INSTANCES
        sim_equal ricsim_g1_$i num_instances $NUM_INSTANCES
        INSTANCE_ID=$(($INSTANCE_ID+$NUM_INSTANCES))
        INSTANCES=$(($INSTANCES+$NUM_INSTANCES))
    done

    api_equal json:policy_ids $INSTANCES

    echo "Create 10000 instances in each OSC RIC"
    for ((i=1; i<=$NUM_RICS; i++))
    do
        api_put_policy 201 "rapp1" ricsim_g2_$i NOTYPE $INSTANCE_ID testdata/STD/pi1_template.json $NUM_INSTANCES
        sim_equal ricsim_g2_$i num_instances $NUM_INSTANCES
        INSTANCE_ID=$(($INSTANCE_ID+$NUM_INSTANCES))
        INSTANCES=$(($INSTANCES+$NUM_INSTANCES))
    done

    api_equal json:policy_ids $INSTANCES


    echo "Delete all instances in each OSC RIC"

    INSTANCE_ID=200000
    for ((i=1; i<=$NUM_RICS; i++))
    do
        api_delete_policy 204 $INSTANCE_ID $NUM_INSTANCES
        INSTANCES=$(($INSTANCES-$NUM_INSTANCES))
        sim_equal ricsim_g1_$i num_instances $NUM_INSTANCES
        INSTANCE_ID=$(($INSTANCE_ID+$NUM_INSTANCES))
    done

    api_equal json:policy_ids $INSTANCES

    echo "Delete all instances in each STD RIC"
    INSTANCE_ID=200000
    for ((i=1; i<=$NUM_RICS; i++))
    do
        api_delete_policy 204 $INSTANCE_ID $NUM_INSTANCES
        INSTANCES=$(($INSTANCES-$NUM_INSTANCES))
        sim_equal ricsim_g2_$i num_instances $NUM_INSTANCES
        INSTANCE_ID=$(($INSTANCE_ID+$NUM_INSTANCES))
    done

    api_equal json:policy_ids 0
done


check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END

print_result