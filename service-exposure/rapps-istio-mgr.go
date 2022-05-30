// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2022: Nordix Foundation
//   %%
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//   ========================LICENSE_END===================================
//

package main

import (
	"bytes"
	"context"
	"fmt"
	netv1beta1 "istio.io/client-go/pkg/apis/networking/v1beta1"
	secv1beta1 "istio.io/client-go/pkg/apis/security/v1beta1"
	versioned "istio.io/client-go/pkg/clientset/versioned"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	k8Yaml "k8s.io/apimachinery/pkg/util/yaml"
	"k8s.io/client-go/rest"
	clientcmd "k8s.io/client-go/tools/clientcmd"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

const (
	NAMESPACE = "istio-nonrtric"
)

var gatewayManifest = `
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: nonrtric-istio-RAPP-NAME-gateway
  namespace: RAPP-NS 
spec:
  selector:
    istio: ingressgateway # use Istio gateway implementation
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "*"
`

var virtualServiceManifest = `
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: nonrtric-istio-RAPP-NAME-vs
  namespace: RAPP-NS 
spec:
  hosts:
  - "*"
  gateways:
  - nonrtric-istio-RAPP-NAME-gateway
  http:
  - name: "RAPP-NAME-routes"
    match:
    - uri:
        prefix: "/RAPP-NAME"
    route:
    - destination:
        port:
          number: 80
        host: RAPP-NAME.RAPP-NS.svc.cluster.local
`

var requestAuthenticationManifest = `
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: "jwt-RAPP-NAME"
  namespace: RAPP-NS 
spec:
  selector:
    matchLabels:
      app.kubernetes.io/instance: RAPP-NAME
  jwtRules:
  - issuer: "http://192.168.49.2:31560/auth/realms/REALM-NAME"
    jwksUri: "http://192.168.49.2:31560/auth/realms/REALM-NAME/protocol/openid-connect/certs"
  - issuer: "http://keycloak.default:8080/auth/realms/REALM-NAME"
    jwksUri: "http://keycloak.default:8080/auth/realms/REALM-NAME/protocol/openid-connect/certs"
  - issuer: "https://192.168.49.2:31561/auth/realms/REALM-NAME"
    jwksUri: "https://192.168.49.2:31561/auth/realms/REALM-NAME/protocol/openid-connect/certs"
  - issuer: "https://keycloak.default:8443/auth/realms/REALM-NAME"
    jwksUri: "https://keycloak.default:8443/auth/realms/REALM-NAME/protocol/openid-connect/certs"
  - issuer: "https://keycloak.est.tech:443/auth/realms/REALM-NAME"
    jwksUri: "https://keycloak.default:8443/auth/realms/REALM-NAME/protocol/openid-connect/certs"
  - issuer: "http://istio-ingressgateway.istio-system:80/auth/realms/REALM-NAME"
    jwksUri: "http://keycloak.default:8080/auth/realms/REALM-NAME/protocol/openid-connect/certs"
`

var authorizationPolicyManifest = `
apiVersion: "security.istio.io/v1beta1"
kind: "AuthorizationPolicy"
metadata:
  name: "RAPP-NAME-policy"
  namespace: RAPP-NS 
spec:
  selector:
    matchLabels:
      app.kubernetes.io/instance: RAPP-NAME
  action: ALLOW
  rules:
  - from:
    - source:
        requestPrincipals: ["http://192.168.49.2:31560/auth/realms/REALM-NAME/", "http://keycloak.default:8080/auth/realms/REALM-NAME/", "https://192.168.49.2:31561/auth/realms/REALM-NAME/", "https://keycloak.default:8443/auth/realms/REALM-NAME/", "https://keycloak.est.tech:443/auth/realms/REALM-NAME/", "http://istio-ingressgateway.istio-system:80/auth/realms/REALM-NAME/"]
  - to:
    - operation:
        methods: ["METHOD-NAME"]
        paths: ["/RAPP-NAME"]
    when:
    - key: request.auth.claims[clientRole]
      values: ["ROLE-NAME"]
`

func connectToK8s() *versioned.Clientset {
	config, err := rest.InClusterConfig()
	if err != nil {
		// fallback to kubeconfig
		home, exists := os.LookupEnv("HOME")
		if !exists {
			home = "/root"
		}

		kubeconfig := filepath.Join(home, ".kube", "config")
		if envvar := os.Getenv("KUBECONFIG"); len(envvar) > 0 {
			kubeconfig = envvar
		}
		config, err = clientcmd.BuildConfigFromFlags("", kubeconfig)
		if err != nil {
			log.Fatalln("failed to create K8s config")
		}
	}

	ic, err := versioned.NewForConfig(config)
	if err != nil {
		log.Fatalf("Failed to create istio client: %s", err)
	}

	return ic
}

func createGateway(clientset *versioned.Clientset, appName string) (string, error) {
	gtClient := clientset.NetworkingV1beta1().Gateways(NAMESPACE)
	manifest := strings.Replace(gatewayManifest, "RAPP-NAME", appName, -1)
	manifest = strings.Replace(manifest, "RAPP-NS", NAMESPACE, -1)

	gt := &netv1beta1.Gateway{}
	dec := k8Yaml.NewYAMLOrJSONDecoder(bytes.NewReader([]byte(manifest)), 1000)

	if err := dec.Decode(&gt); err != nil {
		return "", err
	}

	result, err := gtClient.Create(context.TODO(), gt, metav1.CreateOptions{})

	if err != nil {
		return "", err
	}

	fmt.Printf("Create Gateway %s \n", result.GetName())
	return result.GetName(), nil
}

func createVirtualService(clientset *versioned.Clientset, appName string) (string, error) {
	vsClient := clientset.NetworkingV1beta1().VirtualServices(NAMESPACE)
	manifest := strings.Replace(virtualServiceManifest, "RAPP-NAME", appName, -1)
	manifest = strings.Replace(manifest, "RAPP-NS", NAMESPACE, -1)

	vs := &netv1beta1.VirtualService{}
	dec := k8Yaml.NewYAMLOrJSONDecoder(bytes.NewReader([]byte(manifest)), 1000)

	if err := dec.Decode(&vs); err != nil {
		return "", err
	}

	result, err := vsClient.Create(context.TODO(), vs, metav1.CreateOptions{})

	if err != nil {
		return "", err
	}

	fmt.Printf("Create Virtual Service %s \n", result.GetName())
	return result.GetName(), nil
}

func createRequestAuthentication(clientset *versioned.Clientset, appName, realmName string) (string, error) {
	raClient := clientset.SecurityV1beta1().RequestAuthentications(NAMESPACE)
	manifest := strings.Replace(requestAuthenticationManifest, "RAPP-NAME", appName, -1)
	manifest = strings.Replace(manifest, "REALM-NAME", realmName, -1)
	manifest = strings.Replace(manifest, "RAPP-NS", NAMESPACE, -1)

	ra := &secv1beta1.RequestAuthentication{}
	dec := k8Yaml.NewYAMLOrJSONDecoder(bytes.NewReader([]byte(manifest)), 1000)

	if err := dec.Decode(&ra); err != nil {
		return "", err
	}

	result, err := raClient.Create(context.TODO(), ra, metav1.CreateOptions{})

	if err != nil {
		return "", err
	}

	fmt.Printf("Create Request Authentication %s \n", result.GetName())
	return result.GetName(), nil
}

func createAuthorizationPolicy(clientset *versioned.Clientset, appName, realmName, roleName, methodName string) (string, error) {
	apClient := clientset.SecurityV1beta1().AuthorizationPolicies(NAMESPACE)
	manifest := strings.Replace(authorizationPolicyManifest, "RAPP-NAME", appName, -1)
	manifest = strings.Replace(manifest, "REALM-NAME", realmName, -1)
	manifest = strings.Replace(manifest, "ROLE-NAME", roleName, -1)
	manifest = strings.Replace(manifest, "METHOD-NAME", methodName, -1)
	manifest = strings.Replace(manifest, "RAPP-NS", NAMESPACE, -1)

	ap := &secv1beta1.AuthorizationPolicy{}
	dec := k8Yaml.NewYAMLOrJSONDecoder(bytes.NewReader([]byte(manifest)), 1000)

	if err := dec.Decode(&ap); err != nil {
		return "", err
	}

	result, err := apClient.Create(context.TODO(), ap, metav1.CreateOptions{})

	if err != nil {
		return "", err
	}

	fmt.Printf("Create Authorization Policy %s \n", result.GetName())
	return result.GetName(), nil
}

func removeGateway(clientset *versioned.Clientset, appName string) {
	gtClient := clientset.NetworkingV1beta1().Gateways(NAMESPACE)
	err := gtClient.Delete(context.TODO(), "nonrtric-istio-"+appName+"-gateway", metav1.DeleteOptions{})
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted Gateway nonrtric-istio-" + appName + "-gateway")
	}
}

func removeVirtualService(clientset *versioned.Clientset, appName string) {
	vsClient := clientset.NetworkingV1beta1().VirtualServices(NAMESPACE)
	err := vsClient.Delete(context.TODO(), "nonrtric-istio-"+appName+"-vs", metav1.DeleteOptions{})
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted VirtualServices nonrtric-istio-" + appName + "-vs")
	}
}

func removeRequestAuthentication(clientset *versioned.Clientset, appName string) {
	raClient := clientset.SecurityV1beta1().RequestAuthentications(NAMESPACE)
	err := raClient.Delete(context.TODO(), "jwt-"+appName, metav1.DeleteOptions{})
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted RequestAuthentication jwt-" + appName)
	}
}

func removeAuthorizationPolicy(clientset *versioned.Clientset, appName string) {
	apClient := clientset.SecurityV1beta1().AuthorizationPolicies(NAMESPACE)
	err := apClient.Delete(context.TODO(), appName+"-policy", metav1.DeleteOptions{})
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted AuthorizationPolicy " + appName + "-policy")
	}
}

func createIstioPolicy(res http.ResponseWriter, req *http.Request) {
	query := req.URL.Query()
	realmName := query.Get("realm")
	appName := query.Get("name")
	roleName := query.Get("role")
	methodName := query.Get("method")
	var msg string
	clientset := connectToK8s()
	_, err := createGateway(clientset, appName)
	if err != nil {
		msg = err.Error()
		fmt.Println(err.Error())
	} else {
		_, err := createVirtualService(clientset, appName)
		if err != nil {
			msg = err.Error()
			fmt.Println(err.Error())
		} else {
			_, err := createRequestAuthentication(clientset, appName, realmName)
			if err != nil {
				msg = err.Error()
				fmt.Println(err.Error())
			} else {
				_, err := createAuthorizationPolicy(clientset, appName, realmName, roleName, methodName)
				if err != nil {
					msg = err.Error()
					fmt.Println(err.Error())
				} else {
					msg = "Istio rapp security setup successfully"
				}
			}
		}
	}

	// create response binary data
	data := []byte(msg) // slice of bytes
	// write `data` to response
	res.Write(data)
}

func removeIstioPolicy(res http.ResponseWriter, req *http.Request) {
	query := req.URL.Query()
	appName := query.Get("name")
	clientset := connectToK8s()
	removeAuthorizationPolicy(clientset, appName)
	removeRequestAuthentication(clientset, appName)
	removeVirtualService(clientset, appName)
	removeGateway(clientset, appName)
}

func main() {
	createIstioHandler := http.HandlerFunc(createIstioPolicy)
	http.Handle("/create", createIstioHandler)
	removeIstioHandler := http.HandlerFunc(removeIstioPolicy)
	http.Handle("/remove", removeIstioHandler)
	http.ListenAndServe(":9000", nil)
}
