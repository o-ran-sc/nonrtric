#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021 Nordix Foundation. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================
#


BOLD="\033[1m"
EBOLD="\033[0m"
BOLD="\033[1m"
EBOLD="\033[0m"
RED="\033[31m\033[1m"
ERED="\033[0m"
GREEN="\033[32m\033[1m"
EGREEN="\033[0m"

echo ""
echo "Start test"

APP_TGZ="simple-app-0.1.0.tgz"
VALUES_YAML="simple-app-values.yaml"
INFO_JSON="simple-app.json"
INSTALL_JSON="simple-app-installation.json"
REPO_JSON="cm-repo.json"

PORT=""
HOST=""
URL=""
HM_PATH=""
NAMESPACE="ckhm"  #kube namespace for simple-app
PROXY_TAG=""

OK="All tests ok"
USER=helmadmin
#USER=""
PWD=itisasecret
#PWD=""
PREFIX=/onap/k8sparticipant
PREFIX=""
print_usage() {
    echo "usage: ./test.sh docker|(kube <cluster-ip>)"
}
if [ $# -eq 1 ]; then
    if [ $1 == "docker" ]; then
        PORT=8112
        HOST="localhost"
        URL="http://$USER:$PWD@$HOST:$PORT"$PREFIX
        #URL="http://$HOST:$PORT"$PREFIX
        HM_PATH=$URL
    else
        print_usage
        exit 1
    fi
elif [ $# -eq 2 ]; then
    if [ $1 == "kube" ]; then
        PORT=$(kubectl get svc helmmanagerservice -n nonrtric -o jsonpath='{...ports[?(@.name=="'http'")].nodePort}')
        HOST=$2
        URL="http://$USER:$PWD@$HOST:$PORT"$PREFIX
        #URL="http://$HOST:$PORT"$PREFIX
        HM_PATH=$URL
    else
        print_usage
        exit 1
    fi
else
    print_usage
    exit 1
fi



run-curl() {
    curl_cmd="curl -sw %{http_code} $PROXY_TAG $HM_PATH$@"
    echo $curl_cmd
    res=$($curl_cmd)
    retcode=$?
    status=${res:${#res}-3}
    if [ -z "$res" ]; then
        body="<no-body-returned>"
    elif [ ${#res} -gt 2 ]; then
        body=${res:0:${#res}-3}
    else
        body="<no-body-returned>"
    fi
    if [ $retcode -ne 0 ]; then
        echo -e $RED" FAIL -  Curl failed"$ERED
        echo "  Curl return code: $retcode"
        OK="One or more tests failed"
    else
        if [[ $status -gt 199 ]] && [[ $status -lt 300 ]]; then
            echo -e $GREEN" Curl OK"$EGREEN
            echo "  Response: "$status
            echo "  Body: "$body
        else
            echo -e $RED" FAIL, non 2XX response"$ERED
            echo "  Response: "$status
            echo "  Body: "$body
            OK="One or more tests failed"
        fi
    fi
}

echo "================"
echo "Get apps - empty"
echo "================"
cmd="/helm/charts"
run-curl $cmd
echo


echo "================"
echo "Add repo"
echo "================"
cmd="/helm/repo -X POST -H Content-Type:application/json -d @$REPO_JSON"
run-curl $cmd
echo


echo "============"
echo "Onboard app"
echo "==========="
cmd="/helm/onboard/chart -X POST -F chart=@$APP_TGZ -F values=@$VALUES_YAML -F info=<$INFO_JSON"
run-curl $cmd
echo


echo "====================="
echo "Get apps - simple-app"
echo "====================="
cmd="/helm/charts"
run-curl $cmd
echo


echo "==========="
echo "Install app"
echo "==========="
cmd="/helm/install -X POST -H Content-Type:application/json -d @$INSTALL_JSON"
run-curl $cmd
echo



echo "====================="
echo "Get apps - simple-app"
echo "====================="
cmd="/helm/charts"
run-curl $cmd
echo

echo "================================================================="
echo "helm ls to list installed app - simpleapp chart should be visible"
echo "================================================================="
helm ls -A
echo

echo "=========================================="
echo "sleep 30 - give the app some time to start"
echo "=========================================="
sleep 30

echo "============================"
echo "List svc and  pod of the app"
echo "============================"
kubectl get svc -n $NAMESPACE
kubectl get po -n $NAMESPACE
echo

echo "========================"
echo "Uninstall app simple-app"
echo "========================"
cmd="/helm/uninstall/simple-app/0.1.0 -X DELETE"
run-curl $cmd
echo

echo "==========================================="
echo "sleep 30 - give the app some time to remove"
echo "==========================================="
sleep 30

echo "============================================================"
echo "List svc and  pod of the app - should be gone or terminating"
echo "============================================================"
kubectl get svc -n $NAMESPACE
kubectl get po -n $NAMESPACE
echo


echo "====================="
echo "Get apps - simple-app"
echo "====================="
cmd="/helm/charts"
run-curl $cmd
echo

echo "============"
echo "Delete chart"
echo "==========="
cmd="/helm/chart/simple-app/0.1.0 -X DELETE"
run-curl $cmd
echo

echo "================"
echo "Get apps - empty"
echo "================"
cmd="/helm/charts"
run-curl $cmd
echo

echo -e "Test result $BOLD $OK $EBOLD"
echo "End of test"
