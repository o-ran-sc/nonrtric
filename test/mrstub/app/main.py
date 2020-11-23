
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
from threading import RLock
import logging

# Disable all logging of GET on reading counters and db
class AjaxFilter(logging.Filter):
    def filter(self, record):
        return ("/counter/" not in record.getMessage())

log = logging.getLogger('werkzeug')
log.addFilter(AjaxFilter())

app = Flask(__name__)
lock = RLock()
# list of messages to/from Dmaap
msg_requests=[]
msg_responses={}

# Server info
HOST_IP = "::"
HOST_PORT = 2222

# Metrics vars
cntr_msg_requests_submitted=0
cntr_msg_requests_fetched=0
cntr_msg_responses_submitted=0
cntr_msg_responses_fetched=0

# Request and response constants
AGENT_WRITE_URL="/events/A1-POLICY-AGENT-WRITE"
AGENT_READ_URL="/events/A1-POLICY-AGENT-READ/users/policy-agent"
APP_WRITE_URL="/send-request"
APP_READ_URL="/receive-response"
MIME_TEXT="text/plain"
MIME_JSON="application/json"
CAUGHT_EXCEPTION="Caught exception: "
SERVER_ERROR="Server error :"

#I'm alive function
@app.route('/',
    methods=['GET'])
def index():
    return 'OK', 200


# Helper function to create a Dmaap request message
# args : <GET|PUT|DELETE> <correlation-id> <json-string-payload - may be None> <url>
# response: json formatted string of a complete Dmaap message
def create_message(operation, correlation_id, payload, url):
    if (payload is None):
        payload="{}"
    time_stamp=datetime.datetime.utcnow()
    msg = '{\"apiVersion\":\"1.0\",\"operation\":\"'+operation+'\",\"correlationId\":\"'+correlation_id+'\",\"originatorId\": \"849e6c6b420\",'
    msg = msg + '\"payload\":'+payload+',\"requestId\":\"23343221\", \"target\":\"policy-agent\", \"timestamp\":\"'+str(time_stamp)+'\", \"type\":\"request\",\"url\":\"'+url+'\"}'
    return msg


### MR-stub interface, for MR control

# Send a message to MR
# URI and parameters (GET): /send-request?operation=<GET|PUT|POST|DELETE>&url=<url>
# response: <correlation-id> (http 200) o4 400 for parameter error or 500 for other errors
@app.route(APP_WRITE_URL,
    methods=['PUT','POST'])
def sendrequest():
    global msg_requests
    global cntr_msg_requests_submitted
    with lock:
        print("APP_WRITE_URL lock")
        try:
            oper=request.args.get('operation')
            if (oper is None):
                print(APP_WRITE_URL+" parameter 'operation' missing")
                return Response('Parameter operation missing in request', status=400, mimetype=MIME_TEXT)

            url=request.args.get('url')
            if (url is None):
                print(APP_WRITE_URL+" parameter 'url' missing")
                return Response('Parameter url missing in request', status=400, mimetype=MIME_TEXT)

            if (oper != "GET" and oper != "PUT" and oper != "POST" and oper != "DELETE"):
                print(APP_WRITE_URL+" parameter 'operation' need to be: DEL|PUT|POST|DELETE")
                return Response('Parameter operation does not contain DEL|PUT|POST|DELETE in request', status=400, mimetype=MIME_TEXT)

            print(APP_WRITE_URL+" operation="+oper+" url="+url)
            correlation_id=str(time.time_ns())
            payload=None
            if (oper == "PUT") and (request.json is not None):
                payload=json.dumps(request.json)

            msg=create_message(oper, correlation_id, payload, url)
            print(msg)
            print(APP_WRITE_URL+" MSG(correlationid = "+correlation_id+"): " + json.dumps(json.loads(msg), indent=2))
            msg_requests.append(msg)
            cntr_msg_requests_submitted += 1
            return Response(correlation_id, status=200, mimetype=MIME_TEXT)
        except Exception as e:
            print(APP_WRITE_URL+"-"+CAUGHT_EXCEPTION+" "+str(e) + " "+traceback.format_exc())
            return Response(SERVER_ERROR+" "+str(e), status=500, mimetype=MIME_TEXT)

# Receive a message response for MR for the included correlation id
# URI and parameter, (GET): /receive-response?correlationid=<correlation-id>
# response: <json-array of 1 response> 200 or empty 204 or other errors 500
@app.route(APP_READ_URL,
    methods=['GET'])
def receiveresponse():
    global msg_responses
    global cntr_msg_responses_fetched
    with lock:
        print("APP_READ_URL lock")
        try:
            id=request.args.get('correlationid')
            if (id is None):
                print(APP_READ_URL+" parameter 'correclationid' missing")
                return Response('Parameter correlationid missing in json', status=500, mimetype=MIME_TEXT)

            if (id in msg_responses):
                answer=msg_responses[id]
                del msg_responses[id]
                print(APP_READ_URL+" response (correlationid="+id+"): " + answer)
                cntr_msg_responses_fetched += 1
                return Response(answer, status=200, mimetype=MIME_JSON)

            print(APP_READ_URL+" - no messages (correlationid="+id+"): ")
            return Response('', status=204, mimetype=MIME_JSON)
        except Exception as e:
            print(APP_READ_URL+"-"+CAUGHT_EXCEPTION+" "+str(e) + " "+traceback.format_exc())
            return Response(SERVER_ERROR+" "+str(e), status=500, mimetype=MIME_TEXT)

### Dmaap interface ###

# Read messages stream. URI according to agent configuration.
# URI, (GET): /events/A1-POLICY-AGENT-READ/users/policy-agent
# response: 200 <json array of request messages>, or 500 for other errors
@app.route(AGENT_READ_URL,
    methods=['GET'])
def events_read():
    global msg_requests
    global cntr_msg_requests_fetched

    limit=request.args.get('limit')
    if (limit is None):
        limit=4096
    else:
        limit=int(limit)
    if (limit<0):
        limit=0
    if (limit>4096):
        limit=4096
    print("Limting number of returned messages to: "+str(limit))

    timeout=request.args.get('timeout')
    if (timeout is None):
        timeout=10000
    else:
        timeout=min(int(timeout),60000)

    startTime=int(round(time.time() * 1000))
    currentTime=int(round(time.time() * 1000))

    while(currentTime<startTime+int(timeout)):
        with lock:
            if(len(msg_requests)>0):
                try:
                    msgs=''
                    cntr=0
                    while(cntr<limit and len(msg_requests)>0):
                        if (len(msgs)>1):
                            msgs=msgs+','
                        msgs=msgs+msg_requests.pop(0)
                        cntr_msg_requests_fetched += 1
                        cntr=cntr+1
                    msgs='['+msgs+']'
                    print(AGENT_READ_URL+" MSGs: "+json.dumps(json.loads(msgs), indent=2))
                    return Response(msgs, status=200, mimetype=MIME_JSON)
                except Exception as e:
                    print(AGENT_READ_URL+"-"+CAUGHT_EXCEPTION+" "+str(e) + " "+traceback.format_exc())
                    return Response(SERVER_ERROR+" "+str(e), status=500, mimetype=MIME_TEXT)
        sleep(0.025) # sleep 25 milliseconds
        currentTime=int(round(time.time() * 1000))

    print("timeout: "+str(timeout)+", startTime: "+str(startTime)+", currentTime: "+str(currentTime))
    return Response("[]", status=200, mimetype=MIME_JSON)

# Write messages stream. URI according to agent configuration.
# URI and payload, (PUT or POST): /events/A1-POLICY-AGENT-WRITE <json array of response messages>
# response: OK 200 or 400 for missing json parameters, 500 for other errors
@app.route(AGENT_WRITE_URL,
    methods=['PUT','POST'])
def events_write():
    global msg_responses
    global cntr_msg_responses_submitted
    with lock:
        print("AGENT_WRITE_URL lock")
        try:
            answer=request.json
            print(AGENT_WRITE_URL+ " json=" + json.dumps(answer, indent=2))
            if isinstance(answer, dict):
                #Create a an array if the answer is a dict (single message)
                answer_list=[]
                answer_list.append(answer)
                answer=answer_list

            for item in answer:
                id=item['correlationId']
                if (id is None):
                    print(AGENT_WRITE_URL+" parameter 'correlatonid' missing")
                    return Response('Parameter <correlationid> missing in json', status=400, mimetype=MIME_TEXT)
                msg=item['message']
                if (msg is None):
                    print(AGENT_WRITE_URL+" parameter 'msgs' missing")
                    return Response('Parameter >message> missing in json', status=400, mimetype=MIME_TEXT)
                status=item['status']
                if (status is None):
                    print(AGENT_WRITE_URL+" parameter 'status' missing")
                    return Response('Parameter <status> missing in json', status=400, mimetype=MIME_TEXT)
                if isinstance(msg, list) or isinstance(msg, dict):
                    msg_str=json.dumps(msg)+status[0:3]
                else:
                    msg_str=msg+status[0:3]
                msg_responses[id]=msg_str
                cntr_msg_responses_submitted += 1
                print(AGENT_WRITE_URL+ " msg+status (correlationid="+id+") :" + str(msg_str))
        except Exception as e:
            print(AGENT_WRITE_URL+"-"+CAUGHT_EXCEPTION+" "+str(e) + " "+traceback.format_exc())
            return Response('{"message": "' + SERVER_ERROR + ' ' + str(e) + '","status":"500"}', status=200, mimetype=MIME_JSON)

        return Response('{}', status=200, mimetype=MIME_JSON)


### Functions for metrics read out ###

@app.route('/counter/requests_submitted',
    methods=['GET'])
def requests_submitted():
    return Response(str(cntr_msg_requests_submitted), status=200, mimetype=MIME_TEXT)

@app.route('/counter/requests_fetched',
    methods=['GET'])
def requests_fetched():
    return Response(str(cntr_msg_requests_fetched), status=200, mimetype=MIME_TEXT)

@app.route('/counter/responses_submitted',
    methods=['GET'])
def responses_submitted():
    return Response(str(cntr_msg_responses_submitted), status=200, mimetype=MIME_TEXT)

@app.route('/counter/responses_fetched',
    methods=['GET'])
def responses_fetched():
    return Response(str(cntr_msg_responses_fetched), status=200, mimetype=MIME_TEXT)

@app.route('/counter/current_requests',
    methods=['GET'])
def current_requests():
    return Response(str(len(msg_requests)), status=200, mimetype=MIME_TEXT)

@app.route('/counter/current_responses',
    methods=['GET'])
def current_responses():
    return Response(str(len(msg_responses)), status=200, mimetype=MIME_TEXT)

### Admin ###

# Reset all messsages and counters
@app.route('/reset',
    methods=['GET', 'POST', 'PUT'])
def reset():
    global cntr_msg_requests_submitted
    global cntr_msg_requests_fetched
    global cntr_msg_responses_submitted
    global cntr_msg_responses_fetched
    global msg_requests
    global msg_responses

    cntr_msg_requests_submitted=0
    cntr_msg_requests_fetched=0
    cntr_msg_responses_submitted=0
    cntr_msg_responses_fetched=0
    msg_requests=[]
    msg_responses={}
    return Response('OK', status=200, mimetype=MIME_TEXT)

if __name__ == "__main__":
    app.run(port=HOST_PORT, host=HOST_IP)