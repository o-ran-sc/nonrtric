#
# ============LICENSE_START=======================================================
#  Copyright (C) 2022-2023 Nordix Foundation.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#
This collection of files represent rapp service exposure prototyping in O-RAN.
Prerequisites: Istio should be installed on your cluster with the demo profile. You may need to add istioctl to you $PATH variable.
  istioctl install --set profile=demo
Please refer to the istio documentation for more information.
Please refer to the K8s documentation: Manage TLS Certificates in a Cluster
The deployments have been implemented and tested using minikube.
If you are not using minikube, references to "minikube ip" should be changed to the appropiate value for you host.
The ipAddresses field in cluster-issuer.yaml not referring to the generic localhost ip should be changed to your own ip.
To replicate these tests you will need to setup the various host path referenced in the yaml files on your own machine.

chartmuseum.yaml:             path: /var/chartmuseum/charts
postgres.yaml:                path: "/var/keycloak/data2"

or change them to match your own setup.

Create the istio-nonrtric namespace and enable it for istio injection

   kubectl create ns istio-nonrtric

   kubectl label namespace istio-nonrtric istio-injection=enabled


All go programs need to be built prior to running the Dockerfiles

   go build rapps-helm-installer.go
   go build rapps-keycloak-mgr.go
   go build rapps-istio-mgr.go
   go build rapps-webhook.go
   go build rapps-jwt.go
   go build rapps-rapp-helloworld-provider.go
   go build rapps-rapp-helloworld-invoker1.go
   go build rapps-rapp-helloworld-invoker2.go

Once the go programs have been compile you then need to build a docker image for each of them.

   docker build -f Dockerfile_rim . -t <tag prefix>/rapps-istio-mgr
   docker build -f Dockerfile_rkm . -t <tag prefix>/rapps-keycloak-mgr
   docker build -f Dockerfile_rhi . -t <tag prefix>/rapps-helm-installer
   docker build -f Dockerfile_wh . -t <tag prefix>/rapps-webhook
   docker build -f Dockerfile_jwt . -t <tag prefix>/rapps-jwt
   docker build -f Dockerfile_rhwp  . -t <tag prefix>/rapps-rapp-helloworld-provider
   docker build -f Dockerfile_rhwi1  . -t <tag prefix>/rapps-rapp-helloworld-invoker1
   docker build -f Dockerfile_rhwi2  . -t <tag prefix>/rapps-rapp-helloworld-invoker2

Image references in the yaml files/helm charts should be changed to match your own tagged images.

You will need to package your rapp charts and copy them to the /var/chartmuseum/charts directory before starting.

   cd charts/
   helm package rapp-helloworld-provider
   scp -i $(minikube ssh-key) rapp-helloworld-provider-0.1.0.tgz docker@$(minikube ip):/var/chartmuseum/charts

   helm package rapp-helloworld-invoker1
   scp -i $(minikube ssh-key) rapp-helloworld-invoker1-0.1.0.tgz docker@$(minikube ip):/var/chartmuseum/charts

   helm package rapp-helloworld-invoker2
   scp -i $(minikube ssh-key) rapp-helloworld-invoker2-0.1.0.tgz docker@$(minikube ip):/var/chartmuseum/charts

Start cert-manager using the following command:
   ./cert-manager.sh deploy

Copy keycloak client certs into the istio-nonrtric namespace by running:
   ./copy_tls_secret.sh -n cm-keycloak-client-certs -s default -d istio-nonrtric

Start keycloak and postgres in the default namespace with istio injection by running:

   ./keycloak.sh deploy

To start the management pods run:

   ./start_pods.sh

Once all pods have been started a list of running pods is displayed at the end of the script:
NAME                                                         READY   STATUS    RESTARTS   AGE
chartmuseum-deployment-7b8cd4c9d4-nd7dk                      1/1     Running   0          9s
jwt-proxy-admission-controller-deployment-66797fb6df-mlk8t   1/1     Running   0          8s
keycloak-846ff979bc-ndvdf                                    2/2     Running   0          2m16s
postgres-78b4b9d95-nqjkj                                     2/2     Running   0          2m29s
rapps-helm-installer-deployment-67476694-n5r24               1/1     Running   0          8s
rapps-istio-mgr-deployment-67c67647b6-p5s2k                  1/1     Running   0          8s
rapps-keycloak-mgr-deployment-7464f87575-54h9x               1/1     Running   0          8s


Once these pods are up and running use the following command to install the rapps:

   ./deploy_rapp.sh rapp-helloworld-provider

   ./deploy_rapp.sh rapp-helloworld-invoker1

   ./deploy_rapp.sh rapp-helloworld-invoker2

   Note: The line export host= should be changed to the appropaite ip for the host you are running on.

This will setup keycloak realm + client, istio policies and deploy your chart.

You should install both the provider and the invoker to see the pods communicating.

Check the invoker logs to see the test message:

   kubectl logs rapp-helloworld-invoker1-758468d7d4-njmdn  -n istio-nonrtric
   Received response for rapp-helloworld-provider get request - Hello World!

If you want to test using the rp_test.sh file, the client_secret field needs be changed to match the secret for you keycloak client.
You can find this in the keycloak-mgr log.

To uninstall the management pods and and rapps run:
   ./stop_pods.sh

You can also uninstall individual rapp using the undeploy_rapp.sh script.
   e.g. ./undeploy_rapp.sh rapp-helloworld-provider

Remove postgres and keycloak with the following command:
   ./keycloak.sh undeploy

Remove cert-manager with the following command:
  ./cert-manager.sh undeploy
