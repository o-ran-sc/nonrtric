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


### Admin API functions producer stub


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

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_LOCALHOST/arm/supervision/"$2
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

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_LOCALHOST/arm/create/$2/$3"
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

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_LOCALHOST/arm/delete/$2/$3"
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

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_LOCALHOST/arm/type/$2/$3"

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

    curlString="curl -X DELETE -skw %{http_code} $PROD_STUB_LOCALHOST/arm/type/$2/$3"

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

    curlString="curl -X GET -skw %{http_code} $PROD_STUB_LOCALHOST/jobdata/$2/$3"

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
    curlString="curl -X DELETE -skw %{http_code} $PROD_STUB_LOCALHOST/jobdata/$2/$3"

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
		__var_test "PRODSTUB" "$LOCALHOST$PROD_STUB_EXTERNAL_PORT/counter/" $1 "=" $2 $3
	else
		__print_err "Wrong args to prodstub_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}