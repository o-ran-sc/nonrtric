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

# This is a script that contains container/service management functions and test functions for Producer stub

## Access to Prod stub sim
# Direct access
PROD_STUB_HTTPX="http"
PROD_STUB_HOST_NAME=$LOCALHOST_NAME
PROD_STUB_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_HOST_NAME":"$PROD_STUB_EXTERNAL_PORT

#Docker/Kube internal path
if [ $RUNMODE == "KUBE" ]; then
	PROD_STUB_SERVICE_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_APP_NAME"."$KUBE_SIM_NAMESPACE":"$PROD_STUB_EXTERNAL_PORT
else
	PROD_STUB_SERVICE_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_APP_NAME":"$PROD_STUB_INTERNAL_PORT
fi

# Set http as the protocol to use for all communication to the Producer stub
# args: -
# (Function for test scripts)
use_prod_stub_http() {
	echo -e $BOLD"Producer stub protocol setting"$EBOLD
	echo -e " Using $BOLD http $EBOLD towards Producer stub"

	PROD_STUB_HTTPX="http"
    PROD_STUB_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_HOST_NAME":"$PROD_STUB_EXTERNAL_PORT

	if [ $RUNMODE == "KUBE" ]; then
		PROD_STUB_SERVICE_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_APP_NAME"."$KUBE_SIM_NAMESPACE":"$PROD_STUB_EXTERNAL_PORT
	else
		PROD_STUB_SERVICE_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_APP_NAME":"$PROD_STUB_INTERNAL_PORT
	fi

	echo ""
}

# Set https as the protocol to use for all communication to the Producer stub
# args: -
# (Function for test scripts)
use_prod_stub_https() {
	echo -e $BOLD"Producer stub protocol setting"$EBOLD
	echo -e " Using $BOLD https $EBOLD towards Producer stub"

	PROD_STUB_HTTPX="https"
    PROD_STUB_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_HOST_NAME":"$PROD_STUB_EXTERNAL_SECURE_PORT

	if [ $RUNMODE == "KUBE" ]; then
		PROD_STUB_SERVICE_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_APP_NAME"."$KUBE_SIM_NAMESPACE":"$PROD_STUB_EXTERNAL_SECURE_PORT
	else
		PROD_STUB_SERVICE_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_APP_NAME":"$PROD_STUB_INTERNAL_SECURE_PORT
	fi
	echo ""
}

### Admin API functions producer stub

###########################
### Producer stub functions
###########################

# Start the Producer stub in the simulator group
# args: -
# (Function for test scripts)
start_prod_stub() {

	echo -e $BOLD"Starting $PROD_STUB_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "PRODSTUB"
		retcode_i=$?

		# Check if app shall only be used by the testscipt
		__check_prestarted_image "PRODSTUB"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $ECS_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $ECS_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $ECS_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $ECS_APP_NAME will not be started"$ERED
			exit
		fi

		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $PROD_STUB_APP_NAME deployment and service"
			echo " Setting RC replicas=1"
			__kube_scale deployment $PROD_STUB_APP_NAME $KUBE_SIM_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then
			echo -e " Creating $PROD_STUB_APP_NAME deployment and service"
			export PROD_STUB_APP_NAME
			export KUBE_SIM_NAMESPACE
			export PROD_STUB_IMAGE
			export PROD_STUB_INTERNAL_PORT
			export PROD_STUB_INTERNAL_SECURE_PORT
			export PROD_STUB_EXTERNAL_PORT
			export PROD_STUB_EXTERNAL_SECURE_PORT

            __kube_create_namespace $KUBE_SIM_NAMESPACE

			# Create service
			input_yaml=$SIM_GROUP"/"$PROD_STUB_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/prodstub_svc.yaml
			__kube_create_instance service $PROD_STUB_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$PROD_STUB_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/prodstub_app.yaml
			__kube_create_instance app $PROD_STUB_APP_NAME $input_yaml $output_yaml
		fi

		PROD_STUB_HOST_NAME=$(__kube_get_service_host $PROD_STUB_APP_NAME $KUBE_SIM_NAMESPACE)

		PROD_STUB_EXTERNAL_PORT=$(__kube_get_service_port $PROD_STUB_APP_NAME $KUBE_SIM_NAMESPACE "http")
		PROD_STUB_EXTERNAL_SECURE_PORT=$(__kube_get_service_port $PROD_STUB_APP_NAME $KUBE_SIM_NAMESPACE "https")

		echo " Host IP, http port, https port: $PROD_STUB_HOST_NAME $PROD_STUB_EXTERNAL_PORT $PROD_STUB_EXTERNAL_SECURE_PORT"
		if [ $PROD_STUB_HTTPX == "http" ]; then
            PROD_STUB_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_HOST_NAME":"$PROD_STUB_EXTERNAL_PORT
			PROD_STUB_SERVICE_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_APP_NAME"."$KUBE_SIM_NAMESPACE":"$PROD_STUB_EXTERNAL_PORT
		else
            PROD_STUB_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_HOST_NAME":"$PROD_STUB_EXTERNAL_SECURE_PORT
			PROD_STUB_SERVICE_PATH=$PROD_STUB_HTTPX"://"$PROD_STUB_APP_NAME"."$KUBE_SIM_NAMESPACE":"$PROD_STUB_EXTERNAL_SECURE_PORT
		fi

		__check_service_start $PROD_STUB_APP_NAME $PROD_STUB_PATH$PROD_STUB_ALIVE_URL

		echo -ne " Service $PROD_STUB_APP_NAME - reset  "$SAMELINE
		result=$(__do_curl $PROD_STUB_PATH/reset)
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

        export PROD_STUB_APP_NAME
        export PROD_STUB_APP_NAME_ALIAS
        export PROD_STUB_INTERNAL_PORT
        export PROD_STUB_EXTERNAL_PORT
        export PROD_STUB_INTERNAL_SECURE_PORT
        export PROD_STUB_EXTERNAL_SECURE_PORT
        export DOCKER_SIM_NWNAME

		__start_container $PROD_STUB_COMPOSE_DIR NODOCKERARGS 1 $PROD_STUB_APP_NAME

        __check_service_start $PROD_STUB_APP_NAME $PROD_STUB_PATH$PROD_STUB_ALIVE_URL
	fi
    echo ""
    return 0
}

# Excute a curl cmd towards the prodstub simulator and check the response code.
# args: TEST|CONF <expected-response-code> <curl-cmd-string> [<json-file-to-compare-output>]
__execute_curl_to_prodstub() {
    TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
    echo "(${BASH_LINENO[0]}) - ${TIMESTAMP}: ${FUNCNAME[0]}" $@ >> $HTTPLOG
	echo " CMD: $3" >> $HTTPLOG
	res="$($3)"
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

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_PATH/arm/supervision/"$2
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

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_PATH/arm/create/$2/$3"
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

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_PATH/arm/delete/$2/$3"
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

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_PATH/arm/type/$2/$3"

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

    curlString="curl -X DELETE -skw %{http_code} $PROD_STUB_PATH/arm/type/$2/$3"

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
        _log_test_fail_general "Template file "$7" for jobdata, does not exist"
        return 1
    fi
    targetJson="{\"ei_job_identity\":\"$3\",\"ei_type_identity\":\"$4\",\"target_uri\":\"$5\",\"owner\":\"$6\", \"ei_job_data\":$jobfile}"
    file="./tmp/.p.json"
	echo "$targetJson" > $file

    curlString="curl -X GET -skw %{http_code} $PROD_STUB_PATH/jobdata/$2/$3"

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
        _log_test_fail_general "Template file "$7" for jobdata, does not exist"
        return 1
    fi
    targetJson="{\"ei_job_identity\":\"$3\",\"ei_type_identity\":\"$4\",\"target_uri\":\"$5\",\"owner\":\"$6\", \"ei_job_data\":$jobfile,\"last_updated\":\"????\"}"
    file="./tmp/.p.json"
	echo "$targetJson" > $file

    curlString="curl -X GET -skw %{http_code} $PROD_STUB_PATH/jobdata/$2/$3"

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
    curlString="curl -X DELETE -skw %{http_code} $PROD_STUB_PATH/jobdata/$2/$3"

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
		__var_test "PRODSTUB" "$PROD_STUB_PATH/counter/" $1 "=" $2 $3
	else
		__print_err "Wrong args to prodstub_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}