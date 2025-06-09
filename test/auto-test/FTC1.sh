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


TC_ONELINE_DESCR="Sanity test, create service and then create,update and delete a policy using http/https and A1PMS REST with/without SDNC controller"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CP CR MR DMAAPMR A1PMS RICSIM SDNC NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="CP CR MR DMAAPMR A1PMS RICSIM SDNC NGW KUBEPROXY "
#Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="NGW "

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-MONTREAL ONAP-NEWDELHI ONAP-OSLO ONAP-PARIS ORAN-I-RELEASE ORAN-J-RELEASE ORAN-K-RELEASE ORAN-L-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

sim_generate_policy_uuid
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
  # Tested variants of REST/DMAAP/SDNC config
  TESTED_VARIANTS="REST   DMAAP   REST+SDNC   DMAAP+SDNC"

  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      TESTED_VARIANTS="REST   REST+SDNC"
  fi

  #Test a1pms and simulator protocol versions (others are http only)
  TESTED_PROTOCOLS="HTTP HTTPS"
  for __httpx in $TESTED_PROTOCOLS ; do
      for interface in $TESTED_VARIANTS ; do

          echo "#####################################################################"
          echo "#####################################################################"
          echo "### Testing a1pms: $interface using $__httpx with a1pms V${version}"
          echo "#####################################################################"
          echo "#####################################################################"

          clean_environment

          start_kube_proxy

          if [ $__httpx == "HTTPS" ]; then
              use_a1pms_rest_https
          else
              use_a1pms_rest_http
          fi

          start_a1pms NORPOXY $SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_CONFIG_FILE

          set_a1pms_trace

          # Create service to be able to receive events when rics becomes available
          # Must use rest towards the a1pms since dmaap is not configured yet
          a1pms_api_put_service 201 "ric-registration" 0 "$CR_SERVICE_APP_PATH_0/ric-registration"

          if [ $__httpx == "HTTPS" ]; then
              use_cr_https
              use_simulator_https
              if [[ $interface = *"SDNC"* ]]; then
                  if [[ "$SDNC_FEATURE_LEVEL" == *"NO_NB_HTTPS"* ]]; then
                      deviation "SDNC does not support NB https"
                      use_sdnc_http
                  else
                      use_sdnc_https
                  fi
              fi
              if [[ $interface = *"DMAAP"* ]]; then
                  use_a1pms_dmaap_https
              else
                  use_a1pms_rest_https
              fi
          else
              use_cr_http
              use_simulator_http
              if [[ $interface = *"SDNC"* ]]; then
                  use_sdnc_http
              fi
              if [[ $interface = *"DMAAP"* ]]; then
                  use_a1pms_dmaap_http
              else
                  use_a1pms_rest_http
              fi
          fi
          if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
              :
          else
              if [ $__httpx == "HTTPS" ]; then
                  use_mr_https
              else
                  use_mr_http
              fi
          fi

          start_ric_simulators ricsim_g1 1  OSC_2.1.0
          start_ric_simulators ricsim_g2 1  STD_1.1.3
          start_ric_simulators ricsim_g3 1  STD_2.0.0

          if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
              :
          else
              start_mr    "$MR_READ_TOPIC"  "/events" "users/policy-agent" \
                          "$MR_WRITE_TOPIC" "/events" "users/mr-stub"
          fi

          start_cr 1

          start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

          if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
              start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
          fi


          if [[ $interface = *"SDNC"* ]]; then
              start_sdnc
              controller_api_wait_for_status_ok 200 ricsim_g1_1
              prepare_a1pms_config      SDNC    ".a1pms_config.json"
          else
              prepare_a1pms_config      NOSDNC  ".a1pms_config.json"
          fi

          if [ $RUNMODE == "KUBE" ]; then
              a1pms_load_config                       ".a1pms_config.json"
          else
              #Temporary switch to http/https if dmaap use. Otherwise it is not possible to push config
              if [ $__httpx == "HTTPS" ]; then
                  use_a1pms_rest_https
              else
                  use_a1pms_rest_http
              fi
              a1pms_api_put_configuration 200 ".a1pms_config.json"
              if [ $__httpx == "HTTPS" ]; then
                  if [[ $interface = *"DMAAP"* ]]; then
                      use_a1pms_dmaap_https
                  else
                      use_a1pms_rest_https
                  fi
              else
                  if [[ $interface = *"DMAAP"* ]]; then
                      use_a1pms_dmaap_http
                  else
                      use_a1pms_rest_http
                  fi
              fi
          fi

          if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
              :
          else
              mr_equal requests_submitted 0
          fi

          sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json

          sim_put_policy_type 201 ricsim_g3_1 STD_QOS_0_2_0 testdata/STD2/sim_qos.json

          a1pms_equal json:rics 3 300

          if [ "$A1PMS_VERSION" == "V3" ]; then
            a1pms_equal json:policy-types 3 120
          else
            a1pms_equal json:policy-types 3 120
            a1pms_equal json:policy-instances 0
          fi

          a1pms_equal json:policies 0

          cr_equal 0 received_callbacks 3 120

          cr_api_check_all_sync_events 200 0 ric-registration ricsim_g1_1 ricsim_g2_1 ricsim_g3_1

          echo "############################################"
          echo "############## Health check ################"
          echo "############################################"

          sleep_wait 120 "Let A1PMS configuration take effect"

          a1pms_api_get_status 200

          echo "############################################"
          echo "##### Service registry and supervision #####"
          echo "############################################"

          a1pms_api_put_service 201 "serv1" 1000 "$CR_SERVICE_APP_PATH_0/1"

          a1pms_api_get_service_ids 200 "serv1" "ric-registration"

          a1pms_api_put_services_keepalive 200 "serv1"

          echo "############################################"
          echo "############## RIC Repository ##############"
          echo "############################################"

          a1pms_api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1:AVAILABLE  ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE ricsim_g3_1:me1_ricsim_g3_1,me2_ricsim_g3_1:STD_QOS_0_2_0:AVAILABLE"

          echo "############################################"
          echo "########### A1 Policy Management ###########"
          echo "############################################"

          notificationurl=$CR_SERVICE_APP_PATH_0"/test"
          if [ "$A1PMS_VERSION" == "V3" ]; then
            a1pms_api_post_policy_v3 201 "serv1" ricsim_g1_1 1 5000 NOTRANSIENT testdata/OSC/pi1_template.json
            a1pms_api_put_policy_v3 200 5000 testdata/OSC/pi1_template.json
            a1pms_api_post_policy_v3 201 "serv1" ricsim_g3_1 STD_QOS_0_2_0 5200 true testdata/STD2/pi_qos_template.json
            a1pms_api_put_policy_v3 200 5200 testdata/STD2/pi_qos_template.json
            a1pms_api_post_policy_v3 201 "serv1" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT testdata/STD/pi1_template.json
            a1pms_api_put_policy_v3 200 5100 testdata/STD/pi1_template.json
            # Check Default policy status STD2, and then set a custom status
            a1pms_api_get_policy_status_v3 200 5200 STD2 "NOT_ENFORCED" "OTHER_REASON"
            sim_put_policy_status 200 ricsim_g3_1 5200 "ENFORCED" "SCOPE_NOT_APPLICABLE"
            a1pms_api_get_policy_status_v3 200 5200 STD2 "ENFORCED" "SCOPE_NOT_APPLICABLE"
          else
            a1pms_api_put_policy 201 "serv1" ricsim_g1_1 1 5000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json
            a1pms_api_put_policy 200 "serv1" ricsim_g1_1 1 5000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json
            a1pms_api_put_policy 201 "serv1" ricsim_g3_1 STD_QOS_0_2_0 5200 true $notificationurl testdata/STD2/pi_qos_template.json
            a1pms_api_put_policy 200 "serv1" ricsim_g3_1 STD_QOS_0_2_0 5200 true $notificationurl testdata/STD2/pi_qos_template.json

            a1pms_api_put_policy 201 "serv1" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json
            a1pms_api_put_policy 200 "serv1" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json
          fi

          a1pms_equal json:policies 3

          a1pms_api_delete_policy 204 5000

          a1pms_api_delete_policy 204 5100

          a1pms_api_delete_policy 204 5200

          a1pms_equal json:policies 0

          if [ "$A1PMS_VERSION" != "V3" ]; then
            a1pms_equal json:policy-instances 0
          fi

          cr_equal 0 received_callbacks 3

          if [[ $interface = *"DMAAP"* ]]; then

              VAL=14 # Number of a1pms API calls over DMAAP
              mr_equal requests_fetched $VAL
              mr_equal responses_submitted $VAL
              mr_equal responses_fetched $VAL
              mr_equal current_requests 0
              mr_equal current_responses 0
          else
              if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
                  :
              else
                  mr_equal requests_submitted 0
              fi
          fi

          if [[ $interface = *"SDNC"* ]]; then
              sim_contains_str ricsim_g1_1 remote_hosts $SDNC_APP_NAME
              sim_contains_str ricsim_g2_1 remote_hosts $SDNC_APP_NAME
              sim_contains_str ricsim_g3_1 remote_hosts $SDNC_APP_NAME
          else
              sim_contains_str ricsim_g1_1 remote_hosts $A1PMS_APP_NAME
              sim_contains_str ricsim_g2_1 remote_hosts $A1PMS_APP_NAME
              sim_contains_str ricsim_g3_1 remote_hosts $A1PMS_APP_NAME
          fi

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
