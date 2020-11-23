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

# This is a script that contains specific test functions for A1 Controller API

# Generic function to query the RICs via the A1-controller API.
# args: <operation> <url> [<body>]
# <operation>: getA1Policy,putA1Policy,getA1PolicyType,deleteA1Policy,getA1PolicyStatus
# response: <json-body><3-digit-response-code>
# (Not for test scripts)
__do_curl_to_controller() {
    echo " (${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
    if [ $# -ne 2 ] && [ $# -ne 3 ]; then
		((RES_CONF_FAIL++))
        echo "-Incorrect number of parameters to __do_curl_to_controller " $@ >> $HTTPLOG
        echo "-Expected: <operation> <url> [<body>]" >> $HTTPLOG
        echo "-Returning response 000" >> $HTTPLOG
        echo "000"
        return 1
    fi
    if [ $# -eq 2 ]; then
        json='{"input":{"near-rt-ric-url":"'$2'"}}'
    else
        # Escape quotes in the body
        body=$(echo "$3" | sed 's/"/\\"/g')
        json='{"input":{"near-rt-ric-url":"'$2'","body":"'"$body"'"}}'
    fi
	payload="./tmp/.sdnc.payload.json"
    echo "$json" > $payload
    echo "  FILE ($payload) : $json"  >> $HTTPLOG
    curlString="curl -skw %{http_code} -X POST $SDNC_HTTPX://$SDNC_USER:$SDNC_PWD@localhost:$SDNC_LOCAL_PORT$SDNC_API_URL$1 -H accept:application/json -H Content-Type:application/json --data-binary @$payload"
    echo "  CMD: "$curlString >> $HTTPLOG
    res=$($curlString)
    retcode=$?
    echo "  RESP: "$res >> $HTTPLOG
    if [ $retcode -ne 0 ]; then
        echo "  RETCODE: "$retcode >> $HTTPLOG
        echo "000"
        return 1
    fi

	status=${res:${#res}-3}

    if [ $status -ne 200 ]; then
        echo "000"
        return 1
    fi
    body=${res:0:${#res}-3}
	echo "  JSON: "$body >> $HTTPLOG
	reply="./tmp/.sdnc-reply.json"
    echo "$body" > $reply
    res=$(python3 ../common/extract_sdnc_reply.py $reply)
    echo "  EXTRACED BODY+CODE: "$res >> $HTTPLOG
    echo "$res"
    return 0
}

# Controller API Test function: getA1Policy (return ids only)
# arg: <response-code> (OSC <ric-id> <policy-type-id> [ <policy-id> [<policy-id>]* ]) | ( STD <ric-id> [ <policy-id> [<policy-id>]* ] )
# (Function for test scripts)
controller_api_get_A1_policy_ids() {
	__log_test_start $@

    paramError=1
    if [ $# -gt 3 ] && [ $2 == "OSC" ]; then
        url="$RIC_SIM_HTTPX://$3:$RIC_SIM_PORT/a1-p/policytypes/$4/policies"
		paramError=0
    elif [ $# -gt 2 ] && [ $2 == "STD" ]; then
        url="$RIC_SIM_HTTPX://$3:$RIC_SIM_PORT/A1-P/v1/policies"
        paramError=0
	fi

    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> (OSC <ric-id> <policy-type-id> [ <policy-id> [<policy-id>]* ]) | ( STD <ric-id> [ <policy-id> [<policy-id>]* ] )" $@
		return 1
	fi

    res=$(__do_curl_to_controller getA1Policy "$url")
    retcode=$?
    status=${res:${#res}-3}

    if [ $retcode -ne 0 ]; then
		__log_test_fail_status_code $1 $retcode "(likely remote server error)"
		return 1
	fi

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
    body=${res:0:${#res}-3}

	targetJson="["
    start=4
    if [ $2 == "OSC" ]; then
        start=5
    fi
    for pid in ${@:$start} ; do
        if [ "$targetJson" != "[" ]; then
            targetJson=$targetJson","
        fi
        targetJson=$targetJson"\"$UUID$pid\""
    done
    targetJson=$targetJson"]"

	echo " TARGET JSON: $targetJson" >> $HTTPLOG

	res=$(python3 ../common/compare_json.py "$targetJson" "$body")

	if [ $res -ne 0 ]; then
		__log_test_fail_body
		return 1
	fi

	__log_test_pass
	return 0
}


# Controller API Test function: getA1PolicyType
# arg: <response-code> OSC <ric-id> <policy-type-id> [<policy-type-file>]
# (Function for test scripts)
controller_api_get_A1_policy_type() {
	__log_test_start $@

    paramError=1
    if [ $# -gt 3 ] && [ $2 == "OSC" ]; then
        url="$RIC_SIM_HTTPX://$3:$RIC_SIM_PORT/a1-p/policytypes/$4"
		paramError=0
	fi

    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> OSC <ric-id> <policy-type-id> [<policy-type-file>]" $@
		return 1
	fi

    res=$(__do_curl_to_controller getA1PolicyType "$url")
    retcode=$?
    status=${res:${#res}-3}

    if [ $retcode -ne 0 ]; then
		__log_test_fail_status_code $1 $retcode "(likely remote server error)"
		return 1
	fi

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
    body=${res:0:${#res}-3}

	if [ $# -eq 5 ]; then

		body=${res:0:${#res}-3}

		targetJson=$(< $5)
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

# Controller API Test function: deleteA1Policy
# arg: <response-code> (STD <ric-id> <policy-id>) | (OSC <ric-id> <policy-type-id> <policy-id>)
# (Function for test scripts)
controller_api_delete_A1_policy() {
	__log_test_start $@

    paramError=1
    if [ $# -eq 5 ] && [ $2 == "OSC" ]; then
        url="$RIC_SIM_HTTPX://$3:$RIC_SIM_PORT/a1-p/policytypes/$4/policies/$UUID$5"
		paramError=0
    elif [ $# -eq 4 ] && [ $2 == "STD" ]; then
        url="$RIC_SIM_HTTPX://$3:$RIC_SIM_PORT/A1-P/v1/policies/$UUID$4"
        paramError=0
	fi

    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> (STD <ric-id> <policy-id>) | (OSC <ric-id> <policy-type-id> <policy-id>)" $@
		return 1
	fi

    res=$(__do_curl_to_controller deleteA1Policy "$url")
    retcode=$?
    status=${res:${#res}-3}

    if [ $retcode -ne 0 ]; then
		__log_test_fail_status_code $1 $retcode "(likely remote server error)"
		return 1
	fi

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# Controller API Test function: putA1Policy
# arg: <response-code> (STD <ric-id> <policy-id> <template-file> ) | (OSC <ric-id> <policy-type-id> <policy-id> <template-file>)
# (Function for test scripts)
controller_api_put_A1_policy() {
	__log_test_start $@

    paramError=1
    if [ $# -eq 6 ] && [ $2 == "OSC" ]; then
        url="$RIC_SIM_HTTPX://$3:$RIC_SIM_PORT/a1-p/policytypes/$4/policies/$UUID$5"
        body=$(sed 's/XXX/'${5}'/g' $6)

		paramError=0
    elif [ $# -eq 5 ] && [ $2 == "STD" ]; then
        url="$RIC_SIM_HTTPX://$3:$RIC_SIM_PORT/A1-P/v1/policies/$UUID$4"
        body=$(sed 's/XXX/'${4}'/g' $5)
        paramError=0
	fi

    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> (STD <ric-id> <policy-id>) | (OSC <ric-id> <policy-type-id> <policy-id>)" $@
		return 1
	fi

    res=$(__do_curl_to_controller putA1Policy "$url" "$body")
    retcode=$?
    status=${res:${#res}-3}

    if [ $retcode -ne 0 ]; then
		__log_test_fail_status_code $1 $retcode "(likely remote server error)"
		return 1
	fi

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}


# Controller API Test function: getA1PolicyStatus
# arg: <response-code> (STD <ric-id> <policy-id> <enforce-status> [<reason>]) | (OSC <ric-id> <policy-type-id> <policy-id> <instance-status> <has-been-deleted>)
# (Function for test scripts)
controller_api_get_A1_policy_status() {
	__log_test_start $@

    targetJson=""
    paramError=1
    if [ $# -ge 5 ] && [ $2 == "OSC" ]; then
        url="$RIC_SIM_HTTPX://$3:$RIC_SIM_PORT/a1-p/policytypes/$4/policies/$UUID$5/status"
        if [ $# -gt 5 ]; then
            targetJson="{\"instance_status\":\"$6\""
            targetJson=$targetJson",\"has_been_deleted\":\"$7\""
            targetJson=$targetJson",\"created_at\":\"????\"}"
        fi
		paramError=0
    elif [ $# -ge 4 ] && [ $2 == "STD" ]; then
        url="$RIC_SIM_HTTPX://$3:$RIC_SIM_PORT/A1-P/v1/policies/$UUID$4/status"
        if [ $# -gt 4 ]; then
            targetJson="{\"enforceStatus\":\"$5\""
            if [ $# -eq 6 ]; then
                targetJson=$targetJson",\"reason\":\"$6\""
            fi
            targetJson=$targetJson"}"
        fi
        paramError=0
	fi

    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> (STD <ric-id> <policy-id> <enforce-status> [<reason>]) | (OSC <ric-id> <policy-type-id> <policy-id> <instance-status> <has-been-deleted>)" $@
		return 1
	fi

    res=$(__do_curl_to_controller getA1PolicyStatus "$url")
    retcode=$?
    status=${res:${#res}-3}

    if [ $retcode -ne 0 ]; then
		__log_test_fail_status_code $1 $retcode "(likely remote server error)"
		return 1
	fi

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ ! -z "$targetJson" ]; then

		body=${res:0:${#res}-3}
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