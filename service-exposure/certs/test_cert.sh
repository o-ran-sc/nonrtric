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

HOST=$(minikube ip)
KEYCLOAK_PORT=$(kubectl -n default get service keycloak -o jsonpath='{.spec.ports[?(@.name=="https")].nodePort}')
REALM="x509"
CLIENT="x509client"
curl -k -X POST https://$HOST:$KEYCLOAK_PORT/auth/realms/$REALM/protocol/openid-connect/token  \
	--data "grant_type=password&scope=openid profile&username=&password=&client_id=$CLIENT" \
	--cert client.crt --key client.key


echo ""
