# ============LICENSE_START=======================================================
#  Copyright (C) 2019 Nordix Foundation.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#!/usr/bin/python

import re
import sys


# Convert word from foo-bar to FooBar
# words begining with a digit will be converted to _digit
def to_enum(s):
    if s[0].isdigit():
        s = "_" + s
    else:
        s = s[0].upper() + s[1:]
    return re.sub(r'(?!^)-([a-zA-Z])', lambda m: m.group(1).upper(), s)

leaf = ""
val = ""
li = []

if len(sys.argv) < 3:
     print('yang2props.py <input yang> <output properties>')
     sys.exit(2)

with open(sys.argv[1], "r") as ins:
    for line in ins:
        # if we see a leaf save the name for later
        if "leaf " in line:
            match = re.search(r'leaf (\S+)', line)
            if match:
                leaf = match.group(1)
      
        # if we see enum convert the value to enum format and see if it changed
        # if the value is different write a property entry
        if "enum " in line:
            match = re.search(r'enum "(\S+)";', line)
            if match:
                val = match.group(1)
                enum = to_enum(val)
                # see if converting to enum changed the string
                if val != enum:
                    property = "yang."+leaf+"."+enum+"="+val
                    if property not in li:
                        li.append( property)


# Open output file
fo = open(sys.argv[2], "w")
fo.write("# yang conversion properties \n")
fo.write("# used to convert Enum back to the original yang value \n")
fo.write("\n".join(li))
fo.write("\n")

# Close opend file
fo.close()

   
