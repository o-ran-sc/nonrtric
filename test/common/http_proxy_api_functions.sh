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

# This is a script that contains container/service managemnt functions for Http Proxy

## Access to Http Proxy Receiver
# Host name may be changed if app started by kube
# Direct access from script
HTTP_PROXY_HTTPX="http"
HTTP_PROXY_HOST_NAME=$LOCALHOST_NAME
HTTP_PROXY_PATH=$HTTP_PROXY_HTTPX"://"$HTTP_PROXY_HOST_NAME":"$HTTP_PROXY_WEB_EXTERNAL_PORT

#########################
### Http Proxy functions
#########################

# Start the Http Proxy in the simulator group
# args: -
# (Function for test scripts)
start_http_proxy() {

	echo -e $BOLD"Starting $HTTP_PROXY_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "HTTPPROXY"
		retcode_i=$?

		# Check if app shall only be used by the testscipt
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
			export HTTP_PROXY_APP_NAME
			export HTTP_PROXY_WEB_EXTERNAL_PORT
			export HTTP_PROXY_WEB_INTERNAL_PORT
			export HTTP_PROXY_EXTERNAL_PORT
			export HTTP_PROXY_INTERNAL_PORT
			export KUBE_SIM_NAMESPACE
			export HTTP_PROXY_IMAGE

			__kube_create_namespace $KUBE_SIM_NAMESPACE

			# Create service
			input_yaml=$SIM_GROUP"/"$HTTP_PROXY_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/proxy_svc.yaml
			__kube_create_instance service $HTTP_PROXY_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$HTTP_PROXY_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/proxy_app.yaml
			__kube_create_instance app $HTTP_PROXY_APP_NAME $input_yaml $output_yaml

		fi

		echo " Retrieving host and ports for service..."
		HTTP_PROXY_HOST_NAME=$(__kube_get_service_host $HTTP_PROXY_APP_NAME $KUBE_SIM_NAMESPACE)
		HTTP_PROXY_WEB_EXTERNAL_PORT=$(__kube_get_service_port $HTTP_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "web")
		HTTP_PROXY_EXTERNAL_PORT=$(__kube_get_service_port $HTTP_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "http")

		HTTP_PROXY_PATH=$HTTP_PROXY_HTTPX"://"$HTTP_PROXY_HOST_NAME":"$HTTP_PROXY_WEB_EXTERNAL_PORT
		HTTP_PROXY_CONFIG_PORT=$HTTP_PROXY_EXTERNAL_PORT
		HTTP_PROXY_CONFIG_HOST_NAME=$HTTP_PROXY_APP_NAME"."$KUBE_SIM_NAMESPACE

		echo " Host IP, http port: $HTTP_PROXY_HOST_NAME $HTTP_PROXY_WEB_EXTERNAL_PORT"

		__check_service_start $HTTP_PROXY_APP_NAME $HTTP_PROXY_PATH$HTTP_PROXY_ALIVE_URL

	else
		# Check if docker app shall be fully managed by the test script
		__check_included_image 'HTTPPROXY'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Http Proxy app is not included in this test script"$ERED
			echo -e $RED"The Http Proxy will not be started"$ERED
			exit
		fi

		export HTTP_PROXY_APP_NAME
		export HTTP_PROXY_WEB_EXTERNAL_PORT
		export HTTP_PROXY_WEB_INTERNAL_PORT
		export DOCKER_SIM_NWNAME

		__start_container $HTTP_PROXY_COMPOSE_DIR NODOCKERARGS 1 $HTTP_PROXY_APP_NAME

        __check_service_start $HTTP_PROXY_APP_NAME $HTTP_PROXY_PATH$HTTP_PROXY_ALIVE_URL

		HTTP_PROXY_CONFIG_PORT=$HTTP_PROXY_INTERNAL_PORT
		HTTP_PROXY_CONFIG_HOST_NAME=$HTTP_PROXY_APP_NAME

	fi
	echo ""
}

