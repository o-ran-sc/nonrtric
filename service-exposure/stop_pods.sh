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

export host=$(minikube ip)

echo "Undeploying applications..."
echo "---------------------------"
curl http://$host:31570/uninstall?chart=rapp-helloworld-invoker1
echo ""
sleep 2
curl http://$host:31570/uninstall?chart=rapp-helloworld-invoker2
echo ""
sleep 2
curl http://$host:31570/uninstall?chart=rapp-helloworld-provider
echo ""

kubectl delete -f  rapps-helm-installer.yaml
kubectl delete -f  rapps-istio-mgr.yaml
kubectl delete -f rapps-keycloak-mgr.yaml
kubectl delete -f chartmuseum.yaml
kubectl delete -f rapps-webhook.yaml
kubectl delete -f MutatingWebhookConfiguration.yaml
