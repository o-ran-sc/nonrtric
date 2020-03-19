
from flask import Flask, request
from time import sleep
import time
import datetime
import json
from flask import Flask
from flask import Response

app = Flask(__name__)

# list of messages to/from Dmaap
msgRequests={}
msgResponses={}

# Server info
HOST_IP = "0.0.0.0"
HOST_PORT = 3905

# Metrics vars
msg_requests_submitted=0
msg_requests_fetched=0
msg_responses_submitted=0
msg_responses_fetched=0


#I'm alive function
@app.route('/',
    methods=['GET'])
def index():
    return 'OK', 200


# Helper function to create a Dmaap request message
# args : <GET|PUT|DELETE> <correlation id> <json-payload - may be None> <url>
# response: json formatted string of a complete Dmaap message
def createMessage(operation, correlationId, payload, url):
    if (payload is None):
        payload="{}"
    timeStamp=datetime.datetime.utcnow()
    msg = '{\"apiVersion\":\"1.0\",\"operation\":\"'+operation+'\",\"correlationId\":\"'+correlationId+'\",\"originatorId\": \"849e6c6b420\",'
    msg = msg+'\"payload\":'+payload+',\"requestId\":\"23343221\", \"target\":\"policy-agent\", \"timestamp\":\"'+str(timeStamp)+'\", \"type\":\"request\",\"url\":\"'+url+'\"}'

    return msg


### MR-stub interface, for MR control

# Send a message to MR
# URI and parameters (GET): /send-request?operation=<GET|PUT|POST|DELETE>&url=<url>
# response: <correlationId> (http 200) o4 400 for parameter error or 500 for other errors
@app.route('/send-request',
    methods=['PUT','POST'])
def sendrequest():
    global msgRequests
    global msg_requests_submitted

    try:
        print("/send-request req: " + str(request.args), flush=True)
        oper=request.args.get('operation')
        if (oper is None):
            return Response('Parameter operation missing in request', status=400, mimetype='text/plain')

        url=request.args.get('url')
        if (url is None):
            return Response('Parameter url missing in request', status=400, mimetype='text/plain')

        if (oper != "GET" and oper != "PUT" and oper != "POST" and oper != "DELETE"):
            return Response('Parameter operation does not contain DEL|PUT|POST|DELETE in request', status=400, mimetype='text/plain')

        correlationId=str(time.time_ns())
        payload="{}"
        if (oper == "PUT"):
            payload=str(request.json)

        msg=createMessage(oper, correlationId, payload, url)
        print("/send-request MSG: " + msg, flush=True)
        msgRequests[correlationId]=msg
        msg_requests_submitted += 1
        return Response(correlationId, status=200, mimetype='text/plain')
    except Exception as e:
        print("Caught exception: "+str(e), flush=True)
        return Response('Server error', status=500, mimetype='text/plain')

# Receive a message response for MR for the included correlation id
# URI and parameter, (GET): /receive-response?correlationId=<correlationId>
# response: <json-array of 1 response> 200 or empty 204 or other errors 500
@app.route('/receive-response',
    methods=['GET'])
def receiveresponse():
    global msgResponses
    global msg_responses_fetched

    try:
        id=request.args.get('correlationId')
        if (id is None):
            return Response('Parameter correlationId missing in json', status=500, mimetype='text/plain')

        if (id in msgResponses):
            ANSWER=msgResponses[id]
            del msgResponses[id]
            print("/receive-response ANSWER(correlationId="+id+"): " + ANSWER, flush=True)
            msg_responses_fetched += 1
            return Response(ANSWER, status=200, mimetype='application/json')

        print("/receive-response - no messages (correlationId="+id+"): ", flush=True)
        return Response('', status=204, mimetype='application/json')
    except Exception as e:
        print("Caught exception: "+str(e), flush=True)
        return Response('Server error', status=500, mimetype='text/plain')



### Dmaap interface ###

# Read messages stream. URI according to agent configuration.
# URI, (GET): /events/A1-POLICY-AGENT-READ/users/policy-agent
# response: 200 <json array of request messages>, or 500 for other errors
@app.route('/events/A1-POLICY-AGENT-READ/users/policy-agent',
    methods=['GET'])
def events():
    global msgRequests
    global msg_requests_fetched

    try:
        msgs=''
        for item in msgRequests:
            if (len(msgs)>1):
                msgs=msgs+','
            msgs=msgs+msgRequests[item]
            msg_requests_fetched += 1

        msgRequests={}
        msgs='['+msgs+']'
        print("/events/A1-POLICY-AGENT-READ/users/policy-agent, MESSAGES: "+msgs, flush=True)
        return Response(msgs, status=200, mimetype='application/json')
    except Exception as e:
        print("Caught exception: "+str(e), flush=True)
        return Response('Server error', status=500, mimetype='text/plain')

# Write messages stream. URI according to agent configuration.
# URI and payload, (PUT or POST): /events/A1-POLICY-AGENT-WRITE <json array of response messages>
# response: OK 200 or 400 for missing json parameters, 500 for other errors
@app.route('/events/A1-POLICY-AGENT-WRITE',
    methods=['PUT','POST'])
def events_write():
    global msgResponses
    global msg_responses_submitted

    print("/events/A1-POLICY-AGENT-WRITE, ANSWER: " + str(request.json), flush=True)
    try:
        ANSWER=request.json

        for item in ANSWER:
            id=item['correlationId']
            if (id is None):
                return Response('Parameter <correlationId> missing in json', status=400, mimetype='text/plain')
            msg=item['message']
            if (msg is None):
                return Response('Parameter >message> missing in json', status=400, mimetype='text/plain')
            status=item['status']
            if (status is None):
                return Response('Parameter <status> missing in json', status=400, mimetype='text/plain')
            msgStr=msg+status[0:3]
            msgResponses[id]=msgStr
            msg_responses_submitted += 1
            print("   msg stored for correlationId "+id+": " + msgStr, flush=True)
    except Exception as e:
        print("Caught exception: "+str(e), flush=True)
        return Response('Server error', status=500, mimetype='text/plain')

    return Response('OK', status=200, mimetype='text/plain')


### Functions for metrics read out ###

@app.route('/msg_requests_submitted',
    methods=['GET'])
def requests_submitted():
    return Response(str(msg_requests_submitted), status=200, mimetype='text/plain')

@app.route('/msg_requests_fetched',
    methods=['GET'])
def requests_fetched():
    return Response(str(msg_requests_fetched), status=200, mimetype='text/plain')

@app.route('/msg_responses_submitted',
    methods=['GET'])
def responses_submitted():
    return Response(str(msg_responses_submitted), status=200, mimetype='text/plain')

@app.route('/msg_responses_fetched',
    methods=['GET'])
def responses_fetched():
    return Response(str(msg_responses_fetched), status=200, mimetype='text/plain')

@app.route('/current_requests',
    methods=['GET'])
def current_requests():
    return Response(str(len(msgRequests)), status=200, mimetype='text/plain')

@app.route('/current_responses',
    methods=['GET'])
def current_responses():
    return Response(str(len(msgResponses)), status=200, mimetype='text/plain')

### Admin ###

# Reset all messsages and counters
@app.route('/reset',
    methods=['GET', 'POST', 'PUT'])
def reset():
    global msg_requests_submitted
    global msg_requests_fetched
    global msg_responses_submitted
    global msg_responses_fetched
    global msgRequests
    global msgResponses

    msg_requests_submitted=0
    msg_requests_fetched=0
    msg_responses_submitted=0
    msg_responses_fetched=0
    msgRequests={}
    msgResponses={}
    return Response('OK', status=200, mimetype='text/plain')

### Main function ###

if __name__ == "__main__":
    app.run(port=HOST_PORT, host=HOST_IP)
