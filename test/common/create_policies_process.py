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

# This script create/update policies spread over a number rics
# Intended for parallel processing
# Returns a string with result, either "0" for ok, or "1<fault description>"

import os
import json
import sys
import requests
import traceback
from time import sleep

# disable warning about unverified https requests
from requests.packages import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

#arg responsecode baseurl ric_base num_rics uuid startid templatepath count pids pid_id proxy
data_out=""
url_out=""
try:

    if len(sys.argv) < 12:
        print("1Expected 12/15 args, got "+str(len(sys.argv)-1))
        print (sys.argv[1:])
        sys.exit()
    responsecode=int(sys.argv[1])
    baseurl=str(sys.argv[2])
    ric_base=str(sys.argv[3])
    num_rics=int(sys.argv[4])
    uuid=str(sys.argv[5])
    start=int(sys.argv[6])
    httpproxy="NOPROXY"
    api_prefix_v3=os.getenv('A1PMS_API_PREFIX_V3', '')
    if ("/v2/" in baseurl) or (api_prefix_v3 + "/v1/" in baseurl):
        if len(sys.argv) != 16:
            print("1Expected 15 args, got "+str(len(sys.argv)-1)+ ". Args: responsecode baseurl ric_base num_rics uuid startid service type transient notification-url templatepath count pids pid_id proxy")
            print (sys.argv[1:])
            sys.exit()

        serv=str(sys.argv[7])
        pt=str(sys.argv[8])
        trans=str(sys.argv[9])
        noti=str(sys.argv[10])
        templatepath=str(sys.argv[11])
        count=int(sys.argv[12])
        pids=int(sys.argv[13])
        pid_id=int(sys.argv[14])
        httpproxy=str(sys.argv[15])
    else:
        if len(sys.argv) != 12:
            print("1Expected 11 args, got "+str(len(sys.argv)-1)+ ". Args: responsecode baseurl ric_base num_rics uuid startid templatepath count pids pid_id proxy")
            print (sys.argv[1:])
            sys.exit()

        templatepath=str(sys.argv[7])
        count=int(sys.argv[8])
        pids=int(sys.argv[9])
        pid_id=int(sys.argv[10])
        httpproxy=str(sys.argv[11])

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

        for i in range(start,stop):
            if (i%pids == (pid_id-1)):
                payload=template.replace("XXX",str(i))
                ric_id=(i%num_rics)+1
                ric=ric_base+str(ric_id)

                connect_ok=False
                retry_cnt=5
                while(retry_cnt>0):
                    try:
                        headers = {'Content-type': 'application/json'}
                        if ("/v2/" in baseurl):
                            url=baseurl

                            data={}
                            data["ric_id"]=ric
                            data["policy_id"]=uuid+str(i)
                            data["service_id"]=serv
                            if (trans != "NOTRANSIENT"):
                                data["transient"]=trans
                            if (pt != "NOTYPE"):
                                data["policytype_id"]=pt
                            else:
                                data["policytype_id"]=""
                            data["policy_data"]=json.loads(payload)

                            url_out=url
                            data_out=json.dumps(data)
                            if proxydict is None:
                                resp=requests.put(url, data_out, headers=headers, verify=False, timeout=90)
                            else:
                                resp=requests.put(url, data_out, headers=headers, verify=False, timeout=90, proxies=proxydict)

                        elif (api_prefix_v3 + '/v1' in baseurl):
                            url=baseurl

                            data={}
                            data["nearRtRicId"]=ric
                            data["serviceId"]=serv
                            if (trans != "NOTRANSIENT"):
                                data["transient"]=trans
                            if (pt != "NOTYPE"):
                                data["policyTypeId"]=pt
                            else:
                                data["policyTypeId"]=""
                            data["policyObject"]=json.loads(payload)

                            url_out=url
                            data_out=json.dumps(data)
                            if proxydict is None:
                                resp=requests.post(url, data_out, headers=headers, verify=False, timeout=90)
                            else:
                                resp=requests.post(url, data_out, headers=headers, verify=False, timeout=90, proxies=proxydict)
                        else:
                            url=baseurl+"&id="+uuid+str(i)+"&ric="+str(ric)
                            url_out=url
                            data_out=json.dumps(json.loads(payload))
                            if proxydict is None:
                                resp=requests.put(url, data_out, headers=headers, verify=False, timeout=90)
                            else:
                                resp=requests.put(url, data_out, headers=headers, verify=False, timeout=90, proxies=proxydict)
                        connect_ok=True
                    except Exception as e1:
                        if (retry_cnt > 1):
                            sleep(0.1)
                            retry_cnt -= 1
                            connect_retry_count += 1
                        else:
                            print("1Put failed for id:"+uuid+str(i)+ ", "+str(e1) + " "+traceback.format_exc())
                            sys.exit()

                    if (connect_ok == True):
                        if (resp.status_code == None):
                            print("1Put failed for id:"+uuid+str(i)+ ", expected response code: "+str(responsecode)+", got: None")
                            sys.exit()

                        if (resp.status_code != responsecode):
                            if (resp.status_code >= 500) and (http_retry_count < 600 ) and (retry_cnt > 1):
                                sleep(0.1)
                                retry_cnt -= 1
                                http_retry_count += 1
                            else:
                                print("1Put failed for id:"+uuid+str(i)+ ", expected response code: "+str(responsecode)+", got: "+str(resp.status_code))
                                print(url_out)
                                print(str(data_out))
                                sys.exit()
                        else:
                            retry_cnt=-1

    print("0 http retries:"+str(http_retry_count) + ", connect retries: "+str(connect_retry_count))
    sys.exit()

except Exception as e:
    print("1"+str(e))
    traceback.print_exc()
    print(str(data_out))
sys.exit()