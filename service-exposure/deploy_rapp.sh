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

if [ -z "$1" ]
  then
    echo "No argument supplied"
    exit 1
fi

rapp=$1

echo "Deploying application..."
echo "------------------------"
curl http://$host:31570/install?chart=$rapp

echo "\n"
echo "Waiting for pod to start..."
echo "---------------------------"
kubectl wait deployment -n istio-nonrtric $rapp --for=condition=available --timeout=90s

echo ""
echo "Checking pod status..."
echo "----------------------"
kubectl get pods -n istio-nonrtric
#kubectl get pods --show-labels -n istio-nonrtric

#if [ "$rapp" == "rapp-helloworld-invoker1" ] || [ "$rapp" == "rapp-helloworld-invoker2" ]; then
if [ "$rapp" != "rapp-helloworld-provider" ]; then
   echo ""
   echo "Inspect the log for $rapp..."
   echo "-----------------------------------------------"
   kubectl logs -l app.kubernetes.io/name=$rapp -n istio-nonrtric
fi
if [ "$rapp" = "rapp-helloworld-invoker1" ]; then
   echo ""
   echo "Inspect the log for $rapp jwt sidecar..."
   echo "-----------------------------------------------------------"
   kubectl logs -l app.kubernetes.io/name=$rapp -c jwt-proxy -n istio-nonrtric
fi
