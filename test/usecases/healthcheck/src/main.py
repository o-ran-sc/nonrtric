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

import argparse
from datetime import datetime
from jinja2 import Template
from flask import Flask, request
import os.path
from os import path
from pygments.util import xrange
from requests import ConnectionError
import requests
import sys
import threading
import time

SERVICE_NAME = 'HealthCheck'
BASE_URL = 'http://localhost:8081'
RIC_CHUNK_SIZE = 10
TIME_BETWEEN_CHECKS = 60

type_to_use = ''
policy_body = ''

app = Flask(__name__)

# Server info
HOST_IP = "::"
HOST_PORT = 9990
APP_URL = "/stats"

stat_page_template = """
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv=\"refresh\" content=\"{{refreshTime}}\">
        <title>Non-RealTime RIC Health Check</title>
    </head>
    <body>
        <h3>General</h3>
        <font face=\"monospace\">
            Policy type ID:.......................{{policyTypeId}}<br>
            Policy body path:..................... {{policyBodyPath}}<br>
            Time of last check:...................{{time}}<br>
            Duration of check:....................{{duration}}<br>
            Number of checks:.....................{{noOfChecks}}<br>
        </font>
        <h3>Near-RT RICs</h3>
        <font face=\"monospace\">
            Number of unavailable rics:...........{{noOfUnavailableRics}}<br>
            Number of rics not supporting type....{{noOfNotSupportingRics}}<br>
            Number of rics supporting type:.......{{noOfSupportingRics}}<br>
        </font>
        <h3>Policies</h3>
        <font face=\"monospace\">
            Number of created policies:...........{{noOfCreatedPolicies}}<br>
            Number of read policies:..............{{noOfReadPolicies}}<br>
            Number of updated policies:...........{{noOfUpdatedPolicies}}<br>
            Number of deleted policies:...........{{noOfDeletedPolicies}}<br>
        </font>
    </body>
</html>
"""
type_to_use = "2"
test_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
policy_body_path = os.path.join(test_dir, 'auto-test','testdata','OSC','pihw_template.json')

duration = 0
no_of_checks = 0
no_of_unavailable_rics = 0
no_of_rics_not_supporting_type = 0
no_of_rics_supporting_type = 0
no_of_created_policies = 0
no_of_read_policies = 0
no_of_updated_policies = 0
no_of_deleted_policies = 0

class Ric:

    def __init__(self, name, supported_types, state):
        self.name = name
        self.supports_type_to_use = self.policy_type_supported(supported_types)
        self.state = state
        self.no_of_created_policies = 0
        self.no_of_read_policies = 0
        self.no_of_updated_policies = 0
        self.no_of_deleted_policies = 0

    def update_supported_types(self, supported_types):
        self.supports_type_to_use = self.policy_type_supported(supported_types)

    def policy_type_supported(self, supported_policy_types):
        for supported_type in supported_policy_types:
            if type_to_use == supported_type:
                return True

        return False


class PolicyCheckThread (threading.Thread):

    def __init__(self, thread_id, ric):
        threading.Thread.__init__(self)
        self.thread_id = thread_id
        self.ric = ric

    def run(self):
        verboseprint(f'Checking ric: {self.ric.name}')
        if put_policy(self.thread_id, self.ric.name):
            verboseprint(f'Created policy: {self.thread_id} in ric: {self.ric.name}')
            self.ric.no_of_created_policies += 1
            time.sleep(0.5)
            if get_policy(self.thread_id):
                verboseprint(f'Read policy: {self.thread_id} from ric: {self.ric.name}')
                self.ric.no_of_read_policies += 1
            if put_policy(self.thread_id, self.ric.name, update_value=1):
                verboseprint(f'Updated policy: {self.thread_id} in ric: {self.ric.name}')
                self.ric.no_of_updated_policies += 1
            if delete_policy(self.thread_id):
                verboseprint(f'Deleted policy: {self.thread_id} from ric: {self.ric.name}')
                self.ric.no_of_deleted_policies += 1


class MonitorServer (threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

    def run(self):
        verboseprint('Staring monitor server')
        app.run(port=HOST_PORT, host=HOST_IP)


@app.route(APP_URL,
    methods=['GET'])
def produceStatsPage():
    t = Template(stat_page_template)
    page = t.render(refreshTime=TIME_BETWEEN_CHECKS, policyTypeId=type_to_use, policyBodyPath=policy_body_path,
    time=datetime.now().strftime("%Y-%m-%d %H:%M:%S"), duration=duration, noOfChecks=no_of_checks,
    noOfUnavailableRics=no_of_unavailable_rics, noOfNotSupportingRics=no_of_rics_not_supporting_type,
    noOfSupportingRics=no_of_rics_supporting_type, noOfCreatedPolicies=no_of_created_policies,
    noOfReadPolicies=no_of_read_policies, noOfUpdatedPolicies=no_of_updated_policies,
    noOfDeletedPolicies=no_of_deleted_policies)
    return page,200

def get_rics_from_agent():
    resp = requests.get(BASE_URL + '/rics')
    if not resp.ok:
        verboseprint(f'Unable to get Rics {resp.status_code}')
        return {}
    return resp.json()


def create_ric_dict(rics_as_json):
    rics = {}
    for ric_info in rics_as_json:
        rics[ric_info["ricName"]] = (Ric(ric_info["ricName"], ric_info["policyTypes"], ric_info['state']))
        verboseprint(f'Adding ric: {rics[ric_info["ricName"]]}')

    return rics


def update_rics():
    added_rics = {}
    for ric_info in get_rics_from_agent():
        if ric_info["ricName"] in rics:
            rics[ric_info["ricName"]].update_supported_types(ric_info["policyTypes"])
            rics[ric_info["ricName"]].state = ric_info['state']
        else:
            added_rics[ric_info["ricName"]] = (Ric(ric_info["ricName"], ric_info["policyTypes"]))
            verboseprint(f'Adding ric: {rics[ric_info["ricName"]]}')

    rics.update(added_rics)


def put_policy(thread_id, ric_name, update_value=0):
    policy_id = f'thread_{thread_id}'
    complete_url = f'{BASE_URL}/policy?type={type_to_use}&id={policy_id}&ric={ric_name}&service={SERVICE_NAME}'
    headers = {'content-type': 'application/json'}
    resp = requests.put(complete_url, policy_body.replace('XXX', str(thread_id + update_value)), headers=headers, verify=False)

    if not resp.ok:
        verboseprint(f'Unable to create policy {resp}')
        return False
    else:
        return True


def get_policy(thread_id):
    policy_id = f'thread_{thread_id}'
    complete_url = f'{BASE_URL}/policy?id={policy_id}'
    resp = requests.get(complete_url)

    if not resp.ok:
        verboseprint(f'Unable to get policy {resp}')
        return False
    else:
        return True


def delete_policy(thread_id):
    policy_id = f'thread_{thread_id}'
    complete_url = f'{BASE_URL}/policy?id={policy_id}'
    resp = requests.delete(complete_url)

    if not resp.ok:
        verboseprint(f'Unable to delete policy for policy ID {policy_id}')
        return False

    return True


def statistics():
    global duration
    global no_of_checks
    global no_of_unavailable_rics
    global no_of_rics_not_supporting_type
    global no_of_rics_supporting_type
    global no_of_created_policies
    global no_of_read_policies
    global no_of_updated_policies
    global no_of_deleted_policies

    no_of_unavailable_rics = 0
    no_of_rics_not_supporting_type = 0
    no_of_rics_supporting_type = 0
    for ric in rics.values():
        if not (ric.state == 'AVAILABLE' or ric.state == 'CONSISTENCY_CHECK'):
            no_of_unavailable_rics += 1
        elif ric.supports_type_to_use:
            no_of_rics_supporting_type += 1
            no_of_created_policies += ric.no_of_created_policies
            no_of_read_policies += ric.no_of_read_policies
            no_of_updated_policies += ric.no_of_updated_policies
            no_of_deleted_policies += ric.no_of_deleted_policies
        else:
            no_of_rics_not_supporting_type += 1

    print(f'*********** Statistics {datetime.now().strftime("%Y-%m-%d %H:%M:%S")} ***********')
    print(f'Duration of check:                  {duration.total_seconds()} seconds')
    print(f'Number of checks:                   {no_of_checks}')
    print(f'Number of unavailable rics:         {no_of_unavailable_rics}')
    print(f'Number of rics not supporting type: {no_of_rics_not_supporting_type}')
    print(f'Number of rics supporting type:     {no_of_rics_supporting_type}')
    print(f'Number of created policies:         {no_of_created_policies}')
    print(f'Number of read policies:            {no_of_read_policies}')
    print(f'Number of updated policies:         {no_of_updated_policies}')
    print(f'Number of deleted policies:         {no_of_deleted_policies}')
    print('******************************************************')


def run_check_threads(rics):
    thread_id = 1
    threads = []
    for ric in rics.values():
        if ric.supports_type_to_use and (ric.state == 'AVAILABLE' or ric.state == 'CONSISTENCY_CHECK'): #or ric.name == 'ric_not_working':
            policy_checker = PolicyCheckThread(thread_id, ric)
            policy_checker.start()
            thread_id += 1
            threads.append(policy_checker)

    for checker in threads:
        checker.join()


def split_rics_equally(chunks):
    # prep with empty dicts
    return_list = [dict() for _ in xrange(chunks)]
    idx = 0
    for k,v in rics.items():
        return_list[idx][k] = v
        if idx < chunks-1:  # indexes start at 0
            idx += 1
        else:
            idx = 0
    return return_list


def get_no_of_chunks(size_of_chunks, size_to_chunk):
    (q, _) = divmod(size_to_chunk, size_of_chunks)
    return q


if __name__ == '__main__':
    parser = argparse.ArgumentParser(prog='PROG')
    parser.add_argument('--policyTypeId', help='The ID of the policy type to use')
    parser.add_argument('--policyBodyPath', help='The path to the JSON body of the policy to create')
    parser.add_argument('-v', '--verbose', action='store_true', help='Turn on verbose printing')
    parser.add_argument('--version', action='version', version='%(prog)s 1.0')
    args = vars(parser.parse_args())

    if args['verbose']:
        def verboseprint(*args, **kwargs):
            print(*args, **kwargs)
    else:
        verboseprint = lambda *a, **k: None # do-nothing function

    if args["policyTypeId"]:
        type_to_use = args["policyTypeId"]

    if args["policyBodyPath"]:
        policy_body_path = args["policyBodyPath"]
        if not os.path.exists(policy_body_path):
            print(f'Policy body {policy_body_path} does not exist.')
            sys.exit(1)

    verboseprint(f'Using policy type {type_to_use}')
    verboseprint(f'Using policy file {policy_body_path}')

    with open(policy_body_path) as json_file:
        policy_body = json_file.read()
        verboseprint(f'Policy body: {policy_body}')

    try:
        rics_from_agent = get_rics_from_agent()
    except ConnectionError:
        print(f'Policy Agent is not answering on {BASE_URL}, cannot start!')
        sys.exit(1)

    rics = create_ric_dict(rics_from_agent)

    monitor_server = MonitorServer()
    monitor_server.start()

    while True:
        start_time = datetime.now()
        chunked_rics = split_rics_equally(get_no_of_chunks(RIC_CHUNK_SIZE, rics.__len__()))
        for ric_chunk in chunked_rics:
            run_check_threads(ric_chunk)

        no_of_checks += 1
        finish_time = datetime.now()
        duration = finish_time - start_time
        statistics()
        sleep_time = TIME_BETWEEN_CHECKS - duration.total_seconds()
        verboseprint(f'Sleeping {sleep_time} seconds')
        time.sleep(sleep_time)
        update_rics()

    verboseprint('Exiting main')
