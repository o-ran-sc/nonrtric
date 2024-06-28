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
import re



#Create a ric info json, example input: ricsim_g1_1:kista_ricsim_g1_1,stockholm_ricsim_g1_1:1,2,4 ricsim_g1_2:kista_ricsim_g1_2,stockholm_ricsim_g1_2:2
#Format of string <ric-id>:<comma-separated-list-of-me's>:<comma-separated-list-of-policy-type-ids>
#To indicate that no types exist, use 'NOTYPE'. Ex. ricsim_g1_1:kista_ricsim_g1_1,stockholm_ricsim_g1_1:NOTYPE
#To indicate that special STD zero length name type, use 'EMPTYTYPE'. Ex. ricsim_g1_1:kista_ricsim_g1_1,stockholm_ricsim_g1_1:EMPTYTYPE
#Save in indicated file

#arg: <file-name-for-result> <api-version> <list-ric-info>
try:
    file_name = sys.argv[1]
    api_version=sys.argv[2]
    ric_string = sys.argv[3]
    ric_string=ric_string.strip()
    ric_string = re.sub(' +',' ',ric_string)
    ric_arr=[]
    rics=ric_string.split(' ')
    if (api_version == "V2"):
        param_ric='ric_id'
        param_me='managed_element_ids'
        param_policy_type='policytype_ids'
        param_state='state'
    elif (api_version == "V3"):
        param_ric='ricId'
        param_me='managedElementIds'
        param_policy_type='policyTypeIds'
        param_state='state'
    else:
        param_ric='ricName'
        param_me='managedElementIds'
        param_policy_type='policyTypes'
        param_state='state'

    for i in range(len(rics)):
        ricDict={}
        items=rics[i].split(':')
        ricDict[param_ric]=items[0]
        ricDict[param_me]=items[1].split(',')
        if (items[2] == "EMPTYTYPE"):
            empty_arr=[]
            empty_arr.append("")
            ricDict[param_policy_type]=empty_arr
        elif (items[2] == "NOTYPE"):
            empty_arr=[]
            ricDict[param_policy_type]=empty_arr
        else:
            ricDict[param_policy_type]=items[2].split(',')
        ricDict[param_state]=items[3]
        ric_arr.append(ricDict)

    with open(file_name, 'w') as f:
        json.dump(ric_arr, f)

    print(0)

except Exception as e:
    print(str(e))
    print(1)
sys.exit()