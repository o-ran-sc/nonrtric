import argparse
import ast
import requests
import json
import time

SDNR_PATH = "/rests/data/network-topology:network-topology/topology=topology-netconf/node=O-RAN-DU-01/yang-ext:mount/o-ran-sc-du-hello-world:network-function/du-to-ru-connection="

UNLOCK_MESSAGE = {
    "o-ran-sc-du-hello-world:du-to-ru-connection": [
        {
            "name":"",
            "administrative-state":"UNLOCKED"
        }
    ]
}


def is_message_new_link_failure(message):
    msg_as_json = json.loads(message)
    event_headers = msg_as_json["event"]["commonEventHeader"]

    link_failure = False
    if (event_headers["domain"] == "fault"):
        fault_fields = msg_as_json["event"]["faultFields"]
        link_failure = fault_fields["specificProblem"] == "CUS Link Failure" and fault_fields["eventSeverity"] == "CRITICAL"

    return link_failure


def is_message_clear_link_failure(message):
    msg_as_json = json.loads(message)
    event_headers = msg_as_json["event"]["commonEventHeader"]

    link_failure_clear = False
    if (event_headers["domain"] == "fault"):
        fault_fields = msg_as_json["event"]["faultFields"]
        link_failure_clear = fault_fields["specificProblem"] == "CUS Link Failure" and fault_fields["eventSeverity"] == "NORMAL"

    return link_failure_clear


def handle_link_failure(message, o_ru_to_o_du_map, sdnr_address):
    verboseprint("Got a link failure: ")
    alarm_msg_as_json = json.loads(message)
    event_headers = alarm_msg_as_json["event"]["commonEventHeader"]
    o_ru_id = event_headers["reportingEntityId"]
    verboseprint("O-RU ID: " + o_ru_id)
    o_du_id = o_ru_to_o_du_map[o_ru_id]
    verboseprint("O-DU ID: " + o_du_id)
    unlock_msg = json.loads(json.dumps(UNLOCK_MESSAGE))
    unlock_msg["o-ran-sc-du-hello-world:du-to-ru-connection"][0]["name"] = o_du_id
    response = requests.post(sdnr_address + SDNR_PATH + o_du_id, json=unlock_msg)
    print(response)


def handle_clear_link_failure(message):
    msg_as_json = json.loads(message)
    event_headers = msg_as_json["event"]["commonEventHeader"]
    o_ru_id = event_headers["reportingEntityId"]
    verboseprint("Cleared Link Failure for O-RU ID: " + o_ru_id)


def read_o_ru_to_o_du_map_from_file(map_file):
    file = open(map_file, "r")
    contents = file.read()
    dictionary = ast.literal_eval(contents)
    file.close()
    return dictionary


if __name__ == '__main__':
    parser = argparse.ArgumentParser(prog='PROG')
    parser.add_argument('--mrHost', help='The URL of the MR host', default="http://message-router.onap")
    parser.add_argument('--mrPort', help='The port of the MR host', type=int, default=3904)
    parser.add_argument('--mrTopic', help='The topic to poll messages from', default="ALARMS-WRITE")
    parser.add_argument('--sdnrHost', help='The URL of the SNDR host', default="http://localhost")
    parser.add_argument('--sdnrPort', help='The port of the SDNR host', type=int, default=9990)
    parser.add_argument('--oRuTooDuMapFile', help='A file with the mapping between O-RU ID and O-DU ID as a dictionary', default="o-ru-to-o-du-map.txt")
    parser.add_argument('--pollTime', help='The time between polls', type=int, default=10)
    parser.add_argument('-v', '--verbose', action='store_true', help='Turn on verbose printing')
    parser.add_argument('--version', action='version', version='%(prog)s 1.0')
    args = vars(parser.parse_args())
    mr_host = args["mrHost"]
    mr_port = args["mrPort"]
    mr_topic = args["mrTopic"]
    sdnr_host = args["sdnrHost"]
    sdnr_port = args["sdnrPort"]
    o_ru_to_o_du_map = read_o_ru_to_o_du_map_from_file(args["oRuTooDuMapFile"])
    pollTime = args["pollTime"]

    if args["verbose"]:

        def verboseprint(*args, **kwargs):
            print(*args, **kwargs)

    else:
        verboseprint = lambda *a, **k: None  # do-nothing function

    verboseprint("Using MR address: " + mr_host + ":" + str(mr_port) + " and topic: " + mr_topic)
    verboseprint("Using SDNR address: " + sdnr_host + ":" + str(sdnr_port))
    verboseprint("Starting with " + str(pollTime) + " seconds between polls")
    mr_address = mr_host + ":" + str(mr_port) + "/events/" + mr_topic + "/users/test/"
    sdnr_address = sdnr_host + ":" + str(sdnr_port)

    while True:
        response = requests.get(mr_address)
        verboseprint("Polling")
        messages = response.json()
        for message in messages:
            if (is_message_new_link_failure(message)):
                handle_link_failure(message, o_ru_to_o_du_map, sdnr_address)
            elif (is_message_clear_link_failure(message)):
                handle_clear_link_failure(message)

        time.sleep(pollTime)
