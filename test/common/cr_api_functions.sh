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

. ../common/api_curl.sh

### Admin API functions for the Callback Reciver


# Excute a curl cmd towards a Callback Reciver admin interface and check the response code.
# args: <expected-response-code> <curl-cmd-string>
__execute_curl_to_cr() {
	echo ${FUNCNAME[1]} "line: "${BASH_LINENO[1]} >> $HTTPLOG
	echo " CMD: $2" >> $HTTPLOG
	res="$($2)"
	echo " RESP: $res" >> $HTTPLOG
	retcode=$?
    if [ $retcode -ne 0 ]; then
		((RES_CONF_FAIL++))
		echo " RETCODE: "$retcode
        echo -e $RED" FAIL - fatal error when executing curl."$ERED
        return 1
    fi
    status=${res:${#res}-3}
    if [ $status -eq $1 ]; then
        echo -e $GREEN" OK"$EGREEN
        return 0
    fi
    echo -e $RED" FAIL - expected http response: "$1" but got http response: "$status $ERED
	((RES_CONF_FAIL++))
    return 1
}

# Tests if a variable value in the CR is equal to a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
cr_equal() {
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		__var_test "CR" "$LOCALHOST$CR_EXTERNAL_PORT/counter/" $1 "=" $2 $3
	else
		((RES_CONF_FAIL++))
		__print_err "Wrong args to cr_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}

# CR API: Check the contents of all current ric sync events from PMS
# <response-code> <id> [ EMPTY | ( <ric-id> )+ ]
# (Function for test scripts)
cr_api_check_all_sync_events() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

	if [ "$PMS_VERSION" != "V2" ]; then
		echo -e $RED" FAIL, function not supported"$ERED
		((RES_FAIL++))
		__check_stop_at_error
		return 1
	fi

    if [ $# -lt 2 ]; then
        __print_err "<response-code> <id> [ EMPTY | ( <ric-id> )+ ]" $@
        return 1
    fi

	query="/get-all-events/"$2
	res="$(__do_curl_to_api CR GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		echo -e $RED" FAIL. Exepected status "$1", got "$status $ERED
		((RES_FAIL++))
		__check_stop_at_error
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		if [ $# -eq 3 ] && [ $3 == "EMPTY" ]; then
			targetJson="["
		else
			targetJson="["
			arr=(${@:3})

			for ((i=0; i<$(($#-2)); i=i+1)); do

				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"{\"ric_id\":\"${arr[$i]}\",\"event_type\":\"AVAILABLE\"}"
			done
		fi

		targetJson=$targetJson"]"
		echo "TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			echo -e $RED" FAIL, returned body not correct"$ERED
			((RES_FAIL++))
			__check_stop_at_error
			return 1
		fi
	fi
	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}