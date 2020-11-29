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


TC_ONELINE_DESCR="Resync of RIC via changes in the consul config or pushed config"

#App names to include in the test, space separated list
INCLUDED_IMAGES="CBS CONSUL CP CR MR PA RICSIM"

#SUPPORTED TEST ENV FILE
SUPPORTED_PROFILES="ONAP-MASTER ONAP-GUILIN ORAN-CHERRY"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh
. ../common/cr_api_functions.sh

#### TEST BEGIN ####

if [ "$PMS_VERSION" == "V2" ]; then
    TESTED_VARIANTS="CONSUL NOCONSUL"
else
    TESTED_VARIANTS="CONSUL"
fi

for consul_conf in $TESTED_VARIANTS ; do
    generate_uuid

    # Clean container and start all needed containers #
    clean_containers

    start_policy_agent

    set_agent_trace

    # Create service to be able to receive events when rics becomes available
    # Must use rest towards the agent since dmaap is not configured yet
    api_put_service 201 "ric-registration" 0 "$CR_PATH/ric-registration"

    # Start one RIC of each type
    start_ric_simulators ricsim_g1 1  OSC_2.1.0
    start_ric_simulators ricsim_g2 1  STD_1.1.3
    if [ "$PMS_VERSION" == "V2" ]; then
        start_ric_simulators ricsim_g3 1  STD_2.0.0
    fi

    start_mr

    start_cr

    start_control_panel

    if [ $consul_conf == "CONSUL" ]; then
        start_consul_cbs
    fi

    prepare_consul_config      NOSDNC  ".consul_config.json"

    if [ "$PMS_VERSION" == "V2" ] && [ $consul_conf == "NOCONSUL" ]; then
        api_put_configuration 200 ".consul_config.json"
        api_get_configuration 200 ".consul_config.json"
    else
        consul_config_app                  ".consul_config.json"
    fi

    if [ "$PMS_VERSION" == "V2" ]; then
        api_equal json:rics 3 120

        cr_equal received_callbacks 3 120

        cr_api_check_all_sync_events 200 ric-registration ricsim_g1_1 ricsim_g2_1 ricsim_g3_1
    else
        api_equal json:rics 2 120
    fi

    # Add an STD RIC and check
    start_ric_simulators ricsim_g2 2  STD_1.1.3

    prepare_consul_config      NOSDNC  ".consul_config.json"
    if [ "$PMS_VERSION" == "V2" ] && [ $consul_conf == "NOCONSUL" ]; then
        api_put_configuration 200 ".consul_config.json"
        api_get_configuration 200 ".consul_config.json"
    else
        consul_config_app                  ".consul_config.json"
    fi

    if [ "$PMS_VERSION" == "V2" ]; then
        api_equal json:rics 4 120

        cr_equal received_callbacks 4 120

        cr_api_check_all_sync_events 200 ric-registration ricsim_g2_2
    else
        api_equal json:rics 3 120
    fi

    check_policy_agent_logs
    check_control_panel_logs

    # Remove one RIC RIC and check
    start_ric_simulators ricsim_g2 1  STD_1.1.3

    prepare_consul_config      NOSDNC  ".consul_config.json"
    if [ "$PMS_VERSION" == "V2" ] && [ $consul_conf == "NOCONSUL" ]; then
        api_put_configuration 200 ".consul_config.json"
        api_get_configuration 200 ".consul_config.json"
    else
        consul_config_app                  ".consul_config.json"
    fi

    if [ "$PMS_VERSION" == "V2" ]; then
        api_equal json:rics 3 120

        cr_equal received_callbacks 4 120
    else
        api_equal json:rics 2 120
    fi

    if [ "$PMS_VERSION" == "V2" ] && [ $consul_conf == "NOCONSUL" ]; then
        api_get_configuration 200 ".consul_config.json"
    fi

    check_policy_agent_logs
    check_control_panel_logs

    store_logs          END_$consul_conf
done


#### TEST COMPLETE ####


print_result

auto_clean_containers
