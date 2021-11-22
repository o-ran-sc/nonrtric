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

# This is a script that contains container/service management functions and test functions for Consul/CBS

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for that exist with staging, snapshot,release tags
__CONSUL_imagesetup() {
	__check_and_create_image_var CONSUL "CONSUL_IMAGE" "CONSUL_IMAGE_BASE" "CONSUL_IMAGE_TAG" REMOTE_PROXY "$CONSUL_DISPLAY_NAME"

}

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CBS_imagesetup() {
	__check_and_create_image_var  CBS "CBS_IMAGE" "CBS_IMAGE_BASE" "CBS_IMAGE_TAG" REMOTE_RELEASE_ONAP "$CBS_DISPLAY_NAME"

}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__CONSUL_imagepull() {
	__check_and_pull_image $2 "$CONSUL_DISPLAY_NAME" $CONSUL_APP_NAME CONSUL_IMAGE
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__CBS_imagepull() {
	__check_and_pull_image $2 "$CBS_DISPLAY_NAME" $CBS_APP_NAME CBS_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CONSUL_imagebuild() {
	echo -e $RED" Image for app CONSUL shall never be built"$ERED
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CBS_imagebuild() {
	echo -e $RED" Image for app CBS shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__CONSUL_image_data() {
	echo -e "$CONSUL_DISPLAY_NAME\t$(docker images --format $1 $CONSUL_IMAGE)" >>   $2
	if [ ! -z "$CONSUL_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $CONSUL_IMAGE_SOURCE)" >>   $2
	fi
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__CBS_image_data() {
	echo -e "$CBS_DISPLAY_NAME\t$(docker images --format $1 $CBS_IMAGE)" >>   $2
	if [ ! -z "$CBS_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $CBS_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__CONSUL_kube_scale_zero() {
	echo -e $RED" Image for app CONSUL is not used in kube"$ERED
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__CBS_kube_scale_zero() {
	echo -e $RED" Image for app CBS is not used in kube"$ERED
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for prestarted apps not managed by the test script.
__CONSUL_kube_scale_zero_and_wait() {
	echo -e $RED" CONSUL app is not used in kube"$ERED
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for prestarted apps not managed by the test script.
__CBS_kube_scale_zero_and_wait() {
	echo -e $RED" CBS app is not used in kube"$ERED
}

# Delete all kube resouces for the app
# This function is called for apps managed by the test script.
__CONSUL_kube_delete_all() {
	echo -e $RED" CONSUL app is not used in kube"$ERED
}

# Delete all kube resouces for the app
# This function is called for apps managed by the test script.
__CBS_kube_delete_all() {
	echo -e $RED" CBS app is not used in kube"$ERED
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prexix>
__CONSUL_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		:
	else
		docker logs $CONSUL_APP_NAME > $1/$2_consul.log 2>&1
	fi
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prexix>
__CBS_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		:
	else
		docker logs $CBS_APP_NAME > $1$2_cbs.log 2>&1
		body="$(__do_curl $LOCALHOST_HTTP:$CBS_EXTERNAL_PORT/service_component_all/$POLICY_AGENT_APP_NAME)"
		echo "$body" > $1$2_consul_config.json 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__CONSUL_initial_setup() {
	CONSUL_SERVICE_PATH="http://"$CONSUL_APP_NAME":"$CONSUL_INTERNAL_PORT
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__CBS_initial_setup() {
	CBS_SERVICE_PATH="http://"$CBS_APP_NAME":"$CBS_INTERNAL_PORT
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernets pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for prestarted apps.
# args: -
__CONSUL_statisics_setup() {
	echo ""
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernets pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for prestarted apps.
# args: -
__CBS_statisics_setup() {
	echo ""
}
#######################################################


####################
### Consul functions
####################

# Function to load config from a file into consul for the Policy Agent
# arg: <json-config-file>
# (Function for test scripts)
consul_config_app() {

	echo -e $BOLD"Configuring Consul"$EBOLD

	if [ $# -ne 1 ]; then
		((RES_CONF_FAIL++))
    	__print_err "need one arg,  <json-config-file>" $@
		exit 1
	fi

	echo " Loading config for "$POLICY_AGENT_APP_NAME" from "$1

	curlString="$CONSUL_SERVICE_PATH/v1/kv/${POLICY_AGENT_CONFIG_KEY}?dc=dc1 -X PUT -H Accept:application/json -H Content-Type:application/json -H X-Requested-With:XMLHttpRequest --data-binary @"$1

	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		echo -e $RED" FAIL - json config could not be loaded to consul" $ERED
		((RES_CONF_FAIL++))
		return 1
	fi
	body="$(__do_curl $CBS_SERVICE_PATH/service_component_all/$POLICY_AGENT_CONFIG_KEY)"
	echo $body > "./tmp/.output"$1

	if [ $? -ne 0 ]; then
		echo -e $RED" FAIL - json config could not be loaded from consul/cbs, contents cannot be checked." $ERED
		((RES_CONF_FAIL++))
		return 1
	else
		targetJson=$(< $1)
		targetJson="{\"config\":"$targetJson"}"
		echo "TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")
		if [ $res -ne 0 ]; then
			echo -e $RED" FAIL - policy json config read from consul/cbs is not equal to the intended json config...." $ERED
			((RES_CONF_FAIL++))
			return 1
		else
			echo -e $GREEN" Config loaded ok to consul"$EGREEN
		fi
	fi

	echo ""

}

# Start Consul and CBS
# args: -
# (Function for test scripts)
start_consul_cbs() {

	echo -e $BOLD"Starting $CONSUL_DISPLAY_NAME and $CBS_DISPLAY_NAME"$EBOLD
	__check_included_image 'CONSUL'
	if [ $? -eq 1 ]; then
		echo -e $RED"The Consul image has not been checked for this test run due to arg to the test script"$ERED
		echo -e $RED"Consul will not be started"$ERED
		exit
	fi
	export CONSUL_APP_NAME
	export CONSUL_INTERNAL_PORT
	export CONSUL_EXTERNAL_PORT
	export CBS_APP_NAME
	export CBS_INTERNAL_PORT
	export CBS_EXTERNAL_PORT
	export CONSUL_HOST
	export CONSUL_DISPLAY_NAME
	export CBS_DISPLAY_NAME

	__start_container $CONSUL_CBS_COMPOSE_DIR "" NODOCKERARGS 2 $CONSUL_APP_NAME $CBS_APP_NAME

	__check_service_start $CONSUL_APP_NAME $CONSUL_SERVICE_PATH$CONSUL_ALIVE_URL
	__check_service_start $CBS_APP_NAME $CBS_SERVICE_PATH$CBS_ALIVE_URL

	echo ""
}

