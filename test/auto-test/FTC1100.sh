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


TC_ONELINE_DESCR="ICS full interfaces walkthrough - with or without istio enabled"

USE_ISTIO=0

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="ICS PRODSTUB CR RICSIM CP HTTPPROXY NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
if [ $USE_ISTIO -eq 0 ]; then
    KUBE_INCLUDED_IMAGES="PRODSTUB CR ICS RICSIM CP HTTPPROXY KUBEPROXY NGW"
else
    KUBE_INCLUDED_IMAGES="PRODSTUB CR ICS RICSIM CP HTTPPROXY KUBEPROXY NGW KEYCLOAK ISTIO AUTHSIDECAR"
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
    echo ""

    istio_enable_istio_namespace $KUBE_SIM_NAMESPACE
    istio_enable_istio_namespace $KUBE_NONRTRIC_NAMESPACE
    istio_enable_istio_namespace $KUBE_A1SIM_NAMESPACE
fi

start_kube_proxy
set_kubeproxy_debug

if [ $USE_ISTIO -eq 1 ]; then
    use_ics_rest_http

    use_prod_stub_http

    use_simulator_http

    use_cr_http
else
    use_ics_rest_https

    use_prod_stub_https

    use_simulator_https

    use_cr_https
fi

start_http_proxy

if [ $USE_ISTIO -eq 1 ]; then
    start_keycloak

    keycloak_api_obtain_admin_token

    keycloak_api_create_realm                   nrtrealm   true   60
    keycloak_api_create_confidential_client     nrtrealm   icsc
    keycloak_api_generate_client_secret         nrtrealm   icsc
    keycloak_api_get_client_secret              nrtrealm   icsc
    keycloak_api_create_client_roles            nrtrealm   icsc nrtrole
    keycloak_api_map_client_roles               nrtrealm   icsc nrtrole

    keycloak_api_get_client_token               nrtrealm   icsc

    CLIENT_TOKEN=$(keycloak_api_read_client_token nrtrealm   icsc)
    echo "CLIENT_TOKEN: "$CLIENT_TOKEN

    ICS_SEC=$(keycloak_api_read_client_secret nrtrealm   icsc)
    echo "ICS_SEC: "$ICS_SEC

    istio_req_auth_by_jwks              $PROD_STUB_APP_NAME $KUBE_SIM_NAMESPACE KUBEPROXY "$KUBE_PROXY_ISTIO_JWKS_KEYS"
    istio_auth_policy_by_issuer         $PROD_STUB_APP_NAME $KUBE_SIM_NAMESPACE KUBEPROXY

    istio_req_auth_by_jwksuri           $PROD_STUB_APP_NAME $KUBE_SIM_NAMESPACE nrtrealm
    istio_auth_policy_by_realm          $PROD_STUB_APP_NAME $KUBE_SIM_NAMESPACE nrtrealm icsc nrtrole

    istio_req_auth_by_jwks              $CR_APP_NAME $KUBE_SIM_NAMESPACE KUBEPROXY "$KUBE_PROXY_ISTIO_JWKS_KEYS"
    istio_auth_policy_by_issuer         $CR_APP_NAME $KUBE_SIM_NAMESPACE KUBEPROXY

    istio_req_auth_by_jwksuri           $CR_APP_NAME $KUBE_SIM_NAMESPACE nrtrealm
    istio_auth_policy_by_realm          $CR_APP_NAME $KUBE_SIM_NAMESPACE nrtrealm icsc nrtrole

    ics_configure_sec nrtrealm icsc $ICS_SEC

fi

start_ics NOPROXY $SIM_GROUP/$ICS_COMPOSE_DIR/$ICS_CONFIG_FILE  #Change NOPROXY to PROXY to run with http proxy

if [ $RUNMODE == "KUBE" ]; then
    ics_api_admin_reset
fi

start_prod_stub

set_ics_debug

start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
    start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
fi

start_ric_simulators ricsim_g3 4  STD_2.0.0

start_cr 1

if [ $USE_ISTIO -eq 1 ]; then
    echo "Sleep 120 to let istio settle - enabling istio on workloads may cause initial dns disturbances - temporary unavailable dns names"
    sleep 120
fi

CB_JOB="$PROD_STUB_SERVICE_PATH$PROD_STUB_JOB_CALLBACK"
CB_SV="$PROD_STUB_SERVICE_PATH$PROD_STUB_SUPERVISION_CALLBACK"
#Targets for ei jobs
if [ $RUNMODE == "KUBE" ]; then
    TARGET1="$RIC_SIM_HTTPX://ricsim-g3-1.ricsim-g3.$KUBE_A1SIM_NAMESPACE:$RIC_SIM_PORT/datadelivery"
    TARGET2="$RIC_SIM_HTTPX://ricsim-g3-2.ricsim-g3.$KUBE_A1SIM_NAMESPACE:$RIC_SIM_PORT/datadelivery"
    TARGET3="$RIC_SIM_HTTPX://ricsim-g3-3.ricsim-g3.$KUBE_A1SIM_NAMESPACE:$RIC_SIM_PORT/datadelivery"
    TARGET8="$RIC_SIM_HTTPX://ricsim-g3-4.ricsim-g3.$KUBE_A1SIM_NAMESPACE:$RIC_SIM_PORT/datadelivery"
    TARGET10="$RIC_SIM_HTTPX://ricsim-g3-4.ricsim-g3.$KUBE_A1SIM_NAMESPACE:$RIC_SIM_PORT/datadelivery"
else
    TARGET1="$RIC_SIM_HTTPX://ricsim_g3_1:$RIC_SIM_PORT/datadelivery"
    TARGET2="$RIC_SIM_HTTPX://ricsim_g3_2:$RIC_SIM_PORT/datadelivery"
    TARGET3="$RIC_SIM_HTTPX://ricsim_g3_3:$RIC_SIM_PORT/datadelivery"
    TARGET8="$RIC_SIM_HTTPX://ricsim_g3_4:$RIC_SIM_PORT/datadelivery"
    TARGET10="$RIC_SIM_HTTPX://ricsim_g3_4:$RIC_SIM_PORT/datadelivery"
fi

#Targets for info jobs
TARGET101="http://localhost:80/target"  # Dummy target, no target for info data in this env...
TARGET102="http://localhost:80/target"  # Dummy target, no target for info data in this env...
TARGET103="http://localhost:80/target"  # Dummy target, no target for info data in this env...
TARGET108="http://localhost:80/target"  # Dummy target, no target for info data in this env...
TARGET110="http://localhost:80/target"  # Dummy target, no target for info data in this env...
TARGET150="http://localhost:80/target"  # Dummy target, no target for info data in this env...
TARGET160="http://localhost:80/target"  # Dummy target, no target for info data in this env...

#Status callbacks for eijobs
STATUS1="$CR_SERVICE_APP_PATH_0/job1-status"
STATUS2="$CR_SERVICE_APP_PATH_0/job2-status"
STATUS3="$CR_SERVICE_APP_PATH_0/job3-status"
STATUS8="$CR_SERVICE_APP_PATH_0/job8-status"
STATUS10="$CR_SERVICE_APP_PATH_0/job10-status"

#Status callbacks for infojobs
INFOSTATUS101="$CR_SERVICE_APP_PATH_0/info-job101-status"
INFOSTATUS102="$CR_SERVICE_APP_PATH_0/info-job102-status"
INFOSTATUS103="$CR_SERVICE_APP_PATH_0/info-job103-status"
INFOSTATUS108="$CR_SERVICE_APP_PATH_0/info-job108-status"
INFOSTATUS110="$CR_SERVICE_APP_PATH_0/info-job110-status"
INFOSTATUS150="$CR_SERVICE_APP_PATH_0/info-job150-status"
INFOSTATUS160="$CR_SERVICE_APP_PATH_0/info-job160-status"

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    #Type registration status callbacks
    TYPESTATUS1="$CR_SERVICE_APP_PATH_0/type-status1"
    TYPESTATUS2="$CR_SERVICE_APP_PATH_0/type-status2"

    ics_api_idc_put_subscription 201 subscription-id-1 owner1 $TYPESTATUS1

    ics_api_idc_get_subscription_ids 200 NOOWNER subscription-id-1

    ics_api_idc_get_subscription_ids 200 owner1 subscription-id-1

    ics_api_idc_get_subscription_ids 200 test EMPTY

    ics_api_idc_get_subscription 200 subscription-id-1 owner1 $TYPESTATUS1

    ics_api_idc_get_subscription 404 test

    ics_api_idc_put_subscription 200 subscription-id-1 owner1 $TYPESTATUS1

    ics_api_idc_put_subscription 200 subscription-id-1 owner1 $TYPESTATUS1

    ics_api_idc_put_subscription 201 subscription-id-2 owner2 $TYPESTATUS2

    ics_api_idc_get_subscription_ids 200 NOOWNER subscription-id-1 subscription-id-2

    ics_api_idc_get_subscription_ids 200 owner1 subscription-id-1

    ics_api_idc_get_subscription_ids 200 owner2 subscription-id-2

    ics_api_idc_get_subscription 200 subscription-id-1 owner1 $TYPESTATUS1
    ics_api_idc_get_subscription 200 subscription-id-2 owner2 $TYPESTATUS2

    ics_api_idc_delete_subscription 204 subscription-id-2

    ics_api_idc_get_subscription_ids 200 NOOWNER subscription-id-1

    ics_api_edp_put_type_2 201 type1 testdata/ics/ei-type-1.json

    cr_equal 0 received_callbacks 1 30
    cr_equal 0 received_callbacks?id=type-status1 1
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type1 testdata/ics/ei-type-1.json REGISTERED

    ics_api_edp_delete_type_2 204 type1

    cr_equal 0 received_callbacks 2 30
    cr_equal 0 received_callbacks?id=type-status1 2
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type1 testdata/ics/ei-type-1.json DEREGISTERED

    ics_api_idc_put_subscription 201 subscription-id-2 owner2 $TYPESTATUS2
    ics_api_idc_get_subscription_ids 200 NOOWNER subscription-id-1 subscription-id-2

    ics_api_edp_put_type_2 201 type1 testdata/ics/ei-type-1.json

    cr_equal 0 received_callbacks 4 30
    cr_equal 0 received_callbacks?id=type-status1 3
    cr_equal 0 received_callbacks?id=type-status2 1
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type1 testdata/ics/ei-type-1.json REGISTERED

    ics_api_idc_delete_subscription 204 subscription-id-2

    ics_api_edp_delete_type_2 204 type1

    cr_equal 0 received_callbacks 5 30
    cr_equal 0 received_callbacks?id=type-status1 4
    cr_equal 0 received_callbacks?id=type-status2 1
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type1 testdata/ics/ei-type-1.json DEREGISTERED

    cr_api_reset 0
fi

### Setup prodstub sim to accept calls for producers, types and jobs
## prod-a type1
## prod-b type1 and type2
## prod-c no-type
## prod-d type4
## prod-e type6
## prod-f type6

## job1 -> prod-a
## job2 -> prod-a
## job3 -> prod-b
## job4 -> prod-a
## job6 -> prod-b
## job8 -> prod-d
## job10 -> prod-e and prod-f

prodstub_arm_producer 200 prod-a
prodstub_arm_producer 200 prod-b
prodstub_arm_producer 200 prod-c
prodstub_arm_producer 200 prod-d
prodstub_arm_producer 200 prod-e
prodstub_arm_producer 200 prod-f

prodstub_arm_type 200 prod-a type1
prodstub_arm_type 200 prod-b type2
prodstub_arm_type 200 prod-b type3
prodstub_arm_type 200 prod-d type4
prodstub_arm_type 200 prod-e type6
prodstub_arm_type 200 prod-f type6

prodstub_disarm_type 200 prod-b type3
prodstub_arm_type 200 prod-b type1
prodstub_disarm_type 200 prod-b type1


prodstub_arm_job_create 200 prod-a job1
prodstub_arm_job_create 200 prod-a job2
prodstub_arm_job_create 200 prod-b job3

prodstub_arm_job_delete 200 prod-a job1
prodstub_arm_job_delete 200 prod-a job2
prodstub_arm_job_delete 200 prod-b job3

prodstub_arm_job_create 200 prod-b job4
prodstub_arm_job_create 200 prod-a job4

prodstub_arm_job_create 200 prod-b job6

prodstub_arm_job_create 200 prod-d job8

prodstub_arm_job_create 200 prod-e job10
prodstub_arm_job_create 200 prod-f job10

### ics status
ics_api_service_status 200

cr_equal 0 received_callbacks 0

### Initial tests - no config made
### GET: type ids, types, producer ids, producers, job ids, jobs
### DELETE: jobs
ics_api_a1_get_type_ids 200 EMPTY
ics_api_a1_get_type 404 test-type

ics_api_edp_get_type_ids 200 EMPTY
ics_api_edp_get_type_2 404 test-type

ics_api_edp_get_producer_ids_2 200 NOTYPE EMPTY
ics_api_edp_get_producer_2 404 test-prod

ics_api_edp_get_producer_status 404 test-prod

ics_api_edp_delete_producer 404 test-prod

ics_api_a1_get_job_ids 200 test-type NOWNER EMPTY
ics_api_a1_get_job_ids 200 test-type test-owner EMPTY

ics_api_a1_get_job 404 test-job

ics_api_a1_get_job_status 404 test-job

ics_api_a1_delete_job 404 test-job

ics_api_edp_get_producer_ids_2 200 NOTYPE EMPTY
ics_api_edp_get_producer_2 404 test-prod

ics_api_edp_get_type_2 404 test-type
ics_api_edp_delete_type_2 404 test-type

### Setup of producer/job and testing apis ###

## Setup prod-a
ics_api_edp_get_type_ids 200 EMPTY
ics_api_edp_get_type_2 404 type1
ics_api_edp_put_producer_2 404 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1

# Create type, delete and create again
ics_api_edp_put_type_2 201 type1 testdata/ics/ei-type-1.json
ics_api_edp_get_type_2 200 type1
ics_api_edp_get_type_ids 200 type1
ics_api_edp_delete_type_2 204 type1
ics_api_edp_get_type_2 404 type1
ics_api_edp_get_type_ids 200 EMPTY
if [[ "$ICS_FEATURE_LEVEL" == *"INFO-TYPE-INFO"* ]]; then
    ics_api_edp_put_type_2 201 type1 testdata/ics/ei-type-1.json testdata/ics/info-type-info.json
else
    ics_api_edp_put_type_2 201 type1 testdata/ics/ei-type-1.json
fi
ics_api_edp_get_type_ids 200 type1
if [[ "$ICS_FEATURE_LEVEL" == *"INFO-TYPE-INFO"* ]]; then
    ics_api_edp_get_type_2 200 type1 testdata/ics/ei-type-1.json testdata/ics/info-type-info.json
else
    ics_api_edp_get_type_2 200 type1 testdata/ics/ei-type-1.json
fi

ics_api_edp_put_producer_2 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1
ics_api_edp_put_producer_2 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 3 30
    cr_equal 0 received_callbacks?id=type-status1 3
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type1 testdata/ics/ei-type-1.json REGISTERED type1 testdata/ics/ei-type-1.json DEREGISTERED type1 testdata/ics/ei-type-1.json REGISTERED
else
    cr_equal 0 received_callbacks 0
fi

ics_api_a1_get_type_ids 200 type1
ics_api_a1_get_type 200 type1 testdata/ics/empty-type.json

ics_api_edp_get_type_ids 200 type1
if [[ "$ICS_FEATURE_LEVEL" == *"INFO-TYPE-INFO"* ]]; then
    ics_api_edp_get_type_2 200 type1 testdata/ics/ei-type-1.json testdata/ics/info-type-info.json
else
    ics_api_edp_get_type_2 200 type1 testdata/ics/ei-type-1.json
fi

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a
ics_api_edp_get_producer_ids_2 200 type1 prod-a
ics_api_edp_get_producer_ids_2 200 type2 EMPTY

ics_api_edp_get_producer_2 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1

ics_api_edp_get_producer_status 200 prod-a ENABLED

ics_api_a1_get_job_ids 200 type1 NOWNER EMPTY
ics_api_a1_get_job_ids 200 type1 test-owner EMPTY

ics_api_a1_get_job 404 test-job

ics_api_a1_get_job_status 404 test-job

ics_api_edp_get_producer_jobs_2 200 prod-a EMPTY

## Create a job for prod-a
## job1 - prod-a
ics_api_a1_put_job 201 job1 type1 $TARGET1 ricsim_g3_1 $STATUS1 testdata/ics/job-template.json

# Check the job data in the producer
prodstub_check_jobdata_3 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ics/job-template.json

ics_api_a1_get_job_ids 200 type1 NOWNER job1
ics_api_a1_get_job_ids 200 type1 ricsim_g3_1 job1

ics_api_a1_get_job_ids 200 NOTYPE NOWNER job1

ics_api_a1_get_job 200 job1 type1 $TARGET1 ricsim_g3_1 $STATUS1 testdata/ics/job-template.json

ics_api_a1_get_job_status 200 job1 ENABLED

prodstub_equal create/prod-a/job1 1

ics_api_edp_get_producer_jobs_2 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ics/job-template.json


## Create a second job for prod-a
## job2 - prod-a
ics_api_a1_put_job 201 job2 type1 $TARGET2 ricsim_g3_2 $STATUS2 testdata/ics/job-template.json

# Check the job data in the producer
prodstub_check_jobdata_3 200 prod-a job2 type1 $TARGET2 ricsim_g3_2 testdata/ics/job-template.json
ics_api_a1_get_job_ids 200 type1 NOWNER job1 job2
ics_api_a1_get_job_ids 200 type1 ricsim_g3_1 job1
ics_api_a1_get_job_ids 200 type1 ricsim_g3_2 job2
ics_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2

ics_api_a1_get_job 200 job2 type1 $TARGET2 ricsim_g3_2 $STATUS2 testdata/ics/job-template.json

ics_api_a1_get_job_status 200 job2 ENABLED

prodstub_equal create/prod-a/job2 1

ics_api_edp_get_producer_jobs_2 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ics/job-template.json job2 type1 $TARGET2 ricsim_g3_2 testdata/ics/job-template.json

## Setup prod-b
ics_api_edp_put_type_2 201 type2 testdata/ics/ei-type-2.json
ics_api_edp_put_producer_2 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type2
if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 4 30
    cr_equal 0 received_callbacks?id=type-status1 4
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type2 testdata/ics/ei-type-2.json REGISTERED
else
    cr_equal 0 received_callbacks 0
fi


ics_api_a1_get_type_ids 200 type1 type2

ics_api_a1_get_type 200 type1 testdata/ics/empty-type.json
ics_api_a1_get_type 200 type2 testdata/ics/empty-type.json

ics_api_edp_get_type_ids 200 type1 type2
if [[ "$ICS_FEATURE_LEVEL" == *"INFO-TYPE-INFO"* ]]; then
    ics_api_edp_get_type_2 200 type1 testdata/ics/ei-type-1.json testdata/ics/info-type-info.json
else
    ics_api_edp_get_type_2 200 type1 testdata/ics/ei-type-1.json
fi
ics_api_edp_get_type_2 200 type2 testdata/ics/ei-type-2.json

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a prod-b

ics_api_edp_get_producer_2 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1
ics_api_edp_get_producer_2 200 prod-b $CB_JOB/prod-b $CB_SV/prod-b type2

ics_api_edp_get_producer_status 200 prod-b ENABLED

## Create job for prod-b
##  job3 - prod-b
ics_api_a1_put_job 201 job3 type2 $TARGET3 ricsim_g3_3 $STATUS3 testdata/ics/job-template.json

prodstub_equal create/prod-b/job3 1

# Check the job data in the producer
prodstub_check_jobdata_3 200 prod-b job3 type2 $TARGET3 ricsim_g3_3 testdata/ics/job-template.json

ics_api_a1_get_job_ids 200 type1 NOWNER job1 job2
ics_api_a1_get_job_ids 200 type2 NOWNER job3
ics_api_a1_get_job_ids 200 type1 ricsim_g3_1 job1
ics_api_a1_get_job_ids 200 type1 ricsim_g3_2 job2
ics_api_a1_get_job_ids 200 type2 ricsim_g3_3 job3

ics_api_a1_get_job 200 job3 type2 $TARGET3 ricsim_g3_3 $STATUS3 testdata/ics/job-template.json

ics_api_a1_get_job_status 200 job3 ENABLED

ics_api_edp_get_producer_jobs_2 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ics/job-template.json job2 type1 $TARGET2 ricsim_g3_2 testdata/ics/job-template.json
ics_api_edp_get_producer_jobs_2 200 prod-b job3 type2 $TARGET3 ricsim_g3_3 testdata/ics/job-template.json


## Setup prod-c (no types)
ics_api_edp_put_producer_2 201 prod-c $CB_JOB/prod-c $CB_SV/prod-c NOTYPE

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a prod-b prod-c

ics_api_edp_get_producer_2 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1
ics_api_edp_get_producer_2 200 prod-b $CB_JOB/prod-b $CB_SV/prod-b type2
ics_api_edp_get_producer_2 200 prod-c $CB_JOB/prod-c $CB_SV/prod-c EMPTY

ics_api_edp_get_producer_status 200 prod-c ENABLED


## Delete job3 and prod-b and re-create if different order

# Delete job then producer
ics_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2 job3
ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a prod-b prod-c

ics_api_a1_delete_job 204 job3

ics_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2
ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a prod-b prod-c

ics_api_edp_delete_producer 204 prod-b

ics_api_edp_get_producer_status 404 prod-b

ics_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2
ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a prod-c

prodstub_equal delete/prod-b/job3 1

ics_api_a1_put_job 201 job3 type2 $TARGET3 ricsim_g3_3 $STATUS3 testdata/ics/job-template.json
ics_api_a1_get_job_status 200 job3 DISABLED

# Put producer then job
ics_api_edp_put_producer_2 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type2

ics_api_edp_get_producer_status 200 prod-b ENABLED

    ics_api_a1_put_job 200 job3 type2 $TARGET3 ricsim_g3_3 $STATUS3 testdata/ics/job-template2.json

ics_api_a1_get_job_status 200 job3 ENABLED

prodstub_check_jobdata_3 200 prod-b job3 type2 $TARGET3 ricsim_g3_3 testdata/ics/job-template2.json

ics_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2 job3
ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a prod-b prod-c

prodstub_equal create/prod-b/job3 3
prodstub_equal delete/prod-b/job3 1

# Delete only the producer
ics_api_edp_delete_producer 204 prod-b

ics_api_edp_get_producer_status 404 prod-b

ics_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2 job3
ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a prod-c

ics_api_a1_get_job_status 200 job3 DISABLED

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 5 30
    cr_equal 0 received_callbacks?id=type-status1 4
    cr_equal 0 received_callbacks?id=job3-status 1
    cr_api_check_all_ics_events 200 0 job3-status DISABLED
else
    cr_equal 0 received_callbacks 1 30
    cr_equal 0 received_callbacks?id=job3-status 1
    cr_api_check_all_ics_events 200 0 job3-status DISABLED
fi

# Re-create the producer
ics_api_edp_put_producer_2 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type2

ics_api_edp_get_producer_status 200 prod-b ENABLED

ics_api_a1_get_job_status 200 job3 ENABLED

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 6 30
    cr_equal 0 received_callbacks?id=type-status1 4
    cr_equal 0 received_callbacks?id=job3-status 2
    cr_api_check_all_ics_events 200 0 job3-status ENABLED
else
    cr_equal 0 received_callbacks 2 30
    cr_equal 0 received_callbacks?id=job3-status 2
    cr_api_check_all_ics_events 200 0 job3-status ENABLED
fi

prodstub_check_jobdata_3 200 prod-b job3 type2 $TARGET3 ricsim_g3_3 testdata/ics/job-template2.json

## Setup prod-d
ics_api_edp_put_type_2 201 type4 testdata/ics/ei-type-4.json
ics_api_edp_put_producer_2 201 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 7 30
    cr_equal 0 received_callbacks?id=type-status1 5
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type4 testdata/ics/ei-type-4.json REGISTERED
fi

ics_api_a1_get_job_ids 200 type4 NOWNER EMPTY

ics_api_a1_put_job 201 job8 type4 $TARGET8 ricsim_g3_4 $STATUS8 testdata/ics/job-template.json

prodstub_check_jobdata_3 200 prod-d job8 type4 $TARGET8 ricsim_g3_4 testdata/ics/job-template.json

prodstub_equal create/prod-d/job8 1
prodstub_equal delete/prod-d/job8 0

ics_api_a1_get_job_ids 200 type4 NOWNER job8

ics_api_a1_get_job_status 200 job8 ENABLED

# Re-PUT the producer with zero types
ics_api_edp_put_producer_2 200 prod-d $CB_JOB/prod-d $CB_SV/prod-d NOTYPE

ics_api_a1_get_job_ids 200 type4 NOWNER job8
ics_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2 job3 job8

ics_api_a1_get_job_status 200 job8 DISABLED

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 8 30
    cr_equal 0 received_callbacks?id=type-status1 5
    cr_equal 0 received_callbacks?id=job8-status 1
    cr_api_check_all_ics_events 200 0 job8-status DISABLED
else
    cr_equal 0 received_callbacks 3 30
    cr_equal 0 received_callbacks?id=job8-status 1
    cr_api_check_all_ics_events 200 0 job8-status DISABLED
fi

prodstub_equal create/prod-d/job8 1
prodstub_equal delete/prod-d/job8 0

## Re-setup prod-d
ics_api_edp_put_type_2 200 type4 testdata/ics/ei-type-4.json
ics_api_edp_put_producer_2 200 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4

ics_api_a1_get_job_ids 200 type4 NOWNER job8
ics_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2 job3 job8

ics_api_a1_get_job_status 200 job8 ENABLED

ics_api_edp_get_producer_status 200 prod-a ENABLED
ics_api_edp_get_producer_status 200 prod-b ENABLED
ics_api_edp_get_producer_status 200 prod-c ENABLED
ics_api_edp_get_producer_status 200 prod-d ENABLED

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 10 30
    cr_equal 0 received_callbacks?id=type-status1 6
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type4 testdata/ics/ei-type-4.json REGISTERED

    cr_equal 0 received_callbacks?id=job8-status 2
    cr_api_check_all_ics_events 200 0 job8-status ENABLED
else
    cr_equal 0 received_callbacks 4 30
    cr_equal 0 received_callbacks?id=job8-status 2
    cr_api_check_all_ics_events 200 0 job8-status ENABLED
fi

prodstub_equal create/prod-d/job8 2
prodstub_equal delete/prod-d/job8 0

## Setup prod-e
ics_api_edp_put_type_2 201 type6 testdata/ics/ei-type-6.json
ics_api_edp_put_producer_2 201 prod-e $CB_JOB/prod-e $CB_SV/prod-e type6

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 11 30
    cr_equal 0 received_callbacks?id=type-status1 7
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type6 testdata/ics/ei-type-6.json REGISTERED
fi

ics_api_a1_get_job_ids 200 type6 NOWNER EMPTY

ics_api_a1_put_job 201 job10 type6 $TARGET10 ricsim_g3_4 $STATUS10 testdata/ics/job-template.json

prodstub_check_jobdata_3 200 prod-e job10 type6 $TARGET10 ricsim_g3_4 testdata/ics/job-template.json

prodstub_equal create/prod-e/job10 1
prodstub_equal delete/prod-e/job10 0

ics_api_a1_get_job_ids 200 type6 NOWNER job10

ics_api_a1_get_job_status 200 job10 ENABLED

## Setup prod-f
ics_api_edp_put_type_2 200 type6 testdata/ics/ei-type-6.json
ics_api_edp_put_producer_2 201 prod-f $CB_JOB/prod-f $CB_SV/prod-f type6

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 12 30
    cr_equal 0 received_callbacks?id=type-status1 8
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type6 testdata/ics/ei-type-6.json REGISTERED
fi

ics_api_a1_get_job_ids 200 type6 NOWNER job10

prodstub_check_jobdata_3 200 prod-f job10 type6 $TARGET10 ricsim_g3_4 testdata/ics/job-template.json

prodstub_equal create/prod-f/job10 1
prodstub_equal delete/prod-f/job10 0

ics_api_a1_get_job_ids 200 type6 NOWNER job10

ics_api_a1_get_job_status 200 job10 ENABLED

## Status updates prod-a and jobs

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a prod-b prod-c prod-d prod-e prod-f

ics_api_edp_get_producer_status 200 prod-a ENABLED
ics_api_edp_get_producer_status 200 prod-b ENABLED
ics_api_edp_get_producer_status 200 prod-c ENABLED
ics_api_edp_get_producer_status 200 prod-d ENABLED
ics_api_edp_get_producer_status 200 prod-e ENABLED
ics_api_edp_get_producer_status 200 prod-f ENABLED

# Arm producer prod-a for supervision failure
prodstub_arm_producer 200 prod-a 400

# Wait for producer prod-a to go disabled
ics_api_edp_get_producer_status 200 prod-a DISABLED 360

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a prod-b prod-c prod-d  prod-e prod-f

ics_api_edp_get_producer_status 200 prod-a DISABLED
ics_api_edp_get_producer_status 200 prod-b ENABLED
ics_api_edp_get_producer_status 200 prod-c ENABLED
ics_api_edp_get_producer_status 200 prod-d ENABLED
ics_api_edp_get_producer_status 200 prod-e ENABLED
ics_api_edp_get_producer_status 200 prod-f ENABLED


ics_api_a1_get_job_status 200 job1 ENABLED
ics_api_a1_get_job_status 200 job2 ENABLED
ics_api_a1_get_job_status 200 job3 ENABLED
ics_api_a1_get_job_status 200 job8 ENABLED
ics_api_a1_get_job_status 200 job10 ENABLED

# Arm producer prod-a for supervision
prodstub_arm_producer 200 prod-a 200

# Wait for producer prod-a to go enabled
ics_api_edp_get_producer_status 200 prod-a ENABLED 360

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a prod-b prod-c prod-d prod-e prod-f

ics_api_edp_get_producer_status 200 prod-a ENABLED
ics_api_edp_get_producer_status 200 prod-b ENABLED
ics_api_edp_get_producer_status 200 prod-c ENABLED
ics_api_edp_get_producer_status 200 prod-d ENABLED
ics_api_edp_get_producer_status 200 prod-e ENABLED
ics_api_edp_get_producer_status 200 prod-f ENABLED

ics_api_a1_get_job_status 200 job1 ENABLED
ics_api_a1_get_job_status 200 job2 ENABLED
ics_api_a1_get_job_status 200 job3 ENABLED
ics_api_a1_get_job_status 200 job8 ENABLED
ics_api_a1_get_job_status 200 job10 ENABLED

# Arm producer prod-a for supervision failure
prodstub_arm_producer 200 prod-a 400

# Wait for producer prod-a to go disabled
ics_api_edp_get_producer_status 200 prod-a DISABLED 360

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a prod-b prod-c prod-d prod-e prod-f

ics_api_edp_get_producer_status 200 prod-a DISABLED
ics_api_edp_get_producer_status 200 prod-b ENABLED
ics_api_edp_get_producer_status 200 prod-c ENABLED
ics_api_edp_get_producer_status 200 prod-d ENABLED
ics_api_edp_get_producer_status 200 prod-e ENABLED
ics_api_edp_get_producer_status 200 prod-f ENABLED

ics_api_a1_get_job_status 200 job1 ENABLED
ics_api_a1_get_job_status 200 job2 ENABLED
ics_api_a1_get_job_status 200 job3 ENABLED
ics_api_a1_get_job_status 200 job8 ENABLED
ics_api_a1_get_job_status 200 job10 ENABLED

# Wait for producer prod-a to be removed
ics_equal json:data-producer/v1/info-producers 5 1000

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-b prod-c prod-d prod-e prod-f


ics_api_edp_get_producer_status 404 prod-a
ics_api_edp_get_producer_status 200 prod-b ENABLED
ics_api_edp_get_producer_status 200 prod-c ENABLED
ics_api_edp_get_producer_status 200 prod-d ENABLED
ics_api_edp_get_producer_status 200 prod-e ENABLED
ics_api_edp_get_producer_status 200 prod-f ENABLED

ics_api_a1_get_job_status 200 job1 DISABLED
ics_api_a1_get_job_status 200 job2 DISABLED
ics_api_a1_get_job_status 200 job3 ENABLED
ics_api_a1_get_job_status 200 job8 ENABLED
ics_api_a1_get_job_status 200 job10 ENABLED

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 14 30
else
    cr_equal 0 received_callbacks 6 30
fi

cr_equal 0 received_callbacks?id=job1-status 1
cr_equal 0 received_callbacks?id=job2-status 1

cr_api_check_all_ics_events 200 0 job1-status DISABLED
cr_api_check_all_ics_events 200 0 job2-status DISABLED


# Arm producer prod-e for supervision failure
prodstub_arm_producer 200 prod-e 400

ics_api_edp_get_producer_status 200 prod-e DISABLED 1000

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-b prod-c prod-d prod-e prod-f

ics_api_edp_get_producer_status 404 prod-a
ics_api_edp_get_producer_status 200 prod-b ENABLED
ics_api_edp_get_producer_status 200 prod-c ENABLED
ics_api_edp_get_producer_status 200 prod-d ENABLED
ics_api_edp_get_producer_status 200 prod-e DISABLED
ics_api_edp_get_producer_status 200 prod-f ENABLED

ics_api_a1_get_job_status 200 job1 DISABLED
ics_api_a1_get_job_status 200 job2 DISABLED
ics_api_a1_get_job_status 200 job3 ENABLED
ics_api_a1_get_job_status 200 job8 ENABLED
ics_api_a1_get_job_status 200 job10 ENABLED

#Disable create for job10 in prod-e
prodstub_arm_job_create 200 prod-e job10 400

#Update tjob 10 - only prod-f will be updated
ics_api_a1_put_job 200 job10 type6 $TARGET10 ricsim_g3_4 $STATUS10 testdata/ics/job-template2.json
#Reset producer and job responses
prodstub_arm_producer 200 prod-e 200
prodstub_arm_job_create 200 prod-e job10 200

ics_api_edp_get_producer_status 200 prod-e ENABLED 360

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-b prod-c prod-d prod-e prod-f

#Wait for job to be updated
sleep_wait 120

prodstub_check_jobdata_3 200 prod-f job10 type6 $TARGET10 ricsim_g3_4 testdata/ics/job-template2.json

prodstub_arm_producer 200 prod-f 400

ics_api_edp_get_producer_status 200 prod-f DISABLED 360

ics_equal json:data-producer/v1/info-producers 4 1000

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-b prod-c prod-d prod-e

ics_api_edp_get_producer_status 404 prod-a
ics_api_edp_get_producer_status 200 prod-b ENABLED
ics_api_edp_get_producer_status 200 prod-c ENABLED
ics_api_edp_get_producer_status 200 prod-d ENABLED
ics_api_edp_get_producer_status 200 prod-e ENABLED
ics_api_edp_get_producer_status 404 prod-f

ics_api_a1_get_job_status 200 job1 DISABLED
ics_api_a1_get_job_status 200 job2 DISABLED
ics_api_a1_get_job_status 200 job3 ENABLED
ics_api_a1_get_job_status 200 job8 ENABLED
ics_api_a1_get_job_status 200 job10 ENABLED

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 14 30
else
    cr_equal 0 received_callbacks 6 30
fi


############################################
# Test of info types
############################################

### Setup prodstub sim to accept calls for producers, info types and jobs
## prod-ia type101
## prod-ib type101 and type102
## prod-ic no-type
## prod-id type104
## prod-ie type106
## prod-if type106
## prod-ig type150  (configured later)
## prod-ig type160  (configured later)

## job101 -> prod-ia
## job102 -> prod-ia
## job103 -> prod-ib
## job104 -> prod-ia
## job106 -> prod-ib
## job108 -> prod-id
## job110 -> prod-ie and prod-if
## job150 -> prod-ig  (configured later)

prodstub_arm_producer 200 prod-ia
prodstub_arm_producer 200 prod-ib
prodstub_arm_producer 200 prod-ic
prodstub_arm_producer 200 prod-id
prodstub_arm_producer 200 prod-ie
prodstub_arm_producer 200 prod-if

prodstub_arm_type 200 prod-ia type101
prodstub_arm_type 200 prod-ib type102
prodstub_arm_type 200 prod-ib type103
prodstub_arm_type 200 prod-id type104
prodstub_arm_type 200 prod-ie type106
prodstub_arm_type 200 prod-if type106

prodstub_disarm_type 200 prod-ib type103
prodstub_arm_type 200 prod-ib type101
prodstub_disarm_type 200 prod-ib type101


prodstub_arm_job_create 200 prod-ia job101
prodstub_arm_job_create 200 prod-ia job102
prodstub_arm_job_create 200 prod-ib job103

prodstub_arm_job_delete 200 prod-ia job101
prodstub_arm_job_delete 200 prod-ia job102
prodstub_arm_job_delete 200 prod-ib job103

prodstub_arm_job_create 200 prod-ib job104
prodstub_arm_job_create 200 prod-ia job104

prodstub_arm_job_create 200 prod-ib job106

prodstub_arm_job_create 200 prod-id job108

prodstub_arm_job_create 200 prod-ie job110
prodstub_arm_job_create 200 prod-if job110


# NOTE: types, jobs and producers are still present related to eitypes


### Initial tests - no config made
### GET: type ids, types, producer ids, producers, job ids, jobs
### DELETE: jobs
ics_api_idc_get_type_ids 200 type1 type2 type4 type6
ics_api_idc_get_type 404 test-type

ics_api_edp_get_type_ids 200 type1 type2 type4 type6
ics_api_edp_get_type_2 404 test-type

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-b prod-c prod-d prod-e
ics_api_edp_get_producer_2 404 test-prod
ics_api_edp_get_producer_status 404 test-prod

ics_api_edp_delete_producer 404 test-prod

ics_api_idc_get_job_ids 200 test-type NOWNER EMPTY
ics_api_idc_get_job_ids 200 test-type test-owner EMPTY

ics_api_idc_get_job 404 test-job

ics_api_idc_get_job_status2 404 test-job

ics_api_idc_delete_job 404 test-job

ics_api_edp_get_producer_jobs_2 404 test-prod

ics_api_edp_get_type_2 404 test-type
ics_api_edp_delete_type_2 404 test-type

### Setup of producer/job and testing apis ###

## Setup prod-ia
ics_api_edp_get_type_ids 200 type1 type2 type4 type6
ics_api_edp_get_type_2 404 type101
ics_api_edp_put_producer_2 404 prod-ia $CB_JOB/prod-ia $CB_SV/prod-ia type101

# Create type, delete and create again
ics_api_edp_put_type_2 201 type101 testdata/ics/info-type-1.json
ics_api_edp_get_type_2 200 type101
ics_api_edp_get_type_ids 200 type101 type1 type2 type4 type6
ics_api_edp_delete_type_2 204 type101
ics_api_edp_get_type_2 404 type101
ics_api_edp_get_type_ids 200 type1 type2 type4 type6
ics_api_edp_put_type_2 201 type101 testdata/ics/info-type-1.json
ics_api_edp_get_type_ids 200 type101 type1 type2 type4 type6
ics_api_edp_get_type_2 200 type101 testdata/ics/info-type-1.json

ics_api_edp_put_producer_2 201 prod-ia $CB_JOB/prod-ia $CB_SV/prod-ia type101
ics_api_edp_put_producer_2 200 prod-ia $CB_JOB/prod-ia $CB_SV/prod-ia type101

if [[ "$ICS_FEATURE_LEVEL" == *"RESP_CODE_CHANGE_1"* ]]; then
    ics_api_edp_delete_type_2 409 type101
else
    ics_api_edp_delete_type_2 406 type101
fi

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 17 30
    cr_equal 0 received_callbacks?id=type-status1 11
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type101 testdata/ics/info-type-1.json REGISTERED type101 testdata/ics/info-type-1.json DEREGISTERED type101 testdata/ics/info-type-1.json REGISTERED
else
    cr_equal 0 received_callbacks 6
fi

ics_api_edp_get_type_ids 200 type101 type1 type2 type4 type6
ics_api_edp_get_type_2 200 type101 testdata/ics/info-type-1.json

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-b prod-c prod-d prod-e
ics_api_edp_get_producer_ids_2 200 type101 prod-ia
ics_api_edp_get_producer_ids_2 200 type102 EMPTY

ics_api_edp_get_producer_2 200 prod-ia $CB_JOB/prod-ia $CB_SV/prod-ia type101

ics_api_edp_get_producer_status 200 prod-ia ENABLED

ics_api_idc_get_job_ids 200 type101 NOWNER EMPTY
ics_api_idc_get_job_ids 200 type101 test-owner EMPTY

ics_api_idc_get_job 404 test-job

ics_api_idc_get_job_status2 404 test-job
ics_api_edp_get_producer_jobs_2 200 prod-ia EMPTY

## Create a job for prod-ia
## job101 - prod-ia
ics_api_idc_put_job 201 job101 type101 $TARGET101 info-owner-1 $INFOSTATUS101 testdata/ics/job-template.json VALIDATE

# Check the job data in the producer
prodstub_check_jobdata_3 200 prod-ia job101 type101 $TARGET101 info-owner-1 testdata/ics/job-template.json

ics_api_idc_get_job_ids 200 type101 NOWNER job101
ics_api_idc_get_job_ids 200 type101 info-owner-1 job101

ics_api_idc_get_job_ids 200 NOTYPE NOWNER job101 job1 job2 job3 job8 job10

ics_api_idc_get_job 200 job101 type101 $TARGET101 info-owner-1 $INFOSTATUS101 testdata/ics/job-template.json

ics_api_idc_get_job_status2 200 job101 ENABLED  1 prod-ia

prodstub_equal create/prod-ia/job101 1

ics_api_edp_get_producer_jobs_2 200 prod-ia job101 type101 $TARGET101 info-owner-1 testdata/ics/job-template.json

## Create a second job for prod-ia
## job102 - prod-ia
ics_api_idc_put_job 201 job102 type101 $TARGET102 info-owner-2 $INFOSTATUS102 testdata/ics/job-template.json  VALIDATE

# Check the job data in the producer
prodstub_check_jobdata_3 200 prod-ia job102 type101 $TARGET102 info-owner-2 testdata/ics/job-template.json
ics_api_idc_get_job_ids 200 type101 NOWNER job101 job102
ics_api_idc_get_job_ids 200 type101 info-owner-1 job101
ics_api_idc_get_job_ids 200 type101 info-owner-2 job102
ics_api_idc_get_job_ids 200 NOTYPE NOWNER job101 job102 job1 job2 job3 job8 job10

ics_api_idc_get_job 200 job102 type101 $TARGET102 info-owner-2 $INFOSTATUS102 testdata/ics/job-template.json

ics_api_idc_get_job_status2 200 job102 ENABLED 1 prod-ia

prodstub_equal create/prod-ia/job102 1

ics_api_edp_get_producer_jobs_2 200 prod-ia job101 type101 $TARGET101 info-owner-1 testdata/ics/job-template.json job102 type101 $TARGET102 info-owner-2 testdata/ics/job-template.json


## Setup prod-ib
ics_api_edp_put_type_2 201 type102 testdata/ics/info-type-2.json
ics_api_edp_put_producer_2 201 prod-ib $CB_JOB/prod-ib $CB_SV/prod-ib type102

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 18 30
    cr_equal 0 received_callbacks?id=type-status1 12
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type102 testdata/ics/info-type-2.json REGISTERED
else
    cr_equal 0 received_callbacks 6
fi

ics_api_idc_get_type_ids 200 type101 type102 type1 type2 type4 type6

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    ics_api_idc_get_type 200 type101 testdata/ics/info-type-1.json ENABLED 1

    ics_api_idc_get_type 200 type102 testdata/ics/info-type-2.json ENABLED 1
else
    ics_api_idc_get_type 200 type101 testdata/ics/info-type-1.json

    ics_api_idc_get_type 200 type102 testdata/ics/info-type-2.json
fi

ics_api_edp_get_type_ids 200 type101 type102 type1 type2 type4 type6
ics_api_edp_get_type_2 200 type101 testdata/ics/info-type-1.json
ics_api_edp_get_type_2 200 type102 testdata/ics/info-type-2.json

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-ib prod-b prod-c prod-d prod-e

ics_api_edp_get_producer_2 200 prod-ia $CB_JOB/prod-ia $CB_SV/prod-ia type101
ics_api_edp_get_producer_2 200 prod-ib $CB_JOB/prod-ib $CB_SV/prod-ib type102

ics_api_edp_get_producer_status 200 prod-ib ENABLED

## Create job for prod-ib
##  job103 - prod-ib
ics_api_idc_put_job 201 job103 type102 $TARGET103 info-owner-3 $INFOSTATUS103 testdata/ics/job-template.json  VALIDATE

prodstub_equal create/prod-ib/job103 1

# Check the job data in the producer
prodstub_check_jobdata_3 200 prod-ib job103 type102 $TARGET103 info-owner-3 testdata/ics/job-template.json

ics_api_idc_get_job_ids 200 type101 NOWNER job101 job102
ics_api_idc_get_job_ids 200 type102 NOWNER job103
ics_api_idc_get_job_ids 200 type101 info-owner-1 job101
ics_api_idc_get_job_ids 200 type101 info-owner-2 job102
ics_api_idc_get_job_ids 200 type102 info-owner-3 job103

ics_api_idc_get_job 200 job103 type102 $TARGET103 info-owner-3 $INFOSTATUS103 testdata/ics/job-template.json

ics_api_idc_get_job_status2 200 job103 ENABLED 1 prod-ib

ics_api_edp_get_producer_jobs_2 200 prod-ia job101 type101 $TARGET101 info-owner-1 testdata/ics/job-template.json job102 type101 $TARGET102 info-owner-2 testdata/ics/job-template.json
ics_api_edp_get_producer_jobs_2 200 prod-ib job103 type102 $TARGET103 info-owner-3 testdata/ics/job-template.json

## Setup prod-ic (no types)
ics_api_edp_put_producer_2 201 prod-ic $CB_JOB/prod-ic $CB_SV/prod-ic NOTYPE

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-ib prod-ic prod-b prod-c prod-d prod-e

ics_api_edp_get_producer_2 200 prod-ia $CB_JOB/prod-ia $CB_SV/prod-ia type101
ics_api_edp_get_producer_2 200 prod-ib $CB_JOB/prod-ib $CB_SV/prod-ib type102
ics_api_edp_get_producer_2 200 prod-ic $CB_JOB/prod-ic $CB_SV/prod-ic EMPTY

ics_api_edp_get_producer_status 200 prod-ic ENABLED


## Delete job103 and prod-ib and re-create if different order

# Delete job then producer
ics_api_idc_get_job_ids 200 NOTYPE NOWNER job101 job102 job103 job1 job2 job3 job8 job10
ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-ib prod-ic prod-b prod-c prod-d prod-e

ics_api_idc_delete_job 204 job103

ics_api_idc_get_job_ids 200 NOTYPE NOWNER job101 job102 job1 job2 job3 job8 job10
ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-ib prod-ic prod-b prod-c prod-d prod-e

ics_api_edp_delete_producer 204 prod-ib

ics_api_edp_get_producer_status 404 prod-ib

ics_api_idc_get_job_ids 200 NOTYPE NOWNER job101 job102 job1 job2 job3 job8 job10
ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-ic prod-b prod-c prod-d prod-e

prodstub_equal delete/prod-ib/job103 1

ics_api_idc_put_job 201 job103 type102 $TARGET103 info-owner-3 $INFOSTATUS103 testdata/ics/job-template.json VALIDATE
ics_api_idc_get_job_status2 200 job103 DISABLED EMPTYPROD

# Put producer then job
ics_api_edp_put_producer_2 201 prod-ib $CB_JOB/prod-ib $CB_SV/prod-ib type102

ics_api_edp_get_producer_status 200 prod-ib ENABLED

ics_api_idc_put_job 200 job103 type102 $TARGET103 info-owner-3 $INFOSTATUS103 testdata/ics/job-template2.json  VALIDATE
ics_api_idc_get_job_status2 200 job103 ENABLED 1 prod-ib

prodstub_check_jobdata_3 200 prod-ib job103 type102 $TARGET103 info-owner-3 testdata/ics/job-template2.json

ics_api_idc_get_job_ids 200 NOTYPE NOWNER job101 job102 job103 job1 job2 job3 job8 job10
ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-ib prod-ic prod-b prod-c prod-d prod-e

prodstub_equal create/prod-ib/job103 3
prodstub_equal delete/prod-ib/job103 1

# Delete only the producer
ics_api_edp_delete_producer 204 prod-ib

ics_api_edp_get_producer_status 404 prod-ib

ics_api_idc_get_job_ids 200 NOTYPE NOWNER job101 job102 job103  job1 job2 job3 job8 job10
ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-ic prod-b prod-c prod-d prod-e

ics_api_idc_get_job_status2 200 job103 DISABLED EMPTYPROD

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 19 30

    cr_equal 0 received_callbacks?id=info-job103-status 1
    cr_api_check_all_ics_events 200 0 info-job103-status DISABLED
else
    cr_equal 0 received_callbacks 7 30
    cr_equal 0 received_callbacks?id=info-job103-status 1
    cr_api_check_all_ics_events 200 0 info-job103-status DISABLED
fi

# Re-create the producer
ics_api_edp_put_producer_2 201 prod-ib $CB_JOB/prod-ib $CB_SV/prod-ib type102

ics_api_edp_get_producer_status 200 prod-ib ENABLED

ics_api_idc_get_job_status2 200 job103 ENABLED 1 prod-ib

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 20 30
    cr_equal 0 received_callbacks?id=info-job103-status 2
    cr_api_check_all_ics_events 200 0 info-job103-status ENABLED
else
    cr_equal 0 received_callbacks 8 30
    cr_equal 0 received_callbacks?id=info-job103-status 2
    cr_api_check_all_ics_events 200 0 info-job103-status ENABLED
fi

prodstub_check_jobdata_3 200 prod-ib job103 type102 $TARGET103 info-owner-3 testdata/ics/job-template2.json

## Setup prod-id
ics_api_edp_put_type_2 201 type104 testdata/ics/info-type-4.json
ics_api_edp_put_producer_2 201 prod-id $CB_JOB/prod-id $CB_SV/prod-id type104

ics_api_idc_get_job_ids 200 type104 NOWNER EMPTY

ics_api_idc_put_job 201 job108 type104 $TARGET108 info-owner-4 $INFOSTATUS108 testdata/ics/job-template.json  VALIDATE

prodstub_check_jobdata_3 200 prod-id job108 type104 $TARGET108 info-owner-4 testdata/ics/job-template.json

prodstub_equal create/prod-id/job108 1
prodstub_equal delete/prod-id/job108 0

ics_api_idc_get_job_ids 200 type104 NOWNER job108

ics_api_idc_get_job_status2 200 job108 ENABLED 1 prod-id

# Re-PUT the producer with zero types
ics_api_edp_put_producer_2 200 prod-id $CB_JOB/prod-id $CB_SV/prod-id NOTYPE

ics_api_idc_get_job_ids 200 type104 NOWNER job108
ics_api_idc_get_job_ids 200 NOTYPE NOWNER job101 job102 job103 job108  job1 job2 job3 job8 job10

ics_api_idc_get_job_status2 200 job108 DISABLED EMPTYPROD

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 22 30
    cr_equal 0 received_callbacks?id=type-status1 13
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type104 testdata/ics/info-type-4.json REGISTERED

    cr_equal 0 received_callbacks?id=info-job108-status 1
    cr_api_check_all_ics_events 200 0 info-job108-status DISABLED
else
    cr_equal 0 received_callbacks 9 30
    cr_equal 0 received_callbacks?id=info-job108-status 1
    cr_api_check_all_ics_events 200 0 info-job108-status DISABLED
fi

prodstub_equal create/prod-id/job108 1
prodstub_equal delete/prod-id/job108 0

## Re-setup prod-id
ics_api_edp_put_type_2 200 type104 testdata/ics/info-type-4.json
ics_api_edp_put_producer_2 200 prod-id $CB_JOB/prod-id $CB_SV/prod-id type104


ics_api_idc_get_job_ids 200 type104 NOWNER job108
ics_api_idc_get_job_ids 200 NOTYPE NOWNER job101 job102 job103 job108 job1 job2 job3 job8 job10

ics_api_idc_get_job_status2 200 job108 ENABLED  1 prod-id

ics_api_edp_get_producer_status 200 prod-ia ENABLED
ics_api_edp_get_producer_status 200 prod-ib ENABLED
ics_api_edp_get_producer_status 200 prod-ic ENABLED
ics_api_edp_get_producer_status 200 prod-id ENABLED

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 24 30

    cr_equal 0 received_callbacks?id=type-status1 14
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type104 testdata/ics/info-type-4.json REGISTERED

    cr_equal 0 received_callbacks?id=info-job108-status 2
    cr_api_check_all_ics_events 200 0 info-job108-status ENABLED
else
    cr_equal 0 received_callbacks 10 30
    cr_equal 0 received_callbacks?id=info-job108-status 2
    cr_api_check_all_ics_events 200 0 info-job108-status ENABLED
fi

prodstub_equal create/prod-id/job108 2
prodstub_equal delete/prod-id/job108 0


## Setup prod-ie
ics_api_edp_put_type_2 201 type106 testdata/ics/info-type-6.json
ics_api_edp_put_producer_2 201 prod-ie $CB_JOB/prod-ie $CB_SV/prod-ie type106

ics_api_idc_get_job_ids 200 type106 NOWNER EMPTY

ics_api_idc_put_job 201 job110 type106 $TARGET110 info-owner-4 $INFOSTATUS110 testdata/ics/job-template.json  VALIDATE

prodstub_check_jobdata_3 200 prod-ie job110 type106 $TARGET110 info-owner-4 testdata/ics/job-template.json

prodstub_equal create/prod-ie/job110 1
prodstub_equal delete/prod-ie/job110 0

ics_api_idc_get_job_ids 200 type106 NOWNER job110

ics_api_idc_get_job_status2 200 job110 ENABLED 1 prod-ie

## Setup prod-if
ics_api_edp_put_type_2 200 type106 testdata/ics/info-type-6.json
ics_api_edp_put_producer_2 201 prod-if $CB_JOB/prod-if $CB_SV/prod-if type106

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 26 30

    cr_equal 0 received_callbacks?id=type-status1 16
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type106 testdata/ics/info-type-6.json REGISTERED type106 testdata/ics/info-type-6.json REGISTERED
fi


ics_api_idc_get_job_ids 200 type106 NOWNER job110

prodstub_check_jobdata_3 200 prod-if job110 type106 $TARGET110 info-owner-4 testdata/ics/job-template.json

prodstub_equal create/prod-if/job110 1
prodstub_equal delete/prod-if/job110 0

ics_api_idc_get_job_ids 200 type106 NOWNER job110

ics_api_idc_get_job_status2 200 job110 ENABLED  2 prod-ie prod-if

## Status updates prod-ia and jobs

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-ib prod-ic prod-id prod-ie prod-if  prod-b prod-c prod-d prod-e

ics_api_edp_get_producer_status 200 prod-ia ENABLED
ics_api_edp_get_producer_status 200 prod-ib ENABLED
ics_api_edp_get_producer_status 200 prod-ic ENABLED
ics_api_edp_get_producer_status 200 prod-id ENABLED
ics_api_edp_get_producer_status 200 prod-ie ENABLED
ics_api_edp_get_producer_status 200 prod-if ENABLED

# Arm producer prod-ia for supervision failure
prodstub_arm_producer 200 prod-ia 400

# Wait for producer prod-ia to go disabled
ics_api_edp_get_producer_status 200 prod-ia DISABLED 360

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-ib prod-ic prod-id  prod-ie prod-if prod-b prod-c prod-d prod-e

ics_api_edp_get_producer_status 200 prod-ia DISABLED
ics_api_edp_get_producer_status 200 prod-ib ENABLED
ics_api_edp_get_producer_status 200 prod-ic ENABLED
ics_api_edp_get_producer_status 200 prod-id ENABLED
ics_api_edp_get_producer_status 200 prod-ie ENABLED
ics_api_edp_get_producer_status 200 prod-if ENABLED


ics_api_idc_get_job_status2 200 job101 ENABLED 1 prod-ia
ics_api_idc_get_job_status2 200 job102 ENABLED 1 prod-ia
ics_api_idc_get_job_status2 200 job103 ENABLED 1 prod-ib
ics_api_idc_get_job_status2 200 job108 ENABLED 1 prod-id
ics_api_idc_get_job_status2 200 job110 ENABLED 2 prod-ie prod-if

# Arm producer prod-ia for supervision
prodstub_arm_producer 200 prod-ia 200

# Wait for producer prod-ia to go enabled
ics_api_edp_get_producer_status 200 prod-ia ENABLED 360

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-ib prod-ic prod-id prod-ie prod-if prod-b prod-c prod-d prod-e

ics_api_edp_get_producer_status 200 prod-ia ENABLED
ics_api_edp_get_producer_status 200 prod-ib ENABLED
ics_api_edp_get_producer_status 200 prod-ic ENABLED
ics_api_edp_get_producer_status 200 prod-id ENABLED
ics_api_edp_get_producer_status 200 prod-ie ENABLED
ics_api_edp_get_producer_status 200 prod-if ENABLED

ics_api_idc_get_job_status2 200 job101 ENABLED 1 prod-ia
ics_api_idc_get_job_status2 200 job102 ENABLED 1 prod-ia
ics_api_idc_get_job_status2 200 job103 ENABLED 1 prod-ib
ics_api_idc_get_job_status2 200 job108 ENABLED 1 prod-id
ics_api_idc_get_job_status2 200 job110 ENABLED 2 prod-ie prod-if

# Arm producer prod-ia for supervision failure
prodstub_arm_producer 200 prod-ia 400

# Wait for producer prod-ia to go disabled
ics_api_edp_get_producer_status 200 prod-ia DISABLED 360

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ia prod-ib prod-ic prod-id prod-ie prod-if prod-b prod-c prod-d prod-e

ics_api_edp_get_producer_status 200 prod-ia DISABLED
ics_api_edp_get_producer_status 200 prod-ib ENABLED
ics_api_edp_get_producer_status 200 prod-ic ENABLED
ics_api_edp_get_producer_status 200 prod-id ENABLED
ics_api_edp_get_producer_status 200 prod-ie ENABLED
ics_api_edp_get_producer_status 200 prod-if ENABLED

ics_api_idc_get_job_status2 200 job101 ENABLED 1 prod-ia
ics_api_idc_get_job_status2 200 job102 ENABLED 1 prod-ia
ics_api_idc_get_job_status2 200 job103 ENABLED 1 prod-ib
ics_api_idc_get_job_status2 200 job108 ENABLED 1 prod-id
ics_api_idc_get_job_status2 200 job110 ENABLED 2 prod-ie prod-if

# Wait for producer prod-ia to be removed
ics_equal json:data-producer/v1/info-producers 9 1000

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ib prod-ic prod-id prod-ie prod-if  prod-b prod-c prod-d prod-e


ics_api_edp_get_producer_status 404 prod-ia
ics_api_edp_get_producer_status 200 prod-ib ENABLED
ics_api_edp_get_producer_status 200 prod-ic ENABLED
ics_api_edp_get_producer_status 200 prod-id ENABLED
ics_api_edp_get_producer_status 200 prod-ie ENABLED
ics_api_edp_get_producer_status 200 prod-if ENABLED

ics_api_idc_get_job_status2 200 job101 DISABLED EMPTYPROD
ics_api_idc_get_job_status2 200 job102 DISABLED EMPTYPROD
ics_api_idc_get_job_status2 200 job103 ENABLED 1 prod-ib
ics_api_idc_get_job_status2 200 job108 ENABLED 1 prod-id
ics_api_idc_get_job_status2 200 job110 ENABLED 2 prod-ie prod-if


if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 28 30

    cr_equal 0 received_callbacks?id=info-job101-status 1
    cr_equal 0 received_callbacks?id=info-job102-status 1
    cr_api_check_all_ics_events 200 0 info-job101-status DISABLED
    cr_api_check_all_ics_events 200 0 info-job102-status DISABLED
else
    cr_equal 0 received_callbacks 12 30

    cr_equal 0 received_callbacks?id=info-job101-status 1
    cr_equal 0 received_callbacks?id=info-job102-status 1
    cr_api_check_all_ics_events 200 0 info-job101-status DISABLED
    cr_api_check_all_ics_events 200 0 info-job102-status DISABLED
fi


# Arm producer prod-ie for supervision failure
prodstub_arm_producer 200 prod-ie 400

ics_api_edp_get_producer_status 200 prod-ie DISABLED 1000

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ib prod-ic prod-id prod-ie prod-if prod-b prod-c prod-d prod-e

ics_api_edp_get_producer_status 404 prod-ia
ics_api_edp_get_producer_status 200 prod-ib ENABLED
ics_api_edp_get_producer_status 200 prod-ic ENABLED
ics_api_edp_get_producer_status 200 prod-id ENABLED
ics_api_edp_get_producer_status 200 prod-ie DISABLED
ics_api_edp_get_producer_status 200 prod-if ENABLED

ics_api_idc_get_job_status2 200 job101 DISABLED EMPTYPROD
ics_api_idc_get_job_status2 200 job102 DISABLED EMPTYPROD
ics_api_idc_get_job_status2 200 job103 ENABLED 1 prod-ib
ics_api_idc_get_job_status2 200 job108 ENABLED 1 prod-id
ics_api_idc_get_job_status2 200 job110 ENABLED 2 prod-ie prod-if

#Disable create for job110 in prod-ie
prodstub_arm_job_create 200 prod-ie job110 400

#Update tjob 10 - only prod-if will be updated
ics_api_idc_put_job 200 job110 type106 $TARGET110 info-owner-4 $INFOSTATUS110 testdata/ics/job-template2.json  VALIDATE
#Reset producer and job responses
prodstub_arm_producer 200 prod-ie 200
prodstub_arm_job_create 200 prod-ie job110 200

ics_api_edp_get_producer_status 200 prod-ie ENABLED 360

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ib prod-ic prod-id prod-ie prod-if  prod-b prod-c prod-d prod-e

#Wait for job to be updated
sleep_wait 120

prodstub_check_jobdata_3 200 prod-if job110 type106 $TARGET110 info-owner-4 testdata/ics/job-template2.json

prodstub_arm_producer 200 prod-if 400

ics_api_edp_get_producer_status 200 prod-if DISABLED 360

ics_equal json:data-producer/v1/info-producers 8 1000

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-ib prod-ic prod-id prod-ie prod-b prod-c prod-d prod-e

ics_api_edp_get_producer_status 404 prod-ia
ics_api_edp_get_producer_status 200 prod-ib ENABLED
ics_api_edp_get_producer_status 200 prod-ic ENABLED
ics_api_edp_get_producer_status 200 prod-id ENABLED
ics_api_edp_get_producer_status 200 prod-ie ENABLED
ics_api_edp_get_producer_status 404 prod-if

ics_api_idc_get_job_status2 200 job101 DISABLED EMPTYPROD
ics_api_idc_get_job_status2 200 job102 DISABLED EMPTYPROD
ics_api_idc_get_job_status2 200 job103 ENABLED 1 prod-ib
ics_api_idc_get_job_status2 200 job108 ENABLED 1 prod-id
ics_api_idc_get_job_status2 200 job110 ENABLED 1 prod-ie

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 28
else
    cr_equal 0 received_callbacks 12
fi
### Test of pre and post validation
if [[ "$ICS_FEATURE_LEVEL" != *"DEFAULT_TYPE_VALIDATION"* ]]; then
    ics_api_idc_get_type_ids 200 type1 type2 type4 type6 type101 type102 type104 type106
    ics_api_idc_put_job 404 job150 type150 $TARGET150 info-owner-1 $INFOSTATUS150 testdata/ics/job-template.json VALIDATE
    ics_api_idc_put_job 201 job160 type160 $TARGET160 info-owner-1 $INFOSTATUS160 testdata/ics/job-template.json


    ics_api_idc_get_job_status2 404 job150
    ics_api_idc_get_job_status2 200 job160 DISABLED EMPTYPROD 60

    prodstub_arm_producer 200 prod-ig
    prodstub_arm_job_create 200 prod-ig job150
    prodstub_arm_job_create 200 prod-ig job160

    ics_api_edp_put_producer_2 201 prod-ig $CB_JOB/prod-ig $CB_SV/prod-ig NOTYPE
    ics_api_edp_get_producer_status 200 prod-ig ENABLED 360

    ics_api_edp_get_producer_2 200 prod-ig $CB_JOB/prod-ig $CB_SV/prod-ig EMPTY

    ics_api_idc_get_job_status2 404 job150
    ics_api_idc_get_job_status2 200 job160 DISABLED EMPTYPROD 60
else
    ics_api_idc_get_type_ids 200 type1 type2 type4 type6 type101 type102 type104 type106
    ics_api_idc_put_job 404 job150 type150 $TARGET150 info-owner-1 $INFOSTATUS150 testdata/ics/job-template.json VALIDATE

    ics_api_idc_get_job_status2 404 job150

    prodstub_arm_producer 200 prod-ig
    prodstub_arm_job_create 200 prod-ig job150
    prodstub_arm_job_create 200 prod-ig job160

    ics_api_edp_put_producer_2 201 prod-ig $CB_JOB/prod-ig $CB_SV/prod-ig NOTYPE
    ics_api_edp_get_producer_status 200 prod-ig ENABLED 360

    ics_api_edp_get_producer_2 200 prod-ig $CB_JOB/prod-ig $CB_SV/prod-ig EMPTY

    ics_api_idc_get_job_status2 404 job150
    #ics_api_idc_get_job_status2 200 job160 DISABLED EMPTYPROD 60
fi

prodstub_arm_type 200 prod-ig type160

ics_api_edp_put_type_2 201 type160 testdata/ics/info-type-60.json
ics_api_idc_get_type_ids 200 type1 type2 type4 type6 type101 type102 type104 type106 type160

ics_api_edp_put_producer_2 200 prod-ig $CB_JOB/prod-ig $CB_SV/prod-ig type160
ics_api_edp_get_producer_status 200 prod-ig ENABLED 360
ics_api_edp_get_producer_2 200 prod-ig $CB_JOB/prod-ig $CB_SV/prod-ig type160

ics_api_idc_put_job 404 job150 type150 $TARGET150 info-owner-1 $INFOSTATUS150 testdata/ics/job-template.json VALIDATE

ics_api_idc_get_job_status2 404 job150
if [[ "$ICS_FEATURE_LEVEL" != *"DEFAULT_TYPE_VALIDATION"* ]]; then
    ics_api_idc_get_job_status2 200 job160 ENABLED 1 prod-ig 60

    prodstub_check_jobdata_3 200 prod-ig job160 type160 $TARGET160 info-owner-1 testdata/ics/job-template.json

    prodstub_equal create/prod-ig/job160 1
    prodstub_equal delete/prod-ig/job160 0
fi

prodstub_arm_type 200 prod-ig type150

ics_api_edp_put_type_2 201 type150 testdata/ics/info-type-50.json
ics_api_idc_get_type_ids 200 type1 type2 type4 type6 type101 type102 type104 type106 type160 type150

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 30 30
    cr_equal 0 received_callbacks?id=type-status1 18
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type160 testdata/ics/info-type-60.json REGISTERED type150 testdata/ics/info-type-50.json REGISTERED
else
    cr_equal 0 received_callbacks 12
fi

ics_api_edp_put_producer_2 200 prod-ig $CB_JOB/prod-ig $CB_SV/prod-ig type160 type150
ics_api_edp_get_producer_status 200 prod-ig ENABLED 360

ics_api_edp_get_producer_2 200 prod-ig $CB_JOB/prod-ig $CB_SV/prod-ig type160 type150

if [[ "$ICS_FEATURE_LEVEL" == *"DEFAULT_TYPE_VALIDATION"* ]]; then
    ics_api_idc_put_job 201 job160 type160 $TARGET160 info-owner-1 $INFOSTATUS160 testdata/ics/job-template.json
fi

ics_api_idc_get_job_status2 404 job150
ics_api_idc_get_job_status2 200 job160 ENABLED  1 prod-ig

ics_api_idc_put_job 201 job150 type150 $TARGET150 info-owner-1 $INFOSTATUS150 testdata/ics/job-template.json VALIDATE

ics_api_idc_get_job_status2 200 job150 ENABLED  1 prod-ig 60
ics_api_idc_get_job_status2 200 job160 ENABLED  1 prod-ig

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 30 30
    cr_equal 0 received_callbacks?id=type-status1 18
else
    cr_equal 0 received_callbacks 12
fi

# Test job deletion at type delete

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then

    if [[ "$ICS_FEATURE_LEVEL" == *"RESP_CODE_CHANGE_1"* ]]; then
        ics_api_edp_delete_type_2 409 type104
    else
        ics_api_edp_delete_type_2 406 type104
    fi

    ics_api_edp_delete_producer 204 prod-id

    ics_api_edp_delete_type_2 204 type104

    cr_equal 0 received_callbacks 32 30
    cr_equal 0 received_callbacks?id=info-job108-status 3
    cr_equal 0 received_callbacks?id=type-status1 19
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type104 testdata/ics/info-type-4.json DEREGISTERED
    cr_api_check_all_ics_events 200 0 info-job108-status DISABLED

    ics_api_edp_get_producer 404 prod-id

    ics_api_idc_get_job 404 job-108

else
    cr_equal 0 received_callbacks 12
fi

check_ics_logs

store_logs END

#### TEST COMPLETE ####

print_result

auto_clean_environment