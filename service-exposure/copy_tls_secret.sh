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

function usage()
{
   echo ""
   echo "Usage: $0 -n secretName -s sourceNamespace -d destinationNamespace"
   echo -e "\t-n Name of the secret"
   echo -e "\t-s Namespace of the secret"
   echo -e "\t-d Namespace to copy the secret to"
   exit 1
}

while getopts "n:s:d:" opt
do
   case "$opt" in
      n ) secretName="$OPTARG" ;;
      s ) sourceNS="$OPTARG" ;;
      d ) destinationNS="$OPTARG" ;;
      ? ) usage ;;
   esac
done

# Check if any of the paramters are empty
if [ -z "$secretName" ] || [ -z "$sourceNS" ] || [ -z "$destinationNS" ]
then
   echo "Some or all of the parameters are empty";
   usage
fi

# Check if the secret exits
kubectl get secret $secretName -n $sourceNS >/dev/null 2>/dev/null
if [ $? -ne 0 ]
then
   echo "$secretName in $sourceNS does not exist"
   usage
fi

# Check if the destination namespace exists
kubectl get ns $destinationNS >/dev/null 2>/dev/null
if [ $? -ne 0 ]
then
   echo "$destinationNS does not exist"
   usage
fi

# Begin script in case all parameters are correct
echo "Copying $secretName from $sourceNS to $destinationNS"

tlsCrt=$(kubectl get secret ${secretName} -n ${sourceNS} -o json -o=jsonpath="{.data.tls\.crt}")
tlsKey=$(kubectl get secret ${secretName} -n ${sourceNS} -o json -o=jsonpath="{.data.tls\.key}")
caCrt=$(kubectl get secret ${secretName} -n ${sourceNS} -o json -o=jsonpath="{.data.ca\.crt}")

kubectl apply -f - <<EOF
apiVersion: v1
data:
  tls.crt: ${tlsCrt}
  tls.key: ${tlsKey}
  ca.crt: ${caCrt}
kind: Secret
metadata:
  name: ${secretName}
  namespace: ${destinationNS}
type: kubernetes.io/tls
EOF
