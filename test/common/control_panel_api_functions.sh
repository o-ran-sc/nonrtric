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

			export POLICY_AGENT_EXTERNAL_SECURE_PORT
			export ECS_EXTERNAL_SECURE_PORT
			export POLICY_AGENT_DOMAIN_NAME=$POLICY_AGENT_APP_NAME.$KUBE_NONRTRIC_NAMESPACE
			export ECS_DOMAIN_NAME=$ECS_APP_NAME.$KUBE_NONRTRIC_NAMESPACE

			#Check if nonrtric namespace exists, if not create it
			__kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

			# Create config map for config
			datafile=$PWD/tmp/$CONTROL_PANEL_CONFIG_FILE
			#Add config to properties file
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

		__start_container $CONTROL_PANEL_COMPOSE_DIR NODOCKERARGS 1 $CONTROL_PANEL_APP_NAME

		__check_service_start $CONTROL_PANEL_APP_NAME $CP_PATH$CONTROL_PANEL_ALIVE_URL
	fi
	echo ""
}

