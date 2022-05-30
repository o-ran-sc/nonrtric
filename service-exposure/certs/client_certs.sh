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


CLIENT_SUBJECT="/C=IE/ST=/L=/O=/OU=Keycloak/CN=localhost/emailAddress=client@mail.com"
PW=changeit

echo $PW > secretfile.txt

openssl req -new -newkey rsa:4096 -nodes -keyout client.key -subj "$CLIENT_SUBJECT" -out client.csr

openssl x509 -req -CA rootCA.crt -CAkey rootCA.key -in client.csr -passin file:secretfile.txt -out client.crt -days 365 -CAcreateserial

rm secretfile.txt 2>/dev/null
