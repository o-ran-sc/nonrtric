#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2020-2023 Nordix Foundation. All rights reserved.
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

# This is a script that contains container/service management functions and test functions for Producer stub


################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__PRODSTUB_imagesetup() {
	__check_and_create_image_var PRODSTUB "PROD_STUB_IMAGE" "PROD_STUB_IMAGE_BASE" "PROD_STUB_IMAGE_TAG" LOCAL "$PROD_STUB_DISPLAY_NAME" $IMAGE_TARGET_PLATFORM_IMG_TAG
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__PRODSTUB_imagepull() {
	echo -e $RED"Image for app PRODSTUB shall never be pulled from remote repo"$ERED
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__PRODSTUB_imagebuild() {
	cd ../prodstub
	echo " Building PRODSTUB - $PROD_STUB_DISPLAY_NAME - image: $PROD_STUB_IMAGE"
	docker build  $IMAGE_TARGET_PLATFORM_CMD_PARAM --build-arg NEXUS_PROXY_REPO=$NEXUS_PROXY_REPO -t $PROD_STUB_IMAGE . &> .dockererr
	if [ $? -eq 0 ]; then
		echo -e  $GREEN"  Build Ok"$EGREEN
		__retag_and_push_image PROD_STUB_IMAGE
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
__PRODSTUB_image_data() {
	echo -e "$PROD_STUB_DISPLAY_NAME\t$(docker images --format $1 $PROD_STUB_IMAGE)" >>   $2
	if [ ! -z "$PROD_STUB_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $PROD_STUB_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__PRODSTUB_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_SIM_NAMESPACE autotest PRODSTUB
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__PRODSTUB_kube_scale_zero_and_wait() {
	echo -e $RED" PRODSTUB app is not scaled in this state"$ERED
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__PRODSTUB_kube_delete_all() {
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest PRODSTUB
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__PRODSTUB_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=PRODSTUB" -n $KUBE_SIM_NAMESPACE --tail=-1 > $1$2_prodstub.log 2>&1
	else
		docker logs $PROD_STUB_APP_NAME > $1$2_prodstub.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__PRODSTUB_initial_setup() {
	use_prod_stub_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__PRODSTUB_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "PRODSTUB $PROD_STUB_APP_NAME $KUBE_SIM_NAMESPACE"
	else
		echo "PRODSTUB $PROD_STUB_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__PRODSTUB_test_requirements() {
	:
}

#######################################################

# Set http as the protocol to use for all communication to the Prod stub sim
# args: -
# (Function for test scripts)
use_prod_stub_http() {
	__prod_stub_set_protocoll "http" $PROD_STUB_INTERNAL_PORT $PROD_STUB_EXTERNAL_PORT
}

# Set https as the protocol to use for all communication to the Prod stub sim
# args: -
# (Function for test scripts)
use_prod_stub_https() {
	__prod_stub_set_protocoll "https" $PROD_STUB_INTERNAL_SECURE_PORT $PROD_STUB_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__prod_stub_set_protocoll() {
	echo -e $BOLD"$PROD_STUB_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $PROD_STUB_DISPLAY_NAME"

	## Access to Prod stub sim

	PROD_STUB_SERVICE_PATH=$1"://"$PROD_STUB_APP_NAME":"$2  # docker access, container->container and script->container via proxy
	if [ $RUNMODE == "KUBE" ]; then
		PROD_STUB_SERVICE_PATH=$1"://"$PROD_STUB_APP_NAME.$KUBE_SIM_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
	fi

	echo ""
}

### Admin API functions producer stub

###########################
### Producer stub functions
###########################

# Export env vars for config files, docker compose and kube resources
# args:
__prodstub_export_vars() {
	export PROD_STUB_APP_NAME
	export PROD_STUB_DISPLAY_NAME

	export DOCKER_SIM_NWNAME
	export KUBE_SIM_NAMESPACE

	export PROD_STUB_IMAGE
	export PROD_STUB_INTERNAL_PORT
	export PROD_STUB_INTERNAL_SECURE_PORT
	export PROD_STUB_EXTERNAL_PORT
	export PROD_STUB_EXTERNAL_SECURE_PORT
}


# Start the Producer stub in the simulator group
# args: -
# (Function for test scripts)
start_prod_stub() {

	echo -e $BOLD"Starting $PROD_STUB_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "PRODSTUB"
		retcode_i=$?

		# Check if app shall only be used by the test script
		__check_prestarted_image "PRODSTUB"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $PROD_STUB_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $PROD_STUB_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $PROD_STUB_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $PROD_STUB_APP_NAME will not be started"$ERED
			exit
		fi

		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $PROD_STUB_APP_NAME deployment and service"
			echo " Setting RC replicas=1"
			__kube_scale deployment $PROD_STUB_APP_NAME $KUBE_SIM_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then
			echo -e " Creating $PROD_STUB_APP_NAME deployment and service"

            __kube_create_namespace $KUBE_SIM_NAMESPACE

			__prodstub_export_vars

			# Create service
			input_yaml=$SIM_GROUP"/"$PROD_STUB_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/prodstub_svc.yaml
			__kube_create_instance service $PROD_STUB_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$PROD_STUB_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/prodstub_app.yaml
			__kube_create_instance app $PROD_STUB_APP_NAME $input_yaml $output_yaml
		fi

		__check_service_start $PROD_STUB_APP_NAME $PROD_STUB_SERVICE_PATH$PROD_STUB_ALIVE_URL

		echo -ne " Service $PROD_STUB_APP_NAME - reset  "$SAMELINE
		result=$(__do_curl $PROD_STUB_SERVICE_PATH/reset)
		if [ $? -ne 0 ]; then
			echo -e " Service $PROD_STUB_APP_NAME - reset  $RED Failed $ERED - will continue"
		else
			echo -e " Service $PROD_STUB_APP_NAME - reset  $GREEN OK $EGREEN"
		fi
	else

		# Check if docker app shall be fully managed by the test script
		__check_included_image 'PRODSTUB'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Producer stub app is not included as managed in this test script"$ERED
			echo -e $RED"The Producer stub will not be started"$ERED
			exit
		fi

		__prodstub_export_vars

		__start_container $PROD_STUB_COMPOSE_DIR "" NODOCKERARGS 1 $PROD_STUB_APP_NAME

        __check_service_start $PROD_STUB_APP_NAME $PROD_STUB_SERVICE_PATH$PROD_STUB_ALIVE_URL
	fi
    echo ""
    return 0
}

# Execute a curl cmd towards the prodstub simulator and check the response code.
# args: TEST|CONF <expected-response-code> <curl-cmd-string> [<json-file-to-compare-output>]
__execute_curl_to_prodstub() {
    TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
    echo "(${BASH_LINENO[0]}) - ${TIMESTAMP}: ${FUNCNAME[0]}" $@ >> $HTTPLOG
	proxyflag=""
	if [ ! -z "$KUBE_PROXY_PATH" ]; then
		if [ $KUBE_PROXY_HTTPX == "http" ]; then
			proxyflag=" --proxy $KUBE_PROXY_PATH"
		else
			proxyflag=" --proxy-insecure --proxy $KUBE_PROXY_PATH"
		fi
	fi
	if [ ! -z "$KUBE_PROXY_CURL_JWT" ]; then
		jwt="-H "\""Authorization: Bearer $KUBE_PROXY_CURL_JWT"\"
		echo " CMD: $3 $proxyflag $jwt" >> $HTTPLOG
		res=$($3 $proxyflag -H "Authorization: Bearer $KUBE_PROXY_CURL_JWT")
	else
		echo " CMD: $3 $proxyflag" >> $HTTPLOG
		res="$($3 $proxyflag)"
	fi
	echo " RESP: $res" >> $HTTPLOG
	retcode=$?
    if [ $retcode -ne 0 ]; then
        __log_conf_fail_general " Fatal error when executing curl, response: "$retcode
        return 1
    fi
    status=${res:${#res}-3}
    if [ $status -eq $2 ]; then
        if [ $# -eq 4 ]; then
            body=${res:0:${#res}-3}
            jobfile=$(cat $4)
            echo " TARGET JSON: $jobfile" >> $HTTPLOG
		    res=$(python3 ../common/compare_json.py "$jobfile" "$body")
            if [ $res -ne 0 ]; then
                if [ $1 == "TEST" ]; then
                    __log_test_fail_body
                 else
                    __log_conf_fail_body
                fi
                return 1
            fi
        fi
        if [ $1 == "TEST" ]; then
            __log_test_pass
        else
            __log_conf_ok
        fi
        return 0
    fi
    if [ $1 == "TEST" ]; then
        __log_test_fail_status_code $2 $status
        else
        __log_conf_fail_status_code $2 $status
    fi
    return 1
}

# Prodstub API: Set (or reset) response code for producer supervision
# <response-code> <producer-id> [<forced_response_code>]
# (Function for test scripts)
prodstub_arm_producer() {
	__log_conf_start $@
	if [ $# -ne 2 ] && [ $# -ne 3 ]; then
		__print_err "<response-code> <producer-id> [<forced_response_code>]" $@
		return 1
	fi

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_SERVICE_PATH/arm/supervision/"$2
	if [ $# -eq 3 ]; then
		curlString=$curlString"?response="$3
	fi

    __execute_curl_to_prodstub CONF $1 "$curlString"
    return $?
}

# Prodstub API: Set (or reset) response code job create
# <response-code> <producer-id> <job-id> [<forced_response_code>]
# (Function for test scripts)
prodstub_arm_job_create() {
	__log_conf_start $@
	if [ $# -ne 3 ] && [ $# -ne 4 ]; then
		__print_err "<response-code> <producer-id> <job-id> [<forced_response_code>]" $@
		return 1
	fi

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_SERVICE_PATH/arm/create/$2/$3"
	if [ $# -eq 4 ]; then
		curlString=$curlString"?response="$4
	fi

    __execute_curl_to_prodstub CONF $1 "$curlString"
    return $?
}

# Prodstub API: Set (or reset) response code job delete
# <response-code> <producer-id> <job-id> [<forced_response_code>]
# (Function for test scripts)
prodstub_arm_job_delete() {
	__log_conf_start $@
	if [ $# -ne 3 ] && [ $# -ne 4 ]; then
		__print_err "<response-code> <producer-id> <job-id> [<forced_response_code>]" $@
		return 1
	fi

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_SERVICE_PATH/arm/delete/$2/$3"
	if [ $# -eq 4 ]; then
		curlString=$curlString"?response="$4
	fi

    __execute_curl_to_prodstub CONF $1 "$curlString"
    return $?
}

# Prodstub API: Arm a type of a producer
# <response-code> <producer-id> <type-id>
# (Function for test scripts)
prodstub_arm_type() {
	__log_conf_start $@
	if [ $# -ne 3 ]; then
		__print_err "<response-code> <producer-id> <type-id>" $@
		return 1
	fi

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_SERVICE_PATH/arm/type/$2/$3"

    __execute_curl_to_prodstub CONF $1 "$curlString"
    return $?
}

# Prodstub API: Disarm a type in a producer
# <response-code> <producer-id> <type-id>
# (Function for test scripts)
prodstub_disarm_type() {
	__log_conf_start $@
	if [ $# -ne 3 ]; then
		__print_err "<response-code> <producer-id> <type-id>" $@
		return 1
	fi

    curlString="curl -X DELETE -skw %{http_code} $PROD_STUB_SERVICE_PATH/arm/type/$2/$3"

    __execute_curl_to_prodstub CONF $1 "$curlString"
    return $?
}

# Prodstub API: Get job data for a job and compare with a target job json
# <response-code> <producer-id> <job-id> <type-id> <target-url> <job-owner> <template-job-file>
# (Function for test scripts)
prodstub_check_jobdata() {
	__log_test_start $@
	if [ $# -ne 7 ]; then
		__print_err "<response-code> <producer-id> <job-id> <type-id> <target-url> <job-owner> <template-job-file>" $@
		return 1
	fi
    if [ -f $7 ]; then
        jobfile=$(cat $7)
        jobfile=$(echo "$jobfile" | sed "s/XXXX/$3/g")
    else
    	__log_test_fail_general "Template file "$7" for jobdata, does not exist"
        return 1
    fi
    targetJson="{\"ei_job_identity\":\"$3\",\"ei_type_identity\":\"$4\",\"target_uri\":\"$5\",\"owner\":\"$6\", \"ei_job_data\":$jobfile}"
    file="./tmp/.p.json"
	echo "$targetJson" > $file

    curlString="curl -X GET -skw %{http_code} $PROD_STUB_SERVICE_PATH/jobdata/$2/$3"

    __execute_curl_to_prodstub TEST $1 "$curlString" $file
    return $?
}

# Prodstub API: Get job data for a job and compare with a target job json
# <response-code> <producer-id> <job-id> <type-id> <target-url> <job-owner> <template-job-file>
# (Function for test scripts)
prodstub_check_jobdata_2() {
	__log_test_start $@
	if [ $# -ne 7 ]; then
		__print_err "<response-code> <producer-id> <job-id> <type-id> <target-url> <job-owner> <template-job-file>" $@
		return 1
	fi
    if [ -f $7 ]; then
        jobfile=$(cat $7)
        jobfile=$(echo "$jobfile" | sed "s/XXXX/$3/g")
    else
    	__log_test_fail_general "Template file "$7" for jobdata, does not exist"
        return 1
    fi
	targetJson="{\"ei_job_identity\":\"$3\",\"ei_type_identity\":\"$4\",\"target_uri\":\"$5\",\"owner\":\"$6\", \"ei_job_data\":$jobfile,\"last_updated\":\"????\"}"

	file="./tmp/.p.json"
	echo "$targetJson" > $file

    curlString="curl -X GET -skw %{http_code} $PROD_STUB_SERVICE_PATH/jobdata/$2/$3"

    __execute_curl_to_prodstub TEST $1 "$curlString" $file
    return $?
}

# Prodstub API: Get job data for a job and compare with a target job json (info-jobs)
# <response-code> <producer-id> <job-id> <type-id> <target-url> <job-owner> <template-job-file>
# (Function for test scripts)
prodstub_check_jobdata_3() {
	__log_test_start $@
	if [ $# -ne 7 ]; then
		__print_err "<response-code> <producer-id> <job-id> <type-id> <target-url> <job-owner> <template-job-file>" $@
		return 1
	fi
    if [ -f $7 ]; then
        jobfile=$(cat $7)
        jobfile=$(echo "$jobfile" | sed "s/XXXX/$3/g")
    else
    	__log_test_fail_general "Template file "$7" for jobdata, does not exist"
        return 1
    fi
    targetJson="{\"info_job_identity\":\"$3\",\"info_type_identity\":\"$4\",\"target_uri\":\"$5\",\"owner\":\"$6\", \"info_job_data\":$jobfile,\"last_updated\":\"????\"}"
    file="./tmp/.p.json"
	echo "$targetJson" > $file

    curlString="curl -X GET -skw %{http_code} $PROD_STUB_SERVICE_PATH/jobdata/$2/$3"

    __execute_curl_to_prodstub TEST $1 "$curlString" $file
    return $?
}

# Prodstub API: Delete the job data
# <response-code> <producer-id> <job-id>
# (Function for test scripts)
prodstub_delete_jobdata() {
	__log_conf_start
	if [ $# -ne 3 ]; then
		__print_err "<response-code> <producer-id> <job-id> " $@
		return 1
	fi
    curlString="curl -X DELETE -skw %{http_code} $PROD_STUB_SERVICE_PATH/jobdata/$2/$3"

    __execute_curl_to_prodstub CONF $1 "$curlString"
    return $?
}

# Tests if a variable value in the prod stub is equal to a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
prodstub_equal() {
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		__var_test "PRODSTUB" "$PROD_STUB_SERVICE_PATH/counter/" $1 "=" $2 $3
	else
		__print_err "Wrong args to prodstub_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}