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

# disable warning about unverified https requests
from requests.packages import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

#arg responsecode baseurl ric_base num_rics startid templatepath count pids pid_id
try:
    if len(sys.argv) != 10:
        print("1Expected 9 args, got "+str(len(sys.argv)-1)+ ". Args: responsecode baseurl ric_base num_rics startid templatepath count pids pid_id")
        sys.exit()

    responsecode=int(sys.argv[1])
    baseurl=sys.argv[2]
    ric_base=sys.argv[3]
    num_rics=int(sys.argv[4])
    start=int(sys.argv[5])
    templatepath=sys.argv[6]
    count=int(sys.argv[7])
    pids=int(sys.argv[8])
    pid_id=int(sys.argv[9])

    with open(templatepath, 'r') as file:
        template = file.read()

        start=start
        stop=count*num_rics+start

        for i in range(start,stop):
            if (i%pids == (pid_id-1)):
                payload=template.replace("XXX",str(i))
                ric_id=(i%num_rics)+1
                ric=ric_base+str(ric_id)
                url=baseurl+"&id="+str(i)+"&ric="+str(ric)
                try:
                    headers = {'Content-type': 'application/json'}
                    resp=requests.put(url, json.dumps(json.loads(payload)), headers=headers, verify=False, timeout=90)
                except Exception as e1:
                    print("1Put failed for id:"+str(i)+ ", "+str(e1) + " "+traceback.format_exc())
                    sys.exit()
                if (resp.status_code == None):
                    print("1Put failed for id:"+str(i)+ ", expected response code: "+responsecode+", got: None")
                    sys.exit()
                if (resp.status_code != responsecode):
                    print("1Put failed for id:"+str(i)+ ", expected response code: "+responsecode+", got: "+str(resp.status_code))
                    sys.exit()

    print("0")
    sys.exit()

except Exception as e:
    print("1"+str(e))
sys.exit()