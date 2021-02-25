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

import os
import json
import sys

# Extract the response code and optional response message body from and SDNC A1 Controller API reply
# Args: <json-output-key> <file-containing-the response>
try:
    with open(sys.argv[2]) as json_file:
        reply = json.load(json_file)

        output=reply[sys.argv[1]]
        status=str(output['http-status'])
        while(len(status) < 3):
            status="0"+status
        resp=status
        if ( 'body' in output.keys()):
            body=str(output['body'])
            try:
                bodyJson=json.loads(body)
                resp=str(json.dumps(bodyJson))+str(status)
            except Exception as e1:
                resp=body+str(status)

        print(resp)

except Exception as e:
    print("000")
sys.exit()