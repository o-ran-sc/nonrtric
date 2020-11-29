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

TC_ONELINE_DESCR="Create/delete policies in parallel over a number of rics using a number of child process"

#App names to include in the test, space separated list
INCLUDED_IMAGES="CBS CONSUL CP CR MR PA RICSIM SDNC"

#SUPPORTED TEST ENV FILE
SUPPORTED_PROFILES="ONAP-MASTER ONAP-GUILIN ORAN-CHERRY"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh

#### TEST BEGIN ####

#Local vars in test script
##########################

# Tested variants of REST/DMAAP/SDNC config
TESTED_VARIANTS="REST   REST+SDNC"

#Test agent and simulator protocol versions (others are http only)
TESTED_PROTOCOLS="HTTP HTTPS"

NUM_RICS=20
NUM_POLICIES_PER_RIC=500

generate_uuid

if [ "$PMS_VERSION" == "V2" ]; then
    notificationurl=$CR_PATH"/test"
else
    notificationurl=""
fi

for __httpx in $TESTED_PROTOCOLS ; do
    for interface in $TESTED_VARIANTS ; do

        echo "#####################################################################"
        echo "#####################################################################"
        echo "### Testing agent: "$interface" and "$__httpx
        echo "#####################################################################"
        echo "#####################################################################"

        if [ $__httpx == "HTTPS" ]; then
            use_cr_https
            use_simulator_https
            use_mr_https
            if [[ $interface = *"SDNC"* ]]; then
                use_sdnc_https
            fi
            use_agent_rest_https
        else
            use_cr_http
            use_simulator_http
            use_mr_http
            if [[ $interface = *"SDNC"* ]]; then
                use_sdnc_http
            fi
            use_agent_rest_http
        fi

        # Clean container and start all needed containers #
        clean_containers

        start_ric_simulators ricsim_g1 $NUM_RICS OSC_2.1.0

        start_consul_cbs

        if [[ $interface = *"SDNC"* ]]; then
            start_sdnc
            prepare_consul_config      SDNC  ".consul_config.json"
        else
            prepare_consul_config      NOSDNC  ".consul_config.json"
        fi

        consul_config_app                  ".consul_config.json"

        start_mr # Not used, but removes error messages from the agent log

        start_cr

        start_control_panel

        start_policy_agent

        set_agent_debug

        api_get_status 200

        for ((i=1; i<=$NUM_RICS; i++))
        do
            sim_print ricsim_g1_$i interface
        done

        echo "Load policy type in group 1 simulators"
        for ((i=1; i<=$NUM_RICS; i++))
        do
            sim_put_policy_type 201 ricsim_g1_$i 1 testdata/OSC/sim_1.json
        done

        if [ "$PMS_VERSION" == "V2" ]; then
            api_equal json:policy-types 1 120  #Wait for the agent to refresh types from the simulator
        else
            api_equal json:policy_types 1 120  #Wait for the agent to refresh types from the simulator
        fi

        api_put_service 201 "serv1" 600 "$CR_PATH/1"

        echo "Check the number of types in the agent for each ric is 1"
        for ((i=1; i<=$NUM_RICS; i++))
        do
            if [ "$PMS_VERSION" == "V2" ]; then
                api_equal json:policy-types?ric_id=ricsim_g1_$i 1 120
            else
                api_equal json:policy_types?ric=ricsim_g1_$i 1 120
            fi
        done

        START_ID=2000

        start_timer "Create $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using "$__httpx

        api_put_policy_parallel 201 "serv1" ricsim_g1_ $NUM_RICS 1 $START_ID NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json $NUM_POLICIES_PER_RIC 7

        print_timer "Create $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using "$__httpx

        INSTANCES=$(($NUM_RICS*$NUM_POLICIES_PER_RIC))
        api_equal json:policies $INSTANCES

        for ((i=1; i<=$NUM_RICS; i++))
        do
            sim_equal ricsim_g1_$i num_instances $NUM_POLICIES_PER_RIC
        done

        start_timer "Delete $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using "$__httpx

        api_delete_policy_parallel 204 $NUM_RICS $START_ID $NUM_POLICIES_PER_RIC 7

        print_timer "Delete $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using "$__httpx

        api_equal json:policies 0

        for ((i=1; i<=$NUM_RICS; i++))
        do
            sim_equal ricsim_g1_$i num_instances 0
        done

        for ((i=1; i<=$NUM_RICS; i++))
        do
            if [ $interface == "REST+SDNC" ]; then
                sim_contains_str ricsim_g1_$i remote_hosts "a1-controller"
            else
                sim_contains_str ricsim_g1_$i remote_hosts "policy-agent"
            fi
        done

        check_policy_agent_logs
        if [[ $interface = *"SDNC"* ]]; then
            check_sdnc_logs
        fi

        store_logs          "${__httpx}__${interface}"

    done

done


#### TEST COMPLETE ####

print_result

auto_clean_containers