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


CA_SUBJECT="/C=IE/ST=/L=/O=/OU=Keycloak/CN=localhost/emailAddress=ca@mail.com"
SERVER_SUBJECT="/C=IE/ST=/L=/O=/OU=Keycloak/CN=localhost/emailAddress=server@mail.com"
PW=changeit

echo $PW > secretfile.txt

openssl req -x509 -sha256 -days 3650 -newkey rsa:4096 -keyout rootCA.key -subj "$CA_SUBJECT" -passout file:secretfile.txt -out rootCA.crt

openssl req -new -newkey rsa:4096 -keyout tls.key -subj "$SERVER_SUBJECT" -out tls.csr -nodes

echo "authorityKeyIdentifier=keyid,issuer" > openssl.ext
echo "basicConstraints=CA:FALSE" >> openssl.ext
echo "subjectAltName = @alt_names" >> openssl.ext
echo "[alt_names]" >> openssl.ext
echo "DNS.1 = localhost" >> openssl.ext

openssl x509 -req -CA rootCA.crt -CAkey rootCA.key -in tls.csr -passin file:secretfile.txt -out tls.crt -days 365 -CAcreateserial -ext openssl.ext

rm secretfile.txt openssl.ext 2>/dev/null
