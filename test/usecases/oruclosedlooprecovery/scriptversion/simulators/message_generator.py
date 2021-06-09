
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

import json
import os
import random
import requests
import time

# Randomly, between 0 and 10 seconds sends a "CUS Link Failure" alarm event to the Message Router. The ID of the O-RU is also
# randomly generated between 0 and 9.
# When the modulo of the ID is 1, a "heartbeat" message will also be sent to MR.

mr_host = "http://localhost"
mr_port = "3904"
MR_PATH = "/events/unauthenticated.SEC_FAULT_OUTPUT"
FAULT_ID = "28"

linkFailureMessage = {
    "event": {
        "commonEventHeader": {
            "domain": "fault",
            "eventId": "nt:network-topology/nt:topology/nt:node/nt:node-id",
            "eventName": "fault_O-RAN-RU-Fault_Alarms_CUS_Link_Failure",
            "eventType": "O-RAN-RU-Fault",
            "sequence": 0,
            "priority": "Normal",
            "reportingEntityId": "SDNR",
            "reportingEntityName": "@controllerName@",
            "sourceId": "",
            "sourceName": "O-RU-ID",
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
            "eventSeverity": "CRITICAL",
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

heartBeatMessage = {
   "event": {
     "commonEventHeader": {
       "version": 3.0,
       "domain": "heartbeat",
       "eventName": "Heartbeat\_vIsbcMmc",
       "eventId": "ab305d54-85b4-a31b-7db2fb6b9e546015",
       "sequence": 0,
       "priority": "Normal",
       "reportingEntityId": "cc305d54-75b4-431badb2eb6b9e541234",
       "reportingEntityName": "EricssonOamVf",
       "sourceId": "de305d54-75b4-431b-adb2-eb6b9e546014",
       "sourceName": "ibcx0001vm002ssc001",
       "nfNamingCode": "ibcx",
       "nfcNamingCode": "ssc",
       "startEpochMicrosec": 1413378172000000,
       "lastEpochMicrosec": 1413378172000000
      }
   }
 }


def sendPostRequest(url, msg):
    try:
        requests.post(url, json=msg)
    except Exception as e:
        print(type(e))
        print(e.args)
        print(e)


if __name__ == "__main__":
    if os.getenv("MR-HOST") is not None:
        mr_host = os.getenv("MR-HOST")
        print("Using MR Host from os: " + mr_host)
    if os.getenv("MR-PORT") is not None:
        mr_port = os.getenv("MR-PORT")
        print("Using MR Port from os: " + mr_port)

    mr_url = mr_host + ":" + mr_port + MR_PATH
    print(mr_url)
    while True:
        random_time = int(10 * random.random())
        if (random_time % 3 == 1):
            print("Sent heart beat")
            sendPostRequest(mr_url, heartBeatMessage)

        o_ru_id = "ERICSSON-O-RU-1122" + str(random_time)
        print("Sent link failure for O-RAN-RU: " + o_ru_id)
        msg_as_json = json.loads(json.dumps(linkFailureMessage))
        msg_as_json["event"]["commonEventHeader"]["sourceName"] = o_ru_id
        sendPostRequest(mr_url, msg_as_json)

        time.sleep(random_time)

