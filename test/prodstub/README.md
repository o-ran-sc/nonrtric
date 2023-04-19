# producer stub - a stub interface to simulate data producers

The producer stub is intended for function tests of simulate data producers.
The simulator handles the callbacks for supervision of producers as well as create/update and delete jobs.
As an initial step, the intended job and producers, with supported types, are setup (armed) in the simulator.
In addition, specific response codes can configured for each callback request.

## Ports and certificates

The prodstub normally opens the port 8092 for http. If a certificate and a key are provided the simulator will also open port 8093 for https.
The certificate and key shall be placed in the same dir and the dir shall be mounted to /usr/src/app/cert in the container.

| Port     | Protocol |
| -------- | ----- |
| 8092     | http  |
| 8093     | https |

The dir cert contains a self-signed cert. Use the script generate_cert_and_key.sh to generate a new certificate and key. The password of the certificate must be set 'test'.
The same urls are available on both the http port 8092 and the https port 8093. If using curl and https, the flag -k shall be given to make curl ignore checking the certificate.

### Prodstub interface

>Create callback<br>
This method receives a callback for create job. The request shall contain a job json. The request is checked towards what has been setup (armed) and the response will be set accordingly. <br>
```URI and payload, (POST): /callbacks/job/<producer_id>,  <job-json>```<br>
```response: 200/201 (or configured response) or 400 for other errors```

>Delete callback<br>
This method receives a callback for delete job. The request is checked towards what has been setup (armed) and the response will be set accordingly. <br>
```URI and payload, (DELETE): /callbacks/job/<producer_id>```<br>
```response: 204 (or configured response) or 400 for other errors```

>Supervision callback<br>
This method receives a callback for producer supervision. The request is checked towards what has been setup (armed) and the response will be set accordingly. <br>
```URI and payload, (GET): /callbacks/supervision/<producer_id>```<br>
```response: 200 (or configured response) or 400 for other errors```

### Control interface

The control interface can be used by any test script.
The following REST operations are available:

>Arm a job create<br>
This method arms a job for creation and sets an optional response code for create/update<br>
```URI and payload, (PUT): /arm/create/<producer_id>/<job_id>[?response=<responsecode>]```<br>
```response: 200 or 400 for other errors```
>Arm a job delete<br>
This method arms a job for deletion and sets an optional response code for delete<br>
```URI and payload, (PUT): /arm/delete/<producer_id>/<job_id>[?response=<responsecode>]```<br>
```response: 200 or 400 for other errors```

>Arm a producer supervision<br>
This method arms a supervision and sets an optional response code for supervision calls<br>
```URI and payload, (PUT): /arm/delete/<producer_id>[?response=<responsecode>]```<br>
```response: 200 or 400 for other errors```

>Arm a type for a producer<br>
This method arms a type for a producer<br>
```URI and payload, (PUT): /arm/type/<producer_id>/<ype-id>```<br>
```response: 200 or 400 for other errors```

>Disarm a type for a producer<br>
This method disarms a type for a producer<br>
```URI and payload, (DELETE): /arm/type/<producer_id>/<ype-id>```<br>
```response: 200 or 400 for other errors```

>Get job data parameters<br>
This method fetches the job data parameters of a job<br>
```URI and payload, (GET): /jobdata/<producer_id>job_id>```<br>
```response: 200 or 400 for other errors```

>Remove job data parameters<br>
This method removes the job data parameters from a job<br>
```URI and payload, (DELETE): /jobdata/<producer_id>job_id>```<br>
```response: 200 or 400 for other errors```

>Start/stop job data delivery<br>
This method start (or stops) delivering job data to the configured target url. Action is either 'start' or s'stop'<br>
```URI and payload, (POST): /jobdata/<producer_id>job_id>?action=action```<br>
```response: 200 or 400 for other errors```

>Counter for create job<br>
This method returns the number of create/update calls to a job<br>
```URI and payload, (GET): /counter/create/producer_id>/<job_id>```<br>
```response: <integer> 200 or 400 for other errors```

>Counter for delete job<br>
This method returns the number of delete calls to a job<br>
```URI and payload, (GET): /counter/delete/producer_id>/<job_id>```<br>
```response: <integer> 200 or 400 for other errors```

>Counter for producer supervision<br>
This method returns the number of supervision calls to a producer<br>
```URI and payload, (GET): /counter/supervision/producer_id>```<br>
```response: <integer> 200 or 400 for other errors```

>Get internal db<br>
This method dumps the internal db of producer and jobs as a json file<br>
```URI and payload, (GET): /status```<br>
```response: <json> 200 or 400 for other errors```

>Reset<br>
This method makes a full reset by removing all producers and jobs<br>
```URI and payload, (GET or PUT or POST): /reset```<br>
```response: <json> 200 or 400 for other errors```

### Build and start

>Build image<br>
```docker build --build-arg NEXUS_PROXY_REPO=nexus3.onap.org:10001/ -t producer-stub .```

>Start the image on both http and https<br>
```docker run --rm -it -p 8092:8092 -p 8093:8093 --name producer-stub producer-stub```

It will listen to http 8092 port and https 8093 port(using default certificates) at the same time.

>Start the image on http and https<br>
By default, this image has default certificates under /usr/src/app/cert
file "cert.crt" is the certificate file
file "key.crt" is the key file
file "generate_cert_and_key.sh" is a shell script to generate certificate and key
file "pass" stores the password when you run the shell script

>Start the container without specifying external certificates:<br>
```docker run --rm -it --p 8092:8092 -p 8093:8093 producer-stub```

It will listen to http 8092 port and https 8093 port(using default certificates) at the same time.

This certificates/key can be overridden by mounting a volume when using "docker run" or "docker-compose"
In 'docker run', use field:
>```-v "$PWD/certificate:/usr/src/app/cert"```

eg:
>```docker run --rm -it --p 8092:8092 -p 8093:8093 -v "/PATH_TO_CERT/cert:/usr/src/app/cert" producer-stub```<br>

In 'docker-compose.yml', use field:<br>
>```volumes: - ./certificate:/usr/src/app/cert:ro```

The script ```prodstub-build-start.sh``` do the build and docker run in one go. This starts the stub container in stand-alone mode for basic test.<br>If the producer-stub should be executed manually with the a1pms, replace docker run with this command to connect to the docker network with the correct service name (--name shall be the same as configured in consul for the read and write streams).
```docker run --rm -it -p 8092:8092 -p 8093:8093 --name producer-stub producer-stub```

### Basic test

Basic test is made with the script ```basic_test.sh nonsecure|secure``` which tests all the available urls with a subset of the possible operations. Choose nonsecure for http and secure for https. Use the script ```prodstub-build-start.sh``` to start the producer-stub in a container first.

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
