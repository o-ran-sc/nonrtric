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

# Deep compare of two json obects
# If a parameter value in the target json is set to '????' then the result json value is not checked for the that parameter
# Any included json array will be sorted before comparison
# An optional array key can be given to sort array of objects containing that key
# Both the jsonTarget and the jsonResult object must to be ordered
# Return 0 for equal json, 1 of not equal json

def compare_json(jsonTarget,jsonResult):

    if isinstance(jsonTarget, dict):
        if not isinstance(jsonResult, dict):
            return 1
        if (len(jsonTarget) != len(jsonResult)):
            return 1
        for key in jsonTarget.keys():
            if (jsonResult.get(key) is None):
                return 1
            if (compare_json(jsonTarget.get(key), jsonResult.get(key)) != 0):
                return 1
    elif isinstance(jsonTarget, list):
        if not isinstance(jsonResult, list):
            return 1
        if (len(jsonTarget) != len(jsonResult)):
            return 1
        for i in range(len(jsonTarget)):
            if (compare_json(jsonTarget[i], jsonResult[i]) != 0):
                return 1
    elif isinstance(jsonTarget, tuple):
        if not isinstance(jsonResult, tuple):
            return 1
        if (jsonTarget[0] != jsonResult[0]):
            return 1
        if isinstance(jsonTarget[1], dict):
            return compare_json(jsonTarget[1], jsonResult[1])
        elif isinstance(jsonTarget[1], list):
            return compare_json(jsonTarget[1], jsonResult[1])
        else:
            if (jsonTarget[1] != "????") and (jsonTarget[1] != jsonResult[1]):
                return 1
    else:
        return 0
    return 0

# Deep ordering of a json structure
def ordered_json(obj):
    if isinstance(obj, dict):
        return sorted((k, ordered_json(v)) for k, v in obj.items())
    if isinstance(obj, list):
        return sorted(ordered_json(x) for x in obj)
    else:
        return obj

# def comparejson(jsonTarget,jsonResult,arrayKey):

#     if isinstance(jsonTarget, dict):
#         if (len(jsonTarget) != len(jsonResult)):
#             return 1
#         for key in jsonTarget.keys():
#             if (jsonResult.get(key) is None):
#                 return 1
#             if (comparejson(jsonTarget.get(key), jsonResult.get(key), arrayKey) != 0):
#                 return 1
#     elif isinstance(jsonTarget, list):
#         if (len(jsonTarget) != len(jsonResult)):
#             return 1
#         if (arrayKey is None):
#             jsonTarget.sort()
#             jsonResult.sort()
#         else:
#             jsonTarget.sort(key=lambda k: k[arrayKey])
#             jsonResult.sort(key=lambda k: k[arrayKey])

#         for i in range(len(jsonTarget)):
#             if (comparejson(jsonTarget[i], jsonResult[i], arrayKey) != 0):
#                 return 1
#     else:
#         if (jsonTarget != "????") and (jsonTarget != jsonResult):
#             return 1
#     return 0

try:
    jsonTarget = json.loads(sys.argv[1])
    jsonResult = json.loads(sys.argv[2])

    orderedTarget=ordered_json(jsonTarget)
    orderedResult=ordered_json(jsonResult)

    print(compare_json(orderedTarget, orderedResult))

except Exception as e:
    print (1)
sys.exit()