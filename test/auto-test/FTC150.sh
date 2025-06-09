#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2020-2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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


TC_ONELINE_DESCR="Sample tests of the SDNC A1 controller restconf API using http/https (no a1pms)"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="RICSIM SDNC KUBEPROXY"
#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES=" RICSIM SDNC KUBEPROXY"
#Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=" "

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES=""

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-MONTREAL ONAP-NEWDELHI ONAP-OSLO ONAP-PARIS ORAN-I-RELEASE ORAN-J-RELEASE ORAN-K-RELEASE ORAN-L-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

sim_generate_policy_uuid

#Test a1pms and simulator protocol versions (others are http only)
NB_TESTED_PROTOCOLS="HTTP HTTPS"
SB_TESTED_PROTOCOLS="HTTP HTTPS"

for __nb_httpx in $NB_TESTED_PROTOCOLS ; do
    for __sb_httpx in $SB_TESTED_PROTOCOLS ; do

        echo "#####################################################################"
        echo "#####################################################################"
        echo "### Testing SDNC using Northbound: $__nb_httpx and Southbound: $__sb_httpx"
        echo "#####################################################################"
        echo "#####################################################################"


        # Clean container and start all needed containers #
        clean_environment

        start_kube_proxy

        start_ric_simulators ricsim_g1 1  OSC_2.1.0
        start_ric_simulators ricsim_g2 1  STD_1.1.3
        start_ric_simulators ricsim_g3 1  STD_2.0.0


        if [ $__nb_httpx == "HTTPS" ]; then
            # "Using secure ports towards SDNC"
            if [[ "$SDNC_FEATURE_LEVEL" == *"NO_NB_HTTPS"* ]]; then
                deviation "SDNC does not support NB https"
                use_sdnc_http
            else
                use_sdnc_https
                fi
        else
            #"Using non-secure ports towards SDNC"
            use_sdnc_http
        fi

        start_sdnc
        controller_api_wait_for_status_ok 200 ricsim_g1_1

        if [ $__sb_httpx == "HTTPS" ]; then
            # "Using secure ports towards SDNC"
            use_simulator_https
        else
            #"Using non-secure ports towards SDNC"
            use_simulator_http
        fi

        # API tests

        controller_api_get_A1_policy_type 404 OSC ricsim_g1_1 1

        sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json


        controller_api_get_A1_policy_ids 200 OSC ricsim_g1_1 1
        controller_api_get_A1_policy_ids 200 STD ricsim_g2_1

        controller_api_get_A1_policy_type 200 OSC ricsim_g1_1 1
        controller_api_get_A1_policy_type 200 OSC ricsim_g1_1 1 testdata/OSC/sim_1.json
        controller_api_get_A1_policy_type 404 OSC ricsim_g1_1 99

        RESP=202
        if [ $FLAVOUR == "ONAP" ] && [[ "$SDNC_FEATURE_LEVEL" != *"TRANS_RESP_CODE"* ]]; then
            deviation "SDNC does not return original response code from sim"
            RESP=200
        fi
        controller_api_put_A1_policy $RESP OSC ricsim_g1_1 1 4000 testdata/OSC/pi1_template.json
        controller_api_put_A1_policy 404 OSC ricsim_g1_1 5 1001 testdata/OSC/pi1_template.json

        RESP=201
        if [ $FLAVOUR == "ONAP" ] && [[ "$SDNC_FEATURE_LEVEL" != *"TRANS_RESP_CODE"* ]]; then
            deviation "SDNC does not return original response code from sim"
            RESP=200
        fi
        controller_api_put_A1_policy $RESP STD ricsim_g2_1   5000 testdata/STD/pi1_template.json

        controller_api_get_A1_policy_ids 200 OSC ricsim_g1_1 1 4000
        controller_api_get_A1_policy_ids 200 STD ricsim_g2_1 5000

        controller_api_get_A1_policy_status 200 OSC ricsim_g1_1 1 4000
        controller_api_get_A1_policy_status 200 STD ricsim_g2_1 5000
        if [[ $TEST_ENV_PROFILE =~ ^ORAN-[A-H] ]] || [[ $TEST_ENV_PROFILE =~ ^ONAP-[A-L] ]]; then
          VAL='NOT IN EFFECT'
          VAL2="false"
        else
          VAL='NOT_ENFORCED'
          VAL2="OTHER_REASON"
        fi
        controller_api_get_A1_policy_status 200 OSC ricsim_g1_1 1 4000 "$VAL" "$VAL2"
        controller_api_get_A1_policy_status 200 STD ricsim_g2_1 5000 "UNDEFINED"

        RESP=202
        if [ $FLAVOUR == "ONAP" ] && [[ "$SDNC_FEATURE_LEVEL" != *"TRANS_RESP_CODE"* ]]; then
            deviation "SDNC does not return original response code from sim"
            RESP=200
        fi
        controller_api_delete_A1_policy $RESP OSC ricsim_g1_1 1 4000

        RESP=204
        if [ $FLAVOUR == "ONAP" ] && [[ "$SDNC_FEATURE_LEVEL" != *"TRANS_RESP_CODE"* ]]; then
            deviation "SDNC does not return original response code from sim"
            RESP=200
        fi
        controller_api_delete_A1_policy $RESP STD ricsim_g2_1 5000

        check_sdnc_logs

        store_logs          "NB_"$__nb_httpx"_SB_"$__sb_httpx

    done

done

#### TEST COMPLETE ####

print_result

auto_clean_environment