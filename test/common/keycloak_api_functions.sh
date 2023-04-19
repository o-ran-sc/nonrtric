#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021 Nordix Foundation. All rights reserved.
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

# This is a script that contains container/service management functions and test functions for Keycloak


################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__KEYCLOAK_imagesetup() {
	__check_and_create_image_var KEYCLOAK "KEYCLOAK_IMAGE" "KEYCLOAK_IMAGE_BASE" "KEYCLOAK_IMAGE_TAG" REMOTE_OTHER "$KEYCLOAK_DISPLAY_NAME" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__KEYCLOAK_imagepull() {
	__check_and_pull_image $2 "$KEYCLOAK_DISPLAY_NAME" $KEYCLOAK_APP_NAME KEYCLOAK_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__KEYCLOAK_imagebuild() {
	echo -e $RED" Image for app KEYCLOAK shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__KEYCLOAK_image_data() {
	echo -e "$KEYCLOAK_DISPLAY_NAME\t$(docker images --format $1 $KEYCLOAK_IMAGE)" >>   $2
	if [ ! -z "$KEYCLOAK_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $KEYCLOAK_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__KEYCLOAK_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_KEYCLOAK_NAMESPACE autotest KEYCLOAK
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__KEYCLOAK_kube_scale_zero_and_wait() {
	echo -e $RED" KEYCLOAK app is not scaled in this state"$ERED
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__KEYCLOAK_kube_delete_all() {
	__kube_delete_all_resources $KUBE_KEYCLOAK_NAMESPACE autotest KEYCLOAK
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__KEYCLOAK_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=KEYCLOAK" -n $KUBE_KEYCLOAK_NAMESPACE --tail=-1 > $1$2_keycloak.log 2>&1
	else
		docker logs $KEYCLOAK_APP_NAME > $1$2_keycloak.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__KEYCLOAK_initial_setup() {
	use_keycloak_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__KEYCLOAK_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "KEYCLOAK $KEYCLOAK_APP_NAME $KUBE_KEYCLOAK_NAMESPACE"
	else
		echo "KEYCLOAK $KEYCLOAK_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__KEYCLOAK_test_requirements() {
	which jq > /dev/null
	if [ $? -ne 0 ]; then
		echo $RED" 'jq' is required to run tests. Pls install 'jq'"
		return 1
	fi
}

#######################################################

# Set http as the protocol to use for all communication to the Keycloak
# args: -
# (Function for test scripts)
use_keycloak_http() {
	__keycloak_set_protocoll "http" $KEYCLOAK_INTERNAL_PORT $KEYCLOAK_EXTERNAL_PORT
}

# Set https as the protocol to use for all communication to the Keycloak
# args: -
# (Function for test scripts)
use_keycloak_https() {
	__keycloak_set_protocoll "https" $KEYCLOAK_INTERNAL_SECURE_PORT $KEYCLOAK_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__keycloak_set_protocoll() {
	echo -e $BOLD"$KEYCLOAK_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $KEYCLOAK_DISPLAY_NAME"

	## Access to Keycloak

	KEYCLOAK_SERVICE_PATH=$1"://"$KEYCLOAK_APP_NAME":"$2  # docker access, container->container and script->container via proxy
	KEYCLOAK_SERVICE_PORT=$2
	KEYCLOAK_SERVICE_HOST=$KEYCLOAK_APP_NAME
	KEYCLOAK_ISSUER_PATH=$1"://"$KEYCLOAK_APP_NAME
	if [ $RUNMODE == "KUBE" ]; then
		KEYCLOAK_SERVICE_PATH=$1"://"$KEYCLOAK_APP_NAME.$KUBE_KEYCLOAK_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
		KEYCLOAK_SERVICE_PORT=$3
		KEYCLOAK_SERVICE_HOST=$KEYCLOAK_APP_NAME.$KUBE_KEYCLOAK_NAMESPACE
		KEYCLOAK_ISSUER_PATH=$1"://"$KEYCLOAK_APP_NAME.$KUBE_KEYCLOAK_NAMESPACE
	fi
	KEYCLOAK_SERVICE_HTTPX=$1

	echo ""
}

### Admin API functions Keycloak

###########################
### Keycloak functions
###########################

# Export env vars for config files, docker compose and kube resources
# args:
__keycloak_export_vars() {
	export KEYCLOAK_APP_NAME
	export KEYCLOAK_DISPLAY_NAME

	export DOCKER_SIM_NWNAME
	export KUBE_KEYCLOAK_NAMESPACE

	export KEYCLOAK_IMAGE
	export KEYCLOAK_INTERNAL_PORT
	export KEYCLOAK_EXTERNAL_PORT

	export KEYCLOAK_ADMIN_USER
	export KEYCLOAK_ADMIN_PWD
	export KEYCLOAK_KC_PROXY
}


# Start the Keycloak in the simulator group
# args: -
# (Function for test scripts)
start_keycloak() {

	echo -e $BOLD"Starting $KEYCLOAK_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "KEYCLOAK"
		retcode_i=$?

		# Check if app shall only be used by the test script
		__check_prestarted_image "KEYCLOAK"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $KEYCLOAK_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $KEYCLOAK_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $KEYCLOAK_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $KEYCLOAK_APP_NAME will not be started"$ERED
			exit
		fi

		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $KEYCLOAK_APP_NAME deployment and service"
			echo " Setting keycloak replicas=1"
			__kube_scale deployment $KEYCLOAK_APP_NAME $KUBE_KEYCLOAK_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then
			echo -e " Creating $KEYCLOAK_APP_NAME deployment and service"

            __kube_create_namespace $KUBE_KEYCLOAK_NAMESPACE

			__keycloak_export_vars

			# Create service and app
			input_yaml=$SIM_GROUP"/"$KEYCLOAK_COMPOSE_DIR"/"svc_app.yaml
			output_yaml=$PWD/tmp/keycloak_svc_app.yaml
			__kube_create_instance "service/app" $KEYCLOAK_APP_NAME $input_yaml $output_yaml

		fi

		__check_service_start $KEYCLOAK_APP_NAME $KEYCLOAK_SERVICE_PATH$KEYCLOAK_ALIVE_URL
	else

		# Check if docker app shall be fully managed by the test script
		__check_included_image 'KEYCLOAK'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Keycloak app is not included as managed in this test script"$ERED
			echo -e $RED"The Keycloak will not be started"$ERED
			exit
		fi

		__keycloak_export_vars

		__start_container $KEYCLOAK_COMPOSE_DIR "" NODOCKERARGS 1 $KEYCLOAK_APP_NAME

        __check_service_start $KEYCLOAK_APP_NAME $KEYCLOAK_SERVICE_PATH$KEYCLOAK_ALIVE_URL
	fi
    echo ""
    return 0
}

# Execute a curl cmd towards the keycloak and check the response code is 2XX.
# args: <curl-cmd-string>
# resp: <returned-payload> if return code is 0 otherwise <error-info>
__execute_curl_to_keycloak() {

	proxyflag=""
	if [ ! -z "$KUBE_PROXY_PATH" ]; then
		if [ $KUBE_PROXY_HTTPX == "http" ]; then
			proxyflag=" --proxy $KUBE_PROXY_PATH"
		else
			proxyflag=" --proxy-insecure --proxy $KUBE_PROXY_PATH"
		fi
	fi
	__cmd="curl -skw %{http_code} $proxyflag $1"
	echo " CMD: $__cmd" >> $HTTPLOG
	res=$($__cmd)
	echo " RESP: $res" >> $HTTPLOG
	retcode=$?
    if [ $retcode -ne 0 ]; then
        __log_conf_fail_general " Fatal error when executing curl, response: "$retcode
        echo "$res"
		return 1
    fi
    status=${res:${#res}-3}
	if [ $status -lt 200 ] && [ $status -gt 299 ]; then
		__log_conf_fail_status_code "2XX" $status
		echo "$res"
		return 1
	fi
	echo ${res:0:${#res}-3}
	return 0
}

# Execute a curl cmd towards the keycloak and check the response code is 2XX.
# args: <operation> <url> <token> <json>
# resp: <returned-payload> if return code is 0 otherwise <error-info>
__execute_curl_to_keycloak2() {
	proxyflag=""
	if [ ! -z "$KUBE_PROXY_PATH" ]; then
		if [ $KUBE_PROXY_HTTPX == "http" ]; then
			proxyflag=" --proxy $KUBE_PROXY_PATH"
		else
			proxyflag=" --proxy-insecure --proxy $KUBE_PROXY_PATH"
		fi
	fi
	if [ $1 == "POST" ]; then
		if [ $# -eq 3 ]; then
			echo  curl -X POST -skw %{http_code} $proxyflag "$2" -H "Authorization: Bearer $3" >> $HTTPLOG
			res=$(curl -X POST -skw %{http_code} $proxyflag "$2" -H "Authorization: Bearer $3")
			retcode=$?
		else
			echo  curl -X POST -skw %{http_code} $proxyflag "$2" -H "Content-Type: application/json" -H "Authorization: Bearer $3" --data-binary "$4" >> $HTTPLOG
			res=$(curl -X POST -skw %{http_code} $proxyflag "$2" -H "Content-Type: application/json" -H "Authorization: Bearer $3" --data-binary "$4")
			retcode=$?
		fi
	elif [ $1 == "PUT" ]; then
		if [ $# -eq 3 ]; then
			echo  curl -X PUT -skw %{http_code} $proxyflag "$2" -H "Authorization: Bearer $3" >> $HTTPLOG
			res=$(curl -X PUT -skw %{http_code} $proxyflag "$2" -H "Authorization: Bearer $3")
			retcode=$?
		else
			echo  curl -X PUT -skw %{http_code} $proxyflag "$2" -H "Content-Type: application/json" -H "Authorization: Bearer $3" --data-binary "$4" >> $HTTPLOG
			res=$(curl -X PUT -skw %{http_code} $proxyflag "$2" -H "Content-Type: application/json" -H "Authorization: Bearer $3" --data-binary "$4")
			retcode=$?
		fi
	elif [ $1 == "GET" ]; then
		echo  curl -X GET -skw %{http_code} $proxyflag "$2" -H "Authorization: Bearer $3" >> $HTTPLOG
		res=$(curl -X GET -skw %{http_code} $proxyflag "$2" -H "Authorization: Bearer $3")
		retcode=$?
	fi
	echo " RESP: $res" >> $HTTPLOG
    if [ $retcode -ne 0 ]; then
        __log_conf_fail_general " Fatal error when executing curl, response: "$retcode
        echo "$res"
		return 1
    fi
    status=${res:${#res}-3}
	if [ $status -lt 200 ] && [ $status -gt 299 ]; then
		__log_conf_fail_status_code "2XX" $status
		echo "$res"
		return 1
	fi
	echo ${res:0:${#res}-3}
	return 0
}

# Extract JWT access token from json structure
# args: <json>
__keycloak_decode_jwt() {
    echo $1 | jq -r .access_token | jq -R 'split(".") | .[1] | @base64d | fromjson'
	return 0
}

# Get the admin token to use for subsequent rest calls to keycloak
# args: -
keycloak_api_obtain_admin_token() {
	__log_conf_start $@
	__curl_string="-X POST $KEYCLOAK_SERVICE_PATH$KEYCLOAK_ADMIN_URL_PREFIX/protocol/openid-connect/token     -H Content-Type:application/x-www-form-urlencoded     -d username="$KEYCLOAK_ADMIN_USER" -d password="$KEYCLOAK_ADMIN_PWD" -d grant_type=password -d client_id="$KEYCLOAK_ADMIN_CLIENT
	__TMP_TOKEN=$(__execute_curl_to_keycloak "$__curl_string")
	if [ $? -ne 0 ]; then
        __log_conf_fail_general " Fatal error when executing curl, response: "$?
        return 1
	fi

	__KEYCLOAK_ADMIN_TOKEN=$(echo "$__TMP_TOKEN" | jq  -r '.access_token')
	if [ $? -ne 0 ]; then
        __log_conf_fail_general " Fatal error when extracting token, response: "$?
        return 1
	fi

	echo "Decoded token:" >> $HTTPLOG
	__keycloak_decode_jwt "$__TMP_TOKEN" >> $HTTPLOG

	__KEYCLOAK_ADMIN_TOKEN_EXP=$(echo "$__TMP_TOKEN" | jq  -r '.expires_in')
	if [ $? -ne 0 ]; then
        __log_conf_fail_general " Fatal error when extracting expiry time, response: "$?
        return 1
	fi
	echo " Admin token obtained. Expires in $__KEYCLOAK_ADMIN_TOKEN_EXP seconds"

	__log_conf_ok
	return 0
}

# Create a realm, name, enabled, expiry-time
# args: <realm-name> true|false <token-expiry>
keycloak_api_create_realm() {
	__log_conf_start $@
	__json='{"realm":"'$1'","enabled":'$2',"accessTokenLifespan":'$3'}'
	res=$(__execute_curl_to_keycloak2 POST "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX" "$__KEYCLOAK_ADMIN_TOKEN" "$__json")
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when creating realm, response: "$?
		return 1
	fi
	__log_conf_ok
	return 0
}

# Update a realm, name, enabled, expiry-time
# args: <realm-name> true|false <token-expiry>
keycloak_api_update_realm() {
	__log_conf_start $@
	__json='{"realm":"'$1'","enabled":'$2',"accessTokenLifespan":'$3'}'
	res=$(__execute_curl_to_keycloak2 PUT "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$1" "$__KEYCLOAK_ADMIN_TOKEN" "$__json")
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when updating realm, response: "$?
		return 1
	fi
	__log_conf_ok
	return 0
}

# Create a client
# args: <realm-name> <client-name>
keycloak_api_create_confidential_client() {
	__log_conf_start $@
	__json='{"clientId":"'$2'","publicClient":false,"serviceAccountsEnabled": true,"rootUrl":"https://example.com/example/","adminUrl":"https://example.com/example/"}'
	res=$(__execute_curl_to_keycloak2 POST "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$1/clients" "$__KEYCLOAK_ADMIN_TOKEN" "$__json")
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when ucreating client, response: "$?
		return 1
	fi
	__log_conf_ok
	return 0
}

__keycloak_api_get_client_id() {
	TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
	echo "(${BASH_LINENO[0]}) - ${TIMESTAMP}: ${FUNCNAME[0]}" $@ >> $HTTPLOG

	res=$(__execute_curl_to_keycloak2 GET "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$1/clients?clientId=$2" "$__KEYCLOAK_ADMIN_TOKEN")
	if [ $? -ne 0 ]; then
		return 1
	fi
	echo $res | jq -r '.[0].id'
	return 0
}

__keycloak_api_get_service_account_id() {
	TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
	echo "(${BASH_LINENO[0]}) - ${TIMESTAMP}: ${FUNCNAME[0]}" $@ >> $HTTPLOG

	res=$(__execute_curl_to_keycloak2 GET "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$1/clients/$2/service-account-user" "$__KEYCLOAK_ADMIN_TOKEN")
	if [ $? -ne 0 ]; then
		return 1
	fi
	echo $res | jq -r '.id'
	return 0
}

# Generate secret for client
# args: <realm-name> <client-name>
keycloak_api_generate_client_secret() {
	__log_conf_start $@
	__c_id=$(__keycloak_api_get_client_id $1 $2)
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when getting client id, response: "$?
		return 1
	fi
	res=$(__execute_curl_to_keycloak2 POST "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$1/clients/$__c_id/client-secret" "$__KEYCLOAK_ADMIN_TOKEN")
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when generating client secret, response: "$?
		return 1
	fi
	__c_sec=$(__execute_curl_to_keycloak2 GET "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$1/clients/$__c_id/client-secret" "$__KEYCLOAK_ADMIN_TOKEN")
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when getting client secret, response: "$?
		return 1
	fi
	__c_sec=$(echo $__c_sec | jq -r .value)
	echo " Client id    : $__c_id"
	echo " Client secret: $__c_sec"
	__log_conf_ok
	return 0
}

# Get secret for client
# args: <realm-name> <client-name>
keycloak_api_get_client_secret() {
	__log_conf_start $@
	__c_id=$(__keycloak_api_get_client_id $1 $2)
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when getting client id, response: "$?
		return 1
	fi
	__c_sec=$(__execute_curl_to_keycloak2 GET "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$1/clients/$__c_id/client-secret" "$__KEYCLOAK_ADMIN_TOKEN")
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when getting client secret, response: "$?
		return 1
	fi
	__c_sec=$(echo $__c_sec | jq -r .value)
	echo " Client id    : $__c_id"
	echo " Client secret: $__c_sec"
	__log_conf_ok
	return 0
}

# Create client roles
# args: <realm-name> <client-name> <role>+
keycloak_api_create_client_roles() {
	__log_conf_start $@
	__c_id=$(__keycloak_api_get_client_id $1 $2)
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when getting client id, response: "$?
		return 1
	fi
	__realm=$1
	shift; shift;
    while [ $# -gt 0 ]; do
		__json='{"name":"'$1'"}'
		res=$(__execute_curl_to_keycloak2 POST "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$__realm/clients/$__c_id/roles" "$__KEYCLOAK_ADMIN_TOKEN" "$__json")
		if [ $? -ne 0 ]; then
			__log_conf_fail_general " Fatal error when creating client role, response: "$?
			return 1
		fi
		shift
	done
	__log_conf_ok
	return 0
}

# Get client role id
# args: <realm-name> <service-account-name> <client-name> <role-name>
__get_client_available_role_id() {
	res=$(__execute_curl_to_keycloak2 GET "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$1/users/$2/role-mappings/clients/$3/available" "$__KEYCLOAK_ADMIN_TOKEN")
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when getting availiable client role id, response: "$?
		return 1
	fi
    __client_role_id=$(echo $res | jq  -r '.[] | select(.name=="'$4'") | .id ')
    echo $__client_role_id
    return 0
}

# Map roles to a client
# args: <realm-name> <client-name> <role>+
keycloak_api_map_client_roles() {
	__log_conf_start $@
	__c_id=$(__keycloak_api_get_client_id $1 $2)
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when getting client id, response: "$?
		return 1
	fi
	__sa_id=$(__keycloak_api_get_service_account_id $1 $__c_id)
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when getting service account id, response: "$?
		return 1
	fi
	__realm=$1
	shift; shift;
	__json="["
	__cntr=0
    while [ $# -gt 0 ]; do
        __client_role_id=$(__get_client_available_role_id $__realm $__sa_id $__c_id $1)
        if [ $? -ne 0 ]; then
			__log_conf_fail_general " Fatal error when getting client role id, response: "$?
			return 1
        fi
        __role='{"name":"'$1'","id":"'$__client_role_id'","composite": false,"clientRole": true}'
        if [ $__cntr -gt 0 ]; then
            __json=$__json","
        fi
        __json=$__json$__role
        let __cntr=__cntr+1
        shift
    done
    __json=$__json"]"

	res=$(__execute_curl_to_keycloak2 POST "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$__realm/users/$__sa_id/role-mappings/clients/$__c_id" "$__KEYCLOAK_ADMIN_TOKEN" "$__json")
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when mapping client roles, response: "$?
		return 1
	fi

	__log_conf_ok
	return 0
}

# Get a client token
# args: <realm-name> <client-name>
keycloak_api_get_client_token() {
	__log_conf_start $@
	__c_id=$(__keycloak_api_get_client_id $1 $2)
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when getting client id, response: "$?
		return 1
	fi
	__c_sec=$(__execute_curl_to_keycloak2 GET "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$1/clients/$__c_id/client-secret" "$__KEYCLOAK_ADMIN_TOKEN")
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when getting client secret, response: "$?
		return 1
	fi
	__c_sec=$(echo $__c_sec | jq -r .value)
	__curl_string="-X POST $KEYCLOAK_SERVICE_PATH$KEYCLOAK_TOKEN_URL_PREFIX/$1/protocol/openid-connect/token     -H Content-Type:application/x-www-form-urlencoded     -d client_id="$2" -d client_secret="$__c_sec" -d grant_type=client_credentials"
	__TMP_TOKEN=$(__execute_curl_to_keycloak "$__curl_string")
	if [ $? -ne 0 ]; then
		__log_conf_fail_general " Fatal error when getting client token, response: "$?
		return 1
	fi
	echo $__TMP_TOKEN| jq -r .access_token
	__log_conf_ok
	return 0
}

# Read a client token
# args: <realm-name> <client-name>
keycloak_api_read_client_token() {
	TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
	echo "(${BASH_LINENO[0]}) - ${TIMESTAMP}: ${FUNCNAME[0]}" $@ >> $HTTPLOG
	__c_id=$(__keycloak_api_get_client_id $1 $2)
	if [ $? -ne 0 ]; then
		echo "<error-no-token>"
		return 1
	fi
	__c_sec=$(__execute_curl_to_keycloak2 GET "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$1/clients/$__c_id/client-secret" "$__KEYCLOAK_ADMIN_TOKEN")
	if [ $? -ne 0 ]; then
		echo "<error-no-token>"
		return 1
	fi
	__c_sec=$(echo $__c_sec | jq -r .value)
	__curl_string="-X POST $KEYCLOAK_SERVICE_PATH$KEYCLOAK_TOKEN_URL_PREFIX/$1/protocol/openid-connect/token     -H Content-Type:application/x-www-form-urlencoded     -d client_id="$2" -d client_secret="$__c_sec" -d grant_type=client_credentials"
	__TMP_TOKEN=$(__execute_curl_to_keycloak "$__curl_string")
	if [ $? -ne 0 ]; then
		echo "<error-no-token>"
		return 1
	fi
	echo $__TMP_TOKEN| jq -r .access_token
	return 0
}

# Read secret for client
# args: <realm-name> <client-name>
keycloak_api_read_client_secret() {
	TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
	echo "(${BASH_LINENO[0]}) - ${TIMESTAMP}: ${FUNCNAME[0]}" $@ >> $HTTPLOG
	__c_id=$(__keycloak_api_get_client_id $1 $2)
	if [ $? -ne 0 ]; then
		echo "<error-no-secret>"
		return 1
	fi
	__c_sec=$(__execute_curl_to_keycloak2 GET "$KEYCLOAK_SERVICE_PATH$KEYCLOAK_REALM_URL_PREFIX/$1/clients/$__c_id/client-secret" "$__KEYCLOAK_ADMIN_TOKEN")
	if [ $? -ne 0 ]; then
		echo "<error-no-secret>"
		return 1
	fi
	__c_sec=$(echo $__c_sec | jq -r .value)
	echo $__c_sec
	return 0
}