#!/bin/bash
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2022 Nordix Foundation.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#

if [ -z "$1" ]
  then
    echo "No argument supplied"
    exit 1
fi

OPERATION=$1

if [ "$OPERATION" == "deploy" ]; then
        echo "Deploying applications..."
        echo "-------------------------"
        istioctl kube-inject -f postgres.yaml | kubectl apply -f -
	sleep 10
        istioctl kube-inject -f keycloak.yaml | kubectl apply -f -
        echo ""
        echo "Waiting for pods to start..."
        echo "----------------------------"
        kubectl wait deployment -n default postgres --for=condition=available --timeout=90s
        kubectl wait deployment -n default keycloak --for=condition=available --timeout=300s
        echo ""
        echo "Checking pod status..."
        echo "----------------------"
        kubectl get pods -n default
elif [ "$OPERATION" == "undeploy" ]; then
        echo "Undeploying applications..."
        echo "---------------------------"
	kubectl delete -f keycloak.yaml
	kubectl delete -f postgres.yaml
else
	echo "Unrecogized operation ${OPERATION}"
	exit 1
fi

exit 0
