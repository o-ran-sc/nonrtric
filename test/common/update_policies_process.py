#  ============LICENSE_START===============================================
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

# This script create/update policies spread over a number rics
# Intended for parallel processing
# Returns a string with result, either "0" for ok, or "1<fault description>"

import json
import sys
import requests
import traceback
from time import sleep

# disable warning about unverified https requests
from requests.packages import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

#arg responsecode baseurl ric_base num_rics uuid startid service type transient notification-url templatepath count pids pid_id proxy policy-ids-file-path
data_out=""
url_out=""
try:

    if len(sys.argv) != 17:
        print("1Expected 16 args, got "+str(len(sys.argv)-1)+ ". Args: responsecode baseurl ric_base num_rics uuid startid service type transient notification-url templatepath count pids pid_id proxy policy-ids-file-path")
        print (sys.argv[1:])
        sys.exit()
    responsecode=int(sys.argv[1])
    baseurl=str(sys.argv[2])
    ric_base=str(sys.argv[3])
    num_rics=int(sys.argv[4])
    uuid=str(sys.argv[5])
    start=int(sys.argv[6])
    serv=str(sys.argv[7])
    pt=str(sys.argv[8])
    trans=str(sys.argv[9])
    noti=str(sys.argv[10])
    templatepath=str(sys.argv[11])
    count=int(sys.argv[12])
    pids=int(sys.argv[13])
    pid_id=int(sys.argv[14])
    httpproxy=str(sys.argv[15])
    policy_ids_file_path=str(sys.argv[16])

    proxydict=None
    if httpproxy != "NOPROXY":
        proxydict = {
            "http" : httpproxy,
            "https" : httpproxy
        }
    if uuid == "NOUUID":
        uuid=""

    with open(templatepath, 'r') as file:
        template = file.read()

        start=start
        stop=count*num_rics+start

        http_retry_count=0
        connect_retry_count=0

        with open(str(policy_ids_file_path)) as file:
            for policyId in file:
                if start%pids == (pid_id-1):
                    payload=template.replace("XXX",str(start))
                    connect_ok=False
                    retry_cnt=5
                    while(retry_cnt>0):
                        try:
                            headers = {'Content-type': 'application/json'}
                            url=baseurl+"/"+str(policyId.strip())
                            url_out=url
                            if proxydict is None:
                                resp=requests.put(url, payload, headers=headers, verify=False, timeout=90)
                            else:
                                resp=requests.put(url, payload, headers=headers, verify=False, timeout=90, proxies=proxydict)
                            connect_ok=True
                        except Exception as e1:
                            if (retry_cnt > 1):
                                sleep(0.1)
                                retry_cnt -= 1
                                connect_retry_count += 1
                            else:
                                print("1Put failed for id:"+policyId.strip()+ ", "+str(e1) + " "+traceback.format_exc())
                                sys.exit()

                        if (connect_ok == True):
                            if (resp.status_code == None):
                                print("1Put failed for id:"+policyId.strip()+ ", expected response code: "+str(responsecode)+", got: None")
                                sys.exit()

                            if (resp.status_code != responsecode):
                                if (resp.status_code >= 500) and (http_retry_count < 600 ) and (retry_cnt > 1):
                                    sleep(0.1)
                                    retry_cnt -= 1
                                    http_retry_count += 1
                                else:
                                    print("1Put failed for id:"+policyId.strip()+ ", expected response code: "+str(responsecode)+", got: "+str(resp.status_code))
                                    print(url_out)
                                    print(json.loads(payload))
                                    sys.exit()
                            else:
                                retry_cnt=-1
                start  += 1
    print("0 http retries:"+str(http_retry_count) + ", connect retries: "+str(connect_retry_count))
    sys.exit()

except Exception as e:
    print("1"+str(e))
    traceback.print_exc()
    print(str(data_out))
sys.exit()