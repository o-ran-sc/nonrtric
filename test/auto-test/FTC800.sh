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

TC_ONELINE_DESCR="Create 10000 policies in sequence using http/https and Agent REST/DMAAP with/without SDNC controller"

#App names to exclude checking pulling images for, space separated list
EXCLUDED_IMAGES="SDNC_ONAP"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh

#### TEST BEGIN ####

generate_uuid

#Local vars in test script
##########################
# Number of policies in each sequence
NUM_POLICIES=10000

# Tested variants of REST/DMAAP/SDNC config
TESTED_VARIANTS="NOSDNC   SDNC"

#Test agent and simulator protocol versions (others are http only)
TESTED_PROTOCOLS="HTTP HTTPS"
for __httpx in $TESTED_PROTOCOLS ; do
    for interface in $TESTED_VARIANTS ; do

        echo "#####################################################################"
        echo "#####################################################################"
        echo "### Testing agent via $interface using $__httpx"
        echo "#####################################################################"
        echo "#####################################################################"

        #Local vars in test script
        ##########################

        if [ $__httpx == "HTTPS" ]; then
            # Path to callback receiver
            CR_PATH="https://$CR_APP_NAME:$CR_EXTERNAL_SECURE_PORT/callbacks"
            use_cr_https
        else
            # Path to callback receiver
            CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"
            use_cr_http
        fi

        # Policy instance start id
        START_ID=1

        clean_containers

        if [ $__httpx == "HTTPS" ]; then
            #"Using secure ports towards simulators"
            use_simulator_https
        else
            #"Using non-secure ports towards simulators"
            use_simulator_http
        fi

        start_ric_simulators ricsim_g1 1 OSC_2.1.0
        start_ric_simulators ricsim_g2 1 STD_1.1.3

        start_mr

        if [ $__httpx == "HTTPS" ]; then
            #echo "Using secure ports between agent and MR"
            use_mr_https
        else
            #"Using non-secure ports between agent and MR"
            use_mr_http
        fi

        start_cr

        if [ $interface == "SDNC" ]; then

            start_sdnc

            if [ $__httpx == "HTTPS" ]; then
                # "Using secure ports towards SDNC"
                use_sdnc_https
            else
                #"Using non-secure ports towards SDNC"
                use_sdnc_http
            fi
        fi

        start_consul_cbs

        if [ $interface == "SDNC" ]; then
            prepare_consul_config      SDNC    ".consul_config.json"
        else
            prepare_consul_config      NOSDNC  ".consul_config.json"
        fi

        consul_config_app                      ".consul_config.json"

        start_control_panel

        start_policy_agent

        set_agent_debug

        if [ $__httpx == "HTTPS" ]; then
            # "Using secure ports towards the agent"
            use_agent_rest_https
        else
            # "Using non-secure ports towards the agent"
            use_agent_rest_http
        fi


        cr_equal received_callbacks 0
        mr_equal requests_submitted 0


        api_get_status 200

        sim_print ricsim_g1_1 interface
        sim_print ricsim_g2_1 interface

        sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json


        api_equal json:policy_types 2 120  #Wait for the agent to refresh types from the simulators

        api_put_service 201 "serv1" 3600 "$CR_PATH/1"

        start_timer "Create polices in OSC via agent REST and $interface using "$__httpx
        api_put_policy 201 "serv1" ricsim_g1_1 1 $START_ID NOTRANSIENT testdata/OSC/pi1_template.json $NUM_POLICIES
        print_timer "Create polices in OSC via agent REST and $interface using "$__httpx

        sim_equal ricsim_g1_1 num_instances $NUM_POLICIES

        START_ID=$(($START_ID+$NUM_POLICIES))

        start_timer "Create polices in STD via agent REST and $interface using "$__httpx
        api_put_policy 201 "serv1" ricsim_g2_1 NOTYPE $START_ID NOTRANSIENT testdata/STD/pi1_template.json $NUM_POLICIES
        print_timer "Create polices in STD via agent REST and $interface using "$__httpx

        sim_equal ricsim_g2_1 num_instances $NUM_POLICIES

        if [ $__httpx == "HTTPS" ]; then
            echo "Using secure ports towards dmaap"
            use_agent_dmaap_https
        else
            echo "Using non-secure ports towards dmaap"
            use_agent_dmaap_http
        fi

        START_ID=$(($START_ID+$NUM_POLICIES))

        start_timer "Create polices in OSC via agent DMAAP, one by one, and $interface using "$__httpx
        api_put_policy 201 "serv1" ricsim_g1_1 1 $START_ID NOTRANSIENT testdata/OSC/pi1_template.json $NUM_POLICIES
        print_timer "Create polices in OSC via agent DMAAP, one by one, and $interface using "$__httpx

        sim_equal ricsim_g1_1 num_instances $((2*$NUM_POLICIES))

        START_ID=$(($START_ID+$NUM_POLICIES))

        start_timer "Create polices in STD via agent DMAAP, one by one, and $interface using "$__httpx
        api_put_policy 201 "serv1" ricsim_g2_1 NOTYPE $START_ID NOTRANSIENT testdata/STD/pi1_template.json $NUM_POLICIES
        print_timer "Create polices in STD via agent DMAAP, one by one, and $interface using "$__httpx

        sim_equal ricsim_g2_1 num_instances $((2*$NUM_POLICIES))

        START_ID=$(($START_ID+$NUM_POLICIES))

        start_timer "Create polices in OSC via agent DMAAP in batch and $interface using "$__httpx
        api_put_policy_batch 201 "serv1" ricsim_g1_1 1 $START_ID NOTRANSIENT testdata/OSC/pi1_template.json $NUM_POLICIES
        print_timer "Create polices in OSC via agent DMAAP in batch and $interface using "$__httpx

        sim_equal ricsim_g1_1 num_instances $((3*$NUM_POLICIES))

        START_ID=$(($START_ID+$NUM_POLICIES))

        start_timer "Create polices in STD via agent DMAAP in batch and $interface using "$__httpx
        api_put_policy_batch 201 "serv1" ricsim_g2_1 NOTYPE $START_ID NOTRANSIENT testdata/STD/pi1_template.json $NUM_POLICIES
        print_timer "Create polices in STD via agent DMAAP in batch and $interface using "$__httpx

        sim_equal ricsim_g2_1 num_instances $((3*$NUM_POLICIES))

        if [ $interface == "SDNC" ]; then
            sim_contains_str ricsim_g1_1 remote_hosts "a1-controller"
            sim_contains_str ricsim_g2_1 remote_hosts "a1-controller"
        else
            sim_contains_str ricsim_g1_1 remote_hosts "policy-agent"
            sim_contains_str ricsim_g2_1 remote_hosts "policy-agent"
        fi

        check_policy_agent_logs

        store_logs          "${__httpx}__${interface}"
    done
done

#### TEST COMPLETE ####

print_result

auto_clean_containers