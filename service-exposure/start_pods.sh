#!/bin/sh
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

echo "Deploying applications..."
echo "-------------------------"
kubectl create -f chartmuseum.yaml
kubectl create -f rapps-keycloak-mgr.yaml
kubectl create -f  rapps-istio-mgr.yaml
kubectl create -f  rapps-helm-installer.yaml
kubectl create -f rapps-webhook.yaml

echo ""
echo "Waiting for pods to start..."
echo "----------------------------"
kubectl wait deployment -n default chartmuseum-deployment --for=condition=available --timeout=90s
kubectl wait deployment -n default rapps-keycloak-mgr-deployment --for=condition=available --timeout=90s
kubectl wait deployment -n default rapps-istio-mgr-deployment --for=condition=available --timeout=90s
kubectl wait deployment -n default rapps-helm-installer-deployment --for=condition=available --timeout=90s
kubectl wait deployment -n default jwt-proxy-admission-controller-deployment --for=condition=available --timeout=90s

echo ""
echo "Configure sidecar injection..."
echo "----------------------------"
kubectl create -f MutatingWebhookConfiguration.yaml

echo ""
echo "Checking pod status..."
echo "----------------------"
kubectl get pods -n default
