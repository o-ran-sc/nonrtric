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
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	kubernetes "k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"net/http"
	"net/url"
	"strings"
	"time"
)

type Jwttoken struct {
	Access_token       string
	Expires_in         int
	Refresh_expires_in int
	Refresh_token      string
	Token_type         string
	Not_before_policy  int
	Session_state      string
	Scope              string
}

var gatewayHost string
var gatewayPort string
var keycloakHost string
var keycloakPort string
var securityEnabled string
var useGateway string
var role string
var rapp string
var methods string
var healthy bool = true
var ttime time.Time
var jwt Jwttoken

const (
  namespace = "istio-nonrtric"
)

func getToken(secretName string) string {
	if ttime.Before(time.Now()) {
		clientSecret, clientId, realmName := getSecret(secretName)
		keycloakUrl := "http://" + keycloakHost + ":" + keycloakPort + "/auth/realms/" + realmName + "/protocol/openid-connect/token"
		resp, err := http.PostForm(keycloakUrl,
			url.Values{"client_secret": {clientSecret}, "grant_type": {"client_credentials"}, "client_id": {clientId}})
		if err != nil {
			fmt.Println(err)
			panic("Something wrong with the credentials or url ")
		}
		defer resp.Body.Close()
		body, err := ioutil.ReadAll(resp.Body)
		json.Unmarshal([]byte(body), &jwt)
		ttime = time.Now()
		ttime = ttime.Add(time.Second * time.Duration(jwt.Expires_in))
	}
	return jwt.Access_token
}

func getSecret(secretName string) (string, string, string) {
        clientset := connectToK8s()
        res, err := clientset.CoreV1().Secrets(namespace).Get(context.TODO(), secretName, metav1.GetOptions{})
        if err != nil {
                fmt.Println(err.Error())
        }
        return string(res.Data["client_secret"]), string(res.Data["client_id"]), string(res.Data["realm"])
}

func MakeRequest(client *http.Client, prefix string, method string, ch chan string) {
	var service = strings.Split(prefix, "/")[1]
	var gatewayUrl = "http://" + gatewayHost + ":" + gatewayPort
	var token = ""
	var jsonValue []byte = []byte{}
	var restUrl string = ""

	if securityEnabled == "true" {
		secretName := role + "-secret"
		token = getToken(secretName)
	} else {
		useGateway = "N"
	}

	if strings.ToUpper(useGateway) != "Y" {
		gatewayUrl = "http://" + service + "."+namespace+":80"
		prefix = ""
	}

	restUrl = gatewayUrl + prefix

	req, err := http.NewRequest(method, restUrl, bytes.NewBuffer(jsonValue))
	if err != nil {
		fmt.Printf("Got error %s", err.Error())
	}
	req.Header.Set("Content-type", "application/json")
	req.Header.Set("Authorization", "Bearer "+token)

	resp, err := client.Do(req)
	if err != nil {
		fmt.Printf("Got error %s", err.Error())
	}
	defer resp.Body.Close()
	body, _ := ioutil.ReadAll(resp.Body)
	respString := string(body[:])
	if respString == "RBAC: access denied" {
		respString += " for " + service + " " + strings.ToLower(method) + " request"
	}
	fmt.Printf("Received response for %s %s request - %s\n", service, strings.ToLower(method), respString)
	ch <- prefix + "," + method
}

func connectToK8s() *kubernetes.Clientset {
	config, err := rest.InClusterConfig()
	if err != nil {
		fmt.Println("failed to create K8s config")
	}

	clientset, err := kubernetes.NewForConfig(config)
	if err != nil {
		fmt.Println("Failed to create K8s clientset")
	}

	return clientset
}

func health(res http.ResponseWriter, req *http.Request) {
	if healthy {
		res.WriteHeader(http.StatusOK)
		res.Write([]byte("healthy"))
	} else {
		res.WriteHeader(http.StatusInternalServerError)
		res.Write([]byte("unhealthy"))
	}
}

func main() {
	ttime = time.Now()
	time.Sleep(1 * time.Second)
	flag.StringVar(&gatewayHost, "gatewayHost", "istio-ingressgateway.istio-system", "Gateway Host")
	flag.StringVar(&gatewayPort, "gatewayPort", "80", "Gateway Port")
	flag.StringVar(&keycloakHost, "keycloakHost", "istio-ingressgateway.istio-system", "Keycloak Host")
	flag.StringVar(&keycloakPort, "keycloakPort", "80", "Keycloak Port")
	flag.StringVar(&useGateway, "useGateway", "Y", "Connect to services through API gateway")
	flag.StringVar(&securityEnabled, "securityEnabled", "true", "Security is required to use this application")
	flag.StringVar(&role, "role", "provider-viewer", "Role granted to application")
	flag.StringVar(&rapp, "rapp", "rapp-provider", "Name of rapp to invoke")
	flag.StringVar(&methods, "methods", "GET", "Methods to access application")
	flag.Parse()

	healthHandler := http.HandlerFunc(health)
	http.Handle("/health", healthHandler)
	go func() {
		http.ListenAndServe(":9000", nil)
	}()

	client := &http.Client{
		Timeout: time.Second * 10,
	}

	ch := make(chan string)
	var prefixArray []string = []string{"/" + rapp}
	var methodArray []string = []string{methods}
	for _, prefix := range prefixArray {
		for _, method := range methodArray {
			go MakeRequest(client, prefix, method, ch)
		}
	}

	ioutil.WriteFile("init.txt", []byte("Initialization done."), 0644)

	for r := range ch {
		go func(resp string) {
			time.Sleep(10 * time.Second)
			elements := strings.Split(resp, ",")
			prefix := elements[0]
			method := elements[1]
			MakeRequest(client, prefix, method, ch)
		}(r)
	}

}
