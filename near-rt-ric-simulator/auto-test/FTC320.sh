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

TC_ONELINE_DESCR="Create and delete 50 policies in each 50 RICs"

. ../common/testcase_common.sh  $@

#### TEST BEGIN ####

#Local vars in test script
##########################
# Path to callback receiver
CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"
# Number of RICs
NUM_RICS=25
# Number of instances per RIC
NUM_INST=50

clean_containers

start_ric_simulators ricsim_g1 $NUM_RICS OSC_2.1.0

start_ric_simulators ricsim_g2 $NUM_RICS STD_1.1.3

start_mr

start_cr

start_consul_cbs

prepare_consul_config      NOSDNC  ".consul_config.json"
consul_config_app                  ".consul_config.json"

start_control_panel

start_policy_agent

use_agent_rest

set_agent_debug

#use_agent_retries 503

echo "Checking agent is alive"
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

echo "Load policy type 1 in each group 1 simulator"
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_put_policy_type 201 ricsim_g1_$i 1 testdata/OSC/sim_1.json
done

echo "Checking the number of policy instances in each group 1 simulator, shall be 0"
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_equal ricsim_g1_$i num_instances 0
done

echo "Checking the number of policy instances in each group 2 simulator, shall be 0"
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_equal ricsim_g1_$i num_instances 0
done

echo "Checking the number of policy types in each group 1 simulator, shall be 1"
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_equal ricsim_g1_$i num_types 1
done

echo "Checking the number of policy types in each group 2 simulator, shall be 0"
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_equal ricsim_g2_$i num_types 0
done

echo "Check the number of rics, shall be the configured number: ""$(($NUM_RICS*2))"
api_equal json:rics "$(($NUM_RICS*2))" 120

api_equal json:policy_types 2 120  #Wait for the agent to refresh types from the simulator

sleep_wait 120

echo "Check the number of types in the agent for each group 1 rics, shall be 1"
for ((i=1; i<=$NUM_RICS; i++))
do
   api_equal json:policy_types?ric=ricsim_g1_$i 1
done

echo "Register the service"
api_put_service 201 "rapp1" 0 "$CR_PATH/callbacks/1"

echo "Loading "$NUM_INST" policies in each group 1 ric"
for ((i=1; i<=$NUM_RICS; i++))
do
   inst=$((1000000+$i*$NUM_INST))
   api_put_policy 201 "rapp1" ricsim_g1_$i 1 $inst testdata/OSC/pi1_template.json $NUM_INST
done

echo "Check the number of types in the agent for each group 1 rics, shall be 1"
for ((i=1; i<=$NUM_RICS; i++))
do
   api_equal json:policy_types?ric=ricsim_g1_$i 1
done

echo "Checking the number of policy types in each group 1 simulator, shall be 1"
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_equal ricsim_g1_$i num_types 1
done



echo "Check the number of types in the agent for each group 2 rics, shall be 1"
for ((i=1; i<=$NUM_RICS; i++))
do
   api_equal json:policy_types?ric=ricsim_g2_$i 1
done

echo "Loading "$NUM_INST" policies in each group 2 ric"
for ((i=1; i<=$NUM_RICS; i++))
do
   api_get_policy_types 200 ricsim_g2_$i EMPTY
   inst=$((2000000+$i*$NUM_INST))
   api_put_policy 201 "rapp1" ricsim_g2_$i NOTYPE $inst testdata/STD/pi1_template.json $NUM_INST
   api_get_policy_types 200 ricsim_g2_$i EMPTY
done

echo "Check the number of types in the agent for each group 2 rics, shall be 1"
for ((i=1; i<=$NUM_RICS; i++))
do
   api_equal json:policy_types?ric=ricsim_g2_$i 1
done

echo "Checking the number of policy instances in each group 1 simulator, shall be "$NUM_INST
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_equal ricsim_g1_$i num_instances 50
done

echo "Checking the number of policy instances in each group 1 simulator, shall be "$NUM_INST
for ((i=1; i<=$NUM_RICS; i++))
do
   sim_equal ricsim_g2_$i num_instances 50
done




check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END

print_result