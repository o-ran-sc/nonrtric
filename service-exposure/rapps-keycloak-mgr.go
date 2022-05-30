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
	"context"
	"fmt"
	"github.com/Nerzal/gocloak/v10"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	kubernetes "k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"net/http"
	"strings"
	"rapps/utils/pemtojwks"
)

const (
	namespace = "istio-nonrtric"
)

func createClient(res http.ResponseWriter, req *http.Request) {
	query := req.URL.Query()
	realmName := query.Get("realm")
	clientName := query.Get("name")
	role := query.Get("role")
	var msg string
	msg, err := create(realmName, clientName, role)
	if err != nil {
		msg = err.Error()
	}
	if realmName != "x509" && realmName != "jwt" {
		createSecret(msg, clientName, realmName, role, namespace)
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
	role := query.Get("role")

	var msg string = "Removed keycloak " + clientName + " from " + realmName + " realm"
	remove(realmName, clientName)
        if realmName != "x509" && realmName != "jwt" {
	        removeSecret(namespace, role)
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

func create(realmName, clientName, clientRoleName string) (string, error) {
	client := gocloak.NewClient("http://keycloak.default:8080")
	ctx := context.Background()
	token, err := client.LoginAdmin(ctx, "admin", "admin", "master")
	if err != nil {
		return "", err
	}

	_, err = client.GetRealm(ctx, token.AccessToken, realmName)
	if err != nil {
		realmRepresentation := gocloak.RealmRepresentation{
			ID:          gocloak.StringP(realmName),
			Realm:       gocloak.StringP(realmName),
			DisplayName: gocloak.StringP(realmName),
			Enabled:     gocloak.BoolP(true),
		}

		realm, err := client.CreateRealm(ctx, token.AccessToken, realmRepresentation)
		if err != nil {
			return "", err
		} else {
			fmt.Println("Created realm", realm)
		}
	} else {
		fmt.Println("Realm already exists", realmName)
	}

	flowAlias := "x509 direct grant"
	flowId := ""
	flows, err := client.GetAuthenticationFlows(ctx, token.AccessToken, realmName)
	if err != nil {
		fmt.Println("Oh no!, failed to get flows :(")
	} else {
		for _, flow := range flows {
			if flow.Alias != nil && *flow.Alias == flowAlias {
				flowId = *flow.ID
			}
		}
		fmt.Println("Retrieved AuthenticationFlow id", flowId)
	}

	newClient1 := gocloak.Client{
		ClientID:                  gocloak.StringP(clientName),
		Enabled:                   gocloak.BoolP(true),
		DirectAccessGrantsEnabled: gocloak.BoolP(true),
		BearerOnly:                gocloak.BoolP(false),
		PublicClient:              gocloak.BoolP(false),
		ServiceAccountsEnabled:    gocloak.BoolP(true),
		ClientAuthenticatorType:   gocloak.StringP("client-secret"),
		DefaultClientScopes:       &[]string{"email"},
		Attributes: &map[string]string{"use.refresh.tokens": "true",
			"client_credentials.use_refresh_token": "true"},
	}

	newClient2 := gocloak.Client{
		ClientID:                  gocloak.StringP(clientName),
		Enabled:                   gocloak.BoolP(true),
		DirectAccessGrantsEnabled: gocloak.BoolP(true),
		BearerOnly:                gocloak.BoolP(false),
		PublicClient:              gocloak.BoolP(false),
		ServiceAccountsEnabled:    gocloak.BoolP(true),
		ClientAuthenticatorType:   gocloak.StringP("client-x509"),
		DefaultClientScopes:       &[]string{"openid", "profile", "email"},
		Attributes: &map[string]string{"use.refresh.tokens": "true",
			"client_credentials.use_refresh_token": "true",
			"x509.subjectdn":                       ".*client@mail.com.*",
			"x509.allow.regex.pattern.comparison":  "true"},
		AuthenticationFlowBindingOverrides: &map[string]string{"direct_grant": flowId},
	}

        jwksString := pemtojwks.CreateJWKS("/certs/client_pub.key", "public", "/certs/client.crt") 
	newClient3 := gocloak.Client{
                ClientID:                  gocloak.StringP(clientName),
                Enabled:                   gocloak.BoolP(true),
                DirectAccessGrantsEnabled: gocloak.BoolP(true),
                BearerOnly:                gocloak.BoolP(false),
                PublicClient:              gocloak.BoolP(false),
                ServiceAccountsEnabled:    gocloak.BoolP(true),
                ClientAuthenticatorType:   gocloak.StringP("client-jwt"),
                DefaultClientScopes:       &[]string{"email"},
                Attributes: &map[string]string{"token.endpoint.auth.signing.alg": "RS256",
		       "use.jwks.string": "true",
                       "jwks.string": jwksString, 
		       "use.refresh.tokens": "true",
                       "client_credentials.use_refresh_token": "true",
		},
        }

	var newClient gocloak.Client
	if strings.HasPrefix(clientName, "x509") {
		newClient = newClient2
	} else if strings.HasPrefix(clientName, "jwt") {
		newClient = newClient3
	} else {
                newClient = newClient1
        }

	clientId, err := client.CreateClient(ctx, token.AccessToken, realmName, newClient)
	if err != nil {
		fmt.Println("Failed to create client", err)
		return "", err
	} else {
		fmt.Println("Created realm client", clientId)
	}

	newClientRole := gocloak.Role{
		Name: gocloak.StringP(clientRoleName),
	}
	clientRoleName, err = client.CreateClientRole(ctx, token.AccessToken, realmName, clientId, newClientRole)
	if err != nil {
		return "", err
	} else {
		fmt.Println("Created client role", clientRoleName)
	}

	user, err := client.GetClientServiceAccount(ctx, token.AccessToken, realmName, clientId)
	if err != nil {
		fmt.Println(err)
		panic("Oh no!, failed to get client user :(")
	} else {
		fmt.Println("Service Account user", *user.Username)
	}

	if strings.HasPrefix(clientName, "x509") {
		newUser := gocloak.User{
			ID:       gocloak.StringP(realmName + "user"),
			Username: gocloak.StringP(realmName + "user"),
			Email:    gocloak.StringP("client@mail.com"),
			Enabled:  gocloak.BoolP(true),
		}

		realmUser, err := client.CreateUser(ctx, token.AccessToken, realmName, newUser)
		if err != nil {
			fmt.Println(err)
			panic("Oh no!, failed to create user :(")
		} else {
			fmt.Println("Created new user", realmUser)
		}
	}

	clientRole, err := client.GetClientRole(ctx, token.AccessToken, realmName, clientId, clientRoleName)
	if err != nil {
		fmt.Println(err)
		panic("Oh no!, failed to get client role :(")
	} else {
		fmt.Println("Retrieved client role", clientRoleName)
	}

	clientRoles := []gocloak.Role{*clientRole}
	err = client.AddClientRoleToUser(ctx, token.AccessToken, realmName, clientId, *user.ID, clientRoles)
	if err != nil {
		fmt.Println(err)
		panic("Oh no!, failed to add client role to user :(")
	} else {
		fmt.Printf("Added %s to %s\n", *clientRole.Name, *user.Username)
	}

	clientroleMapper := gocloak.ProtocolMapperRepresentation{
		ID:             gocloak.StringP("Client Role " + clientName + " Mapper"),
		Name:           gocloak.StringP("Client Role " + clientName + " Mapper"),
		Protocol:       gocloak.StringP("openid-connect"),
		ProtocolMapper: gocloak.StringP("oidc-usermodel-client-role-mapper"),
		Config: &map[string]string{
			"access.token.claim":                   "true",
			"aggregate.attrs":                      "",
			"claim.name":                           "clientRole",
			"id.token.claim":                       "true",
			"jsonType.label":                       "String",
			"multivalued":                          "true",
			"userinfo.token.claim":                 "true",
			"usermodel.clientRoleMapping.clientId": clientName,
		},
	}
	_, err = client.CreateClientProtocolMapper(ctx, token.AccessToken, realmName, clientId, clientroleMapper)
	if err != nil {
		fmt.Println(err)
		panic("Oh no!, failed to add client roleampper to client :(")
	} else {
		fmt.Println("Client rolemapper added to client")
	}

	if strings.HasPrefix(clientName, "x509") {
		clientRole := *newClient.ClientID + "." + clientRoleName

		clientroleMapper := gocloak.ProtocolMapperRepresentation{
			ID:             gocloak.StringP("Hardcoded " + clientName + " Mapper"),
			Name:           gocloak.StringP("Hardcoded " + clientName + " Mapper"),
			Protocol:       gocloak.StringP("openid-connect"),
			ProtocolMapper: gocloak.StringP("oidc-hardcoded-role-mapper"),
			Config: &map[string]string{
				"role": clientRole,
			},
		}
		_, err = client.CreateClientProtocolMapper(ctx, token.AccessToken, realmName, clientId, clientroleMapper)
		if err != nil {
			return "", err
		} else {
			fmt.Println("Created hardcoded-role-mapper for ", clientRole)
		}
	}

	_, err = client.RegenerateClientSecret(ctx, token.AccessToken, realmName, clientId)
	if err != nil {
		return "", err
	}

	cred, err := client.GetClientSecret(ctx, token.AccessToken, realmName, clientId)
	if err != nil {
		return "", err
	} else {
		fmt.Println("Generated client secret", *cred.Value)
	}

	return *cred.Value, nil
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

func createSecret(clientSecret, clientName, realmName, role, namespace string) {
	secretName := role + "-secret"
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
	adminClient := gocloak.NewClient("http://192.168.49.2:31560")
	ctx := context.Background()
	token, err := adminClient.LoginAdmin(ctx, "admin", "admin", "master")
	if err != nil {
		fmt.Println(err)
	}

	clients, err := adminClient.GetClients(ctx, token.AccessToken, realmName,
		gocloak.GetClientsParams{
			ClientID: gocloak.StringP(clientName),
		},
	)
	if err != nil {
		panic("List clients failed:" + err.Error())
	}
	for _, client := range clients {
		err = adminClient.DeleteClient(ctx, token.AccessToken, realmName, *client.ID)
		if err != nil {
			fmt.Println(err)
		} else {
			fmt.Println("Deleted client ", clientName)
		}
	}

	userName := realmName + "user"
	users, err := adminClient.GetUsers(ctx, token.AccessToken, realmName,
		gocloak.GetUsersParams{
			Username: gocloak.StringP(userName),
		})
	if err != nil {
		panic("List users failed:" + err.Error())
	}
	for _, user := range users {
		err = adminClient.DeleteUser(ctx, token.AccessToken, realmName, *user.ID)
		if err != nil {
			fmt.Println(err)
		} else {
			fmt.Println("Deleted user ", userName)
		}
	}

}

func removeSecret(namespace, role string) {
	clientset := connectToK8s()
	secretName := role + "-secret"
	secrets := clientset.CoreV1().Secrets(namespace)
	err := secrets.Delete(context.TODO(), secretName, metav1.DeleteOptions{})
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted Secret", secretName)
	}
}
