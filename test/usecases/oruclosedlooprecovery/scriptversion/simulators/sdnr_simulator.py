
#  ============LICENSE_START===============================================
#  Copyright (C) 2021 Nordix Foundation. All rights reserved.
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

from flask import Flask
from flask import Response
from flask_httpauth import HTTPBasicAuth
import json
import os
import random
import requests
import threading
import time

# Provides an endpoint for the "UNLOCK" configuration change for an O-DU.
# Stores the ID of the O-DU and randomly, after between 0 and 10 seconds, sends an Alarm Notification that clears the
# "CUS Link Failure" alarm event to MR.
app = Flask(__name__)
auth = HTTPBasicAuth()

mr_host = "http://localhost"
mr_port = "3904"
MR_PATH = "/events/unauthenticated.SEC_FAULT_OUTPUT"

# Server info
HOST_IP = "::"
HOST_PORT = 9990
APP_URL = "/rests/data/network-topology:network-topology/topology=topology-netconf/node=<string:o_du_id>/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions=<string:o_du_id2>/radio-resource-management-policy-ratio=rrm-pol-1"

USERNAME = "admin"
PASSWORD = "Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U"

FAULT_ID = "28"

linkFailureMessage = {
    "event": {
        "commonEventHeader": {
            "domain": "fault",
            "eventId": "nt:network-topology/nt:topology/nt:node/nt:node-id",
            "eventName": "fault_O-RAN-RU-Fault_Alarms_CUS_Link_Failure",
            "eventType": "O-RAN-RU-Fault_Alarms",
            "sequence": 0,
            "priority": "High",
            "reportingEntityId": "SDNR",
            "reportingEntityName": "",
            "sourceId": "",
            "sourceName": "oru1",
            "startEpochMicrosec": "@timestamp@",
            "lastEpochMicrosec": "@timestamp@",
            "nfNamingCode": "",
            "nfVendorName": "ietf-hardware (RFC8348) /hardware/component[not(parent)][1]/mfg-name",
            "timeZoneOffset": "+00:00",
            "version": "4.1",
            "vesEventListenerVersion": "7.2.1"
        },
        "faultFields": {
            "faultFieldsVersion": "4.0",
            "alarmCondition": FAULT_ID,
            "alarmInterfaceA": "o-ran-fm:alarm-notif/fault-source",
            "eventSourceType": "ietf-hardware (RFC8348) /hardware/component[not(parent)][1]/mfg-model or \"O-RU\"",
            "specificProblem": "",
            "eventSeverity": "NORMAL",
            "vfStatus": "Active",
            "alarmAdditionalInformation": {
                "eventTime": "@eventTime@",
                "equipType": "@type@",
                "vendor": "@vendor@",
                "model": "@model@"
            }
        }
    }
}


class AlarmClearThread (threading.Thread):

    def __init__(self, sleep_time, o_du_id):
        threading.Thread.__init__(self)
        self.sleep_time = sleep_time
        self.o_du_id = o_du_id

    def run(self):
        print(f'Sleeping: {self.sleep_time} before clearing O-DU: {self.o_du_id}')
        time.sleep(self.sleep_time)
        msg_as_json = json.loads(json.dumps(linkFailureMessage))
        msg_as_json["event"]["commonEventHeader"]["sourceName"] = self.o_du_id
        print("Sedning alarm clear for O-DU: " + self.o_du_id)
        requests.post(mr_host + ":" + mr_port + MR_PATH, json=msg_as_json);


# I'm alive function
@app.route('/',
    methods=['GET'])
def index():
    return 'OK', 200


@auth.verify_password
def verify_password(username, password):
    if username == USERNAME and password == PASSWORD:
        return username


@app.route(APP_URL,
    methods=['PUT'])
@auth.login_required
def sendrequest(o_du_id, o_du_id2):
    print("Got request with O-DU ID: " + o_du_id)
    random_time = int(10 * random.random())
    alarm_clear_thread = AlarmClearThread(random_time, o_du_id)
    alarm_clear_thread.start()

    return Response(status=200)


if __name__ == "__main__":
    if os.getenv("MR-HOST") is not None:
        mr_host = os.getenv("MR-HOST")
        print("Using MR Host from os: " + mr_host)
    if os.getenv("MR-PORT") is not None:
        mr_port = os.getenv("MR-PORT")
        print("Using MR Port from os: " + mr_port)

    app.run(port=HOST_PORT, host=HOST_IP)
