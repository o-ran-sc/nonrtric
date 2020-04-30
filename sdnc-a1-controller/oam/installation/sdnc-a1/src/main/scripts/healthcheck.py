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
#


# coding=utf-8
import os
import httplib
import base64
import time

username = os.environ['ODL_ADMIN_USERNAME']
password = os.environ['ODL_ADMIN_PASSWORD']
TIMEOUT=1000
INTERVAL=30
timePassed=0

headers = {'Authorization':'Basic %s' % base64.b64encode(username + ":" + password),
           'X-FromAppId': 'csit-sdnc',
           'X-TransactionId': 'csit-sdnc',
           'Accept':"application/json",
           'Content-type':"application/json"}

def makeHealthcheckCall(headers, timePassed):
    connected = False
    # WAIT 10 minutes maximum and test every 30 seconds if HealthCheck API is returning 200
    while timePassed < TIMEOUT:
        try:
            conn = httplib.HTTPConnection("localhost",8181)
            req = conn.request("POST", "/restconf/operations/SLI-API:healthcheck",headers=headers)
            res = conn.getresponse()
            res.read()
            if res.status == 200:
                print ("Healthcheck Passed in %d seconds." %timePassed)
                connected = True
                break
            else:
                print ("Sleep: %d seconds before testing if Healthcheck worked. Total wait time up now is: %d seconds. Timeout is: %d seconds" %(INTERVAL, timePassed, TIMEOUT))
        except:
            print ("Cannot execute REST call. Sleep: %d seconds before testing if Healthcheck worked. Total wait time up now is: %d seconds. Timeout is: %d seconds" %(INTERVAL, timePassed, TIMEOUT))
        timePassed = timeIncrement(timePassed)

    if timePassed > TIMEOUT:
        print ("TIME OUT: Healthcheck not passed in  %d seconds... Could cause problems for testing activities..." %TIMEOUT)
    return connected


def timeIncrement(timePassed):
    time.sleep(INTERVAL)
    timePassed = timePassed + INTERVAL
    return timePassed

makeHealthcheckCall(headers, timePassed)
