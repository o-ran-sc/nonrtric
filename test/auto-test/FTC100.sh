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


TC_ONELINE_DESCR="Full pms API walkthrough using pms REST/DMAAP and with/without SDNC A1 Controller"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CBS CONSUL CP CR MR PMS RICSIM SDNC NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="CP CR MR PMS RICSIM SDNC KUBEPROXY NGW"
#Prestarted app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="CBS CONSUL NGW"

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-GUILIN ONAP-HONOLULU ONAP-ISTANBUL ONAP-JAKARTA ORAN-CHERRY ORAN-D-RELEASE ORAN-E-RELEASE ORAN-F-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

generate_policy_uuid

# Tested variants of REST/DMAAP/SDNC config
TESTED_VARIANTS="REST   DMAAP   REST+SDNC   DMAAP+SDNC"

#Test pms and simulator protocol versions (others are http only)
TESTED_PROTOCOLS="HTTP HTTPS"

for __httpx in $TESTED_PROTOCOLS ; do
    for interface in $TESTED_VARIANTS ; do

        echo "#####################################################################"
        echo "#####################################################################"
        echo "### Testing pms: $interface using $__httpx"
        echo "#####################################################################"
        echo "#####################################################################"

        # Clean container and start all needed containers #
        clean_environment

        start_kube_proxy

        if [ $__httpx == "HTTPS" ]; then
            use_cr_https
            use_pms_rest_https
        else
            use_pms_rest_http
            use_cr_http
        fi

        start_pms NORPOXY $SIM_GROUP/$PMS_COMPOSE_DIR/$PMS_CONFIG_FILE

        set_pms_debug

        # Create service to be able to receive events when rics becomes available
        # Must use rest towards the pms since dmaap is not configured yet
        pms_api_put_service 201 "ric-registration" 0 "$CR_SERVICE_APP_PATH_0/ric-registration"


        if [ $__httpx == "HTTPS" ]; then
            use_simulator_https
            use_mr_https
            if [[ $interface = *"SDNC"* ]]; then
                use_sdnc_https
            fi
            if [[ $interface = *"DMAAP"* ]]; then
                use_pms_dmaap_https
            else
                use_pms_rest_https
            fi
        else
            use_simulator_http
            use_mr_http
            if [[ $interface = *"SDNC"* ]]; then
                use_sdnc_http
            fi
            if [[ $interface = *"DMAAP"* ]]; then
                use_pms_dmaap_http
            else
                use_pms_rest_http
            fi
        fi

        start_ric_simulators ricsim_g1 1  OSC_2.1.0
        start_ric_simulators ricsim_g2 1  STD_1.1.3

        sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json
        sim_put_policy_type 201 ricsim_g1_1 2 testdata/OSC/sim_2.json

        if [ "$PMS_VERSION" == "V2" ]; then
            start_ric_simulators ricsim_g3 1  STD_2.0.0
            sim_put_policy_type 201 ricsim_g3_1 STD_QOS_0_2_0 testdata/STD2/sim_qos.json
            sim_put_policy_type 201 ricsim_g3_1 STD_QOS2_0.1.0 testdata/STD2/sim_qos2.json
        fi

        start_mr

        start_cr 1

        start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

        if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
            start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
        fi

        if [[ $interface = *"SDNC"* ]]; then
            start_sdnc
            prepare_consul_config      SDNC    ".consul_config.json"
        else
            prepare_consul_config      NOSDNC  ".consul_config.json"
        fi

        if [ $RUNMODE == "KUBE" ]; then
            pms_load_config                       ".consul_config.json"
        else
            if [[ "$PMS_FEATURE_LEVEL" == *"NOCONSUL"* ]]; then
                #Temporary switch to http/https if dmaap use. Otherwise it is not possibble to push config
                if [ $__httpx == "HTTPS" ]; then
                    use_pms_rest_https
                else
                    use_pms_rest_http
                fi

                if [[ $interface != *"DMAAP"* ]]; then
                    echo "{}" > ".consul_config_incorrect.json"
                    pms_api_put_configuration 400 ".consul_config_incorrect.json"
                fi

                pms_api_put_configuration 200 ".consul_config.json"
                pms_api_get_configuration 200 ".consul_config.json"
                if [ $__httpx == "HTTPS" ]; then
                    if [[ $interface = *"DMAAP"* ]]; then
                        use_pms_dmaap_https
                    else
                        use_pms_rest_https
                    fi
                else
                    if [[ $interface = *"DMAAP"* ]]; then
                        use_pms_dmaap_http
                    else
                        use_pms_rest_http
                    fi
                fi

            else
                start_consul_cbs
                consul_config_app                   ".consul_config.json"
            fi
        fi

        if [ "$PMS_VERSION" == "V2" ]; then
            pms_equal json:rics 3 300

            pms_equal json:policy-types 5 120

            pms_equal json:policies 0

            pms_equal json:policy-instances 0
        else
            pms_equal json:rics 2 300

            pms_equal json:policy_schemas 3 120

            pms_equal json:policy_types 3

            pms_equal json:policies 0

            pms_equal json:policy_ids 0
        fi

        if [ "$PMS_VERSION" == "V2" ]; then
            cr_equal 0 received_callbacks 3 120
            cr_api_check_all_sync_events 200 0 ric-registration ricsim_g1_1 ricsim_g2_1 ricsim_g3_1
        fi
        mr_equal requests_submitted 0


        echo "############################################"
        echo "############## Health check ################"
        echo "############################################"

        pms_api_get_status 200

        pms_api_get_status_root 200

        echo "############################################"
        echo "##### Service registry and supervision #####"
        echo "############################################"

        pms_api_get_services 404 "service1"

        pms_api_put_service 201 "service1" 1000 "$CR_SERVICE_APP_PATH_0/1"

        pms_api_put_service 200 "service1" 2000 "$CR_SERVICE_APP_PATH_0/1"


        pms_api_put_service 400 "service2" -1 "$CR_SERVICE_APP_PATH_0/2"

        pms_api_put_service 400 "service2" "wrong" "$CR_SERVICE_APP_PATH_0/2"

        pms_api_put_service 400 "service2" 100 "/test"

        pms_api_put_service 400 "service2" 100 "test-path"

        pms_api_put_service 201 "service2" 300 "ftp://localhost:80/test"

        pms_api_get_services 200 "service1" "service1" 2000 "$CR_SERVICE_APP_PATH_0/1"

        pms_api_get_service_ids 200 "service1" "service2" "ric-registration"


        pms_api_put_service 201 "service3" 5000 "$CR_SERVICE_APP_PATH_0/3"


        pms_api_get_service_ids 200 "service1" "service2" "service3" "ric-registration"


        pms_api_get_services 200 "service1" "service1" 2000 "$CR_SERVICE_APP_PATH_0/1"

        pms_api_get_services 200 NOSERVICE "service1" 2000 "$CR_SERVICE_APP_PATH_0/1" "service2" 300 "ftp://localhost:80/test" "service3" 5000 "$CR_SERVICE_APP_PATH_0/3"  "ric-registration" 0 "$CR_SERVICE_APP_PATH_0/ric-registration"

        pms_api_get_services 200

        deviation "TR2 - Keep alive shall return 200/201 according to doc, only 200 works - test combo $interface and $__httpx"
        #The below should work, keept here until fixed or other decision made
        #pms_api_put_services_keepalive 201 "service1"
        #Using the below until decision
        pms_api_put_services_keepalive 200 "service1"

        deviation "TR2 - Keep alive shall return 200/201 according to doc, only 200 works - test combo $interface and $__httpx"
        #The below should work, keept here until fixed or other decision made
        #pms_api_put_services_keepalive 201 "service3"
        #Using the below until decision
        pms_api_put_services_keepalive 200 "service3"

        pms_api_put_services_keepalive 200 "service1"

        pms_api_put_services_keepalive 200 "service3"

        pms_api_put_services_keepalive 404 "service5"

        pms_api_get_service_ids 200 "service1" "service2" "service3"  "ric-registration"

        pms_api_delete_services 204 "service1"

        pms_api_get_service_ids 200 "service2" "service3" "ric-registration"


        pms_api_put_service 201 "service1" 50 "$CR_SERVICE_APP_PATH_0/1"

        pms_api_get_service_ids 200 "service1" "service2" "service3"  "ric-registration"


        pms_api_delete_services 204 "service1"
        pms_api_delete_services 204 "service3"

        pms_equal json:services 2

        pms_api_delete_services 204 "service2"

        pms_equal json:services 1


        echo "############################################"
        echo "############## RIC Repository ##############"
        echo "############################################"

        if [ "$PMS_VERSION" == "V2" ]; then
            pms_api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE  ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE ricsim_g3_1:me1_ricsim_g3_1,me2_ricsim_g3_1:STD_QOS_0_2_0,STD_QOS2_0.1.0:AVAILABLE"
        else
            pms_api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE  ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"
        fi
        pms_api_get_rics 200 1 "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

        pms_api_get_rics 404 47

        pms_api_get_rics 404 "test"

        if [ "$PMS_VERSION" == "V2" ]; then
            pms_api_get_ric 200 me1_ricsim_g1_1 NORIC "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

            pms_api_get_ric 200 me2_ricsim_g1_1 NORIC "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

            pms_api_get_ric 200 me1_ricsim_g2_1 NORIC "ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"

            pms_api_get_ric 200 me2_ricsim_g2_1 NORIC "ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"

            pms_api_get_ric 200 me1_ricsim_g3_1 NORIC "ricsim_g3_1:me1_ricsim_g3_1,me2_ricsim_g3_1:STD_QOS_0_2_0,STD_QOS2_0.1.0:AVAILABLE"

            pms_api_get_ric 200 me2_ricsim_g3_1 NORIC "ricsim_g3_1:me1_ricsim_g3_1,me2_ricsim_g3_1:STD_QOS_0_2_0,STD_QOS2_0.1.0:AVAILABLE"

            pms_api_get_ric 200 NOME      ricsim_g1_1 "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2:AVAILABLE"

            pms_api_get_ric 200 NOME      ricsim_g2_1 "ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"

            pms_api_get_ric 200 NOME      ricsim_g3_1 "ricsim_g3_1:me1_ricsim_g3_1,me2_ricsim_g3_1:STD_QOS_0_2_0,STD_QOS2_0.1.0:AVAILABLE"

            pms_api_get_ric 404 NOME test1

            pms_api_get_ric 404 test NORIC

            pms_api_get_ric 400 me1_ricsim_g1_1 ricsim_g1_1

            pms_api_get_ric 400 me1_ricsim_g1_1 TESTRIC

            pms_api_get_ric 400 TESTME ricsim_g1_1

        else
            pms_api_get_ric 200 me1_ricsim_g1_1 ricsim_g1_1

            pms_api_get_ric 200 me2_ricsim_g1_1 ricsim_g1_1

            pms_api_get_ric 200 me1_ricsim_g2_1 ricsim_g2_1

            pms_api_get_ric 200 me2_ricsim_g2_1 ricsim_g2_1

            pms_api_get_ric 404 test
        fi

        echo "############################################"
        echo "########### A1 Policy Management ###########"
        echo "############################################"

        if [ "$PMS_VERSION" == "V2" ]; then
            deviation "TR9 - pms modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            pms_api_get_policy_type 200 1 testdata/OSC/1-pms-modified.json
            deviation "TR9 - pms modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            pms_api_get_policy_type 200 2 testdata/OSC/2-pms-modified.json
            deviation "TR9 - pms modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            pms_api_get_policy_type 200 STD_QOS_0_2_0 testdata/STD2/qos-pms-modified.json
            deviation "TR9 - pms modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            pms_api_get_policy_type 200 STD_QOS2_0.1.0 testdata/STD2/qos2-pms-modified.json

            pms_api_get_policy_type 404 3
        else
            deviation "TR9 - pms modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            pms_api_get_policy_schema 200 1 testdata/OSC/1-pms-modified.json
            deviation "TR9 - pms modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            pms_api_get_policy_schema 200 2 testdata/OSC/2-pms-modified.json

            pms_api_get_policy_schema 404 3
        fi

        if [ "$PMS_VERSION" == "V2" ]; then
            pms_api_get_policy_schemas 404
        else
            deviation "TR9 - pms modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            pms_api_get_policy_schemas 200 NORIC testdata/OSC/1-pms-modified.json testdata/OSC/2-pms-modified.json NOFILE
            deviation "TR9 - pms modify the type with type id - test combo $interface and $__httpx"
            #Behaviour accepted for now
            pms_api_get_policy_schemas 200 ricsim_g1_1 testdata/OSC/1-pms-modified.json testdata/OSC/2-pms-modified.json

            pms_api_get_policy_schemas 200 ricsim_g2_1 NOFILE

            pms_api_get_policy_schemas 404 test
        fi


        if [ "$PMS_VERSION" == "V2" ]; then
            pms_api_get_policy_types 200 NORIC 1 2 EMPTY STD_QOS_0_2_0 STD_QOS2_0.1.0
        else
            pms_api_get_policy_types 200 NORIC 1 2 EMPTY
        fi

        pms_api_get_policy_types 200 ricsim_g1_1 1 2

        pms_api_get_policy_types 200 ricsim_g2_1 EMPTY

        if [ "$PMS_VERSION" == "V2" ]; then
            pms_api_get_policy_types 200 ricsim_g3_1 STD_QOS_0_2_0 STD_QOS2_0.1.0
        fi

        pms_api_get_policy_types 404 dummy-ric



        pms_api_put_service 201 "service10" 3600 "$CR_SERVICE_APP_PATH_0/1"

        if [ "$PMS_VERSION" == "V2" ]; then
            notificationurl=$CR_SERVICE_APP_PATH_0"/test"
        else
            notificationurl=""
        fi
        if [[ $interface != *"DMAAP"* ]]; then
            # Badly formatted json is not possible to send via dmaap
            pms_api_put_policy 400 "unregistered-service" ricsim_g1_1 1 2000 NOTRANSIENT $notificationurl testdata/OSC/pi_bad_template.json
        fi
        deviation "TR10 - pms allows policy creation on unregistered service (orig problem) - test combo $interface and $__httpx"
        #Kept until decison
        #pms_api_put_policy 400 "unregistered-service" ricsim_g1_1 1 2000 NOTRANSIENT testdata/OSC/pi1_template.json
        #Allow 201 for now
        pms_api_put_policy 201 "unregistered-service" ricsim_g1_1 1 2000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json

        pms_api_put_policy 201 "service10" ricsim_g1_1 1 5000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json
        pms_api_put_policy 200 "service10" ricsim_g1_1 1 5000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json

        pms_api_put_policy 200 "service10" ricsim_g1_1 1 5000 true $notificationurl testdata/OSC/pi1_template.json
        pms_api_put_policy 200 "service10" ricsim_g1_1 1 5000 false $notificationurl testdata/OSC/pi1_template.json

        pms_api_put_policy 201 "service10" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json
        pms_api_put_policy 200 "service10" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json

        pms_api_put_policy 200 "service10" ricsim_g2_1 NOTYPE 5100 true $notificationurl testdata/STD/pi1_template.json
        pms_api_put_policy 200 "service10" ricsim_g2_1 NOTYPE 5100 false $notificationurl testdata/STD/pi1_template.json

        if [ "$PMS_VERSION" == "V2" ]; then
            pms_api_put_policy 201 "service10" ricsim_g3_1 STD_QOS2_0.1.0 5200 NOTRANSIENT $notificationurl testdata/STD2/pi_qos2_template.json
            pms_api_put_policy 200 "service10" ricsim_g3_1 STD_QOS2_0.1.0 5200 NOTRANSIENT $notificationurl testdata/STD2/pi_qos2_template.json

            pms_api_put_policy 200 "service10" ricsim_g3_1 STD_QOS2_0.1.0 5200 true $notificationurl testdata/STD2/pi_qos2_template.json
            pms_api_put_policy 200 "service10" ricsim_g3_1 STD_QOS2_0.1.0 5200 false $notificationurl testdata/STD2/pi_qos2_template.json
        fi

        pms_api_get_policy_status 404 1
        pms_api_get_policy_status 404 2
        VAL='NOT IN EFFECT'
        pms_api_get_policy_status 200 5000 OSC "$VAL" "false"
        pms_api_get_policy_status 200 5100 STD "UNDEFINED"
        if [ "$PMS_VERSION" == "V2" ]; then
            pms_api_get_policy_status 200 5200 STD2 EMPTY EMPTY
        fi


        deviation "TR10 - pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #pms_equal json:policies 2
        #Allow 3 for now
        if [ "$PMS_VERSION" == "V2" ]; then
            pms_equal json:policies 4
        else
            pms_equal json:policies 3
        fi

        deviation "TR10 - pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #pms_equal json:policy_ids 2
        #Allow 3 for now
        if [ "$PMS_VERSION" == "V2" ]; then
            pms_equal json:policy-instances 4
        else
            pms_equal json:policy_ids 3
        fi

        deviation "TR10 - pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #pms_api_get_policy_ids 200 NORIC NOSERVICE NOTYPE 5000 5100
        #Allow policy create with unregistered service for now
        if [ "$PMS_VERSION" == "V2" ]; then
            pms_api_get_policy_ids 200 NORIC NOSERVICE NOTYPE 5000 5100 2000 5200
        else
            pms_api_get_policy_ids 200 NORIC NOSERVICE NOTYPE 5000 5100 2000
        fi

        deviation "TR10 - pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #pms_api_get_policy_ids 200 ricsim_g1_1 NOSERVICE NOTYPE 5000
        #Allow policy create with unregistered service for now
        if [ "$PMS_VERSION" == "V2" ]; then
            pms_api_get_policy_ids 200 ricsim_g1_1 NOSERVICE NOTYPE 5000 2000

            pms_api_get_policy_ids 200 ricsim_g2_1 NOSERVICE NOTYPE 5100

            pms_api_get_policy_ids 200 ricsim_g3_1 NOSERVICE NOTYPE 5200

            pms_api_get_policy_ids 200 NORIC "service10" NOTYPE 5000 5100 5200
        else
            pms_api_get_policy_ids 200 ricsim_g1_1 NOSERVICE NOTYPE 5000 2000

            pms_api_get_policy_ids 200 ricsim_g2_1 NOSERVICE NOTYPE 5100


            pms_api_get_policy_ids 200 NORIC "service10" NOTYPE 5000 5100
        fi

        deviation "TR10 - pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #pms_api_get_policy_ids 200 NORIC NOSERVICE 1 5000
        #Allow policy create with unregistered service for now

        pms_api_get_policy_ids 200 NORIC NOSERVICE 1 5000 2000

        pms_api_get_policy_ids 200 NORIC NOSERVICE 2 NOID

        if [ "$PMS_VERSION" == "V2" ]; then
            pms_api_get_policy_ids 200 NORIC NOSERVICE STD_QOS2_0.1.0 5200
        fi

        pms_api_get_policy_ids 200 ricsim_g2_1 NOSERVICE 1 NOID

        if [ "$PMS_VERSION" == "V2" ]; then
            pms_api_get_policy 200 5000 testdata/OSC/pi1_template.json "service10" ricsim_g1_1 1 false $notificationurl

            pms_api_get_policy 200 5100 testdata/STD/pi1_template.json "service10" ricsim_g2_1 NOTYPE false $notificationurl

            pms_api_get_policy 200 5200 testdata/STD2/pi_qos2_template.json "service10" ricsim_g3_1 STD_QOS2_0.1.0 false $notificationurl

            pms_api_get_policies 200 ricsim_g1_1 "service10" 1 5000 ricsim_g1_1 "service10" 1 false $notificationurl testdata/OSC/pi1_template.json
        else
            pms_api_get_policy 200 5000 testdata/OSC/pi1_template.json

            pms_api_get_policy 200 5100 testdata/STD/pi1_template.json

            pms_api_get_policies 200 ricsim_g1_1 "service10" 1 5000 ricsim_g1_1 "service10" 1 testdata/OSC/pi1_template.json
        fi

        deviation "TR10 - pms allows policy creation on unregistered service (side effect of orig. problem)- test combo $interface and $__httpx"
        #kept until decision
        #pms_api_delete_policy 404 2000
        #Allow policy create with unregistered service for now
        pms_api_delete_policy 204 2000

        pms_api_delete_policy 404 1500

        pms_api_delete_policy 204 5000

        if [ "$PMS_VERSION" == "V2" ]; then

            pms_api_delete_policy 204 5200
        fi

        pms_equal json:policies 1


        if [ "$PMS_VERSION" == "V2" ]; then
            pms_equal json:policy-instances 1
        else
            pms_equal json:policy_ids 1
        fi

        pms_api_delete_policy 204 5100

        pms_equal json:policies 0

        if [ "$PMS_VERSION" == "V2" ]; then
            pms_equal json:policy-instances 0
        else
            pms_equal json:policy_ids 0
        fi

        if [ "$PMS_VERSION" == "V2" ]; then
            cr_equal 0 received_callbacks 3
        fi

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
            sim_contains_str ricsim_g1_1 remote_hosts $SDNC_APP_NAME
            sim_contains_str ricsim_g2_1 remote_hosts $SDNC_APP_NAME
            if [ "$PMS_VERSION" == "V2" ]; then
                sim_contains_str ricsim_g3_1 remote_hosts $SDNC_APP_NAME
            fi
        else
            sim_contains_str ricsim_g1_1 remote_hosts $PMS_APP_NAME
            sim_contains_str ricsim_g2_1 remote_hosts $PMS_APP_NAME
            if [ "$PMS_VERSION" == "V2" ]; then
                sim_contains_str ricsim_g3_1 remote_hosts $PMS_APP_NAME
            fi
        fi

        check_pms_logs

        if [[ $interface = *"SDNC"* ]]; then
            check_sdnc_logs
        fi

        store_logs          "${__httpx}__${interface}"

    done

done

#### TEST COMPLETE ####


print_result

auto_clean_environment
