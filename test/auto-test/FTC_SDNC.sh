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

TC_ONELINE_DESCR="Create 1 policy with SDNC and 1 policy without SDNC over agent REST"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh
. ../common/controller_api_functions.sh

#### TEST BEGIN ####

VARIANTS="NOSDNC SDNC"

for TEST in $VARIANTS ; do


    clean_containers

    start_ric_simulators ricsim_g1 1 OSC_2.1.0
    start_ric_simulators ricsim_g2 1 STD_1.1.3

    start_mr

    start_cr

    start_consul_cbs

    prepare_consul_config      $TEST  ".consul_config.json"
    consul_config_app                  ".consul_config.json"

    start_control_panel

    if [ $TEST == "SDNC" ]; then
        start_sdnc
    fi

    start_policy_agent


    set_agent_debug

    use_agent_rest_http

    echo "Using: "$TEST

    api_get_status 200

    sim_print ricsim_g1_1 interface
    sim_print ricsim_g2_1 interface

    sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json

    api_equal json:policy_types 2 120  #Wait for the agent to refresh types from the simulator

    api_put_service 201 "rapp1" 3600 "http://callback-receiver:8090/callbacks/1"

    api_put_policy 201 "rapp1" ricsim_g1_1 1 2000 testdata/OSC/pi1_template.json 10

    api_put_policy 201 "rapp1" ricsim_g2_1 NOTYPE 3000 testdata/STD/pi1_template.json 10

    sim_equal ricsim_g1_1 num_instances 10
    sim_equal ricsim_g2_1 num_instances 10


    check_policy_agent_logs
    check_control_panel_logs

    store_logs          $TEST

    echo ""
    echo -e $BOLD"Test complete for variant: "$TEST $EBOLD
    echo ""

done

#### TEST COMPLETE ####

print_result

