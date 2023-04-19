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
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	kubernetes "k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"net/http"
	"net/url"
	"rapps/utils/pemtojwks"
)

const (
	namespace = "istio-nonrtric"
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

type RealmRepresentation struct {
	Id          string `json:"id,omitempty"`
	Realm       string `json:"realm,omitempty"`
	DisplayName string `json:"displayName,omitempty"`
	Enabled     bool   `json:"enabled"`
}

type Client struct {
	ClientID                           string            `json:"clientId,omitempty"`
	Enabled                            bool              `json:"enabled,omitempty"`
	DirectAccessGrantsEnabled          bool              `json:"directAccessGrantsEnabled,omitempty"`
	BearerOnly                         bool              `json:"bearerOnly,omitempty"`
	PublicClient                       bool              `json:"publicClient,omitempty"`
	ServiceAccountsEnabled             bool              `json:"serviceAccountsEnabled,omitempty"`
	ClientAuthenticatorType            string            `json:"clientAuthenticatorType,omitempty"`
	DefaultClientScopes                []string          `json:"defaultClientScopes,omitempty"`
	Attributes                         map[string]string `json:"attributes,omitempty"`
	AuthenticationFlowBindingOverrides map[string]string `json:"authenticationFlowBindingOverrides,omitempty"`
}

type Role struct {
	Name string `json:"name,omitempty"`
}

type User struct {
	ID       string `json:"id,omitempty"`
	Username string `json:"username,omitempty"`
	Email    string `json:"email,omitempty"`
	Enabled  bool   `json:"enabled"`
}

type ProtocolMapperRepresentation struct {
	Name           string            `json:"name,omitempty"`
	Protocol       string            `json:"protocol,omitempty"`
	ProtocolMapper string            `json:"protocolMapper,omitempty"`
	Config         map[string]string `json:"config,omitempty"`
}

type RoleRepresentation struct {
	ID         string `json:"id,omitempty"`
	Name       string `json:"name,omitempty"`
	Composite  bool   `json:"composite"`
	ClientRole bool   `json:"clientRole"`
}

type AuthenticationFlowRepresentation struct {
	Alias                   string   `json:"alias,omitempty"`
	Description             string   `json:"description,omitempty"`
	ProviderId              string   `json:"providerId,omitempty"`
	TopLevel                bool     `json:"topLevel"`
	BuiltIn                 bool     `json:"builtIn"`
	AthenticationExecutions []string `json:"authenticationExecutions,omitempty"`
}

type Execution struct {
	Provider string `json:"provider,omitempty"`
}

type AuthenticatorConfigRepresentation struct {
	Alias  string            `json:"alias,omitempty"`
	Config map[string]string `json:"config,omitempty"`
}

var keycloakUrl string = "http://keycloak:8080"
var token Jwttoken
var flowAlias string = "x509 direct grant"

func createClient(res http.ResponseWriter, req *http.Request) {
	body, err := ioutil.ReadAll(req.Body)
	if err != nil {
		panic(err.Error())
	}
	keyVal := make(map[string]string)
	json.Unmarshal(body, &keyVal)
	realmName := keyVal["realm"]
	clientName := keyVal["name"]
	role := keyVal["role"]
	authType := keyVal["authType"]
	tlsCrt := keyVal["tlsCrt"]
	email := keyVal["email"]
	subjectDN := keyVal["subjectDN"]
	mappingSource := keyVal["mappingSource"]

	var msg string
	msg, err = create(realmName, clientName, role, authType, tlsCrt, email, subjectDN, mappingSource)
	if err != nil {
		msg = err.Error()
	}
	if authType == "client-secret" {
		createSecret(msg, clientName, realmName, namespace)
	}
	// create response binary data
	data := []byte(msg) // slice of bytes
	// write `data` to response
	res.Write(data)
}

func removeClient(res http.ResponseWriter, req *http.Request) {
	query := req.URL.Query()
	realmName := query.Get("realm")
	clientName := query.Get("name")
	authType := query.Get("authType")

	var msg string = "Removed keycloak " + clientName + " from " + realmName + " realm"
	remove(realmName, clientName)
	if authType == "client-secret" {
		removeSecret(namespace, clientName)
	}
	// create response binary data
	data := []byte(msg) // slice of bytes
	// write `data` to response
	res.Write(data)
}

func main() {
	createHandler := http.HandlerFunc(createClient)
	http.Handle("/create", createHandler)
	removeHandler := http.HandlerFunc(removeClient)
	http.Handle("/remove", removeHandler)
	http.ListenAndServe(":9000", nil)
}

func getAdminToken() {
	var resp = &http.Response{}
	var err error
	username := "admin"
	password := "admin"
	clientId := "admin-cli"
	restUrl := keycloakUrl + "/realms/master/protocol/openid-connect/token"
	resp, err = http.PostForm(restUrl,
		url.Values{"username": {username}, "password": {password}, "grant_type": {"password"}, "client_id": {clientId}})
	if err != nil {
		fmt.Println(err)
		panic("Something wrong with the credentials or url ")
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	json.Unmarshal([]byte(body), &token)
}

func sendRequest(method, url string, data []byte) (int, string) {
	fmt.Printf("Sending %s request to %s\n", method, url)
	req, err := http.NewRequest(method, url, bytes.NewBuffer(data))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+token.Access_token)

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()
	body, _ := ioutil.ReadAll(resp.Body)
	respString := string(body)
	fmt.Println("response Status:", resp.Status)
	return resp.StatusCode, respString
}

func create(realmName, clientName, clientRoleName, authType, tlsCrt, email, subjectDN, mappingSource string) (string, error) {
	getAdminToken()
	var userId string = ""
	var jsonValue []byte = []byte{}
	restUrl := keycloakUrl + "/realms/" + realmName
	statusCode, _ := sendRequest("GET", restUrl, nil)

	if statusCode != 200 {
		realmRepresentation := RealmRepresentation{
			Id:          realmName,
			Realm:       realmName,
			DisplayName: realmName,
			Enabled:     true,
		}
		restUrl := keycloakUrl + "/admin/realms"
		jsonValue, _ := json.Marshal(realmRepresentation)
		statusCode, _ = sendRequest("POST", restUrl, jsonValue)
	}

	var flowId string = ""
	if authType == "client-x509" {
		flowId = getFlowId(realmName)
		if flowId == "" {
			createx509Flow(realmName, mappingSource)
			flowId = getFlowId(realmName)
		}
		newUser := User{
			ID:       realmName + "user",
			Username: realmName + "user",
			Email:    email,
			Enabled:  true,
		}
		restUrl = keycloakUrl + "/admin/realms/" + realmName + "/users"
		jsonValue, _ = json.Marshal(newUser)
		statusCode, _ = sendRequest("POST", restUrl, jsonValue)
		userId = getUserId(realmName, realmName+"user")
	}

	newClient := getClient(authType, clientName, flowId, tlsCrt, subjectDN)
	restUrl = keycloakUrl + "/admin/realms/" + realmName + "/clients"
	jsonValue, _ = json.Marshal(newClient)
	statusCode, _ = sendRequest("POST", restUrl, jsonValue)

	clientId, clientSecret := getClientInfo(realmName, clientName)

	newClientRole := Role{
		Name: clientRoleName,
	}
	restUrl = keycloakUrl + "/admin/realms/" + realmName + "/clients/" + clientId + "/roles"
	jsonValue, _ = json.Marshal(newClientRole)
	statusCode, _ = sendRequest("POST", restUrl, jsonValue)

	restUrl = keycloakUrl + "/admin/realms/" + realmName + "/clients/" + clientId + "/roles/" + clientRoleName
	statusCode, data := sendRequest("GET", restUrl, nil)
	roles := make(map[string]interface{})
	err := json.Unmarshal([]byte(data), &roles)
	if err != nil {
		fmt.Println(err)
	}
	roleId := fmt.Sprintf("%v", roles["id"])

	if authType != "client-x509" {
		restUrl = keycloakUrl + "/admin/realms/" + realmName + "/clients/" + clientId + "/service-account-user"
		statusCode, data = sendRequest("GET", restUrl, nil)
		serviceAccount := make(map[string]interface{})
		err = json.Unmarshal([]byte(data), &serviceAccount)
		if err != nil {
			fmt.Println(err)
		}
		userId = fmt.Sprintf("%v", serviceAccount["id"])
	}

	roleRepresentation := RoleRepresentation{
		ID:         roleId,
		Name:       clientRoleName,
		Composite:  false,
		ClientRole: true,
	}

	restUrl = keycloakUrl + "/admin/realms/" + realmName + "/users/" + userId + "/role-mappings/clients/" + clientId
	jsonValue, _ = json.Marshal([]RoleRepresentation{roleRepresentation})
	statusCode, data = sendRequest("POST", restUrl, jsonValue)

	clientroleMapper := ProtocolMapperRepresentation{
		Name:           "Client Role " + clientName + " Mapper",
		Protocol:       "openid-connect",
		ProtocolMapper: "oidc-usermodel-client-role-mapper",
		Config: map[string]string{
			"access.token.claim":                   "true",
			"aggregate.attrs":                      "",
			"claim.name":                           "clientRole",
			"id.token.claim":                       "true",
			"jsonType.label":                       "String",
			"multivalued":                          "true",
			"usermodel.clientRoleMapping.clientId": clientName,
			"userinfo.token.claim":                 "false",
		},
	}

	restUrl = keycloakUrl + "/admin/realms/" + realmName + "/clients/" + clientId + "/protocol-mappers/models"
	jsonValue, _ = json.Marshal(clientroleMapper)
	statusCode, _ = sendRequest("POST", restUrl, jsonValue)
	return clientSecret, nil
}

func getClient(authType, clientName, flowId, tlsCrt, subjectDN string) Client {
	var newClient Client
	newClient.ClientID = clientName
	newClient.Enabled = true
	newClient.DirectAccessGrantsEnabled = true
	newClient.BearerOnly = false
	newClient.PublicClient = false
	newClient.ServiceAccountsEnabled = true
	newClient.ClientAuthenticatorType = authType
	newClient.DefaultClientScopes = []string{"email"}
	if authType == "client-secret" {
		newClient.Attributes = map[string]string{
			"use.refresh.tokens":                   "true",
			"client_credentials.use_refresh_token": "true"}
	} else if authType == "client-x509" {
		newClient.Attributes = map[string]string{
			"use.refresh.tokens":                   "true",
			"client_credentials.use_refresh_token": "true",
			"x509.subjectdn":                       ".*" + subjectDN + ".*",
			"x509.allow.regex.pattern.comparison":  "true"}
		newClient.AuthenticationFlowBindingOverrides = map[string]string{
			"direct_grant": flowId}
	} else {
		jwksString, publicKey, kid := pemtojwks.CreateJWKS(tlsCrt)
		newClient.Attributes = map[string]string{
			"token.endpoint.auth.signing.alg":      "RS256",
			"jwt.credential.public.key":            publicKey,
			"jwt.credential.kid":                   kid,
			"use.jwks.url":                         "false",
			"jwks.url":                             jwksString,
			"use.refresh.tokens":                   "true",
			"client_credentials.use_refresh_token": "true",
		}
	}
	return newClient
}

func getClientInfo(realmName, clientName string) (string, string) {
	restUrl := keycloakUrl + "/admin/realms/" + realmName + "/clients?clientId=" + clientName
	_, data := sendRequest("GET", restUrl, nil)

	clients := make([]map[string]interface{}, 0)
	err := json.Unmarshal([]byte(data), &clients)
	if err != nil {
		fmt.Println(err)
	}
	clientId := fmt.Sprintf("%v", clients[0]["id"])
	clientSecret := fmt.Sprintf("%v", clients[0]["secret"])
	return clientId, clientSecret
}

func createx509Flow(realmName, mappingSource string) {
	var jsonValue []byte = []byte{}
	authenticationFlowRepresentation := AuthenticationFlowRepresentation{
		Alias:                   flowAlias,
		Description:             "OpenID Connect Resource Owner Grant",
		ProviderId:              "basic-flow",
		TopLevel:                true,
		BuiltIn:                 false,
		AthenticationExecutions: []string{},
	}
	restUrl := keycloakUrl + "/admin/realms/" + realmName + "/authentication/flows"
	jsonValue, _ = json.Marshal(authenticationFlowRepresentation)
	sendRequest("POST", restUrl, jsonValue)

	execution := Execution{
		Provider: "direct-grant-auth-x509-username",
	}
	restUrl = keycloakUrl + "/admin/realms/" + realmName + "/authentication/flows/" + flowAlias + "/executions/execution"
	jsonValue, _ = json.Marshal(execution)
	sendRequest("POST", restUrl, jsonValue)

	restUrl = keycloakUrl + "/admin/realms/" + realmName + "/authentication/flows/" + flowAlias + "/executions"
	_, data := sendRequest("GET", restUrl, nil)
	executionInfo := make([]map[string]interface{}, 0)
	err := json.Unmarshal([]byte(data), &executionInfo)
	if err != nil {
		fmt.Println(err)
	}
	executionId := fmt.Sprintf("%v", executionInfo[0]["id"])

	authenticatorConfigRepresentation := AuthenticatorConfigRepresentation{
		Alias: flowAlias + " config",
		Config: map[string]string{
			"x509-cert-auth.canonical-dn-enabled":           "false",
			"x509-cert-auth.serialnumber-hex-enabled":       "false",
			"x509-cert-auth.ocsp-fail-open":                 "false",
			"x509-cert-auth.regular-expression":             "(.*?)(?:$)",
			"x509-cert-auth.crl-checking-enabled":           "false",
			"x509-cert-auth.certificate-policy-mode":        "All",
			"x509-cert-auth.timestamp-validation-enabled":   "false",
			"x509-cert-auth.confirmation-page-disallowed":   "false",
			"x509-cert-auth.mapper-selection":               "Username or Email",
			"x509-cert-auth.revalidate-certificate-enabled": "false",
			"x509-cert-auth.crldp-checking-enabled":         "false",
			"x509-cert-auth.mapping-source-selection":       mappingSource,
			"x509-cert-auth.ocsp-checking-enabled":          "false",
		},
	}
	restUrl = keycloakUrl + "/admin/realms/" + realmName + "/authentication/executions/" + executionId + "/config"
	jsonValue, _ = json.Marshal(authenticatorConfigRepresentation)
	sendRequest("POST", restUrl, jsonValue)
}

func getFlowId(realmName string) string {
	var flowId string = ""
	restUrl := keycloakUrl + "/admin/realms/" + realmName + "/authentication/flows"
	_, data := sendRequest("GET", restUrl, nil)
	flows := make([]map[string]interface{}, 0)
	err := json.Unmarshal([]byte(data), &flows)
	if err != nil {
		fmt.Println(err)
	}

	for i, _ := range flows {
		id := fmt.Sprintf("%v", flows[i]["id"])
		alias := fmt.Sprintf("%v", flows[i]["alias"])
		if alias == flowAlias {
			flowId = id
		}
	}
	return flowId
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

func createSecret(clientSecret, clientName, realmName, namespace string) {
	secretName := clientName + "-secret"
	clientset := connectToK8s()
	secrets := clientset.CoreV1().Secrets(namespace)
	secret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{
			Name:      secretName,
			Namespace: namespace,
			Labels: map[string]string{
				"app": secretName,
			},
		},
		Type:       "Opaque",
		StringData: map[string]string{"client_secret": clientSecret, "client_id": clientName, "realm": realmName},
	}

	_, err := secrets.Create(context.TODO(), secret, metav1.CreateOptions{})
	if err != nil {
		fmt.Println("Failed to create K8s secret.", err)
	}

	fmt.Println("Created K8s secret successfully")
}

func remove(realmName, clientName string) {
	getAdminToken()
	clientId, _ := getClientInfo(realmName, clientName)

	restUrl := keycloakUrl + "/admin/realms/" + realmName + "/clients/" + clientId
	sendRequest("DELETE", restUrl, nil)

	var userId string = ""
	userName := realmName + "user"
	userId = getUserId(realmName, userName)
	if userId != "" {
		restUrl = keycloakUrl + "/admin/realms/" + realmName + "/users/" + userId
		sendRequest("DELETE", restUrl, nil)
	}

	flowId := getFlowId(realmName)
	if flowId != "" {
		restUrl = keycloakUrl + "/admin/realms/" + realmName + "/authentication/flows/" + flowId
		sendRequest("DELETE", restUrl, nil)
	}
}

func getUserId(realmName, userName string) string {
	var userId string = ""
	restUrl := keycloakUrl + "/admin/realms/" + realmName + "/users?username=demouser"
	_, data := sendRequest("GET", restUrl, nil)
	user := make([]map[string]interface{}, 0)
	err := json.Unmarshal([]byte(data), &user)
	if err != nil {
		fmt.Println(err)
	}
	if len(user) > 0 {
		userId = fmt.Sprintf("%v", user[0]["id"])
	}
	return userId
}

func removeSecret(namespace, clientName string) {
	clientset := connectToK8s()
	secretName := clientName + "-secret"
	secrets := clientset.CoreV1().Secrets(namespace)
	err := secrets.Delete(context.TODO(), secretName, metav1.DeleteOptions{})
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted Secret", secretName)
	}
}
