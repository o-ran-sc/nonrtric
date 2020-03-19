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

print(" Clean old ric configurations in Consul config file")

p = os.path.abspath('..')
consul_config = p + '/consul_cbs' + '/config.json'


def write_json(data, filename=consul_config):
    with open(filename, 'w') as f:
        json.dump(data, f, indent=4)


with open(consul_config) as json_file:
    clean = json.load(json_file)
    clean['ric'] = []


write_json(clean)
print(" Clean old ric configurations from Consul config file, done")


