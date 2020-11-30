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

# This is a script that contains specific test functions for RAPP Catalogue API

. ../common/api_curl.sh

# Tests if a variable value in the RAPP Catalogue is equal to a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
rc_equal() {
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		__var_test RC "$LOCALHOST$RC_EXTERNAL_PORT/" $1 "=" $2 $3
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
