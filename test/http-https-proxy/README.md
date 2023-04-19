# http proxy

The http proxy is a generic http proxy which is able to proxy both http and https destination calls.
The call to the proxy (to control the proxy call) also supports both http and https (https is using a self signed cert).
The main usage for the proxy is as a gateway to all services and pod inside a kubernetes cluster.
However, it can be used a basic standard http proxy as well.

## Ports and certificates

The proxy opens the http and https port according to the table below.

| Port     | Proxy protocol | Usage |
| -------- | ------ |----- |
| 8080     | http   | Proxy call for http, can proxy both http and https |
| 8433     | https  | Proxy call for https, can proxy both http and https |
| 8081     | http   | Http port for alive check, returns json with basic statistics |
| 8434     | https  | Https port for alive check, returns json with basic statistics |

The dir cert contains a self-signed cert. Use the script generate_cert_and_key.sh to generate a new certificate and key before building the container, the certs need to be re-generated. If another cert is used, all three files (cert.crt, key.crt and pass) in the cert dir should be mounted to the dir '/usr/src/app/cert' in the container.

### Proxy usage

| Operation | curl example |
| --------- | ------------ |
| proxy http call via http | curl --proxy <http://localhost:8080> <http://100.110.120.130:1234> |
| proxy https call via http | curl -k --proxy <http://localhost:8080> <https://100.110.120.130:5678> |
| proxy http call via https | curl --proxy-insecure --proxy <https://localhost:8433> <http://100.110.120.130:1234> |
| proxy https call via https | curl --proxy-insecure --proxy <https://localhost:8433> <https://100.110.120.130:5678> |
| alive check and get stats | curl localhost:8081 |
| alive check and get stats via proxy |  curl --proxy localhost:8080 <http://localhost:8081> |

### Build and start

>Build image<br>
```docker build --build-arg NEXUS_PROXY_REPO=nexus3.onap.org:10001/ -t nodejs-http-proxy:latest .```

>Start the image on both http and https<br>
```docker run --rm -it -p 8080:8080 -p 8081:8081 -p 8433:8433 -p 8434:8434 nodejs-http-proxy:latest```

It will listen to http ports 8080/8081 and https ports 8433/8434 (using default certificates) at the same time.

The script ```proxy-build-start.sh``` do the above two steps in one go. This starts the container in stand-alone mode for basic test.<br>

### Basic test

Basic test is made with the script ```basic_test.sh``` which tests proxy. Use the script ```proxy-build-start.sh``` to start the proxy in a container first.

## License

Copyright (C) 2021 Nordix Foundation. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
