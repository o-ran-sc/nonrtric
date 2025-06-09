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

TC_ONELINE_DESCR="Basic use case, register service, create/update policy, delete policy, de-register service using both STD and OSC interface while mixing REST"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES=" MR CR A1PMS RICSIM CP KUBEPROXY NGW"
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
VERSIONS_TO_RUN=2

if [ -n "$A1PMS_V3_FLAG" ] && [ "${A1PMS_V3_FLAG,,}" == "true" ]; then
  VERSIONS_TO_RUN=3
fi

for version in $(seq 2 $VERSIONS_TO_RUN); do

  echo "#####################################################################"
  echo "#####################################################################"
  echo "### Testing a1pms: V${version}"
  echo "#####################################################################"
  echo "#####################################################################"

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
  sim_generate_policy_uuid

  use_simulator_http
  use_mr_http
  use_a1pms_rest_http


  clean_environment

  start_kube_proxy

  start_ric_simulators  ricsim_g1 3 OSC_2.1.0

  start_ric_simulators  ricsim_g2 5 STD_1.1.3

  start_ric_simulators ricsim_g3 1  STD_2.0.0

  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      :
  else
      start_mr
  fi

  start_cr 1


  start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

  if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
      start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
  fi

  start_a1pms NORPOXY $SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_CONFIG_FILE

  set_a1pms_debug

  use_a1pms_rest_http


  prepare_a1pms_config      NOSDNC  ".a1pms_config.json"

  if [ $RUNMODE == "KUBE" ]; then
      a1pms_load_config                       ".a1pms_config.json"
  else
      a1pms_api_put_configuration 200 ".a1pms_config.json"
  fi

  sleep_wait 120 "Let A1PMS configuration take effect"

  a1pms_api_get_status 200

  sim_print ricsim_g1_1 interface

  sim_print ricsim_g2_1 interface

  sim_print ricsim_g3_1 interface

  sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json

  sim_put_policy_type 201 ricsim_g3_1 STD_QOS_0_2_0 testdata/STD2/sim_qos.json
  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_equal json:policy-types 7 300
  else
    a1pms_equal json:policy-types 3 300
  fi

  # Create policies

  notificationurl=$CR_SERVICE_APP_PATH_0"/test"

  use_a1pms_rest_http

  a1pms_api_put_service 201 "service1" 3600 "$CR_SERVICE_APP_PATH_0/1"

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_post_policy_v3 201 "service1" ricsim_g1_1 1 2000 NOTRANSIENT testdata/OSC/pi1_template.json 1
  else
    a1pms_api_put_policy 201 "service1" ricsim_g1_1 1 2000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json 1
  fi

  sim_equal ricsim_g1_1 num_instances 1

  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      use_a1pms_rest_http
  else
      use_a1pms_dmaap_http
  fi

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_post_policy_v3 201 "service1" ricsim_g1_1 1 3000 NOTRANSIENT testdata/OSC/pi1_template.json 1
  else
    a1pms_api_put_policy 201 "service1" ricsim_g1_1 1 3000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json 1
  fi

  sim_equal ricsim_g1_1 num_instances 2


  use_a1pms_rest_http

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_post_policy_v3 201 "service1" ricsim_g2_1 NOTYPE 2100 NOTRANSIENT testdata/STD/pi1_template.json 1
  else
    a1pms_api_put_policy 201 "service1" ricsim_g2_1 NOTYPE 2100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json 1
  fi

  sim_equal ricsim_g2_1 num_instances 1


  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      use_a1pms_rest_http
  else
      use_a1pms_dmaap_http
  fi

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_post_policy_v3 201 "service1" ricsim_g2_1 NOTYPE 3100 NOTRANSIENT testdata/STD/pi1_template.json 1
  else
    a1pms_api_put_policy 201 "service1" ricsim_g2_1 NOTYPE 3100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json 1
  fi

  sim_equal ricsim_g2_1 num_instances 2

  use_a1pms_rest_http

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_post_policy_v3 201 "service1" ricsim_g3_1 STD_QOS_0_2_0 2200 true testdata/STD2/pi_qos_template.json 1
  else
    a1pms_api_put_policy 201 "service1" ricsim_g3_1 STD_QOS_0_2_0 2200 true $notificationurl testdata/STD2/pi_qos_template.json 1
  fi

  sim_equal ricsim_g3_1 num_instances 1

  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      use_a1pms_rest_http
  else
      use_a1pms_dmaap_http
  fi

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_post_policy_v3 201 "service1" ricsim_g3_1 STD_QOS_0_2_0 3200 NOTRANSIENT testdata/STD2/pi_qos_template.json 1
  else
    a1pms_api_put_policy 201 "service1" ricsim_g3_1 STD_QOS_0_2_0 3200 NOTRANSIENT $notificationurl testdata/STD2/pi_qos_template.json 1
  fi

  sim_equal ricsim_g3_1 num_instances 2

  if [ "$A1PMS_VERSION" == "V3" ]; then
    # Check policies
    a1pms_api_get_policy 200 2000 testdata/OSC/pi1_template.json "service1" ricsim_g1_1 1 false $notificationurl
    a1pms_api_get_policy 200 3000 testdata/OSC/pi1_template.json "service1" ricsim_g1_1 1 false $notificationurl
    a1pms_api_get_policy 200 2100 testdata/STD/pi1_template.json "service1" ricsim_g2_1 NOTYPE false $notificationurl
    a1pms_api_get_policy 200 3100 testdata/STD/pi1_template.json "service1" ricsim_g2_1 NOTYPE false $notificationurl
    a1pms_api_get_policy 200 2200 testdata/STD2/pi_qos_template.json "service1" ricsim_g3_1 STD_QOS_0_2_0 true $notificationurl
    a1pms_api_get_policy 200 3200 testdata/STD2/pi_qos_template.json "service1" ricsim_g3_1 STD_QOS_0_2_0 true $notificationurl
  fi

  #Update policies
  use_a1pms_rest_http

  a1pms_api_put_service 200 "service1" 3600 "$CR_SERVICE_APP_PATH_0/1"

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_put_policy_v3 200 2000 testdata/OSC/pi1_template.json 1
  else
    a1pms_api_put_policy 200 "service1" ricsim_g1_1 1 2000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json 1
  fi

  sim_equal ricsim_g1_1 num_instances 2


  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      use_a1pms_rest_http
  else
      use_a1pms_dmaap_http
  fi

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_put_policy_v3 200 3000 testdata/OSC/pi1_template.json 1
  else
    a1pms_api_put_policy 200 "service1" ricsim_g1_1 1 3000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json 1
  fi

  sim_equal ricsim_g1_1 num_instances 2


  use_a1pms_rest_http

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_put_policy_v3 200 2100 testdata/STD/pi1_template.json 1
  else
    a1pms_api_put_policy 200 "service1" ricsim_g2_1 NOTYPE 2100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json 1
  fi

  sim_equal ricsim_g2_1 num_instances 2


  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      use_a1pms_rest_http
  else
      use_a1pms_dmaap_http
  fi

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_put_policy_v3 200 3100 testdata/STD/pi1_template.json 1
  else
    a1pms_api_put_policy 200 "service1" ricsim_g2_1 NOTYPE 3100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json 1
  fi

  sim_equal ricsim_g2_1 num_instances 2

  use_a1pms_rest_http

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_put_policy_v3 200 2200 testdata/STD2/pi_qos_template.json 1
  else
    a1pms_api_put_policy 200 "service1" ricsim_g3_1 STD_QOS_0_2_0 2200 true $notificationurl testdata/STD2/pi_qos_template.json 1
  fi

  sim_equal ricsim_g3_1 num_instances 2


  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      use_a1pms_rest_http
  else
      use_a1pms_dmaap_http
  fi

  if [ "$A1PMS_VERSION" == "V3" ]; then
    a1pms_api_put_policy_v3 200 3200 testdata/STD2/pi_qos_template.json 1
  else
    a1pms_api_put_policy 200 "service1" ricsim_g3_1 STD_QOS_0_2_0 3200 true $notificationurl testdata/STD2/pi_qos_template.json 1
  fi

  sim_equal ricsim_g3_1 num_instances 2

  if [ "$A1PMS_VERSION" != "V3" ]; then
    # Check policies
    a1pms_api_get_policy 200 2000 testdata/OSC/pi1_template.json "service1" ricsim_g1_1 1 false $notificationurl
    a1pms_api_get_policy 200 3000 testdata/OSC/pi1_template.json "service1" ricsim_g1_1 1 false $notificationurl
    a1pms_api_get_policy 200 2100 testdata/STD/pi1_template.json "service1" ricsim_g2_1 NOTYPE false $notificationurl
    a1pms_api_get_policy 200 3100 testdata/STD/pi1_template.json "service1" ricsim_g2_1 NOTYPE false $notificationurl
    a1pms_api_get_policy 200 2200 testdata/STD2/pi_qos_template.json "service1" ricsim_g3_1 STD_QOS_0_2_0 true $notificationurl
    a1pms_api_get_policy 200 3200 testdata/STD2/pi_qos_template.json "service1" ricsim_g3_1 STD_QOS_0_2_0 true $notificationurl
  fi

  sim_equal ricsim_g1_1 num_instances 2
  sim_equal ricsim_g2_1 num_instances 2

  sim_equal ricsim_g3_1 num_instances 2

  # Remove policies

  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      use_a1pms_rest_http
  else
      use_a1pms_dmaap_http
  fi
  a1pms_api_delete_policy 204 2000
  use_a1pms_rest_http
  a1pms_api_delete_policy 204 3000
  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      use_a1pms_rest_http
  else
      use_a1pms_dmaap_http
  fi
  a1pms_api_delete_policy 204 2100
  use_a1pms_rest_http
  a1pms_api_delete_policy 204 3100
  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      use_a1pms_rest_http
  else
      use_a1pms_dmaap_http
  fi
  a1pms_api_delete_policy 204 2200
  use_a1pms_rest_http
  a1pms_api_delete_policy 204 3200

  sim_equal ricsim_g1_1 num_instances 0
  sim_equal ricsim_g2_1 num_instances 0

  sim_equal ricsim_g3_1 num_instances 0

  # Check remote host access to simulator

  sim_contains_str ricsim_g1_1 remote_hosts $A1PMS_APP_NAME
  sim_contains_str ricsim_g2_1 remote_hosts $A1PMS_APP_NAME
  sim_contains_str ricsim_g3_1 remote_hosts $A1PMS_APP_NAME

  # Check policy removal
  use_a1pms_rest_http
  a1pms_api_get_policy 404 2000
  a1pms_api_get_policy 404 3000
  a1pms_api_get_policy 404 2100
  a1pms_api_get_policy 404 3100

  a1pms_api_get_policy 404 2200
  a1pms_api_get_policy 404 3200

  # Remove the service
  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
      use_a1pms_rest_http
  else
      use_a1pms_dmaap_http
  fi
  a1pms_api_delete_services 204 "service1"

  a1pms_api_get_services 404 "service1"

  check_a1pms_logs
done
  #### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_environment
