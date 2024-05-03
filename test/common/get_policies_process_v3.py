#  ============LICENSE_START===============================================
#  Copyright (C) 2020-2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2024 OpenInfra Foundation Europe. All rights reserved.
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

# This script gets policies spread over a number rics
# Intended for parallel processing
# Returns a string with result, either "0" for ok, or "1<fault description>"

import sys
import requests
import traceback
from time import sleep

# disable warning about unverified https requests
from requests.packages import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

#arg responseCode baseurl policyIdsFilePath proxy

try:
    if len(sys.argv) != 8:
        print("1Expected 7 args, got "+str(len(sys.argv)-1)+ ". Args: responseCode baseurl policyIdsFilePath startId pids pidID proxy")
        sys.exit()

    responseCode=int(sys.argv[1])
    baseurl=str(sys.argv[2])
    policyIdsFilePath=str(sys.argv[3])
    startId=int(sys.argv[4])
    pids=int(sys.argv[5])
    pidId=int(sys.argv[6])
    httpproxy=str(sys.argv[7])

    proxydict=None
    if httpproxy != "NOPROXY":
        proxydict = {
            "http" : httpproxy,
            "https" : httpproxy
        }

    http_retry_count=0
    connect_retry_count=0

    with open(str(policyIdsFilePath)) as file:
        for policyId in file:
            if startId%pids == (pidId - 1):
                connect_ok=False
                retry_cnt=5
                while(retry_cnt>0):
                    url=str(baseurl+policyId.strip())
                    try:
                        if proxydict is None:
                            resp=requests.get(url, verify=False, timeout=90)
                        else:
                            resp=requests.get(url, verify=False, timeout=90, proxies=proxydict)
                        connect_ok=True
                    except Exception as e1:
                        if (retry_cnt > 1):
                            sleep(0.1)
                            retry_cnt -= 1
                            connect_retry_count += 1
                        else:
                            print("1Get failed for id:"+policyId.strip()+ ", "+str(e1) + " "+traceback.format_exc())
                            sys.exit()

                    if (connect_ok == True):
                        if (resp.status_code == None):
                            print("1Get failed for id:"+policyId.strip()+ ", expected response code: "+str(responseCode)+", got: None")
                            sys.exit()
                        if (resp.status_code != responseCode):
                            if (resp.status_code >= 500) and (http_retry_count < 600 ) and (retry_cnt > 1):
                                sleep(0.1)
                                retry_cnt -= 1
                                http_retry_count += 1
                            else:
                                print("1Get failed for id:"+policyId.strip()+ ", expected response code: "+str(responseCode)+", got: "+str(resp.status_code)+str(resp.raw))
                                sys.exit()
                        else:
                            retry_cnt=-1
            startId  += 1
    print("0 http retries:"+str(http_retry_count) + ", connect retries: "+str(connect_retry_count))
    sys.exit()

except Exception as e:
    print("1"+str(e))
    traceback.print_exc()
sys.exit()