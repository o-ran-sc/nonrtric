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

TC_ONELINE_DESCR="Create/delete policies in parallel over a number of rics using a number of child process"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM SDNC NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM SDNC KUBEPROXY NGW"
#Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="NGW"

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-MONTREAL ONAP-NEWDELHI ONAP-OSLO ONAP-PARIS ORAN-I-RELEASE ORAN-J-RELEASE ORAN-K-RELEASE ORAN-L-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

#Local vars in test script
##########################

# Tested variants of REST/DMAAP/SDNC config
TESTED_VARIANTS="REST   REST+SDNC"

#Test a1pms and simulator protocol versions (others are http only)
TESTED_PROTOCOLS="HTTP HTTPS"

NUM_RICS=20
NUM_POLICIES_PER_RIC=500

sim_generate_policy_uuid

notificationurl=$CR_SERVICE_APP_PATH_0"/test"

VERSIONS_TO_RUN=2

if [ -n "$A1PMS_V3_FLAG" ] && [ "${A1PMS_V3_FLAG,,}" == "true" ]; then
  VERSIONS_TO_RUN=3
fi

for version in $(seq 2 $VERSIONS_TO_RUN); do
  if [ "$version" == "3" ]; then
    if [ -n "$A1PMS_V3" ] && [ -n "$A1PMS_API_PREFIX_V3" ] && [ -n "$A1PMS_ALIVE_URL_V3" ]; then
      export A1PMS_VERSION="$A1PMS_V3"
      export A1PMS_API_PREFIX="$A1PMS_API_PREFIX_V3"
      export A1PMS_ALIVE_URL="$A1PMS_API_PREFIX$A1PMS_ALIVE_URL_V3"
    else
      echo "One/All parameters not set in env file (<A1PMS_V3>, <A1PMS_API_PREFIX_V3>, <A1PMS_ALIVE_URL_V3>)"
      break
    fi
  fi
    for __httpx in $TESTED_PROTOCOLS ; do
        for interface in $TESTED_VARIANTS ; do

            echo "#####################################################################"
            echo "#####################################################################"
            echo "### Testing a1pms: "$interface" and "$__httpx" with a1pms V"$version
            echo "#####################################################################"
            echo "#####################################################################"

            if [ $__httpx == "HTTPS" ]; then
                use_cr_https
                use_simulator_https
                if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
                    :
                else
                    use_mr_https
                fi
                if [[ $interface = *"SDNC"* ]]; then
                    if [[ "$SDNC_FEATURE_LEVEL" == *"NO_NB_HTTPS"* ]]; then
                        deviation "SDNC does not support NB https"
                        use_sdnc_http
                    else
                        use_sdnc_https
                    fi
                fi
                use_a1pms_rest_https
            else
                use_cr_http
                use_simulator_http
                if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
                    :
                else
                    use_mr_http
                fi
                if [[ $interface = *"SDNC"* ]]; then
                    use_sdnc_http
                fi
                use_a1pms_rest_http
            fi

            # Clean container and start all needed containers #
            clean_environment

            start_kube_proxy

            start_ric_simulators ricsim_g1 $NUM_RICS OSC_2.1.0

            start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

            if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
                start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
            fi

            start_a1pms NORPOXY $SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_CONFIG_FILE

            set_a1pms_debug



            if [[ $interface = *"SDNC"* ]]; then
                start_sdnc
                controller_api_wait_for_status_ok 200 ricsim_g1_1
                prepare_a1pms_config      SDNC  ".a1pms_config.json"
            else
                prepare_a1pms_config      NOSDNC  ".a1pms_config.json"
            fi

            if [ $RUNMODE == "KUBE" ]; then
                a1pms_load_config                       ".a1pms_config.json"
            else
                a1pms_api_put_configuration 200 ".a1pms_config.json"
            fi

            if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
                :
            else
                start_mr #Just to prevent errors in the a1pms log...
            fi

            start_cr 1

            sleep_wait 120 "Let A1PMS configuration take effect"

            a1pms_api_get_status 200

            for ((i=1; i<=$NUM_RICS; i++))
            do
                sim_print ricsim_g1_$i interface
            done

            echo "Load policy type in group 1 simulators"
            for ((i=1; i<=$NUM_RICS; i++))
            do
                sim_put_policy_type 201 ricsim_g1_$i 1 testdata/OSC/sim_1.json
            done

            if [ "$A1PMS_VERSION" == "V2" ]; then
              a1pms_equal json:policy-types 1 300  #Wait for the a1pms to refresh types from the simulator
            elif [ "$A1PMS_VERSION" == "V3" ]; then
              a1pms_equal json:policy-types 20 300
            fi

            a1pms_api_put_service 201 "serv1" 600 "$CR_SERVICE_APP_PATH_0/1"

            echo "Check the number of types in the a1pms for each ric is 1"
            for ((i=1; i<=$NUM_RICS; i++))
            do
              if [ "$A1PMS_VERSION" == "V2" ]; then
                a1pms_equal json:policy-types?ric_id=ricsim_g1_$i 1 120
              elif [ "$A1PMS_VERSION" == "V3" ]; then
                a1pms_equal json:policy-types?nearRtRicId=ricsim_g1_$i 1 120
              fi
            done

            START_ID=2000

            start_timer "Create $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using $__httpx and V$version policymanagementservice"

            if [ "$A1PMS_VERSION" == "V3" ]; then
              a1pms_api_post_policy_parallel 201 "serv1" ricsim_g1_ $NUM_RICS 1 $START_ID NOTRANSIENT testdata/OSC/pi1_template.json $NUM_POLICIES_PER_RIC 7
            else
              a1pms_api_put_policy_parallel 201 "serv1" ricsim_g1_ $NUM_RICS 1 $START_ID NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json $NUM_POLICIES_PER_RIC 7
            fi

            print_timer "Create $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using $__httpx and V$version policymanagementservice"

            INSTANCES=$(($NUM_RICS*$NUM_POLICIES_PER_RIC))
            a1pms_equal json:policies $INSTANCES

            for ((i=1; i<=$NUM_RICS; i++))
            do
                sim_equal ricsim_g1_$i num_instances $NUM_POLICIES_PER_RIC
            done

            if [ "$A1PMS_VERSION" == "V3" ]; then
              allPolicyIds_file_path="./tmp/.policyIds.split.res.txt"
              allPolicies_file_path="./tmp/.allPolicies.get.res.json"
              res="$(__do_curl $A1PMS_SERVICE_PATH$A1PMS_API_PREFIX"/v1/policies")"
              echo $res > "./tmp/.allPolicies.get.res.json"
              jq -r '.[].policyId' $allPolicies_file_path > $allPolicyIds_file_path
            fi

            if [ "$A1PMS_VERSION" == "V3" ]; then
              start_timer "Update $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using $__httpx and V$version policymanagementservice"
              a1pms_api_update_policy_parallel 200 "serv1" ricsim_g1_ $NUM_RICS 1 14000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json $NUM_POLICIES_PER_RIC 7 $allPolicyIds_file_path
              print_timer "Update $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using $__httpx and V$version policymanagementservice"
            fi

            start_timer "GET $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using $__httpx and V$version policymanagementservice"

            if [ "$A1PMS_VERSION" == "V3" ]; then
              a1pms_api_get_policy_parallel_v3 200 $NUM_RICS $allPolicyIds_file_path $START_ID $NUM_POLICIES_PER_RIC 7
            else
              a1pms_api_get_policy_parallel 200 $NUM_RICS $START_ID $NUM_POLICIES_PER_RIC 7
            fi

            print_timer "GET $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using $__httpx and V$version policymanagementservice"

            INSTANCES=$(($NUM_RICS*$NUM_POLICIES_PER_RIC))
            a1pms_equal json:policies $INSTANCES

            for ((i=1; i<=$NUM_RICS; i++))
            do
                sim_equal ricsim_g1_$i num_instances $NUM_POLICIES_PER_RIC
            done

            start_timer "Delete $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using $__httpx and V$version policymanagementservice"

            if [ "$A1PMS_VERSION" == "V3" ]; then
              a1pms_api_delete_policy_parallel_v3 204 $NUM_RICS $allPolicyIds_file_path $START_ID $NUM_POLICIES_PER_RIC 7
            else
              a1pms_api_delete_policy_parallel 204 $NUM_RICS $START_ID $NUM_POLICIES_PER_RIC 7
            fi

            print_timer "Delete $((NUM_POLICIES_PER_RIC*$NUM_RICS)) polices over $interface using $__httpx and V$version policymanagementservice"

            a1pms_equal json:policies 0

            for ((i=1; i<=$NUM_RICS; i++))
            do
                sim_equal ricsim_g1_$i num_instances 0
            done

            for ((i=1; i<=$NUM_RICS; i++))
            do
                if [ $interface == "REST+SDNC" ]; then
                    sim_contains_str ricsim_g1_$i remote_hosts $SDNC_APP_NAME
                else
                    sim_contains_str ricsim_g1_$i remote_hosts $A1PMS_APP_NAME
                fi
            done

            check_a1pms_logs
            if [[ $interface = *"SDNC"* ]]; then
                check_sdnc_logs
            fi

            store_logs          "${__httpx}__${interface}"

        done

    done

done


#### TEST COMPLETE ####

print_result

auto_clean_environment