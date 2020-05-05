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


TC_ONELINE_DESCR="Full agent API walk through using agent REST/DMAAP and with/without SDNC A1 Controller"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh
. ../common/controller_api_functions.sh

#### TEST BEGIN ####

#Local vars in test script
##########################

# Path to callback receiver
CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"

# Tested variants of REST/DMAAP/SDNC config
TESTED_VARIANTS="REST   DMAAP   REST+SDNC   DMAAP+SDNC"

#Test agent and simulator protocol versions (others are http only)
TESTED_PROTOCOLS="HTTP HTTPS"

for __httpx in $TESTED_PROTOCOLS ; do
    for interface in $TESTED_VARIANTS ; do

        echo "#####################################################################"
        echo "#####################################################################"
        echo "### Testing agent: "$interface
        echo "#####################################################################"
        echo "#####################################################################"


        # Clean container and start all needed containers #
        clean_containers

        if [ $__httpx == "HTTPS" ]; then
            echo "Using secure ports towards simulators"
            use_simulator_https
        else
            echo "Using non-secure ports towards simulators"
            use_simulator_http
        fi

        start_ric_simulators ricsim_g1 1  OSC_2.1.0
        start_ric_simulators ricsim_g2 1  STD_1.1.3

        start_mr

        start_cr

        start_consul_cbs

        if [ $interface == "REST+SDNC" ] || [ $interface == "DMAAP+SDNC" ]; then
            prepare_consul_config      SDNC    ".consul_config.json"
        else
            prepare_consul_config      NOSDNC  ".consul_config.json"
        fi

        consul_config_app                      ".consul_config.json"

        start_control_panel

        if [ $interface == "REST+SDNC" ] || [ $interface == "DMAAP+SDNC" ]; then
            start_sdnc
        fi

        start_policy_agent

        if [ $interface == "DMAAP" ] || [ $interface == "DMAAP+SDNC" ]; then
            use_agent_dmaap
        else
            if [ $__httpx == "HTTPS" ]; then
                echo "Using secure ports towards the agent"
                use_agent_rest_https
            else
                echo "Using non-secure ports towards the agent"
                use_agent_rest_http
            fi
        fi


        cr_equal received_callbacks 0
        mr_equal requests_submitted 0

        sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json
        sim_put_policy_type 201 ricsim_g1_1 2 testdata/OSC/sim_2.json

        api_equal json:rics 2 60

        api_equal json:policy_schemas 3 120

        api_equal json:policy_types 3

        api_equal json:policies 0

        api_equal json:policy_ids 0




        echo "############################################"
        echo "############## Health check ################"
        echo "############################################"

        api_get_status 200

        echo "############################################"
        echo "##### Service registry and supervision #####"
        echo "############################################"

        api_get_services 404 "rapp1"

        api_put_service 201 "rapp1" 1000 "$CR_PATH/1"

        api_put_service 200 "rapp1" 2000 "$CR_PATH/1"


        api_put_service 400 "rapp2" -1 "$CR_PATH/2"

        api_put_service 400 "rapp2" "wrong" "$CR_PATH/2"

        api_put_service 400 "rapp2" 100 "/test"

        api_put_service 400 "rapp2" 100 "test-path"

        api_put_service 201 "rapp2" 300 "ftp://localhost:80/test"

        api_get_services 200 "rapp1" "rapp1" 2000 "$CR_PATH/1"

        api_get_service_ids 200 "rapp1" "rapp2"


        api_put_service 201 "rapp3" 5000 "$CR_PATH/3"


        api_get_service_ids 200 "rapp1" "rapp2" "rapp3"


        api_get_services 200 "rapp1" "rapp1" 2000 "$CR_PATH/1"

        api_get_services 200 NOSERVICE "rapp1" 2000 "$CR_PATH/1" "rapp2" 300 "ftp://localhost:80/test" "rapp3" 5000 "$CR_PATH/3"

        api_get_services 200

        echo -e $YELLOW"TR2"$EYELLOW
        api_put_services_keepalive 201 "rapp1"
        echo -e $YELLOW"TR2"$EYELLOW
        api_put_services_keepalive 201 "rapp3"

        api_put_services_keepalive 200 "rapp1"

        api_put_services_keepalive 200 "rapp3"

        api_put_services_keepalive 404 "rapp5"

        api_get_service_ids 200 "rapp1" "rapp2" "rapp3"

        api_delete_services 204 "rapp1"

        api_get_service_ids 200 "rapp2" "rapp3"


        api_put_service 201 "rapp1" 50 "$CR_PATH/1"

        api_get_service_ids 200 "rapp1" "rapp2" "rapp3"


        api_delete_services 204 "rapp1"
        api_delete_services 204 "rapp3"

        api_equal json:services 1

        api_delete_services 204 "rapp2"

        api_equal json:services 0


        echo "############################################"
        echo "############## RIC Repository ##############"
        echo "############################################"

        api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE  ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"

        api_get_rics 200 1 "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

        api_get_rics 404 47

        api_get_rics 404 "test"


        api_get_ric 200 me1_ricsim_g1_1 ricsim_g1_1

        api_get_ric 200 me2_ricsim_g1_1 ricsim_g1_1

        api_get_ric 200 me1_ricsim_g2_1 ricsim_g2_1

        api_get_ric 200 me2_ricsim_g2_1 ricsim_g2_1

        api_get_ric 404 test


        echo "############################################"
        echo "########### A1 Policy Management ###########"
        echo "############################################"
        echo -e $YELLOW"TR9"$EYELLOW
        api_get_policy_schema 200 1 testdata/OSC/1-agent-modified.json
        echo -e $YELLOW"TR9"$EYELLOW
        api_get_policy_schema 200 2 testdata/OSC/2-agent-modified.json

        api_get_policy_schema 404 3
        echo -e $YELLOW"TR9"$EYELLOW
        api_get_policy_schemas 200 NORIC testdata/OSC/1-agent-modified.json testdata/OSC/2-agent-modified.json NOFILE
        echo -e $YELLOW"TR9"$EYELLOW
        api_get_policy_schemas 200 ricsim_g1_1 testdata/OSC/1-agent-modified.json testdata/OSC/2-agent-modified.json

        api_get_policy_schemas 200 ricsim_g2_1 NOFILE

        api_get_policy_schemas 404 test



        api_get_policy_types 200 NORIC 1 2 EMPTY

        api_get_policy_types 200 ricsim_g1_1 1 2

        api_get_policy_types 200 ricsim_g2_1 EMPTY

        api_get_policy_types 404 dummy-ric



        api_put_service 201 "rapp10" 3600 "$CR_PATH/1"
        echo -e $YELLOW"TR10"$EYELLOW
        api_put_policy 400 "unregistered-r-app" ricsim_g1_1 1 2000 testdata/OSC/pi1_template.json

        api_put_policy 201 "rapp10" ricsim_g1_1 1 5000 testdata/OSC/pi1_template.json
        api_put_policy 200 "rapp10" ricsim_g1_1 1 5000 testdata/OSC/pi1_template.json

        api_put_policy 201 "rapp10" ricsim_g2_1 NOTYPE 5100 testdata/STD/pi1_template.json
        api_put_policy 200 "rapp10" ricsim_g2_1 NOTYPE 5100 testdata/STD/pi1_template.json

        VAL='NOT IN EFFECT'
        api_get_policy_status 200 5000 OSC "$VAL" "false"
        api_get_policy_status 200 5100 STD "UNDEFINED"


        echo -e $YELLOW"TR10"$EYELLOW
        api_equal json:policies 2
        echo -e $YELLOW"TR10"$EYELLOW
        api_equal json:policy_ids 2
        echo -e $YELLOW"TR10"$EYELLOW
        api_get_policy_ids 200 NORIC NOSERVICE NOTYPE 5000 5100
        echo -e $YELLOW"TR10"$EYELLOW
        api_get_policy_ids 200 ricsim_g1_1 NOSERVICE NOTYPE 5000

        api_get_policy_ids 200 ricsim_g2_1 NOSERVICE NOTYPE 5100


        api_get_policy_ids 200 NORIC "rapp10" NOTYPE 5000 5100
        echo -e $YELLOW"TR10"$EYELLOW
        api_get_policy_ids 200 NORIC NOSERVICE 1 5000

        api_get_policy_ids 200 NORIC NOSERVICE 2 NOID

        api_get_policy_ids 200 ricsim_g2_1 NOSERVICE 1 NOID


        api_get_policy 200 5000 testdata/OSC/pi1_template.json

        api_get_policy 200 5100 testdata/STD/pi1_template.json



        api_get_policies 200 ricsim_g1_1 "rapp10" 1 5000 ricsim_g1_1 "rapp10" 1 testdata/OSC/pi1_template.json




        echo -e $YELLOW"TR10"$EYELLOW
        api_delete_policy 404 2000

        api_delete_policy 404 1500

        api_delete_policy 204 5000

        api_equal json:policies 1

        api_equal json:policy_ids 1

        api_delete_policy 204 5100

        api_equal json:policies 0

        api_equal json:policy_ids 0

        cr_equal received_callbacks 0

        if [ $interface == "DMAAP" ] || [ $interface == "DMAAP+SDNC" ]; then
            mr_greater requests_submitted 0
            VAL=$(mr_read requests_submitted)
            mr_equal requests_fetched $VAL
            mr_equal responses_submitted $VAL
            mr_equal responses_fetched $VAL
            mr_equal current_requests 0
            mr_equal current_responses 0
        else
            mr_equal requests_submitted 0
        fi

        check_policy_agent_logs
        check_control_panel_logs

        store_logs          $interface

    done

done

#### TEST COMPLETE ####


print_result

auto_clean_containers
