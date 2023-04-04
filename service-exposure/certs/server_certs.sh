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


CA_SUBJECT="/C=IE/ST=Dublin/L=Dublin/O=Keycloak/OU=Keycloak/CN=localhost/emailAddress=ca@mail.com"
SERVER_SUBJECT="/C=IE/ST=Dublin/L=Dublin/O=Keycloak/OU=Keycloak/CN=localhost/emailAddress=server@mail.com"
PW=changeit
CERTNAME=tls
CANAME=rootCA
IP=$(minikube ip)
DAYS=3650
TRUSTSTORE=server.truststore
KEYSTORE=server.keystore
STORETYPE=PKCS12

rm $TRUSTSTORE $KEYSTORE ${CANAME}.key ${CANAME}.crt ${CERTNAME}.key ${CERTNAME}.csr ${CERTNAME}.crt ${CERTNAME}.p12 2>/dev/null
echo $PW > secretfile.txt

openssl req -x509 -sha256 -days $DAYS -newkey rsa:4096 -keyout ${CANAME}.key -subj "$CA_SUBJECT" -passout file:secretfile.txt -out ${CANAME}.crt

openssl req -new -newkey rsa:4096 -keyout ${CERTNAME}.key -subj "$SERVER_SUBJECT" -out ${CERTNAME}.csr -nodes

echo "subjectKeyIdentifier   = hash" > x509.ext
echo "authorityKeyIdentifier = keyid:always,issuer:always" >> x509.ext
echo "basicConstraints       = CA:TRUE" >> x509.ext
echo "keyUsage               = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment, keyAgreement, keyCertSign" >> x509.ext
echo "subjectAltName         = DNS.1:localhost, IP.1:127.0.0.1, DNS.2:minikube, IP.2:${IP}, DNS.3:keycloak.default, DNS.4:keycloak.est.tech, DNS.5:keycloak" >> x509.ext
echo "issuerAltName          = issuer:copy" >> x509.ext
echo "[ ca ]" >> x509.ext
echo "# X509 extensions for a ca" >> x509.ext
echo "keyUsage                = critical, cRLSign, keyCertSign" >> x509.ext
echo "basicConstraints        = CA:TRUE, pathlen:0" >> x509.ext
echo "subjectKeyIdentifier    = hash" >> x509.ext
echo "authorityKeyIdentifier  = keyid:always,issuer:always" >> x509.ext
echo "" >> x509.ext
echo "[ server ]" >> x509.ext
echo "# X509 extensions for a server" >> x509.ext
echo "keyUsage                = critical,digitalSignature,keyEncipherment" >> x509.ext
echo "extendedKeyUsage        = serverAuth,clientAuth" >> x509.ext
echo "basicConstraints        = critical,CA:FALSE" >> x509.ext
echo "subjectKeyIdentifier    = hash" >> x509.ext
echo "authorityKeyIdentifier  = keyid,issuer:always" >> x509.ext 

openssl x509 -req -CA ${CANAME}.crt -CAkey ${CANAME}.key -in ${CERTNAME}.csr -passin file:secretfile.txt -out ${CERTNAME}.crt -days $DAYS -CAcreateserial -extfile x509.ext

keytool -import -trustcacerts -file ${CANAME}.crt -keystore $TRUSTSTORE -storepass $PW  -storetype $STORETYPE -noprompt

openssl pkcs12 -export -clcerts -in ${CERTNAME}.crt -inkey ${CERTNAME}.key -passout file:secretfile.txt -out ${CERTNAME}.p12

keytool -importkeystore -srckeystore ${CERTNAME}.p12 -srcstorepass $PW -srcstoretype $STORETYPE -destkeystore $KEYSTORE -deststorepass $PW -deststoretype $STORETYPE 

rm secretfile.txt x509.ext 2>/dev/null
