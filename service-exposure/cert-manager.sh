#!/bin/bash
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2023 Nordix Foundation.
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
WORKDIR=$(dirname "$(realpath "$0")")

if [ "$OPERATION" == "deploy" ]; then
        echo "Deploying cert-manager application..."
        echo "-------------------------------------"
        kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.11.0/cert-manager.yaml
        echo ""
        echo "Waiting for pods to start..."
        echo "----------------------------"
	kubectl wait deployment -n cert-manager cert-manager --for=condition=available --timeout=300s
	kubectl wait deployment -n cert-manager cert-manager-cainjector --for=condition=available --timeout=300s
        kubectl wait deployment -n cert-manager cert-manager-webhook --for=condition=available --timeout=300s
        echo ""
        echo "Checking pod status..."
        echo "----------------------"
        kubectl get pods -n cert-manager
        echo ""
	# Once the pods are up and running we still need to wait for the certificate controller process to start
	# before certificates can be issued
        echo "Waiting for certificate controller..."
        echo "------------------------------------"
	sleep 100
        echo ""
        echo "Creating certificates..."
        echo "------------------------"
	kubectl apply -f $WORKDIR/cluster-issuer.yaml
        kubectl apply -f $WORKDIR/issuer.yaml
        kubectl apply -f $WORKDIR/webhook-server-certificate.yaml
	kubectl apply -f $WORKDIR/keycloak-server-certificate.yaml
        kubectl apply -f $WORKDIR/keycloak-client-certificate.yaml
elif [ "$OPERATION" == "undeploy" ]; then
        echo "Deleting certificates..."
        echo "------------------------"
	kubectl delete -f $WORKDIR/cluster-issuer.yaml
        kubectl delete -f $WORKDIR/issuer.yaml
        kubectl delete -f $WORKDIR/webhook-server-certificate.yaml
	kubectl delete -f $WORKDIR/keycloak-server-certificate.yaml
        kubectl delete -f $WORKDIR/keycloak-client-certificate.yaml
	kubectl delete secret -n default cm-cluster-issuer-rootca-secret
        kubectl delete secret -n default cm-keycloak-client-certs
        kubectl delete secret -n default cm-keycloak-server-certs
        kubectl delete secret -n default cm-webhook-server-certs
        echo "Undeploying cert-manager application..."
        echo "---------------------------------------"
	kubectl delete -f https://github.com/cert-manager/cert-manager/releases/download/v1.11.0/cert-manager.yaml
else
	echo "Unrecogized operation ${OPERATION}"
	exit 1
fi

exit 0
