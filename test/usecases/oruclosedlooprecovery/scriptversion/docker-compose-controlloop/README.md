This docker-compose will create a control loop that will deploy all components of the closed loop recovery use case into a k8s cluster using the k8s participant from CLAMP in ONAP.

It will also bring up the chartmuseum registry that will be used by helm when deploying the charts.
The script named chartmuseum_init.sh will push all the charts into the chartmuseum.
This script is mounted into the k8s-participant docker container but can also be run locally.

Depending on the type of k8s cluster and the operating system being used, different settings might need to be done for the k8s-participant docker container. For example, in case of minikube, the following should be added under k8s-participant (assuming that kube-config file of the host machine has been copied into the config directory):

volumes:
 - ./config/kube-config:/home/policy/.kube/config:ro
 - ~/.minikube/profiles/minikube:/home/policy/.minikube/profiles/minikube

This will mount the kube-config file into the k8s-participant docker container so that it is able to deploy services into the minikube instance running in the host machine. The minikube directory contains the client-certificate and client-key.

Since the kube-api server is running in the host machine instead of the k8s-participant docker container, some extra steps are needed:

1) Linux

Run the following command in the host machine so that the localhost referred to in the kube-config file points to the host machine:

iptables -A INPUT -i docker0 -j ACCEPT

2) Mac

Mac OS does not seem to have the iptables command. However, in order to refer to the host machine from inside the docker container, one may use "host.docker.internal" but this gives rise to another problem:

Unable to connect to the server: x509: certificate is valid for minikubeCA, control-plane.minikube.internal, kubernetes.default.svc.cluster.local, kubernetes.default.svc, kubernetes.default, kubernetes, localhost, not host.docker.internal

As a workaround, the TLS can be disabled. So, the following part should be modified in the kube-config file:

- cluster:
    server: https://host.docker.internal:<PORT>
    insecure-skip-tls-verify: true


## License

Copyright (C) 2021 Nordix Foundation.
Licensed under the Apache License, Version 2.0 (the "License")
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
