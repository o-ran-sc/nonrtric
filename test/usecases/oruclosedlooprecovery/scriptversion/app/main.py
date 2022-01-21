
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

import argparse
import ast
import json
import os
import requests
import time

MR_PATH = "/events/[TOPIC]/users/test/"
SDNR_PATH = "/rests/data/network-topology:network-topology/topology=topology-netconf/node=[O-DU-ID]/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions=[O-DU-ID]/radio-resource-management-policy-ratio=rrm-pol-1"
FAUILT_ID = "28"

UNLOCK_MESSAGE = {
    "o-ran-sc-du-hello-world:radio-resource-management-policy-ratio":
    [
        {
            "id":"rrm-pol-1",
            "radio-resource-management-policy-max-ratio":25,
            "radio-resource-management-policy-members":
                [
                    {
                        "mobile-country-code":"310",
                        "mobile-network-code":"150",
                        "slice-differentiator":1,
                        "slice-service-type":1
                    }
                ],
            "radio-resource-management-policy-min-ratio":15,
            "user-label":"rrm-pol-1",
            "resource-type":"prb",
            "radio-resource-management-policy-dedicated-ratio":20,
            "administrative-state":"unlocked"
        }
    ]
}


def is_message_new_link_failure(message):
    msg_as_json = json.loads(message)
    event_headers = msg_as_json["event"]["commonEventHeader"]

    link_failure = False
    if (event_headers["domain"] == "fault"):
        fault_fields = msg_as_json["event"]["faultFields"]
        link_failure = fault_fields["alarmCondition"] == FAUILT_ID and fault_fields["eventSeverity"] != "NORMAL"

    return link_failure


def is_message_clear_link_failure(message):
    msg_as_json = json.loads(message)
    event_headers = msg_as_json["event"]["commonEventHeader"]

    link_failure_clear = False
    if (event_headers["domain"] == "fault"):
        fault_fields = msg_as_json["event"]["faultFields"]
        link_failure_clear = fault_fields["alarmCondition"] == FAUILT_ID and fault_fields["eventSeverity"] == "NORMAL"

    return link_failure_clear


def handle_link_failure(message, o_ru_to_o_du_map, sdnr_address, sdnr_user, sdnr_pwd):
    verboseprint("Got a link failure: ")
    alarm_msg_as_json = json.loads(message)
    event_headers = alarm_msg_as_json["event"]["commonEventHeader"]
    o_ru_id = event_headers["sourceName"]
    verboseprint("O-RU ID: " + o_ru_id)
    if o_ru_id in o_ru_to_o_du_map:
        o_du_id = o_ru_to_o_du_map[o_ru_id]
        verboseprint("O-DU ID: " + o_du_id)
        unlock_msg = json.loads(json.dumps(UNLOCK_MESSAGE))
        send_path = SDNR_PATH.replace("[O-DU-ID]", o_du_id)
        requests.put(sdnr_address + send_path, auth=(sdnr_user, sdnr_pwd), json=unlock_msg)
    else:
        print("ERROR: No mapping for O-RU ID: " + o_ru_id)


def handle_clear_link_failure(message):
    msg_as_json = json.loads(message)
    event_headers = msg_as_json["event"]["commonEventHeader"]
    o_ru_id = event_headers["sourceName"]
    verboseprint("Cleared Link Failure for O-RU ID: " + o_ru_id)


def read_o_ru_to_o_du_map_from_file(map_file):
    file = open(map_file, "r")
    contents = file.read()
    dictionary = ast.literal_eval(contents)
    file.close()
    return dictionary


def poll_and_handle_messages(mr_address, sdnr_address, sdnr_user, sdnr_pwd):
    while True:
        try:
            verboseprint("Polling")
            response = requests.get(mr_address)
            messages = response.json()
            for message in messages:
                if (is_message_new_link_failure(message)):
                    handle_link_failure(message, o_ru_to_o_du_map, sdnr_address, sdnr_user, sdnr_pwd)
                elif (is_message_clear_link_failure(message)):
                    handle_clear_link_failure(message)
        except Exception as inst:
            print(inst)

        time.sleep(pollTime)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(prog='PROG')
    parser.add_argument('--mrHost', help='The URL of the MR host (default: %(default)s)', default="http://message-router.onap")
    parser.add_argument('--mrPort', help='The port of the MR host (default: %(default)d)', type=int, default=3904)
    parser.add_argument('--mrTopic', help='The topic to poll messages from (default: %(default)s)', default="unauthenticated.SEC_FAULT_OUTPUT")
    parser.add_argument('--sdnrHost', help='The URL of the SNDR host (default: %(default)s)', default="http://localhost")
    parser.add_argument('--sdnrPort', help='The port of the SDNR host (default: %(default)d)', type=int, default=9990)
    parser.add_argument('--sdnrUser', help='Username for SDNR', default="admin")
    parser.add_argument('--sdnrPwd', help='Password for SDNR', default="Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U")
    parser.add_argument('--oRuTooDuMapFile', help='A file with the mapping between O-RU ID and O-DU ID as a dictionary (default: %(default)s)', default="o-ru-to-o-du-map.txt")
    parser.add_argument('--pollTime', help='The time between polls (default: %(default)d)', type=int, default=10)
    parser.add_argument('-v', '--verbose', action='store_true', help='Turn on verbose printing')
    parser.add_argument('--version', action='version', version='%(prog)s 1.0')
    args = vars(parser.parse_args())
    mr_host = args["mrHost"]
    if os.getenv("MR-HOST") is not None:
        mr_host = os.getenv("MR-HOST")
        print("Using MR Host from os: " + mr_host)
    mr_port = args["mrPort"]
    if os.getenv("MR-PORT") is not None:
        mr_port = os.getenv("MR-PORT")
        print("Using MR Port from os: " + mr_port)
    mr_topic = args["mrTopic"]
    sdnr_host = args["sdnrHost"]
    if os.getenv("SDNR-HOST") is not None:
        sdnr_host = os.getenv("SDNR-HOST")
        print("Using SNDR Host from os: " + sdnr_host)
    sdnr_port = args["sdnrPort"]
    if os.getenv("SDNR-PORT") is not None:
        sdnr_port = os.getenv("SDNR-PORT")
        print("Using SNDR Host from os: " + sdnr_port)
    sdnr_user = args["sdnrUser"]
    if os.getenv("SDNR-USER") is not None:
        sdnr_user = os.getenv("SDNR-USER")
        print("Using SNDR User from os: " + sdnr_user)
    sdnr_pwd = args["sdnrPwd"]
    if os.getenv("SDNR-PWD") is not None:
        sdnr_pwd = os.getenv("SDNR-PWD")
        print("Using SNDR Password from os: " + sdnr_pwd)
    o_ru_to_o_du_map = read_o_ru_to_o_du_map_from_file(args["oRuTooDuMapFile"])
    pollTime = args["pollTime"]

    if os.getenv("VERBOSE") is not None or args["verbose"]:

        def verboseprint(*args, **kwargs):
            print(*args, **kwargs)

    else:
        verboseprint = lambda *a, **k: None  # do-nothing function

    verboseprint("Using MR address: " + mr_host + ":" + str(mr_port) + " and topic: " + mr_topic)
    verboseprint("Using SDNR address: " + sdnr_host + ":" + str(sdnr_port))
    verboseprint("Starting with " + str(pollTime) + " seconds between polls")
    mr_address = mr_host + ":" + str(mr_port) + MR_PATH.replace("[TOPIC]", mr_topic)
    sdnr_address = sdnr_host + ":" + str(sdnr_port)

    poll_and_handle_messages(mr_address, sdnr_address, sdnr_user, sdnr_pwd)
