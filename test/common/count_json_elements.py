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

# Print the length of json array, -1 will be printed in any problem is encountered
# Assumes the top level json is an array.
# If not (and the number of keys on top level is 1) then assumes that key contains the array.

arr_len=-1
try:
    with open(sys.argv[1]) as json_file:
        jsonarray = json.load(json_file)
        if isinstance(jsonarray, list):
            arr_len=len(jsonarray)
        elif isinstance(jsonarray, dict) and len(jsonarray) == 1:
            key=next(iter(jsonarray))
            jsonarray=jsonarray[key]
            if isinstance(jsonarray, list):
                arr_len = len(jsonarray)

except Exception as e:
    print(arr_len)
    sys.exit()

print(arr_len)
sys.exit()