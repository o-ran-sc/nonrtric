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

TC_ONELINE_DESCR="Repeatedly create and delete policies in each RICs for 24h (or configured number of days). Via agent REST/DMAAP/DMAAP_BATCH and SDNC using http or https"

#App names to exclude checking pulling images for, space separated list
EXCLUDED_IMAGES="ECS"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh

#### TEST BEGIN ####

generate_uuid

#Local vars in test script
##########################

# Number of RICs per interface type (OSC and STD)
NUM_RICS=30
# Number of policy instances per RIC
NUM_INSTANCES=5

DAYS=3

clean_containers

# use HTTP or HTTPS for all apis
HTTPX=HTTPS

if [ $HTTPX == "HTTP" ]; then
   # Path to callback receiver
   CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"
   use_cr_http
   use_agent_rest_http
   use_sdnc_http
   use_simulator_http
else
   # Path to callback receiver
   CR_PATH="https://$CR_APP_NAME:$CR_EXTERNAL_SECURE_PORT/callbacks"
   use_cr_https
   use_agent_rest_https
   use_sdnc_https
   use_simulator_https
fi

start_ric_simulators ricsim_g1 $NUM_RICS OSC_2.1.0

start_ric_simulators ricsim_g2 $NUM_RICS STD_1.1.3

start_mr

start_cr

start_consul_cbs

prepare_consul_config      SDNC  ".consul_config.json"
consul_config_app                  ".consul_config.json"

start_sdnc

start_control_panel

start_policy_agent


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
api_put_service 201 "serv1" 0 "$CR_PATH/1"

TEST_DURATION=$((24*3600*$DAYS))
TEST_START=$SECONDS

AGENT_INTERFACES="REST REST_PARALLEL DMAAP DMAAP-BATCH"

MR_MESSAGES=0

while [ $(($SECONDS-$TEST_START)) -lt $TEST_DURATION ]; do

    echo ""
    echo "#########################################################################################################"
    echo -e $BOLD"INFO: Test executed for: "$(($SECONDS-$TEST_START)) "seconds. Target is: "$TEST_DURATION" seconds."$EBOLD
    echo "#########################################################################################################"
    echo ""

   for interface in $AGENT_INTERFACES ; do

      echo "############################################"
      echo "## Testing using agent interface: $interface ##"
      echo "############################################"

      if [ $interface == "REST" ] || [ $interface == "REST_PARALLEL" ]; then
         if [ $HTTPX == "HTTP" ]; then
            use_agent_rest_http
         else
            use_agent_rest_https
         fi
      else
         if [ $HTTPX == "HTTPS" ]; then
               echo "Using secure ports towards dmaap"
               use_agent_dmaap_https
         else
               echo "Using non-secure ports towards dmaap"
               use_agent_dmaap_http
         fi
      fi

      echo "Create $NUM_INSTANCES instances in each OSC RIC"
      INSTANCE_ID=200000
      INSTANCES=0
      if [ $interface == "REST_PARALLEL" ]; then
         api_put_policy_parallel 201 "serv1" ricsim_g1_ $NUM_RICS 1 $INSTANCE_ID NOTRANSIENT testdata/OSC/pi1_template.json $NUM_INSTANCES 3
      fi
      for ((i=1; i<=$NUM_RICS; i++))
      do
         if [ $interface == "DMAAP-BATCH" ]; then
            api_put_policy_batch 201 "serv1" ricsim_g1_$i 1 $INSTANCE_ID NOTRANSIENT testdata/OSC/pi1_template.json $NUM_INSTANCES
         elif [ $interface == "DMAAP" ] || [ $interface == "REST" ]; then
            api_put_policy 201 "serv1" ricsim_g1_$i 1 $INSTANCE_ID NOTRANSIENT testdata/OSC/pi1_template.json $NUM_INSTANCES
         fi
         if [ $interface == "DMAAP" ] || [ $interface == "DMAAP-BATCH" ]; then
            MR_MESSAGES=$(($MR_MESSAGES+$NUM_INSTANCES))
         fi
         sim_equal ricsim_g1_$i num_instances $NUM_INSTANCES
         INSTANCE_ID=$(($INSTANCE_ID+$NUM_INSTANCES))
         INSTANCES=$(($INSTANCES+$NUM_INSTANCES))
      done

      api_equal json:policy_ids $INSTANCES

      echo "Create $NUM_INSTANCES instances in each STD RIC"
      if [ $interface == "REST_PARALLEL" ]; then
         api_put_policy_parallel 201 "serv1" ricsim_g2_ $NUM_RICS NOTYPE $INSTANCE_ID NOTRANSIENT testdata/STD/pi1_template.json $NUM_INSTANCES 3
      fi
      for ((i=1; i<=$NUM_RICS; i++))
      do
         if [ $interface == "DMAAP-BATCH" ]; then
            api_put_policy_batch 201 "serv1" ricsim_g2_$i NOTYPE $INSTANCE_ID NOTRANSIENT testdata/STD/pi1_template.json $NUM_INSTANCES
         elif [ $interface == "DMAAP" ] || [ $interface == "REST" ]; then
            api_put_policy 201 "serv1" ricsim_g2_$i NOTYPE $INSTANCE_ID NOTRANSIENT testdata/STD/pi1_template.json $NUM_INSTANCES
         fi
         if [ $interface == "DMAAP" ] || [ $interface == "DMAAP-BATCH" ]; then
            MR_MESSAGES=$(($MR_MESSAGES+$NUM_INSTANCES))
         fi
         sim_equal ricsim_g2_$i num_instances $NUM_INSTANCES
         INSTANCE_ID=$(($INSTANCE_ID+$NUM_INSTANCES))
         INSTANCES=$(($INSTANCES+$NUM_INSTANCES))
      done

      api_equal json:policy_ids $INSTANCES


      echo "Delete all instances in each OSC RIC"

      INSTANCE_ID=200000
      if [ $interface == "REST_PARALLEL" ]; then
         api_delete_policy_parallel 204 $NUM_RICS $INSTANCE_ID $NUM_INSTANCES 3
      fi
      for ((i=1; i<=$NUM_RICS; i++))
      do
         if [ $interface == "DMAAP-BATCH" ]; then
            api_delete_policy_batch 204 $INSTANCE_ID $NUM_INSTANCES
         elif [ $interface == "DMAAP" ] || [ $interface == "REST" ]; then
            api_delete_policy 204 $INSTANCE_ID $NUM_INSTANCES
         fi
         if [ $interface == "DMAAP" ] || [ $interface == "DMAAP-BATCH" ]; then
            MR_MESSAGES=$(($MR_MESSAGES+$NUM_INSTANCES))
         fi
         INSTANCES=$(($INSTANCES-$NUM_INSTANCES))
         sim_equal ricsim_g1_$i num_instances 0
         INSTANCE_ID=$(($INSTANCE_ID+$NUM_INSTANCES))
      done

      api_equal json:policy_ids $INSTANCES

      echo "Delete all instances in each STD RIC"

      if [ $interface == "REST_PARALLEL" ]; then
         api_delete_policy_parallel 204 $NUM_RICS $INSTANCE_ID $NUM_INSTANCES 3
      fi
      for ((i=1; i<=$NUM_RICS; i++))
      do
         if [ $interface == "DMAAP-BATCH" ]; then
            api_delete_policy_batch 204 $INSTANCE_ID $NUM_INSTANCES
         elif [ $interface == "DMAAP" ] || [ $interface == "REST" ]; then
            api_delete_policy 204 $INSTANCE_ID $NUM_INSTANCES
         fi
         if [ $interface == "DMAAP" ] || [ $interface == "DMAAP-BATCH" ]; then
            MR_MESSAGES=$(($MR_MESSAGES+$NUM_INSTANCES))
         fi
         INSTANCES=$(($INSTANCES-$NUM_INSTANCES))
         sim_equal ricsim_g2_$i num_instances 0
         INSTANCE_ID=$(($INSTANCE_ID+$NUM_INSTANCES))
      done

      api_equal json:policy_ids 0

      cr_equal received_callbacks 0

      mr_equal requests_submitted $MR_MESSAGES
      mr_equal requests_fetched $MR_MESSAGES
      mr_equal responses_submitted $MR_MESSAGES
      mr_equal responses_fetched $MR_MESSAGES
      mr_equal current_requests 0
      mr_equal current_responses 0


      for ((i=1; i<=$NUM_RICS; i++))
      do
         sim_contains_str ricsim_g1_$i remote_hosts "a1-controller"
         sim_contains_str ricsim_g2_$i remote_hosts "a1-controller"
      done

   done

done

check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_containers