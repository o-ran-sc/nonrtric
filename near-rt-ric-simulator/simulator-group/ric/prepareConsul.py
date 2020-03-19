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

import json
import subprocess
import os

print(" Update fresh ric configuration in Consul configuration file")

p = os.path.abspath('..')
consul_config = p + '/consul_cbs' + '/config.json'


def write_json(data, filename=consul_config):
    with open(filename, 'w') as f:
        json.dump(data, f, indent=4)


def bash_command(cmd):
    result = []
    sp = subprocess.Popen(['/bin/bash', '-c', cmd], stdout=subprocess.PIPE)
    for line in sp.stdout.readlines():
        result.append(line.decode().strip())
    return result


command = "docker ps | grep simulator | awk '{print $NF}'"

ric_list = bash_command(command)

with open(consul_config) as json_file:
    data = json.load(json_file)
    temp = data['ric']
    for ric in ric_list:
        y = {"name": ric,
             "baseUrl": "http://" + ric + ":8085/",
             "managedElementIds": [
                 "kista_" + ric,
                 "stockholm_" + ric
             ]
             }
        temp.append(y)


write_json(data)
print(" Update Consul config file with fresh ric configuration,  done")
