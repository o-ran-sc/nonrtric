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

# Script to clean all resources from kubernetes having the label 'autotest', i.e started by autotest

BOLD="\033[1m"
EBOLD="\033[0m"
RED="\033[31m\033[1m"
ERED="\033[0m"
GREEN="\033[32m\033[1m"
EGREEN="\033[0m"
YELLOW="\033[33m\033[1m"
EYELLOW="\033[0m"
SAMELINE="\033[0K\r"

KUBECONF=""

__kube_scale_all_resources() {

	echo " Scaling down in namespace $1 ..."
	namespace=$1
	resources="deployment replicaset statefulset"
	for restype in $resources; do
		result=$(kubectl $KUBECONF get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.autotest)].metadata.name}')
		if [ $? -eq 0 ] && [ ! -z "$result" ]; then
			for resid in $result; do
				count=$(kubectl $KUBECONF get $restype $resid  -n $namespace -o jsonpath='{.status.replicas}' 2> /dev/null)
				if [ $? -eq 0 ] && [ ! -z "$count" ]; then
					if [ $count -ne 0 ]; then
						echo "  Scaling $restype $resid in namespace $namespace with label autotest to 0, current count=$count."
						kubectl $KUBECONF scale  $restype $resid -n $namespace --replicas=0 1> /dev/null 2> /dev/null
					fi
				fi
			done
		fi
	done
}

__kube_wait_for_zero_count() {
	echo " Wait for scaling to zero in namespace $1 ..."
	namespace=$1
	resources="deployment replicaset statefulset"
	for restype in $resources; do
		result=$(kubectl $KUBECONF get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.autotest)].metadata.name}')
		if [ $? -eq 0 ] && [ ! -z "$result" ]; then
			for resid in $result; do
				T_START=$SECONDS
				count=1
				scaled=0
				while [ $count -gt 0 ]; do
					count=$(kubectl $KUBECONF get $restype $resid  -n $namespace -o jsonpath='{.status.replicas}' 2> /dev/null)
					if [ $? -eq 0 ] && [ ! -z "$count" ]; then
						if [ $count -ne 0 ]; then
							echo -ne "  Scaling $restype $resid in namespace $namespace with label autotest to 0, current count=$count....$(($SECONDS-$T_START)) seconds"$SAMELINE
							scaled=1
						else
							sleep 0.5
						fi
					else
						count=0
					fi
				done
				if [ $scaled -eq 1 ]; then
					echo -e "  Scaling $restype $resid in namespace $namespace with label autotest to 0, current count=$count....$(($SECONDS-$T_START)) seconds $GREEN OK $EGREEN"
				fi
			done
		fi
	done
}

__kube_delete_all_resources() {
	echo " Delete all in namespace $1 ..."
	namespace=$1
	resources="deployments replicaset statefulset services pods configmaps pvc serviceaccounts secrets authorizationpolicies requestauthentications"
	for restype in $resources; do
		result=$(kubectl $KUBECONF get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.autotest)].metadata.name}' 2> /dev/null)
		if [ $? -eq 0 ] && [ ! -z "$result" ]; then
			for resid in $result; do
				echo  "  Deleting $restype $resid in namespace $namespace with label autotest "
				kubectl $KUBECONF delete --grace-period=1 $restype $resid -n $namespace 1> /dev/null 2> /dev/null
			done
		fi
	done
}

__kube_delete_all_pv() {
	echo " Delete all non-namespaced resources ..."
	resources="pv clusterrolebindings"
	for restype in $resources; do
		result=$(kubectl $KUBECONF get $restype -o jsonpath='{.items[?(@.metadata.labels.autotest)].metadata.name}')
		if [ $? -eq 0 ] && [ ! -z "$result" ]; then
			for resid in $result; do
				echo  "  Deleting $restype $resid with label autotest "
				kubectl $KUBECONF delete --grace-period=1 $restype $resid 1> /dev/null 2> /dev/null
			done
		fi
	done
}

__kube_wait_for_delete() {
	echo " Wait for delete in namespace $1 ..."
	namespace=$1
	resources="deployments replicaset statefulset services pods configmaps pvc secrets"
	for restype in $resources; do
		result=$(kubectl $KUBECONF get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.autotest)].metadata.name}')
		if [ $? -eq 0 ] && [ ! -z "$result" ]; then
			for resid in $result; do
				echo  "  Deleting $restype $resid in namespace $namespace with label autotest "
				kubectl $KUBECONF delete --grace-period=1 $restype $resid -n $namespace #1> /dev/null 2> /dev/null
				echo -ne "  Waiting for $restype $resid in namespace $namespace with label autotest to be deleted..."$SAMELINE
				T_START=$SECONDS
				result="dummy"
				while [ ! -z "$result" ]; do
					sleep 0.5
					result=$(kubectl $KUBECONF get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.autotest)].metadata.name}')
					echo -ne "  Waiting for $restype $resid in namespace $namespace with label autotest to be deleted...$(($SECONDS-$T_START)) seconds "$SAMELINE
					if [ -z "$result" ]; then
						echo -e " Waiting for $restype $resid in namespace $namespace with label autotest to be deleted...$(($SECONDS-$T_START)) seconds $GREEN OK $EGREEN"
					elif [ $(($SECONDS-$T_START)) -gt 300 ]; then
						echo -e " Waiting for $restype $resid in namespace $namespace with label autotest to be deleted...$(($SECONDS-$T_START)) seconds $RED Failed $ERED"
						result=""
					fi
				done
			done
		fi
	done
}

__kube_wait_for_delete_pv() {
	echo " Wait for delete pv ..."
	resources="pv "
	for restype in $resources; do
		result=$(kubectl $KUBECONF get $restype -o jsonpath='{.items[?(@.metadata.labels.autotest)].metadata.name}')
		if [ $? -eq 0 ] && [ ! -z "$result" ]; then
			for resid in $result; do
				echo  "  Deleting $restype $resid with label autotest "
				kubectl $KUBECONF delete --grace-period=1 $restype $resid -n $namespace #1> /dev/null 2> /dev/null
				echo -ne "  Waiting for $restype $resid with label autotest to be deleted..."$SAMELINE
				T_START=$SECONDS
				result="dummy"
				while [ ! -z "$result" ]; do
					sleep 0.5
					result=$(kubectl $KUBECONF get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.autotest)].metadata.name}')
					echo -ne "  Waiting for $restype $resid with label autotest to be deleted...$(($SECONDS-$T_START)) seconds "$SAMELINE
					if [ -z "$result" ]; then
						echo -e " Waiting for $restype $resid with label autotest to be deleted...$(($SECONDS-$T_START)) seconds $GREEN OK $EGREEN"
					elif [ $(($SECONDS-$T_START)) -gt 300 ]; then
						echo -e " Waiting for $restype $resid with label autotest to be deleted...$(($SECONDS-$T_START)) seconds $RED Failed $ERED"
						result=""
					fi
				done
			done
		fi
	done
}


echo "Will remove all kube resources marked with label 'autotest'"

print_usage() {
    echo "Usage: clean_kube.sh [--kubeconfig <kube-config-file>] | [--kubecontext <context name>]"
}

if [ $# -eq 0 ]; then
:
elif [ $# -eq 1 ]; then
    print_usage
    exit
elif [ $# -eq 2 ]; then
    if [ $1 == "--kubeconfig" ]; then
        if [ ! -f $2 ]; then
            echo "File $2 for --kubeconfig is not found"
            print_usage
            exit
        fi
        KUBECONF="--kubeconfig $2"
    elif [ $1 == "--kubecontext" ]; then
        if [ -z $2 ]; then
            echo "No context found for --kubecontext"
            print_usage
            exit
        fi
        KUBECONF="--context $2"
    else
        print_usage
        exit
    fi
else
    print_usage
    exit
fi

# List all namespace and scale/delete per namespace
nss=$(kubectl $KUBECONF get ns  -o jsonpath='{.items[*].metadata.name}')
if [ ! -z "$nss" ]; then
	for ns in $nss; do
		__kube_scale_all_resources $ns
	done
	for ns in $nss; do
		__kube_wait_for_zero_count $ns
	done
	for ns in $nss; do
		__kube_delete_all_resources $ns
	done
	__kube_delete_all_pv
	for ns in $nss; do
		__kube_wait_for_delete $ns
	done
	__kube_wait_for_delete_pv
fi
echo "Done"