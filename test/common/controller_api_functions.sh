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

# This is a script that contains container/service management functions and test functions for A1 Controller API

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__SDNC_imagesetup() {

	sdnc_suffix_tag=$1

	for oia_name in $ONAP_IMAGES_APP_NAMES; do
		if [ "$oia_name" == "SDNC" ]; then
			sdnc_suffix_tag="REMOTE_RELEASE_ONAP"
		fi
	done
	__check_and_create_image_var SDNC "SDNC_A1_CONTROLLER_IMAGE" "SDNC_A1_CONTROLLER_IMAGE_BASE" "SDNC_A1_CONTROLLER_IMAGE_TAG" $sdnc_suffix_tag "$SDNC_DISPLAY_NAME"
	__check_and_create_image_var SDNC "SDNC_DB_IMAGE" "SDNC_DB_IMAGE_BASE" "SDNC_DB_IMAGE_TAG" REMOTE_PROXY "SDNC DB"

}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__SDNC_imagepull() {
	__check_and_pull_image $1 "$SDNC_DISPLAY_NAME" $SDNC_APP_NAME SDNC_A1_CONTROLLER_IMAGE
	__check_and_pull_image $2 "SDNC DB" $SDNC_APP_NAME SDNC_DB_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__SDNC_imagebuild() {
	echo -e $RED" Image for app SDNC shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__SDNC_image_data() {
	echo -e "$SDNC_DISPLAY_NAME\t$(docker images --format $1 $SDNC_A1_CONTROLLER_IMAGE)" >>   $2
	if [ ! -z "$SDNC_A1_CONTROLLER_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $SDNC_A1_CONTROLLER_IMAGE_SOURCE)" >>   $2
	fi
	echo -e "SDNC DB\t$(docker images --format $1 $SDNC_DB_IMAGE)" >>   $2
	if [ ! -z "$SDNC_DB_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $SDNC_DB_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__SDNC_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest SDNC
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for prestarted apps not managed by the test script.
__SDNC_kube_scale_zero_and_wait() {
	echo -e " SDNC replicas kept as is"
}

# Delete all kube resouces for the app
# This function is called for apps managed by the test script.
__SDNC_kube_delete_all() {
	__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest SDNC
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prexix>
__SDNC_store_docker_logs() {
	docker exec -t $SDNC_APP_NAME cat $SDNC_KARAF_LOG> $1$2_SDNC_karaf.log 2>&1
}

#######################################################


SDNC_HTTPX="http"
SDNC_HOST_NAME=$LOCALHOST_NAME
SDNC_PATH=$SDNC_HTTPX"://"$SDNC_HOST_NAME":"$SDNC_EXTERNAL_PORT
SDNC_API_PATH=$SDNC_HTTPX"://"$SDNC_USER":"$SDNC_PWD"@"$SDNC_HOST_NAME":"$SDNC_EXTERNAL_PORT$SDNC_API_URL
#Docker/Kube internal path
if [ $RUNMODE == "KUBE" ]; then
	SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME":"$SDNC_EXTERNAL_PORT
    #presume correct
	SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME"."$KUBE_NONRTRIC_NAMESPACE":"$SDNC_EXTERNAL_PORT
	#test
	#SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME"."$KUBE_NONRTRIC_NAMESPACE":"$SDNC_EXTERNAL_PORT
else
	SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME":"$SDNC_INTERNAL_PORT
fi

use_sdnc_http() {
	echo -e $BOLD"SDNC NB protocol setting"$EBOLD
	echo -e " Using $BOLD http $EBOLD towards SDNC"
	SDNC_HTTPX="http"
	SDNC_PATH=$SDNC_HTTPX"://"$SDNC_HOST_NAME":"$SDNC_EXTERNAL_PORT
	SDNC_API_PATH=$SDNC_HTTPX"://"$SDNC_USER":"$SDNC_PWD"@"$SDNC_HOST_NAME":"$SDNC_EXTERNAL_PORT$SDNC_API_URL
	if [ $RUNMODE == "KUBE" ]; then
		#presume correct
		SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME"."$KUBE_NONRTRIC_NAMESPACE":"$SDNC_EXTERNAL_PORT
		#test
		#SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME":"$SDNC_EXTERNAL_PORT
	else
		SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME":"$SDNC_INTERNAL_PORT
	fi
	echo ""
}

use_sdnc_https() {
	echo -e $BOLD"SDNC NB protocol setting"$EBOLD
	echo -e " Using $BOLD https $EBOLD towards SDNC"
	SDNC_HTTPX="https"
	SDNC_PATH=$SDNC_HTTPX"://"$SDNC_HOST_NAME":"$SDNC_EXTERNAL_SECURE_PORT
	SDNC_API_PATH=$SDNC_HTTPX"://"$SDNC_USER":"$SDNC_PWD"@"$SDNC_HOST_NAME":"$SDNC_EXTERNAL_SECURE_PORT$SDNC_API_URL
	if [ $RUNMODE == "KUBE" ]; then
		#presume correct
		SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME"."$KUBE_NONRTRIC_NAMESPACE":"$SDNC_EXTERNAL_SECURE_PORT
		#test
		#SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME":"$SDNC_EXTERNAL_SECURE_PORT
	else
		SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME":"$SDNC_INTERNAL_SECURE_PORT
	fi
	echo ""
}

##################
### SDNC functions
##################

# Start the SDNC A1 Controller
# args: -
# (Function for test scripts)
start_sdnc() {

	echo -e $BOLD"Starting $SDNC_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "SDNC"
		retcode_i=$?

		# Check if app shall only be used by the testscipt
		__check_prestarted_image "SDNC"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $SDNC_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $SDNC_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $SDNC_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $SDNC_APP_NAME will not be started"$ERED
			exit
		fi


		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $SDNC_APP_NAME deployment and service"
			echo " Setting SDNC replicas=1"
			__kube_scale deployment $SDNC_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi

				# Check if app shall be fully managed by the test script
		if [ $retcode_i -eq 0 ]; then

			echo -e " Creating $SDNC_APP_NAME app and expose service"

			#Check if nonrtric namespace exists, if not create it
			__kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

			export KUBE_NONRTRIC_NAMESPACE
			export SDNC_APP_NAME
			export SDNC_A1_CONTROLLER_IMAGE
			export SDNC_INTERNAL_PORT
			export SDNC_EXTERNAL_PORT
			export SDNC_INTERNAL_SECURE_PORT
			export SDNC_EXTERNAL_SECURE_PORT
			export SDNC_A1_TRUSTSTORE_PASSWORD
			export SDNC_DB_APP_NAME
			export SDNC_DB_IMAGE
			export SDNC_USER
			export SDNC_PWD

			# Create service
			input_yaml=$SIM_GROUP"/"$SDNC_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/sdnc_svc.yaml
			__kube_create_instance service $SDNC_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$SDNC_COMPOSE_DIR"/"$SDNC_KUBE_APP_FILE
			output_yaml=$PWD/tmp/sdnc_app.yaml
			__kube_create_instance app $SDNC_APP_NAME $input_yaml $output_yaml

		fi

        echo " Retrieving host and ports for service..."
		SDNC_HOST_NAME=$(__kube_get_service_host $SDNC_APP_NAME $KUBE_NONRTRIC_NAMESPACE)
		SDNC_EXTERNAL_PORT=$(__kube_get_service_port $SDNC_APP_NAME $KUBE_NONRTRIC_NAMESPACE "http")
		SDNC_EXTERNAL_SECURE_PORT=$(__kube_get_service_port $SDNC_APP_NAME $KUBE_NONRTRIC_NAMESPACE "https")

		echo " Host IP, http port, https port: $SDNC_HOST_NAME $SDNC_EXTERNAL_PORT $SDNC_EXTERNAL_SECURE_PORT"

        if [ $SDNC_HTTPX == "http" ]; then
			SDNC_PATH=$SDNC_HTTPX"://"$SDNC_HOST_NAME":"$SDNC_EXTERNAL_PORT
			SDNC_API_PATH=$SDNC_HTTPX"://"$SDNC_USER":"$SDNC_PWD"@"$SDNC_HOST_NAME":"$SDNC_EXTERNAL_PORT$SDNC_API_URL
            #presume correct
			SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME"."$KUBE_NONRTRIC_NAMESPACE":"$SDNC_EXTERNAL_PORT
			#test
			#SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME":"$SDNC_EXTERNAL_PORT
		else
			SDNC_PATH=$SDNC_HTTPX"://"$SDNC_HOST_NAME":"$SDNC_EXTERNAL_SECURE_PORT
			SDNC_API_PATH=$SDNC_HTTPX"://"$SDNC_USER":"$SDNC_PWD"@"$SDNC_HOST_NAME":"$SDNC_EXTERNAL_SECURE_PORT$SDNC_API_URL
            #presume correct
			SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME"."$KUBE_NONRTRIC_NAMESPACE":"$SDNC_EXTERNAL_SECURE_PORT
			#test
			#SDNC_SERVICE_PATH=$SDNC_HTTPX"://"$SDNC_APP_NAME":"$SDNC_EXTERNAL_SECURE_PORT
		fi

		__check_service_start $SDNC_APP_NAME $SDNC_PATH$SDNC_ALIVE_URL
	else

		__check_included_image 'SDNC'
		if [ $? -eq 1 ]; then
			echo -e $RED"The SDNC A1 Controller app is not included in this test script"$ERED
			echo -e $RED"The Policy Agent will not be started"$ERED
			exit
		fi

		export SDNC_DB_APP_NAME
        export SDNC_APP_NAME
        export SDNC_INTERNAL_PORT
        export SDNC_EXTERNAL_PORT
        export SDNC_INTERNAL_SECURE_PORT
        export SDNC_EXTERNAL_SECURE_PORT
        export SDNC_A1_TRUSTSTORE_PASSWORD
        export DOCKER_SIM_NWNAME
		export SDNC_DISPLAY_NAME
		export SDNC_USER
		export SDNC_PWD

		__start_container $SDNC_COMPOSE_DIR $SDNC_COMPOSE_FILE NODOCKERARGS 1 $SDNC_APP_NAME

		__check_service_start $SDNC_APP_NAME $SDNC_PATH$SDNC_ALIVE_URL
	fi
    echo ""
    return 0
}

# Check the agent logs for WARNINGs and ERRORs
# args: -
# (Function for test scripts)
check_sdnc_logs() {
	__check_container_logs "SDNC A1 Controller" $SDNC_APP_NAME $SDNC_KARAF_LOG WARN ERROR
}

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
	proxyflag=""
	if [ $RUNMODE == "KUBE" ]; then
		if [ ! -z "$KUBE_PROXY_PATH" ]; then
			if [ $KUBE_PROXY_HTTPX == "http" ]; then
				proxyflag=" --proxy $KUBE_PROXY_PATH"
			else
				proxyflag=" --proxy-insecure --proxy $KUBE_PROXY_PATH"
			fi
		fi
	fi
    curlString="curl -skw %{http_code} $proxyflag -X POST $SDNC_API_PATH$1 -H accept:application/json -H Content-Type:application/json --data-binary @$payload"
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
    res=$(python3 ../common/extract_sdnc_reply.py $SDNC_RESPONSE_JSON_KEY $reply)
    echo "  EXTRACED BODY+CODE: "$res >> $HTTPLOG
    echo "$res"
    return 0
}

# Controller API Test function: getA1Policy (return ids only)
# arg: <response-code> (OSC <ric-id> <policy-type-id> [ <policy-id> [<policy-id>]* ]) | ( STD <ric-id> [ <policy-id> [<policy-id>]* ] )
# (Function for test scripts)
controller_api_get_A1_policy_ids() {
	__log_test_start $@

	ric_id=$3
	if [ $RUNMODE == "KUBE" ]; then
		ric_id=$(get_kube_sim_host $3)
	fi
    paramError=1
    if [ $# -gt 3 ] && [ $2 == "OSC" ]; then
        url="$RIC_SIM_HTTPX://$ric_id:$RIC_SIM_PORT/a1-p/policytypes/$4/policies"
		paramError=0
    elif [ $# -gt 2 ] && [ $2 == "STD" ]; then
        url="$RIC_SIM_HTTPX://$ric_id:$RIC_SIM_PORT/A1-P/v1/policies"
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

	ric_id=$3
	if [ $RUNMODE == "KUBE" ]; then
		ric_id=$(get_kube_sim_host $3)
	fi
    paramError=1
    if [ $# -gt 3 ] && [ $2 == "OSC" ]; then
        url="$RIC_SIM_HTTPX://$ric_id:$RIC_SIM_PORT/a1-p/policytypes/$4"
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

	ric_id=$3
	if [ $RUNMODE == "KUBE" ]; then
		ric_id=$(get_kube_sim_host $3)
	fi
    paramError=1
    if [ $# -eq 5 ] && [ $2 == "OSC" ]; then
        url="$RIC_SIM_HTTPX://$ric_id:$RIC_SIM_PORT/a1-p/policytypes/$4/policies/$UUID$5"
		paramError=0
    elif [ $# -eq 4 ] && [ $2 == "STD" ]; then
        url="$RIC_SIM_HTTPX://$ric_id:$RIC_SIM_PORT/A1-P/v1/policies/$UUID$4"
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

	ric_id=$3
	if [ $RUNMODE == "KUBE" ]; then
		ric_id=$(get_kube_sim_host $3)
	fi
    paramError=1
    if [ $# -eq 6 ] && [ $2 == "OSC" ]; then
        url="$RIC_SIM_HTTPX://$ric_id:$RIC_SIM_PORT/a1-p/policytypes/$4/policies/$UUID$5"
        body=$(sed 's/XXX/'${5}'/g' $6)

		paramError=0
    elif [ $# -eq 5 ] && [ $2 == "STD" ]; then
        url="$RIC_SIM_HTTPX://$ric_id:$RIC_SIM_PORT/A1-P/v1/policies/$UUID$4"
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

	ric_id=$3
	if [ $RUNMODE == "KUBE" ]; then
		ric_id=$(get_kube_sim_host $3)
	fi
    targetJson=""
    paramError=1
    if [ $# -ge 5 ] && [ $2 == "OSC" ]; then
        url="$RIC_SIM_HTTPX://$ric_id:$RIC_SIM_PORT/a1-p/policytypes/$4/policies/$UUID$5/status"
        if [ $# -gt 5 ]; then
            targetJson="{\"instance_status\":\"$6\""
            targetJson=$targetJson",\"has_been_deleted\":\"$7\""
            targetJson=$targetJson",\"created_at\":\"????\"}"
        fi
		paramError=0
    elif [ $# -ge 4 ] && [ $2 == "STD" ]; then
        url="$RIC_SIM_HTTPX://$ric_id:$RIC_SIM_PORT/A1-P/v1/policies/$UUID$4/status"
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