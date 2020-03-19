## mrstub - stub interface to interact with the policy agent over Dmaap ##

The mrstub is intended for function test to simulate a message router.
The mrstub exposes the read and write urls, used by the agent, as configured in consul.
In addition, the requests can be feed to the mrstub and the result can be read by polling


### Control interface ###

The control interface can be used by any test script.
The following operations are available:

>Send a message to MR<br>
This method puts a messages in the queue for the agent to pick up. The returned correlationId is used when polling for the reposone of this particular request.<br>
```URI and parameters (GET): /send-request?operation=<GET|PUT|POST|DELETE>&url=<url>```<br><br>
```response: <correlationId> (http 200) o4 400 for parameter error or 500 for other errors```

>Receive a message response for MR for the included correlation id<br>
The method is for polling of messages, return immediately containing the received response (if any) for the supplied correlationId.<br>
```URI and parameter, (GET): /receive-response?correlationId=<correlationId>```<br><br>
```response: <json-array of 1 response> 200 or empty 204 or other errors 500```

### Build and start ###

>Build image<br>
```docker build -t mrstub .```

>Start the image<br>
```docker run -it -p 3905:3905 mrstub```

The script ```mrstub-build-start.sh``` do the above two steps in one go. This starts the stub in stand-alone mode for basic test.<br>If the mrstub should be executed with the agent, replace docker run with this command to connect to the docker network with the correct service name (--name shall be the same as configured in consul for the read and write streams).
```docker run -it -p 3905:3905 --network nonrtric-docker-net --name message-router mrstub```



### Basic test ###

Basic test is made with the script ```basic_test.sh``` which tests all the available url with a subset of the possible operations.