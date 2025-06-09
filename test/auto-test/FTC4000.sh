#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021-2023 Nordix Foundation. All rights reserved.
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

TC_ONELINE_DESCR="Test of Helm Manager"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="KUBEPROXY CHARTMUS LOCALHELM HELMMANAGER"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="KUBEPROXY CHARTMUS LOCALHELM HELMMANAGER"
#Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES=""

#Supported test environment profiles
SUPPORTED_PROFILES="ORAN-G-RELEASE ORAN-H-RELEASE ORAN-I-RELEASE ORAN-J-RELEASE ORAN-K-RELEASE ORAN-L-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

clean_environment

start_kube_proxy

start_chart_museum

localhelm_create_test_chart dummy

localhelm_package_test_chart dummy

chartmus_upload_test_chart dummy

clean_and_create_namespace test-ns

localhelm_installed_chart_release NOTINSTALLED test-release test-ns

start_helm_manager

helm_manager_api_get_charts 200 EMPTY

helm_manager_api_exec_add_repo cm $CHART_MUS_SERVICE_PATH

helm_manager_api_post_repo 201 cm $CHART_MUS_SERVICE_HTTPX $CHART_MUS_SERVICE_HOST $CHART_MUS_SERVICE_PORT

helm_manager_api_post_onboard_chart 200 cm dummy DEFAULT-VERSION test-release test-ns

helm_manager_api_get_charts 200 cm dummy DEFAULT-VERSION test-release test-ns

helm_manager_api_post_install_chart 201 dummy DEFAULT-VERSION

localhelm_installed_chart_release INSTALLED test-release test-ns

helm_manager_api_get_charts 200 cm dummy DEFAULT-VERSION test-release test-ns

helm_manager_api_uninstall_chart 204 dummy DEFAULT-VERSION

helm_manager_api_get_charts 200 cm dummy DEFAULT-VERSION test-release test-ns

helm_manager_api_delete_chart 204 dummy DEFAULT-VERSION

helm_manager_api_get_charts 200 EMPTY

localhelm_installed_chart_release NOTINSTALLED test-release test-ns

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_environment



