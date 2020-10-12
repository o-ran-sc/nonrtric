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
	#echo ${FUNCNAME[1]} "line: "${BASH_LINENO[1]} >> $HTTPLOG
    echo "(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	echo " CMD: $3" >> $HTTPLOG
	res="$($3)"
	echo " RESP: $res" >> $HTTPLOG
	retcode=$?
    if [ $retcode -ne 0 ]; then
		echo " RETCODE: "$retcode
        echo -e $RED" FAIL - fatal error when executing curl."$ERED
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
                echo -e $RED" FAIL, returned body not correct"$ERED
		        return 1
            fi
        fi
        if [ $1 == "TEST" ]; then
            echo -e $GREEN" PASS"$EGREEN
        else
            echo -e $GREEN" OK"$EGREEN
        fi
        return 0
    fi
    echo -e $RED" FAIL - expected http response: "$2" but got http response: "$status $ERED
    return 1
}

# Prodstub API: Set (or reset) response code for producer supervision
# <response-code> <producer-id> [<forced_response_code>]
# (Function for test scripts)
prodstub_arm_supervision() {
	echo -e $BOLD"CONF(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@ $EBOLD
    echo "CONF(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@  >> $HTTPLOG
	if [ $# -ne 2 ] && [ $# -ne 3 ]; then
		((RES_CONF_FAIL++))
		__print_err "<response-code> <producer-id> [<forced_response_code>]" $@
		return 1
	fi

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_LOCALHOST/arm/supervision/"$2
	if [ $# -eq 3 ]; then
		curlString=$curlString"?response="$3
	fi

    __execute_curl_to_prodstub CONF $1 "$curlString"
    retcode=$?
    if [ $? -ne 0 ]; then
        ((RES_CONF_FAIL++))
    fi
	return $retcode
}

# Prodstub API: Set (or reset) response code job create
# <response-code> <producer-id> <job-id> [<forced_response_code>]
# (Function for test scripts)
prodstub_arm_create() {
	echo -e $BOLD"CONF(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@ $EBOLD
    echo "CONF(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@  >> $HTTPLOG
	if [ $# -ne 3 ] && [ $# -ne 4 ]; then
		((RES_CONF_FAIL++))
		__print_err "<response-code> <producer-id> <job-id> [<forced_response_code>]" $@
		return 1
	fi

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_LOCALHOST/arm/create/$2/$3"
	if [ $# -eq 4 ]; then
		curlString=$curlString"?response="$4
	fi

    __execute_curl_to_prodstub CONF $1 "$curlString"
    retcode=$?
    if [ $? -ne 0 ]; then
        ((RES_CONF_FAIL++))
    fi
	return $retcode
}

# Prodstub API: Set (or reset) response code job delete
# <response-code> <producer-id> <job-id> [<forced_response_code>]
# (Function for test scripts)
prodstub_arm_delete() {
	echo -e $BOLD"CONF(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@ $EBOLD
    echo "CONF(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@  >> $HTTPLOG
	if [ $# -ne 3 ] && [ $# -ne 4 ]; then
		((RES_CONF_FAIL++))
		__print_err "<response-code> <producer-id> <job-id> [<forced_response_code>]" $@
		return 1
	fi

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_LOCALHOST/arm/delete/$2/$3"
	if [ $# -eq 4 ]; then
		curlString=$curlString"?response="$4
	fi

    __execute_curl_to_prodstub CONF $1 "$curlString"
    retcode=$?
    if [ $? -ne 0 ]; then
        ((RES_CONF_FAIL++))
    fi
	return $retcode
}

# Prodstub API: Arm a type of a producer
# <response-code> <producer-id> <type-id>
# (Function for test scripts)
prodstub_arm_type() {
	echo -e $BOLD"CONF(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@ $EBOLD
    echo "CONF(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@  >> $HTTPLOG
	if [ $# -ne 3 ]; then
		((RES_CONF_FAIL++))
		__print_err "<response-code> <producer-id> <type-id>" $@
		return 1
	fi

    curlString="curl -X PUT -skw %{http_code} $PROD_STUB_LOCALHOST/arm/type/$2/$3"

    __execute_curl_to_prodstub CONF $1 "$curlString"
    retcode=$?
    if [ $? -ne 0 ]; then
        ((RES_CONF_FAIL++))
    fi
	return $retcode
}

# Prodstub API: Disarm a type in a producer
# <response-code> <producer-id> <type-id>
# (Function for test scripts)
prodstub_disarm_type() {
	echo -e $BOLD"CONF(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@ $EBOLD
    echo "CONF(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@  >> $HTTPLOG
	if [ $# -ne 3 ]; then
		((RES_CONF_FAIL++))
		__print_err "<response-code> <producer-id> <type-id>" $@
		return 1
	fi

    curlString="curl -X DELETE -skw %{http_code} $PROD_STUB_LOCALHOST/arm/type/$2/$3"

    __execute_curl_to_prodstub CONF $1 "$curlString"
    retcode=$?
    if [ $? -ne 0 ]; then
        ((RES_CONF_FAIL++))
    fi
	return $retcode
}

# Prodstub API: Get job data for a job and compare with a target job json
# <response-code> <producer-id> <job-id> <type-id> <target-url> <template-job-file>
# (Function for test scripts)
prodstub_check_jobdata() {
	echo -e $BOLD"TEST(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@ $EBOLD
    echo "TEST(${BASH_LINENO[0]}): "${FUNCNAME[0]} $@  >> $HTTPLOG
	if [ $# -ne 6 ]; then
		((RES_FAIL++))
		__print_err "<response-code> <producer-id> <job-id> <type-id> <target-url> <template-job-file>" $@
		return 1
	fi
    if [ -f $6 ]; then
        jobfile=$(cat $6)
        jobfile=$(echo "$jobfile" | sed "s/XXXX/$3/g")
    else
        echo -e $RED" FAIL.  Template file "$6" for jobdata, does not exist"$ERED
        return 1
    fi
    targetJson="{\"ei_job_identity\":\"$3\",\"ei_type_identity\":\"$4\",\"target_uri\":\"$5\",\"ei_job_data\":$jobfile}"
    file="./tmp/.p.json"
	echo "$targetJson" > $file

    curlString="curl -X GET -skw %{http_code} $PROD_STUB_LOCALHOST/jobdata/$2/$3"

    __execute_curl_to_prodstub TEST $1 "$curlString" $file
    retcode=$?
    if [ $? -ne 0 ]; then
        ((RES_FAIL++))
    fi
	return $retcode
}