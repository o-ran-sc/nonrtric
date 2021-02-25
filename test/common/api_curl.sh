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

# Generic function to query the agent/ECS via the REST or DMAAP interface.
# Used by all other agent/ECS api test functions
# If operation sufffix is '_BATCH' the the send and get response is split in two sequences,
# one for sending the requests and one for receiving the response
# but only when using the DMAAP interface
# REST or DMAAP is controlled of the base url of $XX_ADAPTER
# arg: (PA|ECS|CR|RC GET|PUT|POST|DELETE|GET_BATCH|PUT_BATCH|POST_BATCH|DELETE_BATCH <url>|<correlation-id> [<file>]) | (PA|ECS RESPONSE <correlation-id>)
# (Not for test scripts)
__do_curl_to_api() {
	TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
    echo " (${BASH_LINENO[0]}) - ${TIMESTAMP}: ${FUNCNAME[0]}" $@ >> $HTTPLOG
	proxyflag=""
	if [ $RUNMODE == "KUBE" ]; then
		if [ ! -z "$CLUSTER_KUBE_PROXY_NODEPORT" ]; then
			proxyflag=" --proxy http://localhost:$CLUSTER_KUBE_PROXY_NODEPORT"
		fi
	fi
	paramError=0
	input_url=$3
    if [ $# -gt 0 ]; then
        if [ $1 == "PA" ]; then
			__ADAPTER=$PA_ADAPTER
			__ADAPTER_TYPE=$PA_ADAPTER_TYPE
            __RETRY_CODES=$AGENT_RETRY_CODES
			if [ $PMS_VERSION != "V1" ]; then
				input_url=$PMS_API_PREFIX$3
			fi
        elif [ $1 == "ECS" ]; then
			__ADAPTER=$ECS_ADAPTER
			__ADAPTER_TYPE=$ECS_ADAPTER_TYPE
            __RETRY_CODES=$ECS_RETRY_CODES
		elif [ $1 == "CR" ]; then
			__ADAPTER=$CR_ADAPTER
			__ADAPTER_TYPE=$CR_ADAPTER_TYPE
            __RETRY_CODES=""
		elif [ $1 == "RC" ]; then
			__ADAPTER=$RC_ADAPTER
			__ADAPTER_TYPE=$RC_ADAPTER_TYPE
            __RETRY_CODES=""
        elif [ $1 == "NGW" ]; then
			__ADAPTER=$NGW_ADAPTER
			__ADAPTER_TYPE=$NGW_ADAPTER_TYPE
            __RETRY_CODES=""
        else
            paramError=1
        fi
		if [ $__ADAPTER_TYPE == "MR-HTTP" ]; then
			__ADAPTER=$MR_ADAPTER_HTTP
		fi
		if [ $__ADAPTER_TYPE == "MR-HTTPS" ]; then
			__ADAPTER=$MR_ADAPTER_HTTPS
		fi
    fi
    if [ $# -lt 3 ] || [ $# -gt 4 ]; then
		paramError=1
    else
		timeout=""
		oper=""
		file=''
		httpcode=" -sw %{http_code}"
		accept=''
		content=''
		batch=0
		if [[ $2 == *"_BATCH" ]]; then
			batch=1
		fi
		if [ $# -gt 3 ]; then
			content=" -H Content-Type:application/json"
		fi
		if [ $2 == "GET" ] || [ $2 == "GET_BATCH" ]; then
			oper="GET"
			if [ $# -ne 3 ]; then
				paramError=1
			fi
		elif [ $2 == "PUT" ] || [ $2 == "PUT_BATCH" ]; then
			oper="PUT"
			if [ $# -eq 4 ]; then
				file=" --data-binary @$4"
			fi
			accept=" -H accept:application/json"
		elif [ $2 == "POST" ] || [ $2 == "POST_BATCH" ]; then
			oper="POST"
			accept=" -H accept:*/*"
			if [ $# -ne 3 ]; then
				paramError=1
			fi
		elif [ $2 == "DELETE" ] || [ $2 == "DELETE_BATCH" ]; then
			oper="DELETE"
			if [ $# -ne 3 ]; then
				paramError=1
			fi
		elif [ $2 == "RESPONSE" ]; then
			oper="RESPONSE"
			if [ $# -ne 3 ]; then
				paramError=1
			fi
			#if [ $__ADAPTER == $__RESTBASE ] || [ $__ADAPTER == $__RESTBASE_SECURE ]; then
			if [ $__ADAPTER_TYPE == "REST" ]; then
				paramError=1
			fi
		else
			paramError=1
		fi
	fi

    if [ $paramError -eq 1 ]; then
		((RES_CONF_FAIL++))
        echo "-Incorrect number of parameters to __do_curl_to_api " $@ >> $HTTPLOG
        echo "-Expected: (PA|ECS GET|PUT|POST|DELETE|GET_BATCH|PUT_BATCH|POST_BATCH|DELETE_BATCH <url> [<file>]) | (PA|ECS RESPONSE <correlation-id>)" >> $HTTPLOG
        echo "-Returning response 000" >> $HTTPLOG
        echo "-000"
        return 1
    fi

    #if [ $__ADAPTER == $__RESTBASE ] || [ $__ADAPTER == $__RESTBASE_SECURE ]; then
	if [ $__ADAPTER_TYPE == "REST" ]; then
        url=" "${__ADAPTER}${input_url}
        oper=" -X "$oper
        curlString="curl -k $proxyflag "${oper}${timeout}${httpcode}${accept}${content}${url}${file}
        echo " CMD: "$curlString >> $HTTPLOG
		if [ $# -eq 4 ]; then
			echo " FILE: $(<$4)" >> $HTTPLOG
		fi

		# Do retry for configured response codes, otherwise only one attempt
		maxretries=5
		while [ $maxretries -ge 0 ]; do

			let maxretries=maxretries-1
			res=$($curlString)
			retcode=$?
			if [ $retcode -ne 0 ]; then
				echo " RETCODE: "$retcode >> $HTTPLOG
				echo "000"
				return 1
			fi
			retry=0
			echo " RESP: "$res >> $HTTPLOG
			status=${res:${#res}-3}
			if [ ! -z "${__RETRY_CODES}" ]; then
				for retrycode in $__RETRY_CODES; do
					if [ $retrycode -eq $status ]; then
						echo -e $RED" Retrying (according to set codes for retry), got status $status....."$ERED  >> $HTTPLOG
						sleep 1
						retry=1
					fi
				done
			fi
			if [ $retry -eq 0 ]; then
				maxretries=-1
			fi
		done
        echo $res
        return 0
    else
		if [ $oper != "RESPONSE" ]; then
			requestUrl=$input_url
			if [ $2 == "PUT" ] && [ $# -eq 4 ]; then
				payload="$(cat $4 | tr -d '\n' | tr -d ' ' )"
				echo "payload: "$payload >> $HTTPLOG
				file=" --data-binary "$payload
			elif [ $# -eq 4 ]; then
				echo " FILE: $(cat $4)" >> $HTTPLOG
			fi
			#urlencode the request url since it will be carried by send-request url
			requestUrl=$(python3 -c "from __future__ import print_function; import urllib.parse, sys; print(urllib.parse.quote(sys.argv[1]))"  "$input_url")
			url=" "${__ADAPTER}"/send-request?url="${requestUrl}"&operation="${oper}
			curlString="curl -k $proxyflag -X POST${timeout}${httpcode}${content}${url}${file}"
			echo " CMD: "$curlString >> $HTTPLOG
			res=$($curlString)
			retcode=$?
			if [ $retcode -ne 0 ]; then
				echo " RETCODE: "$retcode >> $HTTPLOG
				echo "000"
				return 1
			fi
			echo " RESP: "$res >> $HTTPLOG
			status=${res:${#res}-3}
			if [ $status -ne 200 ]; then
				echo "000"
				return 1
			fi
			cid=${res:0:${#res}-3}
			if [[ $batch -eq 1 ]]; then
				echo $cid"200"
				return 0
			fi
		fi
		if [ $oper == "RESPONSE" ] || [ $batch -eq 0 ]; then
			if [ $oper == "RESPONSE" ]; then
				cid=$3
			fi
			url=" "${__ADAPTER}"/receive-response?correlationid="${cid}
			curlString="curl -k $proxyflag  -X GET"${timeout}${httpcode}${url}
			echo " CMD: "$curlString >> $HTTPLOG
			res=$($curlString)
			retcode=$?
			if [ $retcode -ne 0 ]; then
				echo " RETCODE: "$retcode >> $HTTPLOG
				echo "000"
				return 1
			fi
			echo " RESP: "$res >> $HTTPLOG
			status=${res:${#res}-3}
			TS=$SECONDS
			# wait of the reply from the agent/ECS...
			while [ $status -eq 204 ]; do
				if [ $(($SECONDS - $TS)) -gt 90 ]; then
					echo " RETCODE: (timeout after 90s)" >> $HTTPLOG
					echo "000"
					return 1
				fi
				sleep 0.01
				echo " CMD: "$curlString >> $HTTPLOG
				res=$($curlString)
				if [ $retcode -ne 0 ]; then
					echo " RETCODE: "$retcode >> $HTTPLOG
					echo "000"
					return 1
				fi
				echo " RESP: "$res >> $HTTPLOG
				status=${res:${#res}-3}
			done
			if [ $status -eq 200 ]; then
				body=${res:0:${#res}-3}
				echo $body
				return 0
			fi
			echo "Status not 200, returning response 000" >> $HTTPLOG
			echo "0000"
			return 1
		fi
    fi
}