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


TC_ONELINE_DESCR="Full agent API walk through using agent REST/DMAAP and with/without SDNC A1 Controller"

#App names to include in the test, space separated list
INCLUDED_IMAGES="CBS CONSUL CP CR MR PA RICSIM SDNC"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh

#### TEST BEGIN ####

generate_uuid

# Tested variants of REST/DMAAP/SDNC config
TESTED_VARIANTS="REST   DMAAP   REST+SDNC   DMAAP+SDNC"

#Test agent and simulator protocol versions (others are http only)
TESTED_PROTOCOLS="HTTP HTTPS"

for __httpx in $TESTED_PROTOCOLS ; do
    for interface in $TESTED_VARIANTS ; do

        echo "#####################################################################"
        echo "#####################################################################"
        echo "### Testing agent: $interface using $__httpx"
        echo "#####################################################################"
        echo "#####################################################################"

        if [ $__httpx == "HTTPS" ]; then
            CR_PATH="https://$CR_APP_NAME:$CR_EXTERNAL_SECURE_PORT/callbacks"
            use_cr_https
            use_simulator_https
            use_mr_https
            if [[ $interface = *"SDNC"* ]]; then
                use_sdnc_https
            fi
            if [[ $interface = *"DMAAP"* ]]; then
                use_agent_dmaap_https
            else
                use_agent_rest_https
            fi
        else
            CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"
            use_cr_http
            use_simulator_http
            use_mr_http
            if [[ $interface = *"SDNC"* ]]; then
                use_sdnc_http
            fi
            if [[ $interface = *"DMAAP"* ]]; then
                use_agent_dmaap_http
            else
                use_agent_rest_http
            fi
        fi

        # Clean container and start all needed containers #
        clean_containers

        start_ric_simulators ricsim_g1 1  OSC_2.1.0
        start_ric_simulators ricsim_g2 1  STD_1.1.3
        if [ "$PMS_VERSION" == "V2" ]; then
            start_ric_simulators ricsim_g3 1  STD_2.0.0
        fi

        start_mr

        start_cr

        start_consul_cbs

        if [[ $interface = *"SDNC"* ]]; then
            start_sdnc
            prepare_consul_config      SDNC    ".consul_config.json"
        else
            prepare_consul_config      NOSDNC  ".consul_config.json"
        fi

        consul_config_app                      ".consul_config.json"

        start_control_panel

        start_policy_agent

        set_agent_debug

        cr_equal received_callbacks 0
        mr_equal requests_submitted 0

        sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json
        sim_put_policy_type 201 ricsim_g1_1 2 testdata/OSC/sim_2.json

        if [ "$PMS_VERSION" == "V2" ]; then
            api_equal json:rics 3 60

            #api_equal json:policy-schemas 3 120

            api_equal json:policy-types 3 120

            api_equal json:policies 0

            api_equal json:policy_instances 0
        else
            api_equal json:rics 2 60

            api_equal json:policy_schemas 3 120

            api_equal json:policy_types 3

            api_equal json:policies 0

            api_equal json:policy_ids 0
        fi


        echo "############################################"
        echo "############## Health check ################"
        echo "############################################"

        api_get_status 200

        echo "############################################"
        echo "##### Service registry and supervision #####"
        echo "############################################"

        api_get_services 404 "service1"

        api_put_service 201 "service1" 1000 "$CR_PATH/1"

        api_put_service 200 "service1" 2000 "$CR_PATH/1"


        api_put_service 400 "service2" -1 "$CR_PATH/2"

        api_put_service 400 "service2" "wrong" "$CR_PATH/2"

        api_put_service 400 "service2" 100 "/test"

        api_put_service 400 "service2" 100 "test-path"

        api_put_service 201 "service2" 300 "ftp://localhost:80/test"

        api_get_services 200 "service1" "service1" 2000 "$CR_PATH/1"

        api_get_service_ids 200 "service1" "service2"


        api_put_service 201 "service3" 5000 "$CR_PATH/3"


        api_get_service_ids 200 "service1" "service2" "service3"


        api_get_services 200 "service1" "service1" 2000 "$CR_PATH/1"

        api_get_services 200 NOSERVICE "service1" 2000 "$CR_PATH/1" "service2" 300 "ftp://localhost:80/test" "service3" 5000 "$CR_PATH/3"

        api_get_services 200

        deviation "TR2 - Keep alive shall return 200/201 according to doc, only 200 works - test combo $interface and $__httpx"
        #The below should work, keept here until fixed or other decision made
        #api_put_services_keepalive 201 "service1"
        #Using the below until decision
        api_put_services_keepalive 200 "service1"

        deviation "TR2 - Keep alive shall return 200/201 according to doc, only 200 works - test combo $interface and $__httpx"
        #The below should work, keept here until fixed or other decision made
        #api_put_services_keepalive 201 "service3"
        #Using the below until decision
        api_put_services_keepalive 200 "service3"

        api_put_services_keepalive 200 "service1"

        api_put_services_keepalive 200 "service3"

        api_put_services_keepalive 404 "service5"

        api_get_service_ids 200 "service1" "service2" "service3"

        api_delete_services 204 "service1"

        api_get_service_ids 200 "service2" "service3"


        api_put_service 201 "service1" 50 "$CR_PATH/1"

        api_get_service_ids 200 "service1" "service2" "service3"


        api_delete_services 204 "service1"
        api_delete_services 204 "service3"

        api_equal json:services 1

        api_delete_services 204 "service2"

        api_equal json:services 0


        echo "############################################"
        echo "############## RIC Repository ##############"
        echo "############################################"

        if [ "$PMS_VERSION" == "V2" ]; then
            api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE  ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE ricsim_g3_1:me1_ricsim_g3_1,me2_ricsim_g3_1:NOTYPE:AVAILABLE"
        else
            api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE  ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"
        fi
        api_get_rics 200 1 "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

        api_get_rics 404 47

        api_get_rics 404 "test"

        if [ "$PMS_VERSION" == "V2" ]; then
            api_get_ric 200 me1_ricsim_g1_1 NORIC "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

            api_get_ric 200 me2_ricsim_g1_1 NORIC "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

            api_get_ric 200 me1_ricsim_g2_1 NORIC "ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"

            api_get_ric 200 me2_ricsim_g2_1 NORIC "ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"

            api_get_ric 200 NOME      ricsim_g1_1 "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

            api_get_ric 200 NOME      ricsim_g2_1 "ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"

            api_get_ric 404 NOME test1

            api_get_ric 404 test NORIC

            api_get_ric 400 me1_ricsim_g1_1 ricsim_g1_1

            api_get_ric 400 me1_ricsim_g1_1 TESTRIC

            api_get_ric 400 TESTME ricsim_g1_1

        else
            api_get_ric 200 me1_ricsim_g1_1 ricsim_g1_1

            api_get_ric 200 me2_ricsim_g1_1 ricsim_g1_1

            api_get_ric 200 me1_ricsim_g2_1 ricsim_g2_1

            api_get_ric 200 me2_ricsim_g2_1 ricsim_g2_1

            api_get_ric 404 test
        fi

        echo "############################################"
        echo "########### A1 Policy Management ###########"
        echo "############################################"

        if [ "$PMS_VERSION" == "V2" ]; then
            deviation "TR9 - agent modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            api_get_policy_type 200 1 testdata/OSC/1-agent-modified.json
            deviation "TR9 - agent modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            api_get_policy_type 200 2 testdata/OSC/2-agent-modified.json

            api_get_policy_type 404 3
        else
            deviation "TR9 - agent modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            api_get_policy_schema 200 1 testdata/OSC/1-agent-modified.json
            deviation "TR9 - agent modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            api_get_policy_schema 200 2 testdata/OSC/2-agent-modified.json

            api_get_policy_schema 404 3
        fi

        if [ "$PMS_VERSION" == "V2" ]; then
            api_get_policy_schemas 404
        else
            deviation "TR9 - agent modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            api_get_policy_schemas 200 NORIC testdata/OSC/1-agent-modified.json testdata/OSC/2-agent-modified.json NOFILE
            deviation "TR9 - agent modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            api_get_policy_schemas 200 ricsim_g1_1 testdata/OSC/1-agent-modified.json testdata/OSC/2-agent-modified.json

            api_get_policy_schemas 200 ricsim_g2_1 NOFILE

            api_get_policy_schemas 404 test
        fi



        api_get_policy_types 200 NORIC 1 2 EMPTY

        api_get_policy_types 200 ricsim_g1_1 1 2

        api_get_policy_types 200 ricsim_g2_1 EMPTY

        api_get_policy_types 404 dummy-ric



        api_put_service 201 "service10" 3600 "$CR_PATH/1"

        if [ "$PMS_VERSION" == "V2" ]; then
            notificationurl="http://localhost:80"
        else
            notificationurl=""
        fi
        deviation "TR10 - agent allows policy creation on unregistered service (orig problem) - test combo $interface and $__httpx"
        #Kept until decison
        #api_put_policy 400 "unregistered-service" ricsim_g1_1 1 2000 NOTRANSIENT testdata/OSC/pi1_template.json
        #Allow 201 for now
        api_put_policy 201 "unregistered-service" ricsim_g1_1 1 2000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json

        api_put_policy 201 "service10" ricsim_g1_1 1 5000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json
        api_put_policy 200 "service10" ricsim_g1_1 1 5000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json

        api_put_policy 200 "service10" ricsim_g1_1 1 5000 true $notificationurl testdata/OSC/pi1_template.json
        api_put_policy 200 "service10" ricsim_g1_1 1 5000 false $notificationurl testdata/OSC/pi1_template.json

        api_put_policy 201 "service10" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json
        api_put_policy 200 "service10" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json

        api_put_policy 200 "service10" ricsim_g2_1 NOTYPE 5100 true $notificationurl testdata/STD/pi1_template.json
        api_put_policy 200 "service10" ricsim_g2_1 NOTYPE 5100 false $notificationurl testdata/STD/pi1_template.json

        VAL='NOT IN EFFECT'
        api_get_policy_status 200 5000 OSC "$VAL" "false"
        api_get_policy_status 200 5100 STD "UNDEFINED"


        deviation "TR10 - agent allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #api_equal json:policies 2
        #Allow 3 for now
        api_equal json:policies 3

        deviation "TR10 - agent allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #api_equal json:policy_ids 2
        #Allow 3 for now
        if [ "$PMS_VERSION" == "V2" ]; then
            api_equal json:policy_instances 3
        else
            api_equal json:policy_ids 3
        fi

        deviation "TR10 - agent allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #api_get_policy_ids 200 NORIC NOSERVICE NOTYPE 5000 5100
        #Allow policy create with unregistered service for now
        api_get_policy_ids 200 NORIC NOSERVICE NOTYPE 5000 5100 2000


        deviation "TR10 - agent allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #api_get_policy_ids 200 ricsim_g1_1 NOSERVICE NOTYPE 5000
        #Allow policy create with unregistered service for now
        api_get_policy_ids 200 ricsim_g1_1 NOSERVICE NOTYPE 5000 2000

        api_get_policy_ids 200 ricsim_g2_1 NOSERVICE NOTYPE 5100


        api_get_policy_ids 200 NORIC "service10" NOTYPE 5000 5100

        deviation "TR10 - agent allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #api_get_policy_ids 200 NORIC NOSERVICE 1 5000
        #Allow policy create with unregistered service for now
        api_get_policy_ids 200 NORIC NOSERVICE 1 5000 2000

        api_get_policy_ids 200 NORIC NOSERVICE 2 NOID

        api_get_policy_ids 200 ricsim_g2_1 NOSERVICE 1 NOID

        if [ "$PMS_VERSION" == "V2" ]; then
            api_get_policy 200 5000 testdata/OSC/pi1_template.json "service10" ricsim_g1_1 1 false $notificationurl

            api_get_policy 200 5100 testdata/STD/pi1_template.json "service10" ricsim_g2_1 NOTYPE false $notificationurl

            api_get_policies 200 ricsim_g1_1 "service10" 1 5000 ricsim_g1_1 "service10" 1 false $notificationurl testdata/OSC/pi1_template.json
        else
            api_get_policy 200 5000 testdata/OSC/pi1_template.json

            api_get_policy 200 5100 testdata/STD/pi1_template.json

            api_get_policies 200 ricsim_g1_1 "service10" 1 5000 ricsim_g1_1 "service10" 1 testdata/OSC/pi1_template.json
        fi

        deviation "TR10 - agent allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #api_delete_policy 404 2000
        #Allow policy create with unregistered service for now
        api_delete_policy 204 2000

        api_delete_policy 404 1500

        api_delete_policy 204 5000

        api_equal json:policies 1

        if [ "$PMS_VERSION" == "V2" ]; then
            api_equal json:policy_instances 1
        else
            api_equal json:policy_ids 1
        fi

        api_delete_policy 204 5100

        api_equal json:policies 0

        if [ "$PMS_VERSION" == "V2" ]; then
            api_equal json:policy_instances 0
        else
            api_equal json:policy_ids 0
        fi

        cr_equal received_callbacks 0

        if [[ $interface = *"DMAAP"* ]]; then
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

        if [[ $interface = *"SDNC"* ]]; then
            sim_contains_str ricsim_g1_1 remote_hosts "a1-controller"
            sim_contains_str ricsim_g2_1 remote_hosts "a1-controller"
            if [ "$PMS_VERSION" == "V2" ]; then
                sim_contains_str ricsim_g3_1 remote_hosts "a1-controller"
            fi
        else
            sim_contains_str ricsim_g1_1 remote_hosts "policy-agent"
            sim_contains_str ricsim_g2_1 remote_hosts "policy-agent"
            if [ "$PMS_VERSION" == "V2" ]; then
                sim_contains_str ricsim_g3_1 remote_hosts "policy-agent"
            fi
        fi

        check_policy_agent_logs
        check_control_panel_logs

        store_logs          "${__httpx}__${interface}"

    done

done

#### TEST COMPLETE ####


print_result

auto_clean_containers
