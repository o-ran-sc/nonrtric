
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

from flask import Flask, request
from time import sleep
import time
import datetime
import json
from flask import Flask
from flask import Response
import traceback

app = Flask(__name__)

# list of callback messages
msg_callbacks={}

# Server info
HOST_IP = "::"
HOST_PORT = 2222

# Metrics vars
cntr_msg_callbacks=0
cntr_msg_fetched=0

# Request and response constants
CALLBACK_URL="/callbacks/<string:id>"
APP_READ_URL="/get-event/<string:id>"

MIME_TEXT="text/plain"
MIME_JSON="application/json"
CAUGHT_EXCEPTION="Caught exception: "
SERVER_ERROR="Server error :"

#I'm alive function
@app.route('/',
    methods=['GET'])
def index():
    return 'OK', 200

### Callback interface, for control

# Fetch the oldest callback message for an id
# URI and parameter, (GET): /get-event/<id>
# response: message + 200 or 204
@app.route(APP_READ_URL,
    methods=['GET'])
def receiveresponse(id):
    global msg_callbacks
    global cntr_msg_fetched

    try:
        if ((id in msg_callbacks.keys()) and (len(msg_callbacks[id]) > 0)):
            cntr_msg_fetched+=1
            msg=str(msg_callbacks[id][0])
            print("Fetching msg for id: "+id+", msg="+msg)
            del msg_callbacks[id][0]
            return msg,200
        print("No messages for id: "+id)
    except Exception as e:
        print(CAUGHT_EXCEPTION+str(e))

    return "",204


# Receive a callback message
# URI and payload, (PUT or POST): /callbacks/<id> <json array of response messages>
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
                msg = request.json
                print("Payload(json): "+str(msg))
            elif (request.content_type == MIME_TEXT):
                msg= request.form
                print("Payload(text): "+str(msg))
            else:
                msg="\"\""
                print("Payload(content-type="+request.content_type+"). Setting data to empty, quoted, string")
        except:
            msg="\"\""
            print("(Exception) Payload does not contain any json or text data, setting empty string as payload")

        cntr_msg_callbacks += 1
        if (id in msg_callbacks.keys()):
            msg_callbacks[id].append(msg)
        else:
            msg_callbacks[id]=[]
            msg_callbacks[id].append(msg)
    except Exception as e:
        print(CAUGHT_EXCEPTION+str(e))
        return 'OK',500

    return 'OK',200


### Functions for metrics read out ###

@app.route('/counter/received_callbacks',
    methods=['GET'])
def requests_submitted():
    return Response(str(cntr_msg_callbacks), status=200, mimetype=MIME_TEXT)

@app.route('/counter/fetched_callbacks',
    methods=['GET'])
def requests_fetched():
    return Response(str(cntr_msg_fetched), status=200, mimetype=MIME_TEXT)

@app.route('/counter/current_messages',
    methods=['GET'])
def current_messages():
    return Response(str(cntr_msg_callbacks-cntr_msg_fetched), status=200, mimetype=MIME_TEXT)



### Admin ###

# Reset all messsages and counters
@app.route('/reset',
    methods=['GET', 'POST', 'PUT'])
def reset():
    global msg_callbacks
    global cntr_msg_fetched
    global cntr_msg_callbacks

    msg_callbacks={}
    cntr_msg_fetched=0
    cntr_msg_callbacks=0

    return Response('OK', status=200, mimetype=MIME_TEXT)

### Main function ###

if __name__ == "__main__":
    app.run(port=HOST_PORT, host=HOST_IP)
