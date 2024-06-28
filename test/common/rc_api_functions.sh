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

# This is a script that contains container/service management functions test functions for RAPP Catalogue API

################ Test engine functions ################

# Create the image var used during the test
# arg: [<image-tag-suffix>] (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__RC_imagesetup() {
	__check_and_create_image_var RC "RAPP_CAT_IMAGE" "RAPP_CAT_IMAGE_BASE" "RAPP_CAT_IMAGE_TAG" $1 "$RAPP_CAT_DISPLAY_NAME" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both arg var may contain: 'remote', 'remote-remove' or 'local'
__RC_imagepull() {
	__check_and_pull_image $1 "$RAPP_CAT_DISPLAY_NAME" $RAPP_CAT_APP_NAME RAPP_CAT_IMAGE
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__RC_image_data() {
	echo -e "$RAPP_CAT_DISPLAY_NAME\t$(docker images --format $1 $RAPP_CAT_IMAGE)" >>   $2
	if [ ! -z "$RAPP_CAT_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $RAPP_CAT_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__RC_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest RC
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__RC_kube_scale_zero_and_wait() {
	__kube_scale_and_wait_all_resources $KUBE_NONRTRIC_NAMESPACE app "$KUBE_NONRTRIC_NAMESPACE"-rappcatalogueservice
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__RC_kube_delete_all() {
	__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest RC
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__RC_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=RC" -n $KUBE_NONRTRIC_NAMESPACE --tail=-1 > $1$2_rc.log 2>&1
	else
		docker logs $RAPP_CAT_APP_NAME > $1$2_rc.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__RC_initial_setup() {
	use_rapp_catalogue_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__RC_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "RC $RAPP_CAT_APP_NAME $KUBE_NONRTRIC_NAMESPACE"
	else
		echo "RC $RAPP_CAT_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__RC_test_requirements() {
	:
}

#######################################################

# Set http as the protocol to use for all communication to the Rapp catalogue
# args: -
# (Function for test scripts)
use_rapp_catalogue_http() {
	__rapp_catalogue_set_protocoll "http" $RAPP_CAT_INTERNAL_PORT $RAPP_CAT_EXTERNAL_PORT
}

# Set https as the protocol to use for all communication to the Rapp catalogue
# args: -
# (Function for test scripts)
use_rapp_catalogue_https() {
	__rapp_catalogue_set_protocoll "https" $RAPP_CAT_INTERNAL_SECURE_PORT $RAPP_CAT_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__rapp_catalogue_set_protocoll() {
	echo -e $BOLD"$RAPP_CAT_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $RAPP_CAT_DISPLAY_NAME"

	## Access to Rapp catalogue

	RC_SERVICE_PATH=$1"://"$RAPP_CAT_APP_NAME":"$2  # docker access, container->container and script->container via proxy
	if [ $RUNMODE == "KUBE" ]; then
		RC_SERVICE_PATH=$1"://"$RAPP_CAT_APP_NAME.$KUBE_NONRTRIC_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
	fi

	# RC_ADAPTER used for switching between REST and DMAAP (only REST supported currently)
	RC_ADAPTER_TYPE="REST"
	RC_ADAPTER=$RC_SERVICE_PATH

	echo ""
}

# Export env vars for config files, docker compose and kube resources
# args:
__rapp_catalogue_export_vars() {

	export RAPP_CAT_APP_NAME
	export RAPP_CAT_DISPLAY_NAME

	export DOCKER_SIM_NWNAME
	export KUBE_NONRTRIC_NAMESPACE

	export RAPP_CAT_IMAGE
	export RAPP_CAT_INTERNAL_PORT
	export RAPP_CAT_INTERNAL_SECURE_PORT
	export RAPP_CAT_EXTERNAL_PORT
	export RAPP_CAT_EXTERNAL_SECURE_PORT
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

		# Check if app shall only be used by the test script
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

			__rapp_catalogue_export_vars

			#Create service
			input_yaml=$SIM_GROUP"/"$RAPP_CAT_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/rac_svc.yaml
			__kube_create_instance service $RAPP_CAT_APP_NAME $input_yaml $output_yaml

			#Create app
			input_yaml=$SIM_GROUP"/"$RAPP_CAT_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/rac_app.yaml
			__kube_create_instance app $RAPP_CAT_APP_NAME $input_yaml $output_yaml
		fi

		__check_service_start $RAPP_CAT_APP_NAME $RC_SERVICE_PATH$RAPP_CAT_ALIVE_URL

	else
		__check_included_image 'RC'
		if [ $? -eq 1 ]; then
			echo -e $RED"The RAPP Catalogue app is not included as managed in this test script"$ERED
			echo -e $RED"The RAPP Catalogue will not be started"$ERED
			exit
		fi

		__rapp_catalogue_export_vars

		__start_container $RAPP_CAT_COMPOSE_DIR "" NODOCKERARGS 1 $RAPP_CAT_APP_NAME

		__check_service_start $RAPP_CAT_APP_NAME $RC_SERVICE_PATH$RAPP_CAT_ALIVE_URL
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
		__var_test RC "$RC_SERVICE_PATH/" $1 "=" $2 $3
	else
		__print_err "Wrong args to ics_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
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
