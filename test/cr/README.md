# callback receiver - a stub interface to receive callbacks

The callback receiver is intended for function tests to simulate a RAPP.
The callback receiver exposes the read and write urls, used by the a1pms, as configured in service.
The callback receiver receives notifications from A1PMS when synchronization happens between A1PMS and RICs. However, the callback receiver can be uses to receive any json payload from any source.

## Ports and certificates

The CR normally opens the port 8090 for http. If a certificate and a key are provided the simulator will also open port 8091 for https.
The certificate and key shall be placed in the same dir and the dir shall be mounted to /usr/src/app/cert in the container.

| Port     | Protocol |
| -------- | ----- |
| 8090     | http  |
| 8091     | https |

The dir cert contains a self-signed cert. Use the script generate_cert_and_key.sh to generate a new certificate and key. The password of the certificate must be set 'test'.
The same urls are available on both the http port 8090 and the https port 8091. If using curl and https, the flag -k shall be given to make curl ignore checking the certificate.

### Control interface

The control interface can be used by any test script.
The following REST operations are available:

>Send a message to CR<br>
This method puts a request message from A1PMS to notify that synchronization between A1PMS and certain RIC happens.<br>
```URI and payload, (PUT or POST): /callbacks/<id> <json messages>```<br>
```response: OK 200 or 500 for other errors```

>Fetch one message for an id from CR<br>
This method fetches the oldest message for an id, and removes the message.<br>
```URI and payload, (GET): /get-event/<id>```<br>
```response:  <json messages> 200 or 500 for other errors```

>Fetch all messages for an id from CR<br>
This method fetches all message in an array for an id, and removes all messages.<br>
```URI and payload, (GET): /get-all-events/<id>```<br>
```response:  <json array of json messages> 200 or 500 for other errors```

>Dump all currently waiting callback messages in CR<br>
This method fetches all message in an array for an id. Messages are left intact in the CR.<br>
```URI and payload, (GET): /db```<br>
```response:  <json> 200```

>Metrics - counters<br>
There are a number of counters that can be read to monitor the message processing. Do a http GET on any of the current counters and an integer value will be returned with http response code 200.
```/counter/received_callbacks``` - The total number of received callbacks<br>
```/counter/fetched_callbacks``` - The total number of fetched callbacks<br>
```/counter/current_messages``` - The current number of callback messages waiting to be fetched<br>
All counters also support the query parameter "id" to fetch counter for one individual id, eg ```/counter/current_messages?id=my-id```
An additional counter is available to log remote hosts calling the server
```/counter/remote_hosts``` - Lists all unique ip/host name that has sent messages on the callback endpoint<br>

### Build and start

>Build image<br>
```docker build --build-arg NEXUS_PROXY_REPO=nexus3.onap.org:10001/ -t callback-receiver .```

>Start the image on both http and https<br>
```docker run --rm -it -p 8090:8090 -p 8091:8091 callback-receiver```

It will listen to http 8090 port and https 8091 port(using default certificates) at the same time.

By default, this image has default certificates under /usr/src/app/cert
file "cert.crt" is the certificate file
file "key.crt" is the key file
file "generate_cert_and_key.sh" is a shell script to generate certificate and key
file "pass" stores the password when you run the shell script

This certificates/key can be overridden by mounting a volume when using "docker run" or "docker-compose"
In 'docker run', use field:<br>
>```-v "$PWD/certificate:/usr/src/app/cert"```<br/>

eg:
>```docker run --rm -it -p 8090:8090 -p 8091:8091 -v "/PATH_TO_CERT/cert:/usr/src/app/cert" callback-receiver```

In 'docker-compose.yml', use field:
>```volumes: - ./certificate:/usr/src/app/cert:ro```

The script ```cr-build-start.sh``` do the above two steps in one go. This starts the callback-receiver container in stand-alone mode for basic test.<br>If the callback-receiver should be executed manually with the a1pms, replace docker run with this command to connect to the docker network with the correct service name (--name shall be aligned with the other components, i.e. the host named given in all callback urls).
>```docker run --rm -it -p 8090:8090 -p 8091:8091 --network nonrtric-docker-net --name callback-receiver callback-receiver```

>Start the image on http only<br>
```docker run --rm -it -p 8090:8090 callback-receiver```

### Basic test

Basic test is made with the script ```basic_test.sh nonsecure|secure``` which tests all the available urls with a subset of the possible operations. Use the script ```cr-build-start.sh``` to start the callback-receiver in a container first.

## License

Copyright (C) 2020 Nordix Foundation. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
