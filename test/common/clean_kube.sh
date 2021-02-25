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

__kube_delete_all_resources() {
	echo "Deleting all from namespace: "$1
	namespace=$1
	resources="deployments replicaset statefulset services pods configmaps pvc pv"
	deleted_resourcetypes=""
	for restype in $resources; do
		result=$(kubectl get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.autotest)].metadata.name}')
		if [ $? -eq 0 ] && [ ! -z "$result" ]; then
			deleted_resourcetypes=$deleted_resourcetypes" "$restype
			for resid in $result; do
				if [ $restype == "replicaset" ] || [ $restype == "statefulset" ]; then
					kubectl scale  $restype $resid -n $namespace --replicas=0 1> /dev/null 2> /dev/null
					T_START=$SECONDS
					count=1
					while [ $count -ne 0 ]; do
						count=$(kubectl get $restype $resid  -n $namespace -o jsonpath='{.status.replicas}' 2> /dev/null)
						echo -ne "  Scaling $restype $resid from namespace $namespace with label autotest to 0,count=$count....$(($SECONDS-$T_START)) seconds"$SAMELINE
						if [ $? -eq 0 ] && [ ! -z "$count" ]; then
							sleep 0.5
						else
							count=0
						fi
					done
					echo -e "  Scaled $restype $resid from namespace $namespace with label $labelname=$labelid to 0,count=$count....$(($SECONDS-$T_START)) seconds$GREEN OK $EGREEN"
				fi
				echo -ne "  Deleting $restype $resid from namespace $namespace with label autotest "$SAMELINE
				kubectl delete $restype $resid -n $namespace 1> /dev/null 2> /dev/null
				if [ $? -eq 0 ]; then
					echo -e "  Deleted $restype $resid from namespace $namespace with label autotest $GREEN OK $EGREEN"
				else
					echo -e "  Deleted $restype $resid from namespace $namespace with label autotest $GREEN Does not exist - OK $EGREEN"
				fi
				#fi
			done
		fi
	done
	if [ ! -z "$deleted_resourcetypes" ]; then
		for restype in $deleted_resources; do
			echo -ne "  Waiting for $restype in namespace $namespace with label autotest to be deleted..."$SAMELINE
			T_START=$SECONDS
			result="dummy"
			while [ ! -z "$result" ]; do
				sleep 0.5
				result=$(kubectl get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.autotest)].metadata.name}')
				echo -ne "  Waiting for $restype in namespace $namespace with label autotest to be deleted...$(($SECONDS-$T_START)) seconds "$SAMELINE
				if [ -z "$result" ]; then
					echo -e " Waiting for $restype in namespace $namespace with label autotest to be deleted...$(($SECONDS-$T_START)) seconds $GREEN OK $EGREEN"
				elif [ $(($SECONDS-$T_START)) -gt 300 ]; then
					echo -e " Waiting for $restype in namespace $namespace with label autotest to be deleted...$(($SECONDS-$T_START)) seconds $RED Failed $ERED"
					result=""
				fi
			done
		done
	fi
}
echo "Will remove all kube resources marked with label 'autotest'"
__kube_delete_all_resources nonrtric
__kube_delete_all_resources nonrtric-ft
__kube_delete_all_resources onap

echo "Done"