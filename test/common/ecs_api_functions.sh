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

############### EXPERIMENTAL #############

##########################################
###### Mainly only function skeletons ####
##########################################

##########################################
### A1-E Enrichment Data Consumer API ####
##########################################
#Function prefix: ecs_api_a1

# API Test function: GET /A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs
# args: <response-code>
# (Function for test scripts)
ecs_api_a1_get_job_ids() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: GET ​/A1-EI​/v1​/eitypes​/{eiTypeId}
# args: <response-code>
# (Function for test scripts)
ecs_api_a1_get_type() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: GET ​/A1-EI​/v1​/eitypes
# args: <response-code>
# (Function for test scripts)
ecs_api_a1_get_type_ids() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: GET ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}​/status
# args: <response-code>
# (Function for test scripts)
ecs_api_a1_get_job_status() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: GET ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args: <response-code>
# (Function for test scripts)
ecs_api_a1_get_job() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: DELETE ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args: <response-code>
# (Function for test scripts)
ecs_api_a1_delete_job() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: PUT ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args: <response-code>
# (Function for test scripts)
ecs_api_a1_put_job() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}


##########################################
####   Enrichment Data Producer API   ####
##########################################
# Function prefix: ecs_api_edp

# API Test function: GET /ei-producer/v1/eitypes
# args: <response-code>
# (Function for test scripts)
ecs_api_edp_get_type_ids() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: GET /ei-producer/v1/eiproducers/{eiProducerId}/status
# args: <response-code>
# (Function for test scripts)
ecs_api_edp_get_producer_status() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}


# API Test function: GET /ei-producer/v1/eiproducers
# args: <response-code> [<producer-id>]*|NOID
# (Function for test scripts)
ecs_api_edp_get_producer_ids() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		echo -e $RED" FAIL. Exepected status "$1", got "$status $ERED
		((RES_FAIL++))
		__check_stop_at_error
		return 1
	fi

	if [ $# -gt 1 ]; then
		body=${res:0:${#res}-3}
		targetJson="["

		for pid in ${@:2} ; do
			if [ "$targetJson" != "[" ]; then
				targetJson=$targetJson","
			fi
			if [ $pid != "NOID" ]; then
				targetJson=$targetJson"\"$pid\""
			fi
		done

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

# API Test function: GET /ei-producer/v1/eitypes/{eiTypeId}
# args: <response-code>
# (Function for test scripts)
ecs_api_edp_get_type() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: GET /ei-producer/v1/eiproducers/{eiProducerId}
# args: <response-code>
# (Function for test scripts)
ecs_api_edp_get_producer() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: DELETE /ei-producer/v1/eiproducers/{eiProducerId}
# args: <response-code>
# (Function for test scripts)
ecs_api_edp_delete_producer() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: PUT /ei-producer/v1/eiproducers/{eiProducerId}
# args: <response-code>
# (Function for test scripts)
ecs_api_edp_put_producer() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: GET /ei-producer/v1/eiproducers/{eiProducerId}/eijobs
# args: <response-code>
# (Function for test scripts)
ecs_api_edp_get_producer_jobs() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}


##########################################
####        Producer Simulator        ####
##########################################
# Function prefix: ecs_api_sim

# API Test function: POST ​/producer_simulator​/job_deleted_error
# args: <response-code>
# (Function for test scripts)
ecs_api_sim_post_job_delete_error() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: GET /producer_simulator​/supervision
# args: <response-code>
# (Function for test scripts)
ecs_api_sim_get_producer_supervision() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: POST /producer_simulator​/job_deleted
# args: <response-code>
# (Function for test scripts)
ecs_api_sim_post_job_deleted() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: GET ​/producer_simulator​/supervision_error
# args: <response-code>
# (Function for test scripts)
ecs_api_get_producer_supervision_error() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: POST /producer_simulator​/job_created_error
# args: <response-code>
# (Function for test scripts)
ecs_api_sim_get_job_created_error() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}

# API Test function: POST ​/producer_simulator​/job_created
# args: <response-code>
# (Function for test scripts)
ecs_api_sim_get_job_created() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
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
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	((RES_TEST++))

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi

	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
	return 0
}