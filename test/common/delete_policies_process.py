#  ============LICENSE_START===============================================
#  Copyright (C) 2020 Nordix Foundation. All rights reserved.
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

# This script delete policies spread over a number rics
# Intended for parallel processing
# Returns a string with result, either "0" for ok, or "1<fault description>"

import os
import json
import sys
import requests
import traceback

# disable warning about unverified https requests
from requests.packages import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

#arg responsecode baseurl num_rics uuid startid count pids pid_id proxy

try:
    if len(sys.argv) != 10:
        print("1Expected 9 args, got "+str(len(sys.argv)-1)+ ". Args: responsecode baseurl num_rics uuid startid count pids pid_id proxy")
        sys.exit()

    responsecode=int(sys.argv[1])
    baseurl=str(sys.argv[2])
    num_rics=int(sys.argv[3])
    uuid=str(sys.argv[4])
    start=int(sys.argv[5])
    count=int(sys.argv[6])
    pids=int(sys.argv[7])
    pid_id=int(sys.argv[8])
    httpproxy=str(sys.argv[9])

    proxydict=None
    if httpproxy != "NOPROXY":
        proxydict = {
            "http" : httpproxy,
            "https" : httpproxy
        }
    if uuid == "NOUUID":
        uuid=""

    total_retry_count=0

    stop=count*num_rics+start
    for i in range(start,stop):
        if (i%pids == (pid_id-1)):
            retry_cnt=5
            while(retry_cnt>0):
                if ("/v2/policies/" in baseurl):
                    url=str(baseurl+uuid+str(i))
                else:
                    url=str(baseurl+"?id="+uuid+str(i))
                try:
                    if proxydict is None:
                        resp=requests.delete(url, verify=False, timeout=90)
                    else:
                        resp=requests.delete(url, verify=False, timeout=90, proxies=proxydict)
                except Exception as e1:
                    print("1Delete failed for id:"+uuid+str(i)+ ", "+str(e1) + " "+traceback.format_exc())
                    sys.exit()
                if (resp.status_code == None):
                    print("1Delete failed for id:"+uuid+str(i)+ ", expected response code: "+str(responsecode)+", got: None")
                    sys.exit()
                if (resp.status_code != responsecode):
                    if (resp.status_code == 503 ) and (retry_cnt > 1):
                        sleep(0.1)
                        retry_cnt -= 1
                        total_retry_count += 1
                    else:
                        print("1Delete failed for id:"+uuid+str(i)+ ", expected response code: "+str(responsecode)+", got: "+str(resp.status_code))
                        sys.exit()
                else:
                    retry_cnt=-1

    if (total_retry_count > 0):
        print("0 retries:"+str(total_retry_count))
    else:
        print("0")
    sys.exit()

except Exception as e:
    print("1"+str(e))
    traceback.print_exc()
sys.exit()