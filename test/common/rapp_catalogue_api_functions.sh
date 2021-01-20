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

# This is a script that contains container/service managemnt functions test functions for RAPP Catalogue API

## Access to RAPP Catalogue
# Host name may be changed if app started by kube
# Direct access from script
RC_HTTPX="http"
RC_HOST_NAME=$LOCALHOST_NAME
RC_PATH=$RC_HTTPX"://"$RC_HOST_NAME":"$RAPP_CAT_EXTERNAL_PORT
# RC_ADAPTER used for switch between REST and DMAAP (only REST supported currently)
RC_ADAPTER_TYPE="REST"
RC_ADAPTER=$RC_PATH


###########################
### RAPP Catalogue
###########################

# Set http as the protocol to use for all communication to the RAPP Catalogue
# args: -
# (Function for test scripts)
use_rapp_catalogue_http() {
	echo -e $BOLD"RAPP Catalogue protocol setting"$EBOLD
	echo -e " Using $BOLD http $EBOLD towards the RAPP Catalogue"
	RC_HTTPX="http"
	RC_PATH=$RC_HTTPX"://"$RC_HOST_NAME":"$RAPP_CAT_EXTERNAL_PORT
	RC_ADAPTER_TYPE="REST"
	RC_ADAPTER=$RC_PATH
	echo ""
}

# Set https as the protocol to use for all communication to the RAPP Catalogue
# args: -
# (Function for test scripts)
use_rapp_catalogue_https() {
	echo -e $BOLD"RAPP Catalogue protocol setting"$EBOLD
	echo -e " Using $BOLD https $EBOLD towards the RAPP Catalogue"
	RC_HTTPX="https"
	RC_PATH=$RC_HTTPX"://"$RC_HOST_NAME":"$RAPP_CAT_EXTERNAL_SECURE_PORT
	RC_ADAPTER_TYPE="REST"
	RC_ADAPTER=$RC_PATH
	echo ""
}

# Start the RAPP Catalogue container
# args: -
# (Function for test scripts)
start_rapp_catalogue() {

	echo -e $BOLD"Starting $RAPP_CAT_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "RC"
		retcode_i=$?

		# Check if app shall only be used by the testscipt
		__check_prestarted_image "RC"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $RAPP_CAT_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $RAPP_CAT_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $RAPP_CAT_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $RAPP_CAT_APP_NAME will not be started"$ERED
			exit
		fi

		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $RAPP_CAT_APP_NAME deployment and service"
			echo " Setting $RAPP_CAT_APP_NAME replicas=1"
			__kube_scale deployment $RAPP_CAT_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then

			echo -e " Creating $RAPP_CAT_APP_NAME app and expose service"

			#Check if nonrtric namespace exists, if not create it
			__kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

			export RAPP_CAT_APP_NAME
			export KUBE_NONRTRIC_NAMESPACE
			export RAPP_CAT_IMAGE
			export RAPP_CAT_INTERNAL_PORT
			export RAPP_CAT_INTERNAL_SECURE_PORT
			export RAPP_CAT_EXTERNAL_PORT
			export RAPP_CAT_EXTERNAL_SECURE_PORT

			#Create service
			input_yaml=$SIM_GROUP"/"$RAPP_CAT_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/rac_svc.yaml
			__kube_create_instance service $RAPP_CAT_APP_NAME $input_yaml $output_yaml

			#Create app
			input_yaml=$SIM_GROUP"/"$RAPP_CAT_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/rac_app.yaml
			__kube_create_instance app $RAPP_CAT_APP_NAME $input_yaml $output_yaml
		fi

		echo " Retrieving host and ports for service..."
		RC_HOST_NAME=$(__kube_get_service_host $RAPP_CAT_APP_NAME $KUBE_NONRTRIC_NAMESPACE)

		RAPP_CAT_EXTERNAL_PORT=$(__kube_get_service_port $RAPP_CAT_APP_NAME $KUBE_NONRTRIC_NAMESPACE "http")
		RAPP_CAT_EXTERNAL_SECURE_PORT=$(__kube_get_service_port $RAPP_CAT_APP_NAME $KUBE_NONRTRIC_NAMESPACE "https")

		echo " Host IP, http port, https port: $RC_HOST_NAME $RAPP_CAT_EXTERNAL_PORT $RAPP_CAT_EXTERNAL_SECURE_PORT"
		if [ $RC_HTTPX == "http" ]; then
			RC_PATH=$RC_HTTPX"://"$RC_HOST_NAME":"$RAPP_CAT_EXTERNAL_PORT
		else
			RC_PATH=$RC_HTTPX"://"$RC_HOST_NAME":"$RAPP_CAT_EXTERNAL_SECURE_PORT
		fi

		__check_service_start $RAPP_CAT_APP_NAME $RC_PATH$RAPP_CAT_ALIVE_URL

		# Update the curl adapter if set to rest, no change if type dmaap
		if [ $RC_ADAPTER_TYPE == "REST" ]; then
			RC_ADAPTER=$RC_PATH
		fi
	else
		__check_included_image 'RC'
		if [ $? -eq 1 ]; then
			echo -e $RED"The RAPP Catalogue app is not included as managed in this test script"$ERED
			echo -e $RED"The RAPP Catalogue will not be started"$ERED
			exit
		fi

		export RAPP_CAT_APP_NAME
        export RAPP_CAT_INTERNAL_PORT
        export RAPP_CAT_EXTERNAL_PORT
        export RAPP_CAT_INTERNAL_SECURE_PORT
        export RAPP_CAT_EXTERNAL_SECURE_PORT
        export DOCKER_SIM_NWNAME

		__start_container $RAPP_CAT_COMPOSE_DIR NODOCKERARGS 1 $RAPP_CAT_APP_NAME

		__check_service_start $RAPP_CAT_APP_NAME $RC_PATH$RAPP_CAT_ALIVE_URL
	fi
	echo ""
}

# Tests if a variable value in the RAPP Catalogue is equal to a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
rc_equal() {
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		#__var_test RC "$LOCALHOST_HTTP:$RC_EXTERNAL_PORT/" $1 "=" $2 $3
		__var_test RC "$RC_PATH/" $1 "=" $2 $3
	else
		__print_err "Wrong args to ecs_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}


##########################################
#########  RAPP Catalogue API   ##########
##########################################
#Function prefix: rapp_cat_api

# API Test function: GET /services
# args: <response-code> [(<service-id> <version> <display-name> <description>)+ | EMPTY ]
# (Function for test scripts)
rapp_cat_api_get_services() {
	__log_test_start $@

	if [ $# -lt 1 ]; then
		__print_err "<response-code> [(<service-id> <version> <display-name> <description>)+ | EMPTY ]" $@
		return 1
	fi
	query="/services"
    res="$(__do_curl_to_api RC GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 1 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		arr=(${@:2})

		if [ $# -eq 2 ]; then
			targetJson="[]"
		else
			for ((i=0; i<$(($#-1)); i=i+4)); do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"{\"name\": \"${arr[$i]}\",\"version\": \"${arr[$i+1]}\",\"display_name\": \"${arr[$i+2]}\",\"description\": \"${arr[$i+3]}\",\"registrationDate\": \"????\"}"
			done
			targetJson=$targetJson"]"
		fi
		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: PUT ​/services/{service-id}
# args: <response-code> <service-id> <version> <display-name> <description>
# (Function for test scripts)
rapp_cat_api_put_service() {
	__log_test_start $@

	if [ $# -ne 5 ]; then
		__print_err "<response-code> <service-id> <version> <display-name> <description>" $@
		return 1
	fi

	inputJson="{\"version\": \"$3\",\"display_name\": \"$4\",\"description\": \"$5\"}"
	file="./tmp/.p.json"
	echo "$inputJson" > $file
	query="/services/$2"
	res="$(__do_curl_to_api RC PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: GET ​/services/{service-id}
# args: <response-code> <service-id>
# (Function for test scripts)
rapp_cat_api_get_service() {
	__log_test_start $@

	if [ $# -lt 2 ] || [ $# -gt 5 ]; then
		__print_err "<response-code> <service-id> <version> <display-name> <description>" $@
		return 1
	fi

	query="/services/$2"
    res="$(__do_curl_to_api RC GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		targetJson="{\"name\": \"$2\",\"version\": \"$3\",\"display_name\": \"$4\",\"description\": \"$5\",\"registrationDate\": \"????\"}"
		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: DELETE ​/services/{service-id}
# args: <response-code> <service-id>
# (Function for test scripts)
rapp_cat_api_delete_service() {
	__log_test_start $@

	if [ $# -ne 2 ]; then
		__print_err "<response-code> <service-id>" $@
		return 1
	fi

	query="/services/$2"
	res="$(__do_curl_to_api RC DELETE $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}
