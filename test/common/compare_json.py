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

# This script compare two jsons for equality, taken into account that the parameter values
# marked with '????' are not checked (only the parameter name need to exist)
# Example of target json with '????'
# [
#   {
#     "callbackUrl": "????",
#     "keepAliveIntervalSeconds": "????",
#     "serviceName": "serv2",
#     "timeSinceLastActivitySeconds": "????"
#   },
#   {
#     "callbackUrl": "????",
#     "keepAliveIntervalSeconds": "????",
#     "serviceName": "serv1",
#     "timeSinceLastActivitySeconds": "????"
#   }
#]


import os
import json
import sys

# # Helper function to compare two json list.
# # Returns true for equal, false for not equal
def compare_json_list(list1, list2):
    if (list1.__len__() != list2.__len__()):
        return False

    for l in list1:
        found = False
        for m in list2:
            res = compare_json_obj(l, m)
            if (res):
                found = True
                break

        if (not found):
            return False

    return True

# Deep compare of two json objects
# If a parameter value in the target json is set to '????' then the result json value is not checked for the that parameter
# Return true for equal json, false for not equal json
def compare_json_obj(obj1, obj2):
    if isinstance(obj1, list):
        if (not isinstance(obj2, list)):
            return False
        return compare_json_list(obj1, obj2)
    elif (isinstance(obj1, dict)):
        if (not isinstance(obj2, dict)):
            return False
        exp = set(obj2.keys()) == set(obj1.keys())
        if (not exp):
            return False
        for k in obj1.keys():
            val1 = obj1.get(k)
            val2 = obj2.get(k)
            if isinstance(val1, list):
                if (not compare_json_list(val1, val2)):
                    return False
            elif isinstance(val1, dict):
                if (not compare_json_obj(val1, val2)):
                    return False
            else:
                #Do not check parameter values marked with '????'
                if ((val1 != "????") and (val2 != val1)) and ((val2 != "????") and (val2 != val1)):
                    return False
    else:
        return obj1 == obj2

    return True


try:
    #Read the input file and compare the two json (target->result)
    jsonTarget = json.loads(sys.argv[1])
    jsonResult = json.loads(sys.argv[2])
    res1=compare_json_obj(jsonTarget, jsonResult)

    #Read the json again (in case the previous calls has re-arranged the jsons)
    jsonTarget = json.loads(sys.argv[1])
    jsonResult = json.loads(sys.argv[2])
    #Compare the opposite order (result->target) to catch special duplicate json key cases
    res2=compare_json_obj(jsonResult, jsonTarget)

    if (res1 and res2):
        print (0)
    else:
        print (1)

except Exception as e:
    print (1)
sys.exit()