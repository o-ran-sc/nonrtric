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

# Script to clean all namespaces from kubernetes having the label 'autotest', i.e started by autotest

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

echo "Will remove all kube namespaces marked with label 'autotest'"

print_usage() {
    echo "Usage: clean_kube_ns.sh [--kubeconfig <kube-config-file>] | [--kubecontext <context name>]"
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

indent1() { sed 's/^/ /'; }

nss=$(kubectl $KUBECONF get ns -o 'jsonpath={.items[?(@.metadata.labels.autotest)].metadata.name}')
if [ ! -z "$nss" ]; then
	for ns in $nss; do
		echo "Deleting namespace: "$ns
		kubectl $KUBECONF delete ns $ns | indent1
	done
fi
echo "Done"