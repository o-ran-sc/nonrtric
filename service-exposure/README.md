This collection of files represent rapp service exposure prototyping in O-RAN.
Prerequisites: Istio should be installed on your cluster with the demo profile (istioctl install --set profile=demo). Please refer to the istio documentation for more information.
The deployments have been implemented and tested using minikube.
To replicate these tests you will need to setup the various host path referenced in the yaml files on your own machine.

chartmuseum.yaml:             path: /var/chartmuseum/charts
keycloak.yaml:                path: /auth/realms/master
keycloak.yaml:                path: /var/keycloak/certs
postgres.yaml:                path: "/var/keycloak/data2"
postgres.yaml:                path: /tmp
rapps-keycloak-mgr.yaml:      path: /var/rapps/certs

or change them to match your own setup.

Create the istio-nonrtric namespace and enable it for istio injection

   kubectl create ns istio-nonrtric 

   kubectl label namespace istio-nonrtric istio-injection=enabled


All go programs need to be built prior to running the Dockerfiles

   go build rapps-helm-installer.go    
   go build rapps-keycloak-mgr.go  
   go build rapps-istio-mgr.go     
   go build rapps-rapp-provider.go
   go build rapps-rapp-invoker.go

Once the go programs have been compile you then need to build a docker image for each of them.

   docker build -f Dockerfile_rim . -t <tag prefix>/rapps-istio-mgr
   docker build -f Dockerfile_rkm . -t <tag prefix>/rapps-keycloak-mgr
   docker build -f Dockerfile_rhi . -t <tag prefix>/rapps-helm-installer
   docker build -f Dockerfile_rri . -t <tag prefix>/rapps-rapp-invoker
   docker build -f Dockerfile_rrp . -t <tag prefix>/rapps-rapp-provider

Image references in the yaml files/helm charts should be changed to match your own tagged images. 

You will need to package your rapp charts and copy them to the /var/chartmuseum/charts directory before starting.

   cd charts/
   helm package rapp-provider
   scp -i $(minikube ssh-key) rapp-provider-0.1.0.tgz docker@$(minikube ip):/var/chartmuseum/charts

   helm package rapp-invoker
   scp -i $(minikube ssh-key) rapp-invoker-0.1.0.tgz docker@$(minikube ip):/var/chartmuseum/charts


Start keycloak and postgres in the default namespace with istio injection:

   istioctl kube-inject -f postgres.yaml | kubectl apply -f -
   istioctl kube-inject -f keycloak.yaml | kubectl apply -f -

To start the management pods run: 

   start_pods.sh

Run: 
   kubectl get pods to ensure all managements pods are up and running
   NAME                                             READY   STATUS    RESTARTS   AGE
   chartmuseum-deployment-7b8cd4c9d4-tpmhl          1/1     Running   0          8s
   keycloak-bc6f78f88-zmxlt                         2/2     Running   0          2m20s
   postgres-6fb4cc8db6-bbhg9                        2/2     Running   0          2m34s
   rapps-helm-installer-deployment-67476694-sxb2d   1/1     Running   0          6s
   rapps-istio-mgr-deployment-67c67647b6-scmqc      1/1     Running   0          7s
   rapps-keycloak-mgr-deployment-7464f87575-trvmx   1/1     Running   0          7s

Get the node port for the helm installer that corresponds to port 80

   kubectl get svc rapps-helm-installer
   NAME                   TYPE       CLUSTER-IP     EXTERNAL-IP   PORT(S)        AGE
   rapps-helm-installer   NodePort   10.96.58.211   <none>        80:31570/TCP   8m9s

Once these pods are up and running run: 
   curl http://<minikube ip>:<helm installer node port>/install?chart=<rapp chart name> 
   to install your rapp

   e.g. curl http://192.168.49.2:31570/install?chart=rapp-provider
        Successfully installed release: rapp-provider

This will setup keycloak realm + client, istio policies and deploy your chart.

You should install both the provider and the invoker to see the pods communicating.

Check the invoker logs to see the test message:

   kubectl logs rapp-invoker-758468d7d4-njmdn  -n istio-nonrtric
   Received response for rapp-provider get request - Hello World!

If you want to test using the rp_test.sh file, the client_secret field needs be changed to match the secret for you keycloak client.
You can find this in the keycloak-mgr log.

To uninstall run: 
   curl http://<minikube ip>:<helm installer node port>/uninstall?chart=<rapp chart name>
   e.g. curl http://192.168.49.2:31570/uninstall?chart=rapp-invoker
        Successfully uninstalled release: rapp-invoker

To stop the management pods run: 
   stop_pods.sh

Remove postgres and keycloak with the following commands:
   kubectl delete -f keycloak.yaml
   kubectl delete -f postgres.yaml
