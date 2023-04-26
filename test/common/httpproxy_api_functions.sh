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

# This is a script that contains container/service management functions for Http Proxy

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__HTTPPROXY_imagesetup() {
	__check_and_create_image_var HTTPPROXY "HTTP_PROXY_IMAGE" "HTTP_PROXY_IMAGE_BASE" "HTTP_PROXY_IMAGE_TAG" LOCAL "$HTTP_PROXY_DISPLAY_NAME" $IMAGE_TARGET_PLATFORM_IMG_TAG
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__HTTPPROXY_imagepull() {
	__check_and_pull_image $2 "$HTTP_PROXY_DISPLAY_NAME" $HTTP_PROXY_APP_NAME HTTP_PROXY_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__HTTPPROXY_imagebuild() {
	cd ../$HTTP_PROXY_BUILD_DIR       # Note: Reusing same impl as for kube proxy
	echo " Building HTTPPROXY - $HTTP_PROXY_DISPLAY_NAME - image: $HTTP_PROXY_IMAGE"
	docker build $IMAGE_TARGET_PLATFORM_CMD_PARAM --build-arg NEXUS_PROXY_REPO=$NEXUS_PROXY_REPO -t $HTTP_PROXY_IMAGE . &> .dockererr
	if [ $? -eq 0 ]; then
		echo -e  $GREEN"  Build Ok"$EGREEN
		__retag_and_push_image HTTP_PROXY_IMAGE
		if [ $? -ne 0 ]; then
			exit 1
		fi
	else
		echo -e $RED"  Build Failed"$ERED
		((RES_CONF_FAIL++))
		cat .dockererr
		echo -e $RED"Exiting...."$ERED
		exit 1
	fi
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__HTTPPROXY_image_data() {
	echo -e "$HTTP_PROXY_DISPLAY_NAME\t$(docker images --format $1 $HTTP_PROXY_IMAGE)" >>   $2
	if [ ! -z "$HTTP_PROXY_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $HTTP_PROXY_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__HTTPPROXY_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_SIM_NAMESPACE autotest HTTPPROXY
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__HTTPPROXY_kube_scale_zero_and_wait() {
	echo -e $RED" HTTPPROXY app is not scaled in this state"$ERED
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__HTTPPROXY_kube_delete_all() {
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest HTTPPROXY
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__HTTPPROXY_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=HTTPPROXY" -n $KUBE_SIM_NAMESPACE --tail=-1 > $1$2_httpproxy.log 2>&1
	else
		docker logs $HTTP_PROXY_APP_NAME > $1$2_httpproxy.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__HTTPPROXY_initial_setup() {
	use_http_proxy_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__HTTPPROXY_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "HTTPPROXY $HTTP_PROXY_APP_NAME $KUBE_SIM_NAMESPACE"
	else
		echo "HTTPPROXY $HTTP_PROXY_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__HTTPPROXY_test_requirements() {
	:
}

#######################################################

# Set http as the protocol to use for all communication to the http proxy
# args: -
# (Function for test scripts)
use_http_proxy_http() {
	__http_proxy_set_protocoll "http" $HTTP_PROXY_INTERNAL_PORT $HTTP_PROXY_EXTERNAL_PORT $HTTP_PROXY_WEB_INTERNAL_PORT $HTTP_PROXY_WEB_EXTERNAL_PORT
}

# Set https as the protocol to use for all communication to the http proxy
# args: -
# (Function for test scripts)
use_http_proxy_https() {
	__http_proxy_set_protocoll "https" $HTTP_PROXY_INTERNAL_SECURE_PORT $HTTP_PROXY_EXTERNAL_SECURE_PORT $HTTP_PROXY_WEB_INTERNAL_SECURE_PORT $HTTP_PROXY_WEB_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__http_proxy_set_protocoll() {
	echo -e $BOLD"$HTTP_PROXY_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $HTTP_PROXY_DISPLAY_NAME"

	## Access to http proxy
	## HTTP_PROXY_CONFIG_HOST_NAME and HTTP_PROXY_CONFIG_PORT used by apps as config for proxy host and port

	HTTP_PROXY_SERVICE_PATH=$1"://"$HTTP_PROXY_APP_NAME":"$2  # docker access, container->container and script->container via proxy
	HTTP_PROXY_WEB_PATH=$1"://"$HTTP_PROXY_APP_NAME":"$4
	HTTP_PROXY_CONFIG_HOST_NAME=$HTTP_PROXY_APP_NAME
	HTTP_PROXY_CONFIG_PORT=$2
	if [ $RUNMODE == "KUBE" ]; then
		HTTP_PROXY_CONFIG_HOST_NAME=$HTTP_PROXY_APP_NAME"."$KUBE_SIM_NAMESPACE
		HTTP_PROXY_CONFIG_PORT=$3
		HTTP_PROXY_SERVICE_PATH=$1"://"$HTTP_PROXY_APP_NAME.$KUBE_SIM_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
		HTTP_PROXY_WEB_PATH=$1"://"$HTTP_PROXY_APP_NAME.$KUBE_SIM_NAMESPACE":"$5
	fi

	echo ""
}

# Export env vars for config files, docker compose and kube resources
# args:
__http_proxy_export_vars() {

	export HTTP_PROXY_APP_NAME
	export HTTP_PROXY_DISPLAY_NAME

	export HTTP_PROXY_WEB_EXTERNAL_PORT
	export HTTP_PROXY_WEB_INTERNAL_PORT
	export HTTP_PROXY_EXTERNAL_PORT
	export HTTP_PROXY_INTERNAL_PORT

	export HTTP_PROXY_WEB_EXTERNAL_SECURE_PORT
	export HTTP_PROXY_WEB_INTERNAL_SECURE_PORT
	export HTTP_PROXY_EXTERNAL_SECURE_PORT
	export HTTP_PROXY_INTERNAL_SECURE_PORT

	export KUBE_SIM_NAMESPACE
	export DOCKER_SIM_NWNAME
	export HTTP_PROXY_IMAGE
}

# Start the Http Proxy in the simulator group
# args: -
# (Function for test scripts)
start_http_proxy() {

	echo -e $BOLD"Starting $HTTP_PROXY_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "HTTPPROXY"
		retcode_i=$?

		# Check if app shall only be used by the test script
		__check_prestarted_image "HTTPPROXY"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $HTTP_PROXY_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $HTTP_PROXY_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $HTTP_PROXY_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $HTTP_PROXY_APP_NAME will not be started"$ERED
			exit
		fi

		# Check if app shall be used - not managed - by the test script
		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $HTTP_PROXY_APP_NAME deployment and service"
			echo " Setting HTTPPROXY replicas=1"
			__kube_scale deployment $HTTP_PROXY_APP_NAME $KUBE_SIM_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then
			echo -e " Creating $HTTP_PROXY_APP_NAME deployment and service"

			__kube_create_namespace $KUBE_SIM_NAMESPACE

			__http_proxy_export_vars

			# Create service
			input_yaml=$SIM_GROUP"/"$HTTP_PROXY_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/proxy_svc.yaml
			__kube_create_instance service $HTTP_PROXY_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$HTTP_PROXY_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/proxy_app.yaml
			__kube_create_instance app $HTTP_PROXY_APP_NAME $input_yaml $output_yaml

		fi

		__check_service_start $HTTP_PROXY_APP_NAME $HTTP_PROXY_WEB_PATH$HTTP_PROXY_ALIVE_URL

	else
		# Check if docker app shall be fully managed by the test script
		__check_included_image 'HTTPPROXY'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Http Proxy app is not included in this test script"$ERED
			echo -e $RED"The Http Proxy will not be started"$ERED
			exit
		fi

		__http_proxy_export_vars

		__start_container $HTTP_PROXY_COMPOSE_DIR "" NODOCKERARGS 1 $HTTP_PROXY_APP_NAME

        __check_service_start $HTTP_PROXY_APP_NAME $HTTP_PROXY_WEB_PATH$HTTP_PROXY_ALIVE_URL
	fi
	echo ""
}

# Turn on debug logging in httpproxy
# args: -
# (Function for test scripts)
set_httpproxy_debug() {
	echo -e $BOLD"Setting httpproxy debug logging"$EBOLD
	curlString="$HTTP_PROXY_WEB_PATH/debug -X PUT"
	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		__print_err "could not set debug logging" $@
		((RES_CONF_FAIL++))
		return 1
	fi
	echo ""
	return 0
}
