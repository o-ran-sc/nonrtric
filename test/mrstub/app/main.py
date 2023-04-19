
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
from threading import RLock, Thread
import logging
import os
import requests

# Disable all logging of GET on reading counters
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

generic_messages={}

# Server info
HOST_IP = "::"
HOST_PORT = 2222

# Metrics vars
cntr_msg_requests_submitted=0
cntr_msg_requests_fetched=0
cntr_msg_responses_submitted=0
cntr_msg_responses_fetched=0

# Request and response constants
ORU_WRITE_URL="/events/unauthenticated.SEC_FAULT_OUTPUT"
ORU_READ_URL="/events/unauthenticated.SEC_FAULT_OUTPUT/users/test/"
A1PMS_WRITE_URL="/events/A1-POLICY-AGENT-WRITE"
A1PMS_READ_URL="/events/A1-POLICY-AGENT-READ/users/policy-agent"
APP_WRITE_URL="/send-request"
APP_READ_URL="/receive-response"
MIME_TEXT="text/plain"
MIME_JSON="application/json"
CAUGHT_EXCEPTION="Caught exception: "
SERVER_ERROR="Server error :"

topic_write=""
topic_read=""
generic_topics_upload_baseurl=""

uploader_thread=None
downloader_thread=None
generic_uploader_thread=None

# Function to upload A1PMS messages to dmaap
def dmaap_uploader():
    global msg_requests
    global cntr_msg_requests_fetched

    print("Starting uploader")

    headers = {'Content-type': 'application/json', 'Accept': '*/*'}
    #url="http://"+topic_host+"/events/"+topic_read
    url=topic_read

    while True:
        while (len(msg_requests)>0):
            msg=msg_requests[0]
            if msg is not None:
                try:
                    print("Sending to dmaap : "+ url)
                    print("Sending to dmaap : "+ msg)
                    resp=requests.post(url, data=msg, headers=headers, timeout=10)
                    if (resp.status_code<199 & resp.status_code > 299):
                        print("Failed, response code: " + str(resp.status_code))
                        sleep(1)
                    else:
                        print("Dmaap response code: " + str(resp.status_code))
                        print("Dmaap response text: " + str(resp.text))
                        with lock:
                            msg_requests.pop(0)
                            cntr_msg_requests_fetched += 1
                except Exception as e:
                    print("Failed, exception: "+ str(e))
                    sleep(1)
        sleep(0.01)


# Function to download A1PMS messages from dmaap
def dmaap_downloader():
    global msg_responses
    global cntr_msg_responses_submitted

    print("Starting uploader")

    while True:

        try :
            #url="http://"+topic_host+"/events/"+topic_write+"/users/mr-stub?timeout=15000&limit=100"
            url=topic_write
            headers = {'Accept': 'application/json'}
            print("Reading from dmaap: " + url)
            resp=requests.get(url, headers=headers)
            if (resp.status_code<199 & resp.status_code > 299):
                print("Failed, response code: " + resp.status_code)
                sleep(1)
            else:
                print("Recieved data from dmaap mr")
                try:
                    data=resp.json()
                    print("Recieved data (raw): " + str(resp.text))
                    if isinstance(data, list):
                        for item in data:
                            item=json.loads(item)
                            corrid=str(item["correlationId"])
                            status=str(item["status"])
                            msg=str(item["message"])
                            item_str=msg+status[0:3]
                            with lock:
                                msg_responses[corrid]=item_str
                                cntr_msg_responses_submitted += 1
                    else:
                        print("Data from dmaap is not json array: " + str(resp.text))
                        sleep(1)
                except Exception as e:
                    print("Corrupt data from dmaap mr -  dropping " + str(data))
                    print("CAUGHT_EXCEPTION" + str(e) + " "+traceback.format_exc())
                    sleep(1)
        except Exception as e:
            sleep(1)

# Function to upload generic messages to dmaap
def dmaap_generic_uploader():
    global msg_requests
    global cntr_msg_requests_fetched

    print("Starting generic uploader")

    headers_json = {'Content-type': 'application/json', 'Accept': '*/*'}
    headers_text = {'Content-type': 'text/plain', 'Accept': '*/*'}

    while True:
        if (len(generic_messages)):
            keys_copy = list(generic_messages.keys())
            for topicname in keys_copy:    #topicname contains the path of the topics, eg. "/event/<topic>"
                topic_queue=generic_messages[topicname]
                if (len(topic_queue)>0):
                    if (topicname.endswith(".text")):
                        msg=topic_queue[0]
                        headers=headers_text
                    else:
                        msg=topic_queue[0]
                        msg=json.dumps(msg)
                        headers=headers_json
                    url=generic_topics_upload_baseurl+topicname
                    print("Sending to dmaap : "+ url)
                    print("Sending to dmaap : "+ msg)
                    print("Sending to dmaap : "+ str(headers))
                    try:
                        resp=requests.post(url, data=msg, headers=headers, timeout=10)
                        if (resp.status_code<199 & resp.status_code > 299):
                            print("Failed, response code: " + str(resp.status_code))
                            sleep(1)
                        else:
                            print("Dmaap response code: " + str(resp.status_code))
                            print("Dmaap response text: " + str(resp.text))
                            with lock:
                                topic_queue.pop(0)
                                cntr_msg_requests_fetched += 1
                    except Exception as e:
                        print("Failed, exception: "+ str(e))
                        sleep(1)
        sleep(0.01)

#I'm alive function
@app.route('/',
    methods=['GET'])
def index():
    return 'OK', 200


# Helper function to create a Dmaap A1PMS request message
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

# Send a A1PMS message to MR
# URI and parameters (PUT or POST): /send-request?operation=<GET|PUT|POST|DELETE>&url=<url>
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
            if (oper == "PUT") and len(request.data) > 0:
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

# Receive a A1PMS message response for MR for the included correlation id
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
            cid=request.args.get('correlationid')
            if (cid is None):
                print(APP_READ_URL+" parameter 'correclationid' missing")
                return Response('Parameter correlationid missing in json', status=500, mimetype=MIME_TEXT)

            if (cid in msg_responses):
                answer=msg_responses[cid]
                del msg_responses[cid]
                print(APP_READ_URL+" response (correlationid="+cid+"): " + answer)
                cntr_msg_responses_fetched += 1
                return Response(answer, status=200, mimetype=MIME_JSON)

            print(APP_READ_URL+" - no messages (correlationid="+cid+"): ")
            return Response('', status=204, mimetype=MIME_JSON)
        except Exception as e:
            print(APP_READ_URL+"-"+CAUGHT_EXCEPTION+" "+str(e) + " "+traceback.format_exc())
            return Response(SERVER_ERROR+" "+str(e), status=500, mimetype=MIME_TEXT)

### Dmaap interface ###

# Read A1PMS messages stream. URI according to agent configuration.
# URI, (GET): /events/A1-POLICY-AGENT-READ/users/policy-agent
# response: 200 <json array of request messages>, or 500 for other errors
@app.route(A1PMS_READ_URL,
    methods=['GET'])
def events_read():
    global msg_requests
    global cntr_msg_requests_fetched

    if topic_write or topic_read:
        return Response('Url not available when running as mrstub frontend', status=404, mimetype=MIME_TEXT)

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

    start_time=int(round(time.time() * 1000))
    current_time=int(round(time.time() * 1000))

    while(current_time<start_time+int(timeout)):
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
                    print(A1PMS_READ_URL+" MSGs: "+json.dumps(json.loads(msgs), indent=2))
                    return Response(msgs, status=200, mimetype=MIME_JSON)
                except Exception as e:
                    print(A1PMS_READ_URL+"-"+CAUGHT_EXCEPTION+" "+str(e) + " "+traceback.format_exc())
                    return Response(SERVER_ERROR+" "+str(e), status=500, mimetype=MIME_TEXT)
        sleep(0.025) # sleep 25 milliseconds
        current_time=int(round(time.time() * 1000))

    print("timeout: "+str(timeout)+", start_time: "+str(start_time)+", current_time: "+str(current_time))
    return Response("[]", status=200, mimetype=MIME_JSON)

# Write A1PMS messages stream. URI according to agent configuration.
# URI and payload, (PUT or POST): /events/A1-POLICY-AGENT-WRITE <json array of response messages>
# response: OK 200 or 400 for missing json parameters, 500 for other errors
@app.route(A1PMS_WRITE_URL,
    methods=['PUT','POST'])
def events_write():
    global msg_responses
    global cntr_msg_responses_submitted

    if topic_write or topic_read:
        return Response('Url not available when running as mrstub frontend', status=404, mimetype=MIME_TEXT)

    with lock:
        print("A1PMS_WRITE_URL lock")
        try:
            answer=request.json
            print(A1PMS_WRITE_URL+ " json=" + json.dumps(answer, indent=2))
            if isinstance(answer, dict):
                #Create a an array if the answer is a dict (single message)
                answer_list=[]
                answer_list.append(answer)
                answer=answer_list

            for item in answer:
                cid=item['correlationId']
                if (cid is None):
                    print(A1PMS_WRITE_URL+" parameter 'correlatonid' missing")
                    return Response('Parameter <correlationid> missing in json', status=400, mimetype=MIME_TEXT)
                msg=item['message']
                if (msg is None):
                    print(A1PMS_WRITE_URL+" parameter 'msgs' missing")
                    return Response('Parameter >message> missing in json', status=400, mimetype=MIME_TEXT)
                status=item['status']
                if (status is None):
                    print(A1PMS_WRITE_URL+" parameter 'status' missing")
                    return Response('Parameter <status> missing in json', status=400, mimetype=MIME_TEXT)
                if isinstance(msg, list) or isinstance(msg, dict):
                    msg_str=json.dumps(msg)+status[0:3]
                else:
                    msg_str=msg+status[0:3]
                msg_responses[cid]=msg_str
                cntr_msg_responses_submitted += 1
                print(A1PMS_WRITE_URL+ " msg+status (correlationid="+cid+") :" + str(msg_str))
        except Exception as e:
            print(A1PMS_WRITE_URL+"-"+CAUGHT_EXCEPTION+" "+str(e) + " "+traceback.format_exc())
            return Response('{"message": "' + SERVER_ERROR + ' ' + str(e) + '","status":"500"}', status=200, mimetype=MIME_JSON)

        return Response('{}', status=200, mimetype=MIME_JSON)

@app.route(ORU_WRITE_URL,
    methods=['PUT','POST'])
def oru_write():
    global msg_requests
    msg=json.dumps(request.json)
    msg_requests.append(msg)
    return Response('{}', status=200, mimetype=MIME_JSON)

@app.route(ORU_READ_URL,
    methods=['GET'])
def oru_read():
    global msg_requests
    if(len(msg_requests)>0):
        rsp=msg_requests.pop(0)
        res=[]
        res.append(rsp)
        return Response(json.dumps(res), status=200, mimetype=MIME_JSON)
    return Response("[]", status=200, mimetype=MIME_JSON)

# Generic POST catching all urls starting with /events/<topic>.
# Writes the message in a que for that topic
@app.route("/events/<path>",
    methods=['POST'])
def generic_write(path):
    global generic_messages
    global cntr_msg_responses_submitted
    urlkey="/events/"+str(path)
    write_method=str(request.method)
    with lock:
        try:
            if (urlkey.endswith(".text")):
                payload=str(request.data.decode('UTF-8'))
                print(write_method+" on "+urlkey+" text=" + payload)
            else:
                payload=request.json
                print(write_method+" on "+urlkey+" json=" + json.dumps(payload))
            topicmsgs=[]
            if (urlkey in generic_messages.keys()):
                topicmsgs=generic_messages[urlkey]
            else:
                generic_messages[urlkey]=topicmsgs

            if isinstance(payload, list):
                for listitem in payload:
                    topicmsgs.append(listitem)
            else:
                topicmsgs.append(payload)

            cntr_msg_responses_submitted += 1
        except Exception as e:
            print(write_method + "on "+urlkey+"-"+CAUGHT_EXCEPTION+" "+str(e) + " "+traceback.format_exc())
            return Response('{"message": "' + SERVER_ERROR + ' ' + str(e) + '","status":"500"}', status=200, mimetype=MIME_JSON)

        return Response('{}', status=200, mimetype=MIME_JSON)

# Generic GET catching all urls starting with /events/. Returns max 4096 json msgs in an array.
# Returns only the messages previously written to the same urls
@app.route("/events/<path:path>",
    methods=['GET'])
def generic_read(path):
    global generic_messages
    global cntr_msg_requests_fetched

    if generic_topics_upload_baseurl:
        return Response('Url not available when running as mrstub frontend', status=404, mimetype=MIME_TEXT)

    urlpath="/events/"+str(path)
    urlkey="/events/"+str(path).split("/")[0] #Extract topic
    print("GET on topic"+urlkey)
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

    start_time=int(round(time.time() * 1000))
    current_time=int(round(time.time() * 1000))
    topicmsgs=[]
    if (urlkey in generic_messages.keys()):
        topicmsgs=generic_messages[urlkey]

    while(current_time<start_time+int(timeout)):
        with lock:
            if(len(topicmsgs)>0):
                try:
                    msgs=''
                    cntr=0
                    while(cntr<limit and len(topicmsgs)>0):
                        if (len(msgs)>1):
                            msgs=msgs+','
                        msgs=msgs+json.dumps(json.dumps(topicmsgs.pop(0)))
                        cntr_msg_requests_fetched += 1
                        cntr=cntr+1
                    msgs='['+msgs+']'
                    print("GET on "+urlpath+" MSGs: "+msgs)
                    return Response(msgs, status=200, mimetype=MIME_JSON)
                except Exception as e:
                    print("GET on "+urlpath+"-"+CAUGHT_EXCEPTION+" "+str(e) + " "+traceback.format_exc())
                    return Response(SERVER_ERROR+" "+str(e), status=500, mimetype=MIME_TEXT)
        sleep(0.025) # sleep 25 milliseconds
        current_time=int(round(time.time() * 1000))

    print("timeout: "+str(timeout)+", start_time: "+str(start_time)+", current_time: "+str(current_time))
    return Response("[]", status=200, mimetype=MIME_JSON)


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

# Reset all messages and counters
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

# Get env vars, if present
if os.getenv("TOPIC_READ") is not None:

    print("Env variables:")
    print("TOPIC_READ:"+os.environ['TOPIC_READ'])
    print("TOPIC_WRITE:"+os.environ['TOPIC_WRITE'])

    topic_read=os.environ['TOPIC_READ']
    topic_write=os.environ['TOPIC_WRITE']


    if topic_read and downloader_thread is None:
        downloader_thread=Thread(target=dmaap_downloader)
        downloader_thread.start()

    if topic_write and uploader_thread is None:
        uploader_thread=Thread(target=dmaap_uploader)
        uploader_thread.start()

if 'GENERIC_TOPICS_UPLOAD_BASEURL' in os.environ:
    print("GENERIC_TOPICS_UPLOAD_BASEURL:"+os.environ['GENERIC_TOPICS_UPLOAD_BASEURL'])
    generic_topics_upload_baseurl=os.environ['GENERIC_TOPICS_UPLOAD_BASEURL']
    if generic_topics_upload_baseurl and generic_uploader_thread is None:
        generic_uploader_thread=Thread(target=dmaap_generic_uploader)
        generic_uploader_thread.start()

if os.getenv("TOPIC_READ") is None or os.environ['GENERIC_TOPICS_UPLOAD_BASEURL'] is None:
    print("No env variables - OK")

if __name__ == "__main__":
    app.run(port=HOST_PORT, host=HOST_IP)
