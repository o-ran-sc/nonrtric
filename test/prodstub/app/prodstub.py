
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

from flask import Flask
from flask import request

import requests

import json
from jsonschema import validate

import threading
import time
import datetime
import logging

# Disable all logging of GET on reading counters and status
class AjaxFilter(logging.Filter):
    def filter(self, record):
        return ("/counter/" not in record.getMessage()) and ("/status" not in record.getMessage())

log = logging.getLogger('werkzeug')
log.addFilter(AjaxFilter())

app = Flask(__name__)

# # list of callback messages
# msg_callbacks={}

# Server info
HOST_IP = "::"
HOST_PORT = 2222

# Request and response constants
CALLBACK_CREATE_URL="/callbacks/job/<string:producer_id>"
CALLBACK_DELETE_URL="/callbacks/job/<string:producer_id>/<string:job_id>"
CALLBACK_SUPERVISION_URL="/callbacks/supervision/<string:producer_id>"

ARM_CREATE_RESPONSE="/arm/create/<string:producer_id>/<string:job_id>"
ARM_DELETE_RESPONSE="/arm/delete/<string:producer_id>/<string:job_id>"
ARM_SUPERVISION_RESPONSE="/arm/supervision/<string:producer_id>"
ARM_TYPE="/arm/type/<string:producer_id>/<string:type_id>"
COUNTER_SUPERVISION="/counter/supervision/<string:producer_id>"
COUNTER_CREATE="/counter/create/<string:producer_id>/<string:job_id>"
COUNTER_DELETE="/counter/delete/<string:producer_id>/<string:job_id>"

JOB_DATA="/jobdata/<string:producer_id>/<string:job_id>"

STATUS="/status"

#Constants
APPL_JSON='application/json'
UNKNOWN_QUERY_PARAMETERS="Unknown query parameter(s)"
RETURNING_CONFIGURED_RESP="returning configured response code"
JOBID_NO_MATCH="job id in stored json does not match request"
PRODUCER_OR_JOB_NOT_FOUND="producer or job not found"
PRODUCER_NOT_FOUND="producer not found"
TYPE_NOT_FOUND="type not found"
TYPE_IN_USE="type is in use in a job"
JOB_NOT_FOUND="job not found"
JOB_DATA_NOT_FOUND="job data not found"
JSON_CORRUPT="json in request is corrupt or missing"

#Producer and job db, including armed responses
db={}
# producer
#  armed response for supervision
#  armed types
#  supervision counter
#  job
#    job json
#    target_type
#    armed response for create
#    armed response for delete
#    create counter
#    delete counter
#    delivering status

# disable warning about unverified https requests
from requests.packages import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# Helper function to populate a callback dict with the basic structure
# if job_id is None then only the producer level is setup and the producer dict is returned
# if job_id is not None, the job level is setup and the job dict is returned (producer must exist)
def setup_callback_dict(producer_id, job_id):

    producer_dict=None
    if (producer_id in db.keys()):
        producer_dict=db[producer_id]
    else:
        if (job_id is not None):
            return None
        producer_dict={}
        db[producer_id]=producer_dict

        producer_dict['supervision_response']=200
        producer_dict['supervision_counter']=0
        producer_dict['types']=[]

    if (job_id is None):
        return producer_dict

    job_dict=None
    if (job_id in producer_dict.keys()):
        job_dict=producer_dict[job_id]
    else:
        job_dict={}
        producer_dict[job_id]=job_dict
        job_dict['create_response']=201
        job_dict['delete_response']=404
        job_dict['json']=None
        job_dict['create_counter']=0
        job_dict['delete_counter']=0
        job_dict['delivering']="stopped"
        job_dict['delivery_attempts']=0
    return job_dict


# Helper function to get an entry from the callback db
# if job_id is None then only the producer dict is returned (or None if producer is not found)
# if job_id is not None, the job is returned (or None if producer/job is not found)
def get_callback_dict(producer_id, job_id):

    producer_dict=None
    if (producer_id in db.keys()):
        producer_dict=db[producer_id]

    if (producer_dict is None):
        return None

    if (job_id is None):
        return producer_dict

    job_dict=None
    if (job_id in producer_dict.keys()):
        job_dict=producer_dict[job_id]

    return job_dict

# Helper function find if a key/valye exist in the dictionay tree
# True if found
def recursive_search(s_dict, s_key, s_id):
    for pkey in s_dict:
        if (pkey == s_key) and (s_dict[pkey] == s_id):
            return True
        if (isinstance(s_dict[pkey], dict)):
            recursive_search(s_dict[pkey], s_key, s_id)

    return False

# Helper function to find all job dicts
def get_all_jobs():
    job_dicts={}
    for producer_key in db:
        producer_dict = db[producer_key]
        for job_key in producer_dict:
            job_dict = producer_dict[job_key]
            if (isinstance(job_dict, dict)):
                job_dicts[job_key]=job_dict
    return job_dicts

# I'm alive function
# response: always 200
@app.route('/',
    methods=['GET'])
def index():
    return 'OK', 200

# Arm the create callback with a response code
# Omitting the query parameter switch to response back to the standard 200/201 response
# URI and parameters (PUT): /arm/create/<producer_id>/<job_id>[?response=<repsonsecode>]
# Setting
# response: 200 (400 if incorrect query params)
@app.route(ARM_CREATE_RESPONSE,
     methods=['PUT'])
def arm_create(producer_id, job_id):

    arm_response=request.args.get('response')

    if (arm_response is None):
        if (len(request.args) != 0):
            return UNKNOWN_QUERY_PARAMETERS,400
    else:
        if (len(request.args) != 1):
            return UNKNOWN_QUERY_PARAMETERS,400


    print("Arm create received for producer: "+str(producer_id)+" and job: "+str(job_id)+" and response: "+str(arm_response))

    job_dict=setup_callback_dict(producer_id, job_id)

    if (arm_response is None):    #Reset the response depending if a job exists or not
        if (job_dict['json'] is None):
            job_dict['create_response']=201
        else:
            job_dict['create_response']=200
    else:
        job_dict['create_response']=int(arm_response)

    return "",200

# Arm the delete callback with a response code
# Omitting the query parameter switch to response back to the standard 204 response
# URI and parameters (PUT): /arm/delete/<producer_id>/<job-id>[?response=<responsecode>]
# response: 200 (400 if incorrect query params)
@app.route(ARM_DELETE_RESPONSE,
     methods=['PUT'])
def arm_delete(producer_id, job_id):

    arm_response=request.args.get('response')

    if (arm_response is None):
        if (len(request.args) != 0):
            return UNKNOWN_QUERY_PARAMETERS,400
    else:
        if (len(request.args) != 1):
            return UNKNOWN_QUERY_PARAMETERS,400

    print("Arm delete received for producer: "+str(producer_id)+" and job: "+str(job_id)+" and response: "+str(arm_response))

    arm_response=request.args.get('response')

    job_dict=setup_callback_dict(producer_id, job_id)

    if (arm_response is None): #Reset the response depending if a job exists or not
        if (job_dict['json'] is None):
            job_dict['delete_response']=404
        else:
            job_dict['delete_response']=204
    else:
        job_dict['delete_response']=int(arm_response)

    return "",200

# Arm the supervision callback with a response code
# Omitting the query parameter switch to response back to the standard 200 response
# URI and parameters (PUT): /arm/supervision/<producer_id>[?response=<responsecode>]
# response: 200 (400 if incorrect query params)
@app.route(ARM_SUPERVISION_RESPONSE,
     methods=['PUT'])
def arm_supervision(producer_id):

    arm_response=request.args.get('response')

    if (arm_response is None):
        if (len(request.args) != 0):
            return UNKNOWN_QUERY_PARAMETERS,400
    else:
        if (len(request.args) != 1):
            return UNKNOWN_QUERY_PARAMETERS,400

    print("Arm supervision received for producer: "+str(producer_id)+" and response: "+str(arm_response))

    producer_dict=setup_callback_dict(producer_id, None)
    if (arm_response is None):
        producer_dict['supervision_response']=200
    else:
        producer_dict['supervision_response']=int(arm_response)

    return "",200

# Arm a producer with a type
# URI and parameters (PUT): /arm/type/<string:producer_id>/<string:type-id>
# response: 200 (404)
@app.route(ARM_TYPE,
    methods=['PUT'])
def arm_type(producer_id, type_id):

    print("Arm type received for producer: "+str(producer_id)+" and type: "+str(type_id))

    producer_dict=get_callback_dict(producer_id, None)

    if (producer_dict is None):
        return PRODUCER_NOT_FOUND,404

    type_list=producer_dict['types']
    if (type_id not in type_list):
        type_list.append(type_id)

    return "",200

# Disarm a producer with a type
# URI and parameters (DELETE): /arm/type/<string:producer_id>/<string:type-id>
# response: 200 (404)
@app.route(ARM_TYPE,
    methods=['DELETE'])
def disarm_type(producer_id, type_id):

    print("Disarm type received for producer: "+str(producer_id)+" and type: "+str(type_id))

    producer_dict=get_callback_dict(producer_id, None)

    if (producer_dict is None):
        return PRODUCER_NOT_FOUND,404

    if (recursive_search(producer_dict, "ei_job_type",type_id) is True):
        return "TYPE_IN_USE",400
    elif (recursive_search(producer_dict, "ei_type_identity",type_id) is True):
        return "TYPE_IN_USE",400
    elif (recursive_search(producer_dict, "info_type_identity",type_id) is True):
        return "TYPE_IN_USE",400

    type_list=producer_dict['types']
    type_list.remove(type_id)

    return "",200


# Callback for create job
# URI and parameters (POST): /callbacks/job/<producer_id>
# response 201 at create, 200 at update or other configured response code
@app.route(CALLBACK_CREATE_URL,
     methods=['POST'])
def callback_create(producer_id):

    req_json_dict=None
    try:
        req_json_dict = json.loads(request.data)
        with open('job-schema.json') as f:
            schema = json.load(f)
            validate(instance=req_json_dict, schema=schema)
    except Exception:
        return JSON_CORRUPT,400

    producer_dict=get_callback_dict(producer_id, None)
    if (producer_dict is None):
        return PRODUCER_OR_JOB_NOT_FOUND,400
    type_list=producer_dict['types']


    if 'ei_type_identity' in req_json_dict.keys():
        type_key_name='ei_type_identity'
        job_key_name='ei_job_identity'
    elif 'info_type_identity' in req_json_dict.keys():
        type_key_name='info_type_identity'
        job_key_name='info_job_identity'
    else:
        return TYPE_NOT_FOUND, 400

    type_id=req_json_dict[type_key_name]
    job_id=req_json_dict[job_key_name]

    job_dict=get_callback_dict(producer_id, job_id)
    if (job_dict is None):
        return PRODUCER_OR_JOB_NOT_FOUND,400
    return_code=0
    return_msg=""
    if (req_json_dict[job_key_name] == job_id):
        print("Create callback received for producer: "+str(producer_id)+" and job: "+str(job_id))
        print(json.loads(request.data))
        return_code=job_dict['create_response']
        if ((job_dict['create_response'] == 200) or (job_dict['create_response'] == 201)):
            job_dict['json']=req_json_dict
            job_dict['delivering']="delivering"
            if (job_dict['create_response'] == 201): #Set up next response code if create was ok
                job_dict['create_response'] = 200
            if (job_dict['delete_response'] == 404):
                job_dict['delete_response'] = 204
        else:
            if(job_dict['delivering'] == "delivering"):
                job_dict['delivering']="hold"
            return_msg=RETURNING_CONFIGURED_RESP

        job_dict['create_counter']=job_dict['create_counter']+1
    else:
        return JOBID_NO_MATCH, 400

    return return_msg, return_code

# Callback for delete job
# URI and parameters (DELETE): /callbacks/job/<producer_id>/<job_id>
# response: 204 at delete or other configured response code
@app.route(CALLBACK_DELETE_URL,
     methods=['DELETE'])
def callback_delete(producer_id, job_id):

    job_dict=get_callback_dict(producer_id, job_id)
    if (job_dict is None):
        return PRODUCER_OR_JOB_NOT_FOUND,400
    return_code=0
    return_msg=""
    print("Delete callback received for producer: "+str(producer_id)+" and job: "+str(job_id))
    return_code=job_dict['delete_response']
    if (job_dict['delete_response'] == 204):
        job_dict['json']=None
        job_dict['delete_response']=404
        job_dict['delivering']="stopped"
        if (job_dict['create_response'] == 200):
            job_dict['create_response'] = 201 # reset create response if delete was ok
    else:
        return_msg=RETURNING_CONFIGURED_RESP

    job_dict['delete_counter']=job_dict['delete_counter']+1

    return return_msg, return_code

# Callback for supervision of producer
# URI and parameters (GET): /callbacks/supervision/<producer_id>
# response: 200 or other configured response code
@app.route(CALLBACK_SUPERVISION_URL,
     methods=['GET'])
def callback_supervision(producer_id):

    print("Supervision callback received for producer: "+str(producer_id))

    producer_dict=get_callback_dict(producer_id, None)
    if (producer_dict is None):
        return PRODUCER_NOT_FOUND,400
    return_code=producer_dict['supervision_response']
    return_msg=""
    if (return_code != 200):
        return_msg="returning configured response code"

    producer_dict['supervision_counter']=producer_dict['supervision_counter']+1

    return return_msg,producer_dict['supervision_response']

# Get the job definition for a job
# URI and parameters (GET): "/jobdata/<string:producer_id>/<string:job_id>"
# response: 200 or 204
@app.route(JOB_DATA,
     methods=['GET'])
def get_jobdata(producer_id, job_id):

    print("Get job data received for producer: "+str(producer_id)+" and job: "+str(job_id))

    job_dict=get_callback_dict(producer_id, job_id)

    if (job_dict is None):
        return PRODUCER_OR_JOB_NOT_FOUND,400

    if (job_dict['json'] is None):
        return "",204
    else:
        return json.dumps(job_dict['json']), 200

# Delete the job definition for a job
# URI and parameters (DELETE): "/jobdata/<string:producer_id>/<string:job_id>"
# response: 204
@app.route(JOB_DATA,
     methods=['DELETE'])
def del_jobdata(producer_id, job_id):

    print("Delete job data received for producer: "+str(producer_id)+" and job: "+str(job_id))

    job_dict=get_callback_dict(producer_id, job_id)

    if (job_dict is None):
        return PRODUCER_OR_JOB_NOT_FOUND,400

    job_dict['json']=None

    return "",204


# Start data delivery for a job, action : START or STOP
# URI and parameters (POST): "/jobdata/<string:producer_id>/<string:job_id>?action=action"
# response: 200 or 204
@app.route(JOB_DATA,
     methods=['POST'])
def start_jobdata(producer_id, job_id):

    action=request.args.get('action')

    if (action is None):
        return UNKNOWN_QUERY_PARAMETERS,400
    else:
        if (len(request.args) != 1):
            return UNKNOWN_QUERY_PARAMETERS,400
        else:
            if ((action != "START") and (action != "STOP")):
                return UNKNOWN_QUERY_PARAMETERS,400

    print("Job data action received for producer: "+str(producer_id)+" and job: "+str(job_id) + " action: " + action)

    job_dict=get_callback_dict(producer_id, job_id)
    if (job_dict is None):
        return JOB_NOT_FOUND,404

    if (job_dict['json'] is None):
        return JOB_DATA_NOT_FOUND, 400
    else:
        if (action == "START"):
            job_dict['delivering']="delivering"
        else:
            job_dict['delivering']="stopped"
        return "",200


# Counter for create calls for a job
# URI and parameters (GET): "/counter/create/<string:producer_id>/<string:job_id>"
# response: 200 and counter value
@app.route(COUNTER_CREATE,
     methods=['GET'])
def counter_create(producer_id, job_id):
    job_dict=get_callback_dict(producer_id, job_id)
    if (job_dict is None):
        return "-1",200
    return str(job_dict['create_counter']),200

# Counter for delete calls for a job
# URI and parameters (GET): "/counter/delete/<string:producer_id>/<string:job_id>"
# response: 200 and counter value
@app.route(COUNTER_DELETE,
     methods=['GET'])
def counter_delete(producer_id, job_id):
    job_dict=get_callback_dict(producer_id, job_id)
    if (job_dict is None):
        return "-1",200
    return str(job_dict['delete_counter']),200

# Counter for supervision calls for a producer
# URI and parameters (GET): "/counter/supervision/<string:producer_id>"
# response: 200 and counter value
@app.route(COUNTER_SUPERVISION,
     methods=['GET'])
def counter_supervision(producer_id):
    producer_dict=get_callback_dict(producer_id, None)
    if (producer_dict is None):
        return "-1",200
    return str(producer_dict['supervision_counter']),200

# Get status info
# URI and parameters (GET): "/status"
# -
@app.route(STATUS,
    methods=['GET'])
def status():
    global db
    return json.dumps(db),200


# Reset db
@app.route('/reset',
    methods=['GET', 'POST', 'PUT'])
def reset():
    global db
    db={}
    return "",200


def datadelivery() :
    while True:
        try:
            job_dicts=get_all_jobs()
            for key in job_dicts:
                job=job_dicts[key]
                if (job['delivering'] == "delivering" and job['json'] != None):
                    url=job['json']['target_uri']
                    if (str(url).find("localhost:") == -1):   #Dont deliver to localhost...
                        data={}
                        data["date"]=str(datetime.datetime.now())
                        data["job"]=""+key
                        data["sequence_no"]=""+str(job['delivery_attempts'])
                        data["value"]=str(100)
                        print("Sending to "+url+" payload:"+json.dumps(data))

                        requests.post(url, json=data, verify=False, timeout=2) #NOSONAR
                        job['delivery_attempts'] += 1
        except Exception as err:
            print("Error during data delivery: "+ str(err))
        time.sleep(1)


### Main function ###

print("Starting data delivery thread")
thread = threading.Thread(target=datadelivery, args=())
thread.daemon = True
thread.start()

if __name__ == "__main__":
    app.run(port=HOST_PORT, host=HOST_IP)
