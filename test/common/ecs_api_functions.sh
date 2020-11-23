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

# This is a script that contains specific test functions for ECS NB/SB API

. ../common/api_curl.sh

# Tests if a variable value in the ECS is equal to a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
ecs_equal() {
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		__var_test ECS "$LOCALHOST$ECS_EXTERNAL_PORT/" $1 "=" $2 $3
	else
		__print_err "Wrong args to ecs_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}


##########################################
######### A1-E Enrichment  API ##########
##########################################
#Function prefix: ecs_api_a1

# API Test function: GET /A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs
# args: <response-code> <type-id>  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]
# args (flat uri structure): <response-code> <type-id>|NOTYPE  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]
# (Function for test scripts)
ecs_api_a1_get_job_ids() {
	__log_test_start $@

	if [ -z "$FLAT_A1_EI" ]; then
		# Valid number of parameters 4,5,6 etc
    	if [ $# -lt 3 ]; then
			__print_err "<response-code> <type-id>  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]" $@
			return 1
		fi
	else
		echo -e $YELLOW"USING NOT CONFIRMED INTERFACE - FLAT URI STRUCTURE"$EYELLOW
		# Valid number of parameters 4,5,6 etc
    	if [ $# -lt 3 ]; then
			__print_err "<response-code> <type-id>|NOTYPE  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]" $@
			return 1
		fi
	fi
	search=""
	if [ $3 != "NOWNER" ]; then
		search="?owner="$3
	fi

	if [  -z "$FLAT_A1_EI" ]; then
		query="/A1-EI/v1/eitypes/$2/eijobs$search"
	else
		if [ $2 != "NOTYPE" ]; then
			if [ -z "$search" ]; then
				search="?eiTypeId="$2
			else
				search=$search"&eiTypeId="$2
			fi
		fi
		query="/A1-EI/v1/eijobs$search"
	fi
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 3 ]; then
		body=${res:0:${#res}-3}
		targetJson="["

		for pid in ${@:4} ; do
			if [ "$targetJson" != "[" ]; then
				targetJson=$targetJson","
			fi
			if [ $pid != "EMPTY" ]; then
				targetJson=$targetJson"\"$pid\""
			fi
		done

		targetJson=$targetJson"]"
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

# API Test function: GET ​/A1-EI​/v1​/eitypes​/{eiTypeId}
# args: <response-code> <type-id> [<schema-file>]
# (Function for test scripts)
ecs_api_a1_get_type() {
	__log_test_start $@

    if [ $# -lt 2 ] || [ $# -gt 3 ]; then
		__print_err "<response-code> <type-id> [<schema-file>]" $@
		return 1
	fi

	query="/A1-EI/v1/eitypes/$2"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -eq 3 ]; then
		body=${res:0:${#res}-3}
		if [ -f $3 ]; then
			schema=$(cat $3)
		else
			__log_test_fail_general "Schema file "$3", does not exist"
			return 1
		fi
		if [ -z "$FLAT_A1_EI" ]; then
			targetJson="{\"eiJobParametersSchema\":$schema}"
		else
			targetJson=$schema
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

# API Test function: GET /A1-EI/v1/eitypes
# args: <response-code> [ (EMPTY | [<type-id>]+) ]
# (Function for test scripts)
ecs_api_a1_get_type_ids() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ (EMPTY | [<type-id>]+) ]" $@
		return 1
	fi

	query="/A1-EI/v1/eitypes"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	if [ $# -gt 1 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $2 != "EMPTY" ]; then
			for pid in ${@:2} ; do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"\"$pid\""
			done
		fi
		targetJson=$targetJson"]"
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

# API Test function: GET ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}​/status
# args: <response-code> <type-id> <job-id> [<status>]
# args (flat uri structure): <response-code> <job-id> [<status>]
# (Function for test scripts)
ecs_api_a1_get_job_status() {
	__log_test_start $@

	if [ -z "$FLAT_A1_EI" ]; then
		if [ $# -ne 3 ] && [ $# -ne 4 ]; then
			__print_err "<response-code> <type-id> <job-id> [<status>]" $@
			return 1
		fi

		query="/A1-EI/v1/eitypes/$2/eijobs/$3/status"

		res="$(__do_curl_to_api ECS GET $query)"
		status=${res:${#res}-3}

		if [ $status -ne $1 ]; then
			__log_test_fail_status_code $1 $status
			return 1
		fi
		if [ $# -eq 4 ]; then
			body=${res:0:${#res}-3}
			targetJson="{\"operationalState\": \"$4\"}"
			echo " TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")

			if [ $res -ne 0 ]; then
				__log_test_fail_body
				return 1
			fi
		fi
	else
		echo -e $YELLOW"USING NOT CONFIRMED INTERFACE - FLAT URI STRUCTURE"$EYELLOW
		if [ $# -ne 2 ] && [ $# -ne 3 ]; then
			__print_err "<response-code> <job-id> [<status>]" $@
			return 1
		fi

		query="/A1-EI/v1/eijobs/$2/status"

		res="$(__do_curl_to_api ECS GET $query)"
		status=${res:${#res}-3}

		if [ $status -ne $1 ]; then
			__log_test_fail_status_code $1 $status
			return 1
		fi
		if [ $# -eq 3 ]; then
			body=${res:0:${#res}-3}
			targetJson="{\"eiJobStatus\": \"$3\"}"
			echo " TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")

			if [ $res -ne 0 ]; then
				__log_test_fail_body
				return 1
			fi
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: GET ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args: <response-code> <type-id> <job-id> [<target-url> <owner-id> <template-job-file>]
# args (flat uri structure): <response-code> <job-id> [<type-id> <target-url> <owner-id> <template-job-file>]
# (Function for test scripts)
ecs_api_a1_get_job() {
	__log_test_start $@

	if [  -z "$FLAT_A1_EI" ]; then
		if [ $# -ne 3 ] && [ $# -ne 6 ]; then
			__print_err "<response-code> <type-id> <job-id> [<target-url> <owner-id> <template-job-file>]" $@
			return 1
		fi
		query="/A1-EI/v1/eitypes/$2/eijobs/$3"
	else
		echo -e $YELLOW"USING NOT CONFIRMED INTERFACE - FLAT URI STRUCTURE"$EYELLOW
		if [ $# -ne 2 ] && [ $# -ne 7 ]; then
			__print_err "<response-code> <job-id> [<type-id> <target-url> <owner-id> <notification-url> <template-job-file>]" $@
			return 1
		fi
		query="/A1-EI/v1/eijobs/$2"
	fi
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [  -z "$FLAT_A1_EI" ]; then
		if [ $# -eq 6 ]; then
			body=${res:0:${#res}-3}

			if [ -f $6 ]; then
				jobfile=$(cat $6)
				jobfile=$(echo "$jobfile" | sed "s/XXXX/$3/g")
			else
				_log_test_fail_general "Job template file "$6", does not exist"
				return 1
			fi
			targetJson="{\"targetUri\": \"$4\",\"jobOwner\": \"$5\",\"jobParameters\": $jobfile}"
			echo " TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")

			if [ $res -ne 0 ]; then
				__log_test_fail_body
				return 1
			fi
		fi
	else
		if [ $# -eq 7 ]; then
			body=${res:0:${#res}-3}

			if [ -f $7 ]; then
				jobfile=$(cat $7)
				jobfile=$(echo "$jobfile" | sed "s/XXXX/$2/g")
			else
				_log_test_fail_general "Job template file "$6", does not exist"
				return 1
			fi
			targetJson="{\"eiTypeId\": \"$3\", \"jobResultUri\": \"$4\",\"jobOwner\": \"$5\",\"jobStatusNotificationUri\": \"$6\",\"jobDefinition\": $jobfile}"
			echo " TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")

			if [ $res -ne 0 ]; then
				__log_test_fail_body
				return 1
			fi
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: DELETE ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args: <response-code> <type-id> <job-id>
# args (flat uri structure): <response-code> <job-id>
# (Function for test scripts)
ecs_api_a1_delete_job() {
	__log_test_start $@

	if [  -z "$FLAT_A1_EI" ]; then
		if [ $# -ne 3 ]; then
			__print_err "<response-code> <type-id> <job-id>" $@
			return 1
		fi

		query="/A1-EI/v1/eitypes/$2/eijobs/$3"
	else
		echo -e $YELLOW"USING NOT CONFIRMED INTERFACE - FLAT URI STRUCTURE"$EYELLOW
		if [ $# -ne 2 ]; then
			__print_err "<response-code> <job-id>" $@
			return 1
		fi
		query="/A1-EI/v1/eijobs/$2"
	fi
    res="$(__do_curl_to_api ECS DELETE $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: PUT ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args: <response-code> <type-id> <job-id> <target-url> <owner-id> <template-job-file>
# args (flat uri structure): <response-code> <job-id> <type-id> <target-url> <owner-id> <notification-url> <template-job-file>
# (Function for test scripts)
ecs_api_a1_put_job() {
	__log_test_start $@

	if [  -z "$FLAT_A1_EI" ]; then
		if [ $# -lt 6 ]; then
			__print_err "<response-code> <type-id> <job-id> <target-url> <owner-id> <template-job-file>" $@
			return 1
		fi
		if [ -f $6 ]; then
			jobfile=$(cat $6)
			jobfile=$(echo "$jobfile" | sed "s/XXXX/$3/g")
		else
			_log_test_fail_general "Job template file "$6", does not exist"
			return 1
		fi

		inputJson="{\"targetUri\": \"$4\",\"jobOwner\": \"$5\",\"jobParameters\": $jobfile}"
		file="./tmp/.p.json"
		echo "$inputJson" > $file

		query="/A1-EI/v1/eitypes/$2/eijobs/$3"
	else
		echo -e $YELLOW"USING NOT CONFIRMED INTERFACE - FLAT URI STRUCTURE"$EYELLOW
		if [ $# -lt 7 ]; then
			__print_err "<response-code> <job-id> <type-id> <target-url> <owner-id> <notification-url> <template-job-file>" $@
			return 1
		fi
		if [ -f $7 ]; then
			jobfile=$(cat $7)
			jobfile=$(echo "$jobfile" | sed "s/XXXX/$2/g")
		else
			_log_test_fail_general "Job template file "$7", does not exist"
			return 1
		fi

		inputJson="{\"eiTypeId\": \"$3\", \"jobResultUri\": \"$4\",\"jobOwner\": \"$5\",\"jobStatusNotificationUri\": \"$6\",\"jobDefinition\": $jobfile}"
		file="./tmp/.p.json"
		echo "$inputJson" > $file

		query="/A1-EI/v1/eijobs/$2"
	fi

    res="$(__do_curl_to_api ECS PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}


##########################################
####   Enrichment Data Producer API   ####
##########################################
# Function prefix: ecs_api_edp

# API Test function: GET /ei-producer/v1/eitypes
# args: <response-code> [ EMPTY | <type-id>+]
# (Function for test scripts)
ecs_api_edp_get_type_ids() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ EMPTY | <type-id>+]" $@
		return 1
	fi

	query="/ei-producer/v1/eitypes"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 1 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $2 != "EMPTY" ]; then
			for pid in ${@:2} ; do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"\"$pid\""
			done
		fi
		targetJson=$targetJson"]"
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

# API Test function: GET /ei-producer/v1/eiproducers/{eiProducerId}/status
# args: <response-code> <producer-id> [<status> [<timeout>]]
# (Function for test scripts)
ecs_api_edp_get_producer_status() {
	__log_test_start $@

    if [ $# -lt 2 ] || [ $# -gt 4 ]; then
		__print_err "<response-code> <producer-id> [<status> [<timeout>]]" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers/$2/status"
	start=$SECONDS
	for (( ; ; )); do
		res="$(__do_curl_to_api ECS GET $query)"
		status=${res:${#res}-3}

		if [ $# -eq 4 ]; then
			duration=$((SECONDS-start))
			echo -ne " Response=${status} after ${duration} seconds, waiting for ${3} ${SAMELINE}"
			if [ $duration -gt $4 ]; then
				echo ""
				duration=-1  #Last iteration
			fi
		else
			duration=-1 #single test, no wait
		fi

		if [ $status -ne $1 ]; then
			if [ $duration -eq -1 ]; then
				__log_test_fail_status_code $1 $status
				return 1
			fi
		fi
		if [ $# -ge 3 ] && [ $status -eq $1 ]; then
			body=${res:0:${#res}-3}
			targetJson="{\"operational_state\": \"$3\"}"
			echo " TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")

			if [ $res -ne 0 ]; then
				if [ $duration -eq -1 ]; then
					__log_test_fail_body
					return 1
				fi
			else
				duration=-1  #Goto pass
			fi
		fi
		if [ $duration -eq -1 ]; then
			if [ $# -eq 4 ]; then
				echo ""
			fi
			__log_test_pass
			return 0
		else
			sleep 1
		fi
	done
}


# API Test function: GET /ei-producer/v1/eiproducers
# args: <response-code> [ EMPTY | <producer-id>+]
# (Function for test scripts)
ecs_api_edp_get_producer_ids() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ EMPTY | <producer-id>+]" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 1 ]; then
		body=${res:0:${#res}-3}
		targetJson="["

		for pid in ${@:2} ; do
			if [ "$targetJson" != "[" ]; then
				targetJson=$targetJson","
			fi
			if [ $pid != "EMPTY" ]; then
				targetJson=$targetJson"\"$pid\""
			fi
		done

		targetJson=$targetJson"]"
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

# API Test function: GET /ei-producer/v1/eitypes/{eiTypeId}
# args: <response-code> <type-id> [<job-schema-file> (EMPTY | [<producer-id>]+)]
# (Function for test scripts)
ecs_api_edp_get_type() {
	__log_test_start $@

	paramError=1
	if [ $# -eq 2 ]; then
		paramError=0
	fi
	if [ $# -gt 3 ]; then
		paramError=0
	fi
    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> <type-id> [<job-schema-file> 'EMPTY' | ([<producer-id>]+)]" $@
		return 1
	fi

	query="/ei-producer/v1/eitypes/$2"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	if [ $# -gt 3 ]; then
		body=${res:0:${#res}-3}

		if [ -f $3 ]; then
			schema=$(cat $3)
		else
			__log_test_fail_general "Job template file "$3", does not exist"
			return 1
		fi

		targetJson=""
		if [ $4 != "EMPTY" ]; then
			for pid in ${@:4} ; do
				if [ "$targetJson" != "" ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"\"$pid\""
			done
		fi
		targetJson="{\"ei_job_data_schema\":$schema, \"ei_producer_ids\": [$targetJson]}"

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

# API Test function: GET /ei-producer/v1/eiproducers/{eiProducerId}
# args: <response-code> <producer-id> [<job-callback> <supervision-callback> (EMPTY | [<type-id> <schema-file>]+) ]
# (Function for test scripts)
ecs_api_edp_get_producer() {
	__log_test_start $@

	#Possible arg count: 2, 5 6, 8, 10 etc
	paramError=1
	if [ $# -eq 2 ]; then
		paramError=0
	fi
	if [ $# -eq 5 ] && [ "$5" == "EMPTY" ]; then
		paramError=0
	fi
	variablecount=$(($#-4))
	if [ $# -gt 5 ] && [ $(($variablecount%2)) -eq 0 ]; then
		paramError=0
	fi

    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> <producer-id> [<job-callback> <supervision-callback> (NOID | [<type-id> <schema-file>]+) ]" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers/$2"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $# -gt 5 ]; then
			arr=(${@:5})
			for ((i=0; i<$(($#-5)); i=i+2)); do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				if [ -f ${arr[$i+1]} ]; then
					schema=$(cat ${arr[$i+1]})
				else
					_log_test_fail_general "Schema file "${arr[$i+1]}", does not exist"
					return 1
				fi

				targetJson=$targetJson"{\"ei_type_identity\":\"${arr[$i]}\",\"ei_job_data_schema\":$schema}"
			done
		fi
		targetJson=$targetJson"]"
		if [ $# -gt 4 ]; then
			targetJson="{\"supported_ei_types\":$targetJson,\"ei_job_callback_url\": \"$3\",\"ei_producer_supervision_callback_url\": \"$4\"}"
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

# API Test function: DELETE /ei-producer/v1/eiproducers/{eiProducerId}
# args: <response-code> <producer-id>
# (Function for test scripts)
ecs_api_edp_delete_producer() {
	__log_test_start $@

    if [ $# -lt 2 ]; then
		__print_err "<response-code> <producer-id>" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers/$2"
    res="$(__do_curl_to_api ECS DELETE $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: PUT /ei-producer/v1/eiproducers/{eiProducerId}
# args: <response-code> <producer-id> <job-callback> <supervision-callback> NOTYPE|[<type-id> <schema-file>]+
# (Function for test scripts)
ecs_api_edp_put_producer() {
	__log_test_start $@

	#Valid number of parametrer 5,6,8,10,
	paramError=1
	if  [ $# -eq 5 ] && [ "$5" == "NOTYPE" ]; then
		paramError=0
	elif [ $# -gt 5 ] && [ $(($#%2)) -eq 0 ]; then
		paramError=0
	fi
	if [ $paramError -ne 0 ]; then
		__print_err "<response-code> <producer-id> <job-callback> <supervision-callback> NOTYPE|[<type-id> <schema-file>]+" $@
		return 1
	fi

	inputJson="["
	if [ $# -gt 5 ]; then
		arr=(${@:5})
		for ((i=0; i<$(($#-5)); i=i+2)); do
			if [ "$inputJson" != "[" ]; then
				inputJson=$inputJson","
			fi
			if [ -f ${arr[$i+1]} ]; then
				schema=$(cat ${arr[$i+1]})
			else
				_log_test_fail_general "Schema file "${arr[$i+1]}", does not exist"
				return 1
			fi
			inputJson=$inputJson"{\"ei_type_identity\":\"${arr[$i]}\",\"ei_job_data_schema\":$schema}"
		done
	fi
	inputJson="\"supported_ei_types\":"$inputJson"]"

	inputJson=$inputJson",\"ei_job_callback_url\": \"$3\",\"ei_producer_supervision_callback_url\": \"$4\""

	inputJson="{"$inputJson"}"

	file="./tmp/.p.json"
	echo "$inputJson" > $file
	query="/ei-producer/v1/eiproducers/$2"
    res="$(__do_curl_to_api ECS PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /ei-producer/v1/eiproducers/{eiProducerId}/eijobs
# args: <response-code> <producer-id> (EMPTY | [<job-id> <type-id> <target-url> <template-job-file>]+)
# (Function for test scripts)
ecs_api_edp_get_producer_jobs() {
	__log_test_start $@

	#Valid number of parameter 2,3,6,10
	paramError=1
	if [ $# -eq 2 ]; then
		paramError=0
	fi
	if [ $# -eq 3 ] && [ "$3" == "EMPTY" ]; then
		paramError=0
	fi
	variablecount=$(($#-2))
	if [ $# -gt 3 ] && [ $(($variablecount%4)) -eq 0 ]; then
		paramError=0
	fi
	if [ $paramError -eq 1 ]; then
		__print_err "<response-code> <producer-id> (EMPTY | [<job-id> <type-id> <target-url> <template-job-file>]+)" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers/$2/eijobs"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}
	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $# -gt 3 ]; then
			arr=(${@:3})
			for ((i=0; i<$(($#-3)); i=i+4)); do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				if [ -f ${arr[$i+3]} ]; then
					jobfile=$(cat ${arr[$i+3]})
					jobfile=$(echo "$jobfile" | sed "s/XXXX/${arr[$i]}/g")
				else
					_log_test_fail_general "Job template file "${arr[$i+3]}", does not exist"
					return 1
				fi
				targetJson=$targetJson"{\"ei_job_identity\":\"${arr[$i]}\",\"ei_type_identity\":\"${arr[$i+1]}\",\"target_uri\":\"${arr[$i+2]}\",\"ei_job_data\":$jobfile}"
			done
		fi
		targetJson=$targetJson"]"

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


##########################################
####          Service status          ####
##########################################
# Function prefix: ecs_api_service

# API Test function: GET ​/status
# args: <response-code>
# (Function for test scripts)
ecs_api_service_status() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi
	res="$(__do_curl_to_api ECS GET /status)"
    status=${res:${#res}-3}
	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	__log_test_pass
	return 0
}