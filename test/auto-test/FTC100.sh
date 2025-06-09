#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2020-2023 Nordix Foundation. All rights reserved.
#  Modifications Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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


TC_ONELINE_DESCR="Full a1pms API walkthrough using a1pms REST and with/without SDNC A1 Controller"

USE_ISTIO=0

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM SDNC NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
if [ $USE_ISTIO -eq 0 ]; then
    KUBE_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM SDNC KUBEPROXY NGW"
else
    KUBE_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM SDNC KUBEPROXY NGW KEYCLOAK ISTIO AUTHSIDECAR"
fi
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

sim_generate_policy_uuid

if [ $USE_ISTIO -eq 0 ]; then
    # Tested variants of REST/DMAAP/SDNC config
    if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
        TESTED_VARIANTS="REST   REST+SDNC"
    else
        TESTED_VARIANTS="REST   DMAAP   REST+SDNC   DMAAP+SDNC"
    fi

    #Test a1pms and simulator protocol versions (others are http only)
    TESTED_PROTOCOLS="HTTP HTTPS"
else
    if [ $USE_ISTIO -eq 1 ]; then
        echo -e $RED"#########################################"$ERED
        echo -e $RED"# No test of https when running with istio"$ERED
        echo -e $RED"# No test of SDNC when running with istio"$ERED
        echo -e $RED"#########################################"$ERED
    fi
    # Tested variants of REST/DMAAP/SDNC config
    if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
        TESTED_VARIANTS="REST   DMAAP"
    else
        TESTED_VARIANTS="REST"
    fi

    #Test a1pms and simulator protocol versions (others are http only)
    TESTED_PROTOCOLS="HTTP"
fi

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
          echo "### Testing a1pms: ${interface} and ${__httpx} with a1pms V${version} "
          echo "#####################################################################"
          echo "#####################################################################"

          # Clean container and start all needed containers #
          clean_environment

          if [ $RUNMODE != "KUBE" ]; then
              USE_ISTIO=0
              echo "ISTIO not supported by docker - setting USE-ISTIO=0"
          fi

          if [ $USE_ISTIO -eq 1 ]; then
              echo -e $RED"#########################################"$ERED
              echo -e $RED"# Work around istio jwks cache"$ERED
              echo -e $RED"# Cycle istiod down and up to clear cache"$ERED
              echo ""
              __kube_scale deployment istiod istio-system 0
              __kube_scale deployment istiod istio-system 1
              echo -e $RED"# Cycle istiod done"
              echo -e $RED"#########################################"$ERED

              istio_enable_istio_namespace $KUBE_SIM_NAMESPACE
              istio_enable_istio_namespace $KUBE_NONRTRIC_NAMESPACE
              istio_enable_istio_namespace $KUBE_A1SIM_NAMESPACE
          fi


          start_kube_proxy

          if [ $USE_ISTIO -eq 1 ]; then
              start_keycloak

              keycloak_api_obtain_admin_token

              keycloak_api_create_realm                   nrtrealm   true   60
              keycloak_api_create_confidential_client     nrtrealm   a1pmsc
              keycloak_api_generate_client_secret         nrtrealm   a1pmsc
              keycloak_api_get_client_secret              nrtrealm   a1pmsc
              keycloak_api_create_client_roles            nrtrealm   a1pmsc nrtrole
              keycloak_api_map_client_roles               nrtrealm   a1pmsc nrtrole

              keycloak_api_get_client_token               nrtrealm   a1pmsc

              CLIENT_TOKEN=$(keycloak_api_read_client_token nrtrealm   a1pmsc)
              echo "CLIENT_TOKEN: "$CLIENT_TOKEN

              A1PMS_SEC=$(keycloak_api_read_client_secret nrtrealm   a1pmsc)
              echo "A1PMS_SEC: "$A1PMS_SEC

              # Protect ricsim-g3
              istio_req_auth_by_jwks              ricsim-g1 $KUBE_A1SIM_NAMESPACE KUBEPROXY "$KUBE_PROXY_ISTIO_JWKS_KEYS"
              istio_auth_policy_by_issuer         ricsim-g1 $KUBE_A1SIM_NAMESPACE KUBEPROXY

              istio_req_auth_by_jwksuri           ricsim-g1 $KUBE_A1SIM_NAMESPACE nrtrealm
              istio_auth_policy_by_realm          ricsim-g1 $KUBE_A1SIM_NAMESPACE nrtrealm a1pmsc nrtrole

              # Protect ricsim-g2
              istio_req_auth_by_jwks              ricsim-g2 $KUBE_A1SIM_NAMESPACE KUBEPROXY "$KUBE_PROXY_ISTIO_JWKS_KEYS"
              istio_auth_policy_by_issuer         ricsim-g2 $KUBE_A1SIM_NAMESPACE KUBEPROXY

              istio_req_auth_by_jwksuri           ricsim-g2 $KUBE_A1SIM_NAMESPACE nrtrealm
              istio_auth_policy_by_realm          ricsim-g2 $KUBE_A1SIM_NAMESPACE nrtrealm a1pmsc nrtrole

              # Protect ricsim-g3
              istio_req_auth_by_jwks              ricsim-g3 $KUBE_A1SIM_NAMESPACE KUBEPROXY "$KUBE_PROXY_ISTIO_JWKS_KEYS"
              istio_auth_policy_by_issuer         ricsim-g3 $KUBE_A1SIM_NAMESPACE KUBEPROXY

              istio_req_auth_by_jwksuri           ricsim-g3 $KUBE_A1SIM_NAMESPACE nrtrealm
              istio_auth_policy_by_realm          ricsim-g3 $KUBE_A1SIM_NAMESPACE nrtrealm a1pmsc nrtrole

              # Protect CR
              istio_req_auth_by_jwks              $CR_APP_NAME $KUBE_SIM_NAMESPACE KUBEPROXY "$KUBE_PROXY_ISTIO_JWKS_KEYS"
              istio_auth_policy_by_issuer         $CR_APP_NAME $KUBE_SIM_NAMESPACE KUBEPROXY

              istio_req_auth_by_jwksuri           $CR_APP_NAME $KUBE_SIM_NAMESPACE nrtrealm
              istio_auth_policy_by_realm          $CR_APP_NAME $KUBE_SIM_NAMESPACE nrtrealm a1pmsc nrtrole

              a1pms_configure_sec nrtrealm a1pmsc $A1PMS_SEC
          fi

          if [ $__httpx == "HTTPS" ]; then
              use_cr_https
              use_a1pms_rest_https
          else
              use_a1pms_rest_http
              use_cr_http
          fi

          start_a1pms NORPOXY $SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_CONFIG_FILE

          set_a1pms_debug

          # Create service to be able to receive events when rics becomes available
          # Must use rest towards the a1pms since dmaap is not configured yet
          a1pms_api_put_service 201 "ric-registration" 0 "$CR_SERVICE_APP_PATH_0/ric-registration"


          if [ $__httpx == "HTTPS" ]; then
              use_simulator_https
              use_mr_https
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
              use_simulator_http
              use_mr_http
              if [[ $interface = *"SDNC"* ]]; then
                  use_sdnc_http
              fi
              if [[ $interface = *"DMAAP"* ]]; then
                  use_a1pms_dmaap_http
              else
                  use_a1pms_rest_http
              fi
          fi

          start_ric_simulators ricsim_g1 1  OSC_2.1.0
          start_ric_simulators ricsim_g2 1  STD_1.1.3

          sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json
          sim_put_policy_type 201 ricsim_g1_1 2 testdata/OSC/sim_2.json

          start_ric_simulators ricsim_g3 1  STD_2.0.0
          sim_put_policy_type 201 ricsim_g3_1 STD_QOS_0_2_0 testdata/STD2/sim_qos.json
          sim_put_policy_type 201 ricsim_g3_1 STD_QOS2_0.1.0 testdata/STD2/sim_qos2.json

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

              if [[ $interface != *"DMAAP"* ]]; then
                  echo "{}" > ".a1pms_config_incorrect.json"
                  a1pms_api_put_configuration 400 ".a1pms_config_incorrect.json"
              fi

              a1pms_api_put_configuration 200 ".a1pms_config.json"
              a1pms_api_get_configuration 200 ".a1pms_config.json"
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

          a1pms_equal json:rics 3 300

          if [ "$A1PMS_VERSION" == "V2" ]; then
            a1pms_equal json:policy-types 5 120  #Wait for the a1pms to refresh types from the simulator
          elif [ "$A1PMS_VERSION" == "V3" ]; then
            a1pms_equal json:policy-types 5 120  #Wait for the a1pms to refresh types from the simulator
          fi

          a1pms_equal json:policies 0

          if [ "$A1PMS_VERSION" == "V2" ]; then
            a1pms_equal json:policy-instances 0
          fi

          cr_equal 0 received_callbacks 3 120
          cr_api_check_all_sync_events 200 0 ric-registration ricsim_g1_1 ricsim_g2_1 ricsim_g3_1

          if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
              :
          else
              mr_equal requests_submitted 0
          fi


          echo "############################################"
          echo "############## Health check ################"
          echo "############################################"

          sleep_wait 120 "Let A1PMS configuration take effect"

          a1pms_api_get_status 200

          a1pms_api_get_status_root 200

          echo "############################################"
          echo "##### Service registry and supervision #####"
          echo "############################################"

          a1pms_api_get_services 404 "service1"

          a1pms_api_put_service 201 "service1" 1000 "$CR_SERVICE_APP_PATH_0/1"

          a1pms_api_put_service 200 "service1" 2000 "$CR_SERVICE_APP_PATH_0/1"


          a1pms_api_put_service 400 "service2" -1 "$CR_SERVICE_APP_PATH_0/2"

          a1pms_api_put_service 400 "service2" "wrong" "$CR_SERVICE_APP_PATH_0/2"

          a1pms_api_put_service 400 "service2" 100 "/test"

          a1pms_api_put_service 400 "service2" 100 "test-path"

          a1pms_api_put_service 201 "service2" 300 "ftp://localhost:80/test"

          a1pms_api_get_services 200 "service1" "service1" 2000 "$CR_SERVICE_APP_PATH_0/1"

          a1pms_api_get_service_ids 200 "service1" "service2" "ric-registration"

          a1pms_api_put_service 201 "service3" 5000 "$CR_SERVICE_APP_PATH_0/3"


          a1pms_api_get_service_ids 200 "service1" "service2" "service3" "ric-registration"


          a1pms_api_get_services 200 "service1" "service1" 2000 "$CR_SERVICE_APP_PATH_0/1"

          a1pms_api_get_services 200 NOSERVICE "service1" 2000 "$CR_SERVICE_APP_PATH_0/1" "service2" 300 "ftp://localhost:80/test" "service3" 5000 "$CR_SERVICE_APP_PATH_0/3"  "ric-registration" 0 "$CR_SERVICE_APP_PATH_0/ric-registration"

          a1pms_api_get_services 200

          if [ "$A1PMS_VERSION" == "V2" ]; then
            deviation "TR2 - Keep alive shall return 200/201 according to doc, only 200 works - test combo $interface and $__httpx"
            #The below should work, kept here until fixed or other decision made
            #a1pms_api_put_services_keepalive 201 "service1"
            #Using the below until decision
            a1pms_api_put_services_keepalive 200 "service1"

            deviation "TR2 - Keep alive shall return 200/201 according to doc, only 200 works - test combo $interface and $__httpx"
            #The below should work, keept here until fixed or other decision made
            #a1pms_api_put_services_keepalive 201 "service3"
            #Using the below until decision
            a1pms_api_put_services_keepalive 200 "service3"

            a1pms_api_put_services_keepalive 200 "service1"

            a1pms_api_put_services_keepalive 200 "service3"

            a1pms_api_put_services_keepalive 404 "service5"
          fi

          a1pms_api_get_service_ids 200 "service1" "service2" "service3"  "ric-registration"

          a1pms_api_delete_services 204 "service1"

          a1pms_api_get_service_ids 200 "service2" "service3" "ric-registration"


          a1pms_api_put_service 201 "service1" 50 "$CR_SERVICE_APP_PATH_0/1"

          a1pms_api_get_service_ids 200 "service1" "service2" "service3"  "ric-registration"


          a1pms_api_delete_services 204 "service1"
          a1pms_api_delete_services 204 "service3"

          a1pms_equal json:services 2

          a1pms_api_delete_services 204 "service2"

          a1pms_equal json:services 1

          echo "############################################"
          echo "############## RIC Repository ##############"
          echo "############################################"

          a1pms_api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE  ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE ricsim_g3_1:me1_ricsim_g3_1,me2_ricsim_g3_1:STD_QOS_0_2_0,STD_QOS2_0.1.0:AVAILABLE"

          a1pms_api_get_rics 200 1 "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

          a1pms_api_get_rics 404 47

          a1pms_api_get_rics 404 "test"

          a1pms_api_get_ric 200 me1_ricsim_g1_1 NORIC "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

          a1pms_api_get_ric 200 me2_ricsim_g1_1 NORIC "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

          a1pms_api_get_ric 200 me1_ricsim_g2_1 NORIC "ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"

          a1pms_api_get_ric 200 me2_ricsim_g2_1 NORIC "ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"

          a1pms_api_get_ric 200 me1_ricsim_g3_1 NORIC "ricsim_g3_1:me1_ricsim_g3_1,me2_ricsim_g3_1:STD_QOS_0_2_0,STD_QOS2_0.1.0:AVAILABLE"

          a1pms_api_get_ric 200 me2_ricsim_g3_1 NORIC "ricsim_g3_1:me1_ricsim_g3_1,me2_ricsim_g3_1:STD_QOS_0_2_0,STD_QOS2_0.1.0:AVAILABLE"

          a1pms_api_get_ric 200 NOME      ricsim_g1_1 "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

          a1pms_api_get_ric 200 NOME      ricsim_g2_1 "ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"

          a1pms_api_get_ric 200 NOME      ricsim_g3_1 "ricsim_g3_1:me1_ricsim_g3_1,me2_ricsim_g3_1:STD_QOS_0_2_0,STD_QOS2_0.1.0:AVAILABLE"

          a1pms_api_get_ric 404 NOME test1

          a1pms_api_get_ric 404 test NORIC

          a1pms_api_get_ric 400 me1_ricsim_g1_1 ricsim_g1_1

          a1pms_api_get_ric 400 me1_ricsim_g1_1 TESTRIC

          a1pms_api_get_ric 400 TESTME ricsim_g1_1

          echo "############################################"
          echo "########### A1 Policy Management ###########"
          echo "############################################"

          deviation "TR9 - a1pms modify the type with type id - test combo $interface and $__httpx"
          #Behaviour accepted for now
          a1pms_api_get_policy_type 200 1 testdata/OSC/1-a1pms-modified.json
          deviation "TR9 - a1pms modify the type with type id - test combo $interface and $__httpx"
          #Behaviour accepted for now
          a1pms_api_get_policy_type 200 2 testdata/OSC/2-a1pms-modified.json
          deviation "TR9 - a1pms modify the type with type id - test combo $interface and $__httpx"
          #Behaviour accepted for now
          a1pms_api_get_policy_type 200 STD_QOS_0_2_0 testdata/STD2/qos-a1pms-modified.json
          deviation "TR9 - a1pms modify the type with type id - test combo $interface and $__httpx"
          #Behaviour accepted for now
          a1pms_api_get_policy_type 200 STD_QOS2_0.1.0 testdata/STD2/qos2-a1pms-modified.json

          a1pms_api_get_policy_type 404 3
          if [ "$A1PMS_VERSION" != "V3" ]; then
            a1pms_api_get_policy_schemas 404

            a1pms_api_get_policy_types 200 NORIC 1 2 EMPTY STD_QOS_0_2_0 STD_QOS2_0.1.0

            a1pms_api_get_policy_types 200 ricsim_g1_1 1 2

            a1pms_api_get_policy_types 200 ricsim_g2_1 EMPTY

            a1pms_api_get_policy_types 200 ricsim_g3_1 STD_QOS_0_2_0 STD_QOS2_0.1.0

            a1pms_api_get_policy_types 404 dummy-ric
          fi

          if [ "$A1PMS_VERSION" == "V3" ]; then

            a1pms_api_get_policy_types_v3 200 NORIC 1:ricsim_g1_1 2:ricsim_g1_1 EMPTY:ricsim_g2_1 STD_QOS_0_2_0:ricsim_g3_1 STD_QOS2_0.1.0:ricsim_g3_1

            a1pms_api_get_policy_types_v3 200 ricsim_g1_1 1:ricsim_g1_1 2:ricsim_g1_1

            a1pms_api_get_policy_types_v3 200 ricsim_g2_1 EMPTY:ricsim_g2_1

            a1pms_api_get_policy_types_v3 200 ricsim_g3_1 STD_QOS_0_2_0:ricsim_g3_1 STD_QOS2_0.1.0:ricsim_g3_1

            a1pms_api_get_policy_types_v3 404 dummy-ric

            a1pms_api_post_policy_v3 201 "unregistered-service" ricsim_g1_1 1 2000 NOTRANSIENT testdata/OSC/pi1_template.json

            a1pms_api_post_policy_v3 201 "service10" ricsim_g1_1 1 5000 NOTRANSIENT testdata/OSC/pi1_template.json

            a1pms_api_post_policy_v3 201 NOSERVICE ricsim_g2_1 NOTYPE 5100 NOTRANSIENT testdata/STD/pi1_template.json

            a1pms_api_post_policy_v3 201 NOSERVICE ricsim_g3_1 STD_QOS2_0.1.0 5200 NOTRANSIENT testdata/STD2/pi_qos2_template.json

            if [ -n "$A1PMS_VALIDATE_INSTANCE_SCHEMA" ] && [ "$A1PMS_VALIDATE_INSTANCE_SCHEMA" = "true" ]; then
              # Test for schema validation at create - should fail
              a1pms_api_post_policy_v3 400 NOSERVICE ricsim_g3_1 STD_QOS2_0.1.0 5200 NOTRANSIENT testdata/STD2/pi_qos2_bad_template.json
            else
              deviation "TR10 - policy create instance schema validation added after oslo, so not tested here - test combo $interface and $__httpx"
            fi

          fi

          a1pms_api_put_service 201 "service10" 3600 "$CR_SERVICE_APP_PATH_0/1"

          notificationurl=$CR_SERVICE_APP_PATH_0"/test"
          deviation "TR10 - a1pms allows policy creation on unregistered service (orig problem) - test combo $interface and $__httpx"

          if [ "$A1PMS_VERSION" != "V3" ]; then

            if [[ $interface != *"DMAAP"* ]]; then
                # Badly formatted json is not possible to send via dmaap
                a1pms_api_put_policy 400 "unregistered-service" ricsim_g1_1 1 2000 NOTRANSIENT $notificationurl testdata/OSC/pi_bad_template.json
            fi
            a1pms_api_put_policy 201 "unregistered-service" ricsim_g1_1 1 2000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json

            a1pms_api_put_policy 201 "service10" ricsim_g1_1 1 5000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json
            a1pms_api_put_policy 200 "service10" ricsim_g1_1 1 5000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json

            a1pms_api_put_policy 200 "service10" ricsim_g1_1 1 5000 true $notificationurl testdata/OSC/pi1_template.json
            a1pms_api_put_policy 200 "service10" ricsim_g1_1 1 5000 false $notificationurl testdata/OSC/pi1_template.json

            a1pms_api_put_policy 201 "service10" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json
            a1pms_api_put_policy 200 "service10" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json

            a1pms_api_put_policy 200 "service10" ricsim_g2_1 NOTYPE 5100 true $notificationurl testdata/STD/pi1_template.json
            a1pms_api_put_policy 200 "service10" ricsim_g2_1 NOTYPE 5100 false $notificationurl testdata/STD/pi1_template.json

            a1pms_api_put_policy 201 "service10" ricsim_g3_1 STD_QOS2_0.1.0 5200 NOTRANSIENT $notificationurl testdata/STD2/pi_qos2_template.json
            a1pms_api_put_policy 200 "service10" ricsim_g3_1 STD_QOS2_0.1.0 5200 NOTRANSIENT $notificationurl testdata/STD2/pi_qos2_template.json

            a1pms_api_put_policy 200 "service10" ricsim_g3_1 STD_QOS2_0.1.0 5200 true $notificationurl testdata/STD2/pi_qos2_template.json
            a1pms_api_put_policy 200 "service10" ricsim_g3_1 STD_QOS2_0.1.0 5200 false $notificationurl testdata/STD2/pi_qos2_template.json

            a1pms_api_get_policy_status 404 1
            a1pms_api_get_policy_status 404 2
            if [[ $TEST_ENV_PROFILE =~ ^ORAN-[A-H] ]] || [[ $TEST_ENV_PROFILE =~ ^ONAP-[A-L] ]]; then
              VAL='NOT IN EFFECT'
              VAL2="false"
              VAL3=EMPTY
              VAL4=EMPTY
            else
              VAL="NOT_ENFORCED"
              VAL2="OTHER_REASON"
              VAL3="NOT_ENFORCED"
              VAL4="OTHER_REASON"
            fi
            a1pms_api_get_policy_status 200 5000 OSC "$VAL" "$VAL2"
            a1pms_api_get_policy_status 200 5100 STD "UNDEFINED"
            a1pms_api_get_policy_status 200 5200 STD2 $VAL3 $VAL4

            deviation "TR10 - a1pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
            #kept until decision
            #a1pms_equal json:policy_ids 2
            #Allow 3 for now
            a1pms_equal json:policy-instances 4
          fi

          deviation "TR10 - a1pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
          #kept until decision
          #a1pms_equal json:policies 2
          #Allow 3 for now
          a1pms_equal json:policies 4

          deviation "TR10 - a1pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
          #kept until decision
          #a1pms_api_get_policy_ids 200 NORIC NOSERVICE NOTYPE 5000 5100
          #Allow policy create with unregistered service for now

          if [ "$A1PMS_VERSION" != "V3" ]; then
            a1pms_api_get_policy_ids 200 NORIC NOSERVICE NOTYPE 5000 5100 2000 5200

            deviation "TR10 - a1pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
            #kept until decision
            #a1pms_api_get_policy_ids 200 ricsim_g1_1 NOSERVICE NOTYPE 5000
            #Allow policy create with unregistered service for now
            a1pms_api_get_policy_ids 200 ricsim_g1_1 NOSERVICE NOTYPE 5000 2000

            a1pms_api_get_policy_ids 200 ricsim_g2_1 NOSERVICE NOTYPE 5100

            a1pms_api_get_policy_ids 200 ricsim_g3_1 NOSERVICE NOTYPE 5200

            a1pms_api_get_policy_ids 200 NORIC "service10" NOTYPE 5000 5100 5200

            deviation "TR10 - a1pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
            #kept until decision
            #a1pms_api_get_policy_ids 200 NORIC NOSERVICE 1 5000
            #Allow policy create with unregistered service for now

            a1pms_api_get_policy_ids 200 NORIC NOSERVICE 1 5000 2000

            a1pms_api_get_policy_ids 200 NORIC NOSERVICE 2 NOID

            a1pms_api_get_policy_ids 200 NORIC NOSERVICE STD_QOS2_0.1.0 5200

            a1pms_api_get_policy_ids 200 ricsim_g2_1 NOSERVICE 1 NOID
          fi

          if [ "$A1PMS_VERSION" == "V3" ]; then
            a1pms_api_get_all_policies_v3 200 NORIC NOSERVICE NOTYPE 5000:ricsim_g1_1 5100:ricsim_g2_1 2000:ricsim_g1_1 5200:ricsim_g3_1

            deviation "TR10 - a1pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
            #kept until decision
            #a1pms_api_get_policy_ids 200 ricsim_g1_1 NOSERVICE NOTYPE 5000
            #Allow policy create with unregistered service for now
            a1pms_api_get_all_policies_v3 200 ricsim_g1_1 NOSERVICE NOTYPE 5000:ricsim_g1_1 2000:ricsim_g1_1

            a1pms_api_get_all_policies_v3 200 ricsim_g2_1 NOSERVICE NOTYPE 5100:ricsim_g2_1

            a1pms_api_get_all_policies_v3 200 ricsim_g3_1 NOSERVICE NOTYPE 5200:ricsim_g3_1

            a1pms_api_get_all_policies_v3 200 NORIC "service10" NOTYPE 5000:ricsim_g1_1

            deviation "TR10 - a1pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
            #kept until decision
            #a1pms_api_get_policy_ids 200 NORIC NOSERVICE 1 5000
            #Allow policy create with unregistered service for now

            a1pms_api_get_all_policies_v3 200 NORIC NOSERVICE 1 5000:ricsim_g1_1 2000:ricsim_g1_1

            a1pms_api_get_all_policies_v3 200 NORIC NOSERVICE 2 NOID

            a1pms_api_get_all_policies_v3 200 NORIC NOSERVICE STD_QOS2_0.1.0 5200:ricsim_g3_1

            a1pms_api_get_all_policies_v3 200 ricsim_g2_1 NOSERVICE 1 NOID
          fi

          a1pms_api_get_policy 200 5000 testdata/OSC/pi1_template.json "service10" ricsim_g1_1 1 false $notificationurl

          a1pms_api_get_policy 200 5100 testdata/STD/pi1_template.json "service10" ricsim_g2_1 NOTYPE false $notificationurl

          a1pms_api_get_policy 200 5200 testdata/STD2/pi_qos2_template.json "service10" ricsim_g3_1 STD_QOS2_0.1.0 false $notificationurl

          if [ "$A1PMS_VERSION" != "V3" ]; then
            a1pms_api_get_policies 200 ricsim_g1_1 "service10" 1 5000 ricsim_g1_1 "service10" 1 false $notificationurl testdata/OSC/pi1_template.json
          fi

          if [ "$A1PMS_VERSION" == "V3" ]; then
            a1pms_api_put_policy_v3 200 5100 testdata/STD/pi1_template.json

            a1pms_api_put_policy_v3 200 5200 testdata/STD2/pi_qos2_template.json

            if [ -n "$A1PMS_VALIDATE_INSTANCE_SCHEMA" ] && [ "$A1PMS_VALIDATE_INSTANCE_SCHEMA" = "true" ]; then
              # Test for schema validation at update - should fail
              a1pms_api_put_policy_v3 400 5200 testdata/STD2/pi_qos2_bad_template.json
            else
              deviation "TR10 - policy update instance schema validation added after oslo, so not tested here - test combo $interface and $__httpx"
            fi

            if [[ $interface != *"DMAAP"* ]]; then
            	a1pms_api_put_policy_v3 400 2000 testdata/OSC/pi_bad_template.json
            fi
          fi
          deviation "TR10 - a1pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
          #kept until decision
          #a1pms_api_delete_policy 404 2000
          #Allow policy create with unregistered service for now
          a1pms_api_delete_policy 204 2000

          a1pms_api_delete_policy 404 1500

          a1pms_api_delete_policy 204 5000

          a1pms_api_delete_policy 204 5200

          a1pms_equal json:policies 1

          if [ "$A1PMS_VERSION" != "V3" ]; then
            a1pms_equal json:policy-instances 1
          fi

          a1pms_api_delete_policy 204 5100

          a1pms_equal json:policies 0

          if [ "$A1PMS_VERSION" != "V3" ]; then
            a1pms_equal json:policy-instances 0
          fi

          cr_equal 0 received_callbacks 3

          if [[ $interface = *"DMAAP"* ]]; then
              mr_greater requests_submitted 0
              VAL=$(mr_read requests_submitted)
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
          if [ $USE_ISTIO -eq 0 ]; then
              if [[ $interface = *"SDNC"* ]]; then
                  sim_contains_str ricsim_g1_1 remote_hosts $SDNC_APP_NAME
                  sim_contains_str ricsim_g2_1 remote_hosts $SDNC_APP_NAME
                  sim_contains_str ricsim_g3_1 remote_hosts $SDNC_APP_NAME
              else
                  sim_contains_str ricsim_g1_1 remote_hosts $A1PMS_APP_NAME
                  sim_contains_str ricsim_g2_1 remote_hosts $A1PMS_APP_NAME
                  sim_contains_str ricsim_g3_1 remote_hosts $A1PMS_APP_NAME
              fi
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
