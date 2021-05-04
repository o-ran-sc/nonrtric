
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

from flask import Flask, request
from flask import Response
import json
import random
import requests
import threading

# Provides an endpoint for the "UNLOCK" configuration change for an O-DU.
# Stores the ID of the O-DU and randomly, after between 0 and 10 seconds, sends an Alarm Notification that clears the
# "CUS Link Failure" alarm event to MR.
app = Flask(__name__)

# Server info
HOST_IP = "::"
HOST_PORT = 9990
APP_URL = "/rests/data/network-topology:network-topology/topology=topology-netconf/node=O-RAN-DU-01/yang-ext:mount/"

linkFailureMessage = {
    "event": {
        "commonEventHeader": {
            "domain": "fault",
            "eventId": "nt:network-topology/nt:topology/nt:node/nt:node-id",
            "eventName": "fault_O-RAN-RU-Fault_Alarms_CUS_Link_Failure",
            "eventType": "O-RAN-RU-Fault_Alarms",
            "sequence": 0,
            "priority": "High",
            "reportingEntityId": "uro1",
            "reportingEntityName": "@controllerName@",
            "sourceId": "",
            "sourceName": "nt:network-topology/nt:topology/nt:node/nt:node-id",
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
            "alarmCondition": "o-ran-fm:alarm-notif/fault-id",
            "alarmInterfaceA": "o-ran-fm:alarm-notif/fault-source",
            "eventSourceType": "ietf-hardware (RFC8348) /hardware/component[not(parent)][1]/mfg-model or \"O-RU\"",
            "specificProblem": "CUS Link Failure",
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

    def __init__(self, sleep_time, o_ru_id):
        threading.Thread.__init__(self)
        self.sleep_time = sleep_time
        self.o_ru_id = o_ru_id

    def run(self):
        print(f'Sleeping: {self.sleep_time} before clearing O-DU: {self.o_ru_id}')
        msg_as_json = json.loads(json.dumps(linkFailureMessage))
        msg_as_json["event"]["commonEventHeader"]["reportingEntityId"] = self.o_ru_id
        requests.post("http://localhost:3904/events/ALARMS-WRITE", json=msg_as_json);


# I'm alive function
@app.route('/',
    methods=['GET'])
def index():
    return 'OK', 200


@app.route(APP_URL + "o-ran-sc-du-hello-world:network-function/<id>",
    methods=['POST'])
def sendrequest(id):
    o_du_id = id.split("=")[1]
    print("Config change for O-DU with ID " + o_du_id)
    payload = json.loads(json.dumps(request.json))
    o_ru_id = payload["o-ran-sc-du-hello-world:du-to-ru-connection"][0]["name"]
    random_time = int(10 * random.random())
    alarm_clear_thread = AlarmClearThread(random_time, o_ru_id)
    alarm_clear_thread.start()

    return Response(status=201)


if __name__ == "__main__":
    app.run(port=HOST_PORT, host=HOST_IP)
