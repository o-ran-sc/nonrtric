
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
import socket
from threading import RLock
from hashlib import md5

# Disable all logging of GET on reading counters and db
class AjaxFilter(logging.Filter):
    def filter(self, record):
        return ("/counter/" not in record.getMessage()) and ("/db" not in record.getMessage())

log = logging.getLogger('werkzeug')
log.addFilter(AjaxFilter())

app = Flask(__name__)

lock = RLock()

# list of callback messages
msg_callbacks={}

# Server info
HOST_IP = "::"
HOST_PORT = 2222

# Metrics vars
cntr_msg_callbacks=0
cntr_batch_callbacks=0
cntr_msg_fetched=0
cntr_callbacks={}
hosts_set=set()

# Request and response constants
CALLBACK_URL="/callbacks/<string:id>"
CALLBACK_MR_URL="/callbacks-mr/<string:id>" #Json list with string encoded items
CALLBACK_TEXT_URL="/callbacks-text/<string:id>" # Callback for string of text
APP_READ_URL="/get-event/<string:id>"
APP_READ_ALL_URL="/get-all-events/<string:id>"
DUMP_ALL_URL="/db"
NULL_URL="/callbacks-null"  # Url for ignored callback. Callbacks are not checked, counted or stored

MIME_TEXT="text/plain"
MIME_JSON="application/json"
CAUGHT_EXCEPTION="Caught exception: "
SERVER_ERROR="Server error :"
TIME_STAMP="cr-timestamp"

forced_settings={}
forced_settings['delay']=None


# Remote host lookup and print host name
def remote_host_logging(request):

    if request.environ.get('HTTP_X_FORWARDED_FOR') is None:
        host_ip=str(request.environ['REMOTE_ADDR'])
    else:
        host_ip=str(request.environ['HTTP_X_FORWARDED_FOR'])
    prefix='::ffff:'
    if (host_ip.startswith('::ffff:')):
        host_ip=host_ip[len(prefix):]
    try:
        name, alias, addresslist = socket.gethostbyaddr(host_ip)
        print("Calling host: "+str(name))
        hosts_set.add(name)
    except Exception:
        print("Calling host not possible to retrieve IP: "+str(host_ip))
        hosts_set.add(host_ip)


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

    with lock:
        try:
            if ((id in msg_callbacks.keys()) and (len(msg_callbacks[id]) > 0)):
                cntr_msg_fetched+=1
                cntr_callbacks[id][1]+=1
                msg=msg_callbacks[id][0]
                print("Fetching msg for id: "+id+", msg="+str(msg))

                if (isinstance(msg,dict)):
                    del msg[TIME_STAMP]
                    if ("md5" in msg.keys()):
                        print("EXTRACTED MD5")
                        msg=msg["md5"]
                        print("MD5: "+str(msg))

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

    with lock:
        try:
            if ((id in msg_callbacks.keys()) and (len(msg_callbacks[id]) > 0)):
                cntr_msg_fetched+=len(msg_callbacks[id])
                cntr_callbacks[id][1]+=len(msg_callbacks[id])
                msg=msg_callbacks[id]
                print("Fetching all msgs for id: "+id+", msg="+str(msg))
                for sub_msg in msg:
                    if (isinstance(sub_msg, dict)):
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
        remote_host_logging(request)
        print("raw data: str(request.data): "+str(request.data))
        do_delay()
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

        with lock:
            cntr_msg_callbacks += 1
            if (isinstance(msg, dict)):
                msg[TIME_STAMP]=str(datetime.now())
            if (id in msg_callbacks.keys()):
                msg_callbacks[id].append(msg)
            else:
                msg_callbacks[id]=[]
                msg_callbacks[id].append(msg)

            if (id in cntr_callbacks.keys()):
                cntr_callbacks[id][0] += 1
                cntr_callbacks[id][2] += 1
            else:
                cntr_callbacks[id]=[]
                cntr_callbacks[id].append(1)
                cntr_callbacks[id].append(0)
                cntr_callbacks[id].append(0)

    except Exception as e:
        print(CAUGHT_EXCEPTION+str(e))
        traceback.print_exc()
        return 'NOTOK',500

    return 'OK',200


# Receive a json callback message with payload formatted according to output from the message router
# Array of stringified json objects
# URI and payload, (PUT or POST): /callbacks-mr/<id> <json messages>
# json is a list of string encoded json items
# response: OK 200 or 500 for other errors
@app.route(CALLBACK_MR_URL,
    methods=['PUT','POST'])
def events_write_mr(id):
    global msg_callbacks
    global cntr_msg_callbacks
    global cntr_batch_callbacks

    storeas=request.args.get('storeas') #If set, store payload as a md5 hash code and dont log the payload
                                        #Large payloads will otherwise overload the server
    try:
        print("Received callback (mr) for id: "+id +", content-type="+request.content_type)
        print("raw data: str(request.data): "+str(request.data))
        if (storeas is None):
            print("raw data: str(request.data): "+str(request.data))
        do_delay()
        list_data=False
        try:
            #if (request.content_type == MIME_JSON):
            if (MIME_JSON in request.content_type):
                data = request.data
                msg_list = json.loads(data)
                if (storeas is None):
                    print("Payload(json): "+str(msg_list))
                list_data=True
            else:
                msg_list=[]
                print("Payload(content-type="+request.content_type+"). Setting empty json as payload")
        except Exception as e:
            msg_list=[]
            print("(Exception) Payload does not contain any json, setting empty json as payload")
            traceback.print_exc()

        with lock:
            remote_host_logging(request)
            if (list_data):
                cntr_batch_callbacks += 1
            for msg in msg_list:
                if (storeas is None):
                    msg=json.loads(msg)
                else:
                    #Convert to compact json without ws between parameter and value...
                    #It seem that ws is added somewhere along to way to this server
                    msg=json.loads(msg)
                    msg=json.dumps(msg, separators=(',', ':'))

                    md5msg={}
                    md5msg["md5"]=md5(msg.encode('utf-8')).hexdigest()
                    msg=md5msg
                    print("msg (json converted to md5 hash): "+str(msg["md5"]))
                cntr_msg_callbacks += 1
                if (isinstance(msg, dict)):
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
                    cntr_callbacks[id].append(0)
            if (id in msg_callbacks.keys() and list_data):
                cntr_callbacks[id][2] += 1

    except Exception as e:
        print(CAUGHT_EXCEPTION+str(e))
        traceback.print_exc()
        return 'NOTOK',500

    return 'OK',200

# Receive a callback message of a single text message (content type ignored)
# or a json array of strings (content type json)
# URI and payload, (PUT or POST): /callbacks-text/<id> <text message>
# response: OK 200 or 500 for other errors
@app.route(CALLBACK_TEXT_URL,
    methods=['PUT','POST'])
def events_write_text(id):
    global msg_callbacks
    global cntr_msg_callbacks
    global cntr_batch_callbacks

    storeas=request.args.get('storeas') #If set, store payload as a md5 hash code and dont log the payload
                                        #Large payloads will otherwise overload the server
    try:
        print("Received callback for id: "+id +", content-type="+request.content_type)
        remote_host_logging(request)
        if (storeas is None):
            print("raw data: str(request.data): "+str(request.data))
        do_delay()

        try:
            msg_list=None
            list_data=False
            if (MIME_JSON in request.content_type):  #Json array of strings
                msg_list=json.loads(request.data)
                list_data=True
            else:
                data=request.data.decode("utf-8")    #Assuming string
                msg_list=[]
                msg_list.append(data)
            with lock:
                cntr_batch_callbacks += 1
                for msg in msg_list:
                    if (storeas == "md5"):
                        md5msg={}
                        print("msg: "+str(msg))
                        print("msg (endcode str): "+str(msg.encode('utf-8')))
                        md5msg["md5"]=md5(msg.encode('utf-8')).hexdigest()
                        msg=md5msg
                        print("msg (data converted to md5 hash): "+str(msg["md5"]))

                    if (isinstance(msg, dict)):
                        msg[TIME_STAMP]=str(datetime.now())

                    cntr_msg_callbacks += 1
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
                        cntr_callbacks[id].append(0)
                if (id in cntr_callbacks.keys() and list_data):
                    cntr_callbacks[id][2] += 1
        except Exception as e:
            print(CAUGHT_EXCEPTION+str(e))
            traceback.print_exc()
            return 'NOTOK',500


    except Exception as e:
        print(CAUGHT_EXCEPTION+str(e))
        traceback.print_exc()
        return 'NOTOK',500

    return 'OK',200

# Receive a callback message but ignore contents and return 200
# URI and payload, (PUT or POST): /callbacks-text/<id> <text message>
# response: OK 200
@app.route(NULL_URL,
    methods=['PUT','POST'])
def null_url(id):
    return 'OK',200

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

@app.route('/counter/received_callback_batches',
    methods=['GET'])
def batches_submitted():
    req_id = request.args.get('id')
    if (req_id is None):
        return Response(str(cntr_batch_callbacks), status=200, mimetype=MIME_TEXT)

    if (req_id in cntr_callbacks.keys()):
        return Response(str(cntr_callbacks[req_id][2]), status=200, mimetype=MIME_TEXT)
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

@app.route('/counter/remote_hosts',
    methods=['GET'])
def remote_hosts():
    global hosts_set

    hosts=",".join(hosts_set)
    return Response(str(hosts), status=200, mimetype=MIME_TEXT)


#Set force delay response, in seconds, for all callbacks
#/forceresponse?delay=<seconds>
@app.route('/forcedelay', methods=['POST'])
def forcedelay():

  try:
    forced_settings['delay']=int(request.args.get('delay'))
  except Exception:
    forced_settings['delay']=None
  return Response("Force delay: " + str(forced_settings['delay']) + " sec set for all callback responses", 200, mimetype=MIME_TEXT)

# Helper: Delay if delayed response code is set
def do_delay():
  if (forced_settings['delay'] is not None):
    try:
      val=int(forced_settings['delay'])
      if (val < 1):
          return Response("Force delay too short: " + str(forced_settings['delay']) + " sec", 500, mimetype=MIME_TEXT)
      print("Delaying "+str(val)+ " sec.")
      time.sleep(val)
    except Exception:
      return Response("Force delay : " + str(forced_settings['delay']) + " sec failed", 500, mimetype=MIME_TEXT)
### Admin ###

# Reset all messages and counters
@app.route('/reset',
    methods=['GET', 'POST', 'PUT'])
def reset():
    global msg_callbacks
    global cntr_msg_fetched
    global cntr_msg_callbacks
    global cntr_batch_callbacks
    global cntr_callbacks
    global forced_settings

    with lock:
        msg_callbacks={}
        cntr_msg_fetched=0
        cntr_msg_callbacks=0
        cntr_batch_callbacks=0
        cntr_callbacks={}
        forced_settings['delay']=None

        return Response('OK', status=200, mimetype=MIME_TEXT)

### Main function ###

if __name__ == "__main__":
    app.run(port=HOST_PORT, host=HOST_IP)
