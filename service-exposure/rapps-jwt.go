// -
//
//	========================LICENSE_START=================================
//	O-RAN-SC
//	%%
//	Copyright (C) 2022-2023: Nordix Foundation
//	%%
//	Licensed under the Apache License, Version 2.0 (the "License");
//	you may not use this file except in compliance with the License.
//	You may obtain a copy of the License at
//
//	     http://www.apache.org/licenses/LICENSE-2.0
//
//	Unless required by applicable law or agreed to in writing, software
//	distributed under the License is distributed on an "AS IS" BASIS,
//	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//	See the License for the specific language governing permissions and
//	limitations under the License.
//	========================LICENSE_END===================================
package main

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	kubernetes "k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"net"
	"net/http"
	"net/url"
	"rapps/utils/generatejwt"
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

var keycloakHost string
var keycloakPort string
var keycloakAlias string
var realmName string
var clientId string
var namespace string
var authenticator string
var tlsCrt string
var tlsKey string
var caCrt string
var healthy bool = true
var jwt Jwttoken

const (
	scope                 = "email"
	client_assertion_type = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
)

func getToken(res http.ResponseWriter, req *http.Request) {
	var resp = &http.Response{}
	var err error
	authenticator = req.Header.Get("authenticator")
	clientId = req.Header.Get("client")
	realmName = req.Header.Get("realm")
	namespace = req.Header.Get("ns")
	tlsCrt = req.Header.Get("tlsCrt")
	tlsKey = req.Header.Get("tlsKey")
	caCrt = req.Header.Get("caCrt")
	keycloakUrl := "http://" + keycloakHost + ":" + keycloakPort + "/realms/" + realmName + "/protocol/openid-connect/token"
	fmt.Printf("Making token request to %s\n", keycloakUrl)
	res.Header().Set("Content-type", "application/json")
	res.Header().Set("Authorization", "")

	if authenticator == "client-jwt" {
		resp, err = getJwtToken(keycloakUrl, clientId)
	} else if authenticator == "client-x509" {
		keycloakPort = "443"
		keycloakUrl := "https://" + keycloakAlias + ":" + keycloakPort + "/realms/" + realmName + "/protocol/openid-connect/token"
		resp, err = getx509Token(keycloakUrl, clientId, tlsCrt, tlsKey, caCrt)
	} else {
		resp, err = getSecretToken(keycloakUrl, clientId)
	}

	if err != nil {
		fmt.Println(err)
		res.WriteHeader(http.StatusInternalServerError)
		res.Write([]byte(err.Error()))
		panic("Something wrong with the credentials or url ")
	}

	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	json.Unmarshal([]byte(body), &jwt)
	fmt.Printf("Token: %s\n", jwt.Access_token)

	res.Header().Set("Authorization", "Bearer "+jwt.Access_token)
	res.WriteHeader(http.StatusOK)
	res.Write([]byte("Successfully retrieved JWT access token"))
}

func getJwtToken(keycloakUrl, clientId string) (*http.Response, error) {
	var resp = &http.Response{}
	var err error
	client_assertion := getClientAssertion()

	if jwt.Refresh_token != "" {
		resp, err = http.PostForm(keycloakUrl, url.Values{"client_assertion_type": {client_assertion_type},
			"client_assertion": {client_assertion}, "grant_type": {"refresh_token"},
			"refresh_token": {jwt.Refresh_token}, "client_id": {clientId}, "scope": {scope}})
	} else {
		resp, err = http.PostForm(keycloakUrl, url.Values{"client_assertion_type": {client_assertion_type},
			"client_assertion": {client_assertion}, "grant_type": {"client_credentials"},
			"client_id": {clientId}, "scope": {scope}})
	}

	return resp, err
}

func getClientAssertion() string {
	aud := "https://keycloak:8443/realms/" + realmName
	clientAssertion := generatejwt.CreateJWT(tlsKey, "", clientId, aud)
	return clientAssertion
}

func getx509Token(keycloakUrl, clientId, tlsCrt, tlsKey, caCrt string) (*http.Response, error) {
	var resp = &http.Response{}
	var err error

	client := getClient()
	resp, err = client.PostForm(keycloakUrl, url.Values{"username": {""}, "password": {""}, "grant_type": {"password"}, "client_id": {clientId}, "scope": {scope}})

	return resp, err
}

func getClient() *http.Client {
	caCert, _ := ioutil.ReadFile(caCrt)
	caCertPool := x509.NewCertPool()
	caCertPool.AppendCertsFromPEM(caCert)

	cert, _ := tls.LoadX509KeyPair(tlsCrt, tlsKey)

	dialer := &net.Dialer{
		Timeout:   30 * time.Second,
		KeepAlive: 30 * time.Second,
		DualStack: true,
	}

	client := &http.Client{
		Transport: &http.Transport{
			DialContext: func(ctx context.Context, network, addr string) (net.Conn, error) {
				fmt.Println("address original =", addr)
				if addr == keycloakAlias+":"+keycloakPort {
					addr = keycloakHost + ":" + keycloakPort
					fmt.Println("address modified =", addr)
				}
				return dialer.DialContext(ctx, network, addr)
			},
			TLSClientConfig: &tls.Config{
				RootCAs:      caCertPool,
				Certificates: []tls.Certificate{cert},
			},
		},
	}
	return client
}

func getSecretToken(keycloakUrl, clientId string) (*http.Response, error) {
	var resp = &http.Response{}
	var err error

	secretName := clientId + "-secret"
	clientSecret := getSecret(secretName)
	resp, err = http.PostForm(keycloakUrl,
		url.Values{"client_secret": {clientSecret}, "grant_type": {"client_credentials"}, "client_id": {clientId}})

	return resp, err
}

func getSecret(secretName string) string {
	clientset := connectToK8s()
	res, err := clientset.CoreV1().Secrets(namespace).Get(context.TODO(), secretName, metav1.GetOptions{})
	if err != nil {
		fmt.Println(err.Error())
	}
	return string(res.Data["client_secret"])
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
	flag.StringVar(&keycloakHost, "keycloakHost", "istio-ingressgateway.istio-system", "Keycloak Host")
	flag.StringVar(&keycloakPort, "keycloakPort", "80", "Keycloak Port")
	flag.StringVar(&keycloakAlias, "keycloakAlias", "keycloak.est.tech", "Keycloak URL Alias")
	flag.Parse()

	healthHandler := http.HandlerFunc(health)
	http.Handle("/health", healthHandler)
	tokenHandler := http.HandlerFunc(getToken)
	http.Handle("/token", tokenHandler)
	http.ListenAndServe(":8888", nil)

	ioutil.WriteFile("init.txt", []byte("Initialization done."), 0644)
}
