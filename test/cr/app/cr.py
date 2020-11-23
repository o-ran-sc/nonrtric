
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

from flask import Flask, request, Response
from time import sleep
import time
from datetime import datetime
import json
import traceback
import logging

# Disable all logging of GET on reading counters and db
class AjaxFilter(logging.Filter):
    def filter(self, record):
        return ("/counter/" not in record.getMessage()) and ("/db" not in record.getMessage())

log = logging.getLogger('werkzeug')
log.addFilter(AjaxFilter())

app = Flask(__name__)

# list of callback messages
msg_callbacks={}

# Server info
HOST_IP = "::"
HOST_PORT = 2222

# Metrics vars
cntr_msg_callbacks=0
cntr_msg_fetched=0
cntr_callbacks={}

# Request and response constants
CALLBACK_URL="/callbacks/<string:id>"
APP_READ_URL="/get-event/<string:id>"
APP_READ_ALL_URL="/get-all-events/<string:id>"
DUMP_ALL_URL="/db"

MIME_TEXT="text/plain"
MIME_JSON="application/json"
CAUGHT_EXCEPTION="Caught exception: "
SERVER_ERROR="Server error :"
TIME_STAMP="cr-timestamp"

#I'm alive function
@app.route('/',
    methods=['GET'])
def index():
    return 'OK', 200

### Callback interface, for control

# Fetch the oldest callback message for an id
# URI and parameter, (GET): /get-event/<id>
# response: message + 200 or just 204 or just 500(error)
@app.route(APP_READ_URL,
    methods=['GET'])
def receiveresponse(id):
    global msg_callbacks
    global cntr_msg_fetched

    try:
        if ((id in msg_callbacks.keys()) and (len(msg_callbacks[id]) > 0)):
            cntr_msg_fetched+=1
            cntr_callbacks[id][1]+=1
            msg=msg_callbacks[id][0]
            print("Fetching msg for id: "+id+", msg="+str(msg))
            del msg[TIME_STAMP]
            del msg_callbacks[id][0]
            return json.dumps(msg),200
        print("No messages for id: "+id)
    except Exception as e:
        print(CAUGHT_EXCEPTION+str(e))
        traceback.print_exc()
        return "",500

    return "",204

# Fetch all callback message for an id in an array
# URI and parameter, (GET): /get-all-events/<id>
# response: message + 200 or just 500(error)
@app.route(APP_READ_ALL_URL,
    methods=['GET'])
def receiveresponse_all(id):
    global msg_callbacks
    global cntr_msg_fetched

    try:
        if ((id in msg_callbacks.keys()) and (len(msg_callbacks[id]) > 0)):
            cntr_msg_fetched+=len(msg_callbacks[id])
            cntr_callbacks[id][1]+=len(msg_callbacks[id])
            msg=msg_callbacks[id]
            print("Fetching all msgs for id: "+id+", msg="+str(msg))
            for sub_msg in msg:
                del sub_msg[TIME_STAMP]
            del msg_callbacks[id]
            return json.dumps(msg),200
        print("No messages for id: "+id)
    except Exception as e:
        print(CAUGHT_EXCEPTION+str(e))
        traceback.print_exc()
        return "",500

    msg=[]
    return json.dumps(msg),200

# Receive a callback message
# URI and payload, (PUT or POST): /callbacks/<id> <json messages>
# response: OK 200 or 500 for other errors
@app.route(CALLBACK_URL,
    methods=['PUT','POST'])
def events_write(id):
    global msg_callbacks
    global cntr_msg_callbacks

    try:
        print("Received callback for id: "+id +", content-type="+request.content_type)
        try:
            if (request.content_type == MIME_JSON):
                data = request.data
                msg = json.loads(data)
                print("Payload(json): "+str(msg))
            else:
                msg={}
                print("Payload(content-type="+request.content_type+"). Setting empty json as payload")
        except Exception as e:
            msg={}
            print("(Exception) Payload does not contain any json, setting empty json as payload")
            traceback.print_exc()

        cntr_msg_callbacks += 1
        msg[TIME_STAMP]=str(datetime.now())
        if (id in msg_callbacks.keys()):
            msg_callbacks[id].append(msg)
        else:
            msg_callbacks[id]=[]
            msg_callbacks[id].append(msg)

        if (id in cntr_callbacks.keys()):
            cntr_callbacks[id][0] += 1
        else:
            cntr_callbacks[id]=[]
            cntr_callbacks[id].append(1)
            cntr_callbacks[id].append(0)

    except Exception as e:
        print(CAUGHT_EXCEPTION+str(e))
        traceback.print_exc()
        return 'NOTOK',500

    return 'OK',200

### Functions for test ###

# Dump the whole db of current callbacks
# URI and parameter, (GET): /db
# response: message + 200
@app.route(DUMP_ALL_URL,
    methods=['GET'])
def dump_db():
    return json.dumps(msg_callbacks),200

### Functions for metrics read out ###

@app.route('/counter/received_callbacks',
    methods=['GET'])
def requests_submitted():
    req_id = request.args.get('id')
    if (req_id is None):
        return Response(str(cntr_msg_callbacks), status=200, mimetype=MIME_TEXT)

    if (req_id in cntr_callbacks.keys()):
        return Response(str(cntr_callbacks[req_id][0]), status=200, mimetype=MIME_TEXT)
    else:
        return Response(str("0"), status=200, mimetype=MIME_TEXT)

@app.route('/counter/fetched_callbacks',
    methods=['GET'])
def requests_fetched():
    req_id = request.args.get('id')
    if (req_id is None):
        return Response(str(cntr_msg_fetched), status=200, mimetype=MIME_TEXT)

    if (req_id in cntr_callbacks.keys()):
        return Response(str(cntr_callbacks[req_id][1]), status=200, mimetype=MIME_TEXT)
    else:
        return Response(str("0"), status=200, mimetype=MIME_TEXT)

@app.route('/counter/current_messages',
    methods=['GET'])
def current_messages():
    req_id = request.args.get('id')
    if (req_id is None):
        return Response(str(cntr_msg_callbacks-cntr_msg_fetched), status=200, mimetype=MIME_TEXT)

    if (req_id in cntr_callbacks.keys()):
        return Response(str(cntr_callbacks[req_id][0]-cntr_callbacks[req_id][1]), status=200, mimetype=MIME_TEXT)
    else:
        return Response(str("0"), status=200, mimetype=MIME_TEXT)


### Admin ###

# Reset all messsages and counters
@app.route('/reset',
    methods=['GET', 'POST', 'PUT'])
def reset():
    global msg_callbacks
    global cntr_msg_fetched
    global cntr_msg_callbacks
    global cntr_callbacks

    msg_callbacks={}
    cntr_msg_fetched=0
    cntr_msg_callbacks=0
    cntr_callbacks={}

    return Response('OK', status=200, mimetype=MIME_TEXT)

### Main function ###

if __name__ == "__main__":
    app.run(port=HOST_PORT, host=HOST_IP)
