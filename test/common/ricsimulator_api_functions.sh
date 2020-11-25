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


### Admin API functions for the RIC simulator


# Excute a curl cmd towards a ricsimulator and check the response code.
# args: <expected-response-code> <curl-cmd-string>
__execute_curl_to_sim() {
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

# Tests if a variable value in the ricsimulator is equal to a target value and and optional timeout.
# Arg: <ric-id> <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <ric-id> <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
sim_equal() {

	if [ $# -eq 3 ] || [ $# -eq 4 ]; then
		port=$(__find_sim_port $1)
		__var_test $1 "$RIC_SIM_LOCALHOST$port/counter/" $2 "=" $3 $4
		return 0
	else
		__print_err "needs three or four args: <ric-id> <sim-param> <target-value> [ timeout ]"
		return 1
	fi
}

# Print a variable value from the RIC sim.
# args: <ric-id> <variable-name>
# (Function for test scripts)
sim_print() {

	if [ $# != 2 ]; then
    	__print_err "need two args, <ric-id> <sim-param>" $@
		exit 1
	fi
	port=$(__find_sim_port $1)
	echo -e $BOLD"INFO(${BASH_LINENO[0]}): $1, $2 = $(__do_curl $RIC_SIM_LOCALHOST$port/counter/$2)"$EBOLD
}

# Tests if a variable value in the RIC simulator contains the target string and and optional timeout
# Arg: <ric-id> <variable-name> <target-value> - This test set pass or fail depending on if the variable contains
# the target or not.
# Arg: <ric-id> <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value contains the target
# value or not.
# (Function for test scripts)
sim_contains_str() {

	if [ $# -eq 3 ] || [ $# -eq 4 ]; then
		port=$(__find_sim_port $1)
		__var_test $1 "$RIC_SIM_LOCALHOST$port/counter/" $2 "contain_str" $3 $4
		return 0
	else
		__print_err "needs three or four args: <ric-id> <sim-param> <target-value> [ timeout ]"
		return 1
	fi
}

# Simulator API: Put a policy type in a ric
# args: <response-code> <ric-id> <policy-type-id> <policy-type-file>
# (Function for test scripts)
sim_put_policy_type() {
	__log_conf_start $@
	if [ $# -ne 4 ]; then
		__print_err "<response-code> <ric-id> <policy-type-id> <policy-type-file>" $@
		return 1
	fi
	res=$(__find_sim_port $2)
    curlString="curl -X PUT -skw %{http_code} $RIC_SIM_LOCALHOST"$res"/policytype?id="$3" -H Content-Type:application/json --data-binary @"$4
	__execute_curl_to_sim $1 "$curlString"
	return $?
}

# Simulator API: Delete a policy type in a ric
# <response-code> <ric-id> <policy-type-id>
# (Function for test scripts)
sim_delete_policy_type() {
	__log_conf_start $@
	if [ $# -ne 3 ]; then
		__print_err "<response-code> <ric-id> <policy_type_id>" $@
		return 1
	fi
	res=$(__find_sim_port $2)
    curlString="curl -X DELETE -skw %{http_code} $RIC_SIM_LOCALHOST"$res"/policytype?id="$3
    __execute_curl_to_sim $1 "$curlString"
	return $?
}

# Simulator API: Delete instances (and status), for one ric
# <response-code> <ric-id>
# (Function for test scripts)
sim_post_delete_instances() {
	__log_conf_start $@
	if [ $# -ne 2 ]; then
		__print_err "<response-code> <ric-id>" $@
		return 1
	fi
	res=$(__find_sim_port $2)
    curlString="curl -X POST -skw %{http_code} $RIC_SIM_LOCALHOST"$res"/deleteinstances"
    __execute_curl_to_sim $1 "$curlString"
	return $?
}

# Simulator API: Delete all (instances/types/statuses/settings), for one ric
# <response-code> <ric-id>
# (Function for test scripts)
sim_post_delete_all() {
	__log_conf_start $@
	if [ $# -ne 3 ]; then
		__print_err "<response-code> <numericic-id>" $@
		return 1
	fi
	res=$(__find_sim_port $2)
    curlString="curl -X POST -skw %{http_code} $RIC_SIM_LOCALHOST"$res"/deleteall"
    __execute_curl_to_sim $1 "$curlString"
	return $?
}

# Simulator API: Set (or reset) response code for next A1 message, for one ric
# <response-code> <ric-id> [<forced_response_code>]
# (Function for test scripts)
sim_post_forcedresponse() {
	__log_conf_start $@
	if [ $# -ne 3 ]; then
		__print_err "<response-code> <ric-id> <forced_response_code>" $@
		return 1
	fi
	res=$(__find_sim_port $2)
    curlString="curl -X POST -skw %{http_code} $RIC_SIM_LOCALHOST"$res"/forceresponse"
	if [ $# -eq 3 ]; then
		curlString=$curlString"?code="$3
	fi
    __execute_curl_to_sim $1 "$curlString"
	return $?
}

# Simulator API: Set (or reset) A1 response delay, for one ric
# <response-code> <ric-id> [<delay-in-seconds>]
# (Function for test scripts)
sim_post_forcedelay() {
	__log_conf_start $@
	if [ $# -ne 3 ]; then
		__print_err "<response-code> <ric-id> [<delay-in-seconds>]" $@
		return 1
	fi
	res=$(__find_sim_port $2)
    curlString="curl -X POST -skw %{http_code} $RIC_SIM_LOCALHOST$res/forcedelay"
	if [ $# -eq 3 ]; then
		curlString=$curlString"?delay="$3
	fi
    __execute_curl_to_sim $1 "$curlString"
	return $?
}