#!/bin/sh
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2022-2023 Nordix Foundation.
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

CLIENT_SUBJECT="/C=IE/ST=Dublin/L=Dublin/O=Keycloak/OU=Keycloak/CN=localhost/emailAddress=client@mail.com"
PW=changeit
CERTNAME=client
IP=$(minikube ip)
DAYS=3650

rm ${CERTNAME}.key ${CERTNAME}.csr ${CERTNAME}.crt ${CERTNAME}.p12 ${CERTNAME}.pem ${CERTNAME}_pub.key 2>/dev/null
echo $PW > secretfile.txt

echo "subjectKeyIdentifier   = hash" > x509.ext
echo "authorityKeyIdentifier = keyid:always,issuer:always" >> x509.ext
echo "basicConstraints       = CA:TRUE" >> x509.ext
echo "keyUsage               = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment, keyAgreement, keyCertSign" >> x509.ext
echo "subjectAltName         = DNS.1:localhost, IP.1:127.0.0.1, DNS.2:minikube, IP.2:${IP}, DNS.3:keycloak.default, DNS.4:keycloak.est.tech, DNS.5:keycloak" >> x509.ext
echo "issuerAltName          = issuer:copy" >> x509.ext

openssl req -new -newkey rsa:4096 -nodes -keyout ${CERTNAME}.key -subj "$CLIENT_SUBJECT" -out ${CERTNAME}.csr 

openssl x509 -req -CA rootCA.crt -CAkey rootCA.key -in ${CERTNAME}.csr -passin file:secretfile.txt -out ${CERTNAME}.crt -days $DAYS -CAcreateserial -extfile x509.ext 


openssl pkcs12 -export -clcerts -in ${CERTNAME}.crt -inkey ${CERTNAME}.key -passout file:secretfile.txt -out ${CERTNAME}.p12

openssl pkcs12 -in ${CERTNAME}.p12 -password pass:$PW -passout file:secretfile.txt -out ${CERTNAME}.pem -clcerts -nodes

openssl rsa -in ${CERTNAME}.key -outform PEM -pubout -out ${CERTNAME}_pub.key

rm secretfile.txt x509.ext 2>/dev/null
