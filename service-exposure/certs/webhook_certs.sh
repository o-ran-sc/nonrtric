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

cfssl gencert -initca ./ca-csr.json | cfssljson -bare ca

cfssl gencert \
  -ca=ca.pem \
  -ca-key=ca-key.pem \
  -config=ca-config.json \
  -hostname="jwt-proxy-admission-controller,jwt-proxy-admission-controller.default.svc.cluster.local,jwt-proxy-admission-controller.default.svc,localhost,127.0.0.1" \
  -profile=default \
  ca-csr.json | cfssljson -bare webhook-cert


cat <<EOF > rapps-webhook-tls.yaml
apiVersion: v1
kind: Secret
metadata:
  name: webhook-cert
  namespace: default
type: Opaque
data:
  tls.crt: $(cat webhook-cert.pem | base64 | tr -d '\n')
  tls.key: $(cat webhook-cert-key.pem | base64 | tr -d '\n') 
EOF

ca_pem_b64="$(openssl base64 -A <"ca.pem")"
echo $ca_pem_b64
