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

# This is a script that contains container/service management function
# and test functions for Control Panel

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CP_imagesetup() {
	__check_and_create_image_var CP "CONTROL_PANEL_IMAGE" "CONTROL_PANEL_IMAGE_BASE" "CONTROL_PANEL_IMAGE_TAG" $1 "$CONTROL_PANEL_DISPLAY_NAME"
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__CP_imagepull() {
	__check_and_pull_image $1 "$CONTROL_PANEL_DISPLAY_NAME" $CONTROL_PANEL_APP_NAME CONTROL_PANEL_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CP_imagebuild() {
	echo -e $RED" Image for app CP shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__CP_image_data() {
	echo -e "$CONTROL_PANEL_DISPLAY_NAME\t$(docker images --format $1 $CONTROL_PANEL_IMAGE)" >>   $2
	if [ ! -z "$CONTROL_PANEL_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $CONTROL_PANEL_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__CP_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest CP
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for prestarted apps not managed by the test script.
__CP_kube_scale_zero_and_wait() {
	echo -e " CP replicas kept as is"
}

# Delete all kube resouces for the app
# This function is called for apps managed by the test script.
__CP_kube_delete_all() {
	__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest CP
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prexix>
__CP_store_docker_logs() {
	docker logs $CONTROL_PANEL_APP_NAME > $1$2_control-panel.log 2>&1
}

#######################################################


## Access to control panel
# Host name may be changed if app started by kube
# Direct access from script
CP_HTTPX="http"
CP_HOST_NAME=$LOCALHOST_NAME
CP_PATH=$CP_HTTPX"://"$CP_HOST_NAME":"$CONTROL_PANEL_EXTERNAL_PORT

###########################
### Control Panel functions
###########################

# Set http as the protocol to use for all communication to the Control Panel
# args: -
# (Function for test scripts)
use_control_panel_http() {
	echo -e $BOLD"Control Panel, CP, protocol setting"$EBOLD
	echo -e " Using $BOLD http $EBOLD towards CP"
	CP_HTTPX="http"
	CP_PATH=$CP_HTTPX"://"$CP_HOST_NAME":"$CONTROL_PANEL_EXTERNAL_PORT
	echo ""
}

# Set https as the protocol to use for all communication to the Control Panel
# args: -
# (Function for test scripts)
use_control_panel_https() {
	echo -e $BOLD"Control Panel, CP, protocol setting"$EBOLD
	echo -e " Using $BOLD https $EBOLD towards CP"
	CP_HTTPX="https"
	CP_PATH=$CP_HTTPX"://"$CP_HOST_NAME":"$CONTROL_PANEL_EXTERNAL_SECURE_PORT
	echo ""
}

# Start the Control Panel container
# args: -
# (Function for test scripts)
start_control_panel() {

	echo -e $BOLD"Starting $CONTROL_PANEL_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "CP"
		retcode_i=$?

		# Check if app shall only be used by the testscipt
		__check_prestarted_image "CP"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $CONTROL_PANEL_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $CONTROL_PANEL_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $CONTROL_PANEL_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $CONTROL_PANEL_APP_NAME will not be started"$ERED
			exit
		fi

		# Check if app shall be used - not managed - by the test script
		__check_prestarted_image "CP"
		if [ $? -eq 0 ]; then
			echo -e " Using existing $CONTROL_PANEL_APP_NAME deployment and service"
			echo " Setting CP replicas=1"
			__kube_scale deployment $CONTROL_PANEL_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then

			echo -e " Creating $CP_APP_NAME app and expose service"

			#Export all vars needed for service and deployment
			export CONTROL_PANEL_APP_NAME
			export KUBE_NONRTRIC_NAMESPACE
			export CONTROL_PANEL_IMAGE
			export CONTROL_PANEL_INTERNAL_PORT
			export CONTROL_PANEL_INTERNAL_SECURE_PORT
			export CONTROL_PANEL_EXTERNAL_PORT
			export CONTROL_PANEL_EXTERNAL_SECURE_PORT
			export CONTROL_PANEL_CONFIG_MOUNT_PATH
			export CONTROL_PANEL_CONFIG_FILE
			export CP_CONFIG_CONFIGMAP_NAME=$CONTROL_PANEL_APP_NAME"-config"
			export CP_PROXY_CONFIGMAP_NAME=$CONTROL_PANEL_APP_NAME"-proxy"

			export NGW_DOMAIN_NAME=$NRT_GATEWAY_APP_NAME.$KUBE_NONRTRIC_NAMESPACE.svc.cluster.local  # suffix needed for nginx name resolution
			export NRT_GATEWAY_EXTERNAL_PORT

			export CP_NGINX_RESOLVER=$CONTROL_PANEL_NGINX_KUBE_RESOLVER

			#Check if nonrtric namespace exists, if not create it
			__kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

			# Create config map for config
			datafile=$PWD/tmp/$CONTROL_PANEL_CONFIG_FILE
			#Add config to properties file

			#Trick to prevent these two vars to be replace with space in the config file by cmd envsubst
			export upstream='$upstream'
			export uri='$uri'

			envsubst < $1 > $datafile

			output_yaml=$PWD/tmp/cp_cfc.yaml
			__kube_create_configmap $CP_CONFIG_CONFIGMAP_NAME $KUBE_NONRTRIC_NAMESPACE autotest CP $datafile $output_yaml

			# Create service
			input_yaml=$SIM_GROUP"/"$CONTROL_PANEL_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/cp_svc.yaml
			__kube_create_instance service $CONTROL_PANEL_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$CONTROL_PANEL_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/cp_app.yaml
			__kube_create_instance app $CONTROL_PANEL_APP_NAME $input_yaml $output_yaml

		fi

		echo " Retrieving host and ports for service..."
		CP_HOST_NAME=$(__kube_get_service_host $CONTROL_PANEL_APP_NAME $KUBE_NONRTRIC_NAMESPACE)

		CONTROL_PANEL_EXTERNAL_PORT=$(__kube_get_service_port $CONTROL_PANEL_APP_NAME $KUBE_NONRTRIC_NAMESPACE "http")
		CONTROL_PANEL_EXTERNAL_SECURE_PORT=$(__kube_get_service_port $CONTROL_PANEL_APP_NAME $KUBE_NONRTRIC_NAMESPACE "https")

		echo " Host IP, http port, https port: $CP_HOST_NAME $CONTROL_PANEL_EXTERNAL_PORT $CONTROL_PANEL_EXTERNAL_SECURE_PORT"
		if [ $CP_HTTPX == "http" ]; then
			CP_PATH=$CP_HTTPX"://"$CP_HOST_NAME":"$CONTROL_PANEL_EXTERNAL_PORT
		else
			CP_PATH=$CP_HTTPX"://"$CP_HOST_NAME":"$CONTROL_PANEL_EXTERNAL_SECURE_PORT
		fi

		__check_service_start $CONTROL_PANEL_APP_NAME $CP_PATH$CONTROL_PANEL_ALIVE_URL
	else
		# Check if docker app shall be fully managed by the test script
		__check_included_image 'CP'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Control Panel app is not included in this test script"$ERED
			echo -e $RED"The Control Panel will not be started"$ERED
			exit
		fi

		# Export needed vars for docker compose
        export CONTROL_PANEL_APP_NAME
        export CONTROL_PANEL_INTERNAL_PORT
        export CONTROL_PANEL_EXTERNAL_PORT
        export CONTROL_PANEL_INTERNAL_SECURE_PORT
        export CONTROL_PANEL_EXTERNAL_SECURE_PORT
        export DOCKER_SIM_NWNAME

    	export CONTROL_PANEL_HOST_MNT_DIR
		export CONTROL_PANEL_CONFIG_FILE
		export CONTROL_PANEL_CONFIG_MOUNT_PATH

		export NRT_GATEWAY_APP_NAME
		export NRT_GATEWAY_EXTERNAL_PORT

		export POLICY_AGENT_EXTERNAL_SECURE_PORT
		export ECS_EXTERNAL_SECURE_PORT
		export POLICY_AGENT_DOMAIN_NAME=$POLICY_AGENT_APP_NAME
		export ECS_DOMAIN_NAME=$ECS_APP_NAME

		export CONTROL_PANEL_HOST_MNT_DIR
		export CONTROL_PANEL_CONFIG_MOUNT_PATH
		export CONTROL_PANEL_CONFIG_FILE
		export CONTROL_PANEL_DISPLAY_NAME
		export NGW_DOMAIN_NAME=$NRT_GATEWAY_APP_NAME

		export CP_NGINX_RESOLVER=$CONTROL_PANEL_NGINX_DOCKER_RESOLVER

		dest_file=$SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_HOST_MNT_DIR/$CONTROL_PANEL_CONFIG_FILE

		envsubst '${NGW_DOMAIN_NAME},${CP_NGINX_RESOLVER},${NRT_GATEWAY_EXTERNAL_PORT},${POLICY_AGENT_EXTERNAL_SECURE_PORT},${ECS_EXTERNAL_SECURE_PORT},${POLICY_AGENT_DOMAIN_NAME},${ECS_DOMAIN_NAME}' < $1 > $dest_file

		__start_container $CONTROL_PANEL_COMPOSE_DIR "" NODOCKERARGS 1 $CONTROL_PANEL_APP_NAME

		__check_service_start $CONTROL_PANEL_APP_NAME $CP_PATH$CONTROL_PANEL_ALIVE_URL
	fi
	echo ""
}
