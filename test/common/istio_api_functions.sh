#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021 Nordix Foundation. All rights reserved.
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

# This is a script that contain functions to handle istio configuration


################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__ISTIO_imagesetup() {
	:
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__ISTIO_imagepull() {
	:
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__ISTIO_imagebuild() {
	:
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__ISTIO_image_data() {
	:
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__ISTIO_kube_scale_zero() {
	:
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__ISTIO_kube_scale_zero_and_wait() {
	:
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__ISTIO_kube_delete_all() {
	__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest ISTIO
	__kube_delete_all_resources $KUBE_A1SIM_NAMESPACE autotest ISTIO
	__kube_delete_all_resources $KUBE_ONAP_NAMESPACE autotest ISTIO
	__kube_delete_all_resources $KUBE_KEYCLOAK_NAMESPACE autotest ISTIO
	__kube_delete_all_resources $KUBE_SDNC_NAMESPACE autotest ISTIO
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest ISTIO
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__ISTIO_store_docker_logs() {
	:
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__ISTIO_initial_setup() {
	# See jwt-info.txt in simulator-group/kubeproxy for detailed info
	KUBE_PROXY_CURL_JWT=$ISTIO_GENERIC_JWT
	KUBE_PROXY_ISTIO_JWKS_KEYS=$ISTIO_GENERIC_JWKS_KEY
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__ISTIO_statistics_setup() {
	:
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__ISTIO_test_requirements() {

	kubectl $KUBECONF get requestauthentications -A &> /dev/null
	if [ $? -ne 0 ]; then
		echo $RED" Istio api: kubectl get requestauthentications is not installed"
		exit 1
	fi
	kubectl $KUBECONF get authorizationpolicies -A &> /dev/null
	if [ $? -ne 0 ]; then
		echo $RED" Istio api: kubectl get authorizationpolicies is not installed"
		exit 1
	fi
}

#######################################################


# Enable istio on namespace
# arg: <namespace>
istio_enable_istio_namespace() {
	__log_conf_start $@
    if [ $# -ne 1 ]; then
        __print_err "<namespace>" $@
        return 1
    fi
	__kube_create_namespace $1
	__kube_label_non_ns_instance ns $1 "istio-injection=enabled"
	__log_conf_ok
	return 0
}

# Request authorization by jwksuri
# args: <app> <namespace> <realm>
istio_req_auth_by_jwksuri() {
	__log_conf_start $@
    if [ $# -ne 3 ]; then
        __print_err "<app> <namespace> <realm>" $@
        return 1
    fi
	name="ra-jwksuri-"$3"-"$1"-"$2
	export  ISTIO_TEMPLATE_REPLACE_RA_NAME=$(echo $name | tr '[:upper:]' '[:lower:]')
	export  ISTIO_TEMPLATE_REPLACE_RA_NS=$2
	export  ISTIO_TEMPLATE_REPLACE_RA_APP_NAME=$1
	export  ISTIO_TEMPLATE_REPLACE_RA_ISSUER=$KEYCLOAK_ISSUER_PATH$KEYCLOAK_TOKEN_URL_PREFIX/$3
	export  ISTIO_TEMPLATE_REPLACE_RA_JWKSURI=$KEYCLOAK_SERVICE_PATH$KEYCLOAK_TOKEN_URL_PREFIX/$3/protocol/openid-connect/certs
	inputfile=$SIM_GROUP/$ISTIO_COMPOSE_DIR/ra-jwksuri-template.yaml
	outputfile=tmp/$ISTIO_TEMPLATE_REPLACE_RA_NAME".yaml"
	envsubst < $inputfile > $outputfile
	if [ $? -ne 0 ]; then
		__log_conf_fail_general "Cannot substitute yaml: $inputfile"
		return 1
	fi
	kubectl $KUBECONF apply -f $outputfile &> tmp/kubeerr
	if [ $? -ne 0 ]; then
		__log_conf_fail_general "Cannot apply yaml: $outputfile"
		return 1
	fi
	__log_conf_ok
	return 0
}

# Request authorization by jwks (inline keys)
# args: <app> <namespace> <issuer> <key>
istio_req_auth_by_jwks() {
	__log_conf_start $@
    if [ $# -ne 4 ]; then
        __print_err "<app> <namespace> <issuer> <key>" $@
        return 1
    fi
	name="ra-jwks-"$3"-"$1"-"$2
	export  ISTIO_TEMPLATE_REPLACE_RA_NAME=$(echo $name | tr '[:upper:]' '[:lower:]')
	export  ISTIO_TEMPLATE_REPLACE_RA_NS=$2
	export  ISTIO_TEMPLATE_REPLACE_RA_APP_NAME=$1
	export  ISTIO_TEMPLATE_REPLACE_RA_ISSUER=$3
	export  ISTIO_TEMPLATE_REPLACE_RA_JWKS=$4
	inputfile=$SIM_GROUP/$ISTIO_COMPOSE_DIR/ra-jwks-template.yaml
	outputfile=tmp/$ISTIO_TEMPLATE_REPLACE_RA_NAME".yaml"
	envsubst < $inputfile > $outputfile
	if [ $? -ne 0 ]; then
		__log_conf_fail_general "Cannot substitute yaml: $inputfile"
		return 1
	fi
	kubectl $KUBECONF apply -f $outputfile &> tmp/kubeerr
	if [ $? -ne 0 ]; then
		__log_conf_fail_general "Cannot apply yaml: $outputfile"
		return 1
	fi
	__log_conf_ok
	return 0
}

# Authorization policy - by realm
# args: <app> <namespace> <realm> [<client-id> <client-role>]
istio_auth_policy_by_realm() {
	__log_conf_start $@
    if [ $# -ne 3 ] && [ $# -ne 5 ]; then
        __print_err "<app> <namespace> <realm> [<client-id> <client-role>]" $@
        return 1
    fi
	name="ap-realm-"$3"-"$1"-"$2
	export  ISTIO_TEMPLATE_REPLACE_AP_NAME=$(echo $name | tr '[:upper:]' '[:lower:]')
	export  ISTIO_TEMPLATE_REPLACE_AP_NS=$2
	export  ISTIO_TEMPLATE_REPLACE_AP_APP_NAME=$1
	export  ISTIO_TEMPLATE_REPLACE_AP_PRINCIPAL="$KEYCLOAK_ISSUER_PATH$KEYCLOAK_TOKEN_URL_PREFIX/$3/*"
	inputfile=$SIM_GROUP/$ISTIO_COMPOSE_DIR/ap-principal-template.yaml
	outputfile=tmp/$ISTIO_TEMPLATE_REPLACE_AP_NAME".yaml"
	envsubst < $inputfile > $outputfile
	if [ $? -ne 0 ]; then
		__log_conf_fail_general "Cannot substitute yaml: $inputfile"
		return 1
	fi
	if [ $# -gt 3 ]; then
		export  ISTIO_TEMPLATE_REPLACE_AP_CLIENT=$4
		export  ISTIO_TEMPLATE_REPLACE_AP_ROLE=$5
		inputfile=$SIM_GROUP/$ISTIO_COMPOSE_DIR/ap-role-snippet.yaml
		envsubst < $inputfile >> $outputfile
		if [ $? -ne 0 ]; then
			__log_conf_fail_general "Cannot substitute yaml: $inputfile"
			return 1
		fi
	fi

	kubectl $KUBECONF apply -f $outputfile &> tmp/kubeerr
	if [ $? -ne 0 ]; then
		__log_conf_fail_general "Cannot apply yaml: $outputfile"
		return 1
	fi
	__log_conf_ok
	return 0
}

# Authorization policy - by issuer
# args: <app> <namespace> <issuer>
istio_auth_policy_by_issuer() {
	__log_conf_start $@
    if [ $# -ne 3 ]; then
        __print_err "<app> <namespace> <issuer>" $@
        return 1
    fi
	name="ap-iss-"$3"-"$1"-"$2
	export  ISTIO_TEMPLATE_REPLACE_AP_NAME=$(echo $name | tr '[:upper:]' '[:lower:]')
	export  ISTIO_TEMPLATE_REPLACE_AP_NS=$2
	export  ISTIO_TEMPLATE_REPLACE_AP_APP_NAME=$1
	export  ISTIO_TEMPLATE_REPLACE_AP_PRINCIPAL="$3/*"
	inputfile=$SIM_GROUP/$ISTIO_COMPOSE_DIR/ap-principal-template.yaml
	outputfile=tmp/$ISTIO_TEMPLATE_REPLACE_AP_NAME".yaml"
	envsubst < $inputfile > $outputfile
	if [ $? -ne 0 ]; then
		__log_conf_fail_general "Cannot substitute yaml: $inputfile"
		return 1
	fi
	kubectl $KUBECONF apply -f $outputfile &> tmp/kubeerr
	if [ $? -ne 0 ]; then
		__log_conf_fail_general "Cannot apply yaml: $outputfile"
		return 1
	fi
	__log_conf_ok
	return 0
}


