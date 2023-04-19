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
	"fmt"
	netv1alpha3 "istio.io/client-go/pkg/apis/networking/v1alpha3"
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
	"text/template"
)

const (
	NAMESPACE = "istio-nonrtric"
)

type TemplateConfig struct {
	Name          string
	Namespace     string
	Realm         string
	Client        string
	Authenticator string
	Role          string
	Method        string
	TlsCrt        string
	TlsKey        string
	CaCrt         string
}

var inputs TemplateConfig
var appName string

var config *template.Template

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

func createGateway(clientset *versioned.Clientset) (string, error) {
	gtClient := clientset.NetworkingV1beta1().Gateways(NAMESPACE)
	config = template.Must(template.ParseFiles("./templates/Gateway-template.txt"))
	var manifest bytes.Buffer
	err := config.Execute(&manifest, inputs)
	if err != nil {
		return "", err
	}

	gt := &netv1beta1.Gateway{}
	dec := k8Yaml.NewYAMLOrJSONDecoder(bytes.NewReader([]byte(manifest.String())), 1000)

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

func createVirtualService(clientset *versioned.Clientset) (string, error) {
	vsClient := clientset.NetworkingV1beta1().VirtualServices(NAMESPACE)
	config = template.Must(template.ParseFiles("./templates/VirtualService-template.txt"))
	var manifest bytes.Buffer
	err := config.Execute(&manifest, inputs)
	if err != nil {
		return "", err
	}

	vs := &netv1beta1.VirtualService{}
	dec := k8Yaml.NewYAMLOrJSONDecoder(bytes.NewReader([]byte(manifest.String())), 1000)

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

func createRequestAuthentication(clientset *versioned.Clientset) (string, error) {
	raClient := clientset.SecurityV1beta1().RequestAuthentications(NAMESPACE)
	config = template.Must(template.ParseFiles("./templates/RequestAuthentication-template.txt"))
	var manifest bytes.Buffer
	err := config.Execute(&manifest, inputs)
	if err != nil {
		return "", err
	}

	ra := &secv1beta1.RequestAuthentication{}
	dec := k8Yaml.NewYAMLOrJSONDecoder(bytes.NewReader([]byte(manifest.String())), 1000)

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

func createAuthorizationPolicy(clientset *versioned.Clientset) (string, error) {
	apClient := clientset.SecurityV1beta1().AuthorizationPolicies(NAMESPACE)
	config = template.Must(template.ParseFiles("./templates/AuthorizationPolicy-template.txt"))
	var manifest bytes.Buffer
	err := config.Execute(&manifest, inputs)
	if err != nil {
		return "", err
	}

	ap := &secv1beta1.AuthorizationPolicy{}
	dec := k8Yaml.NewYAMLOrJSONDecoder(bytes.NewReader([]byte(manifest.String())), 1000)

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

func createEnvoyFilter(clientset *versioned.Clientset) (string, error) {
	efClient := clientset.NetworkingV1alpha3().EnvoyFilters(NAMESPACE)
	config = template.Must(template.ParseFiles("./templates/EnvoyFilter-template.txt"))
	var manifest bytes.Buffer
	err := config.Execute(&manifest, inputs)
	if err != nil {
		return "", err
	}

	ef := &netv1alpha3.EnvoyFilter{}
	dec := k8Yaml.NewYAMLOrJSONDecoder(bytes.NewReader([]byte(manifest.String())), 1000)

	if err = dec.Decode(&ef); err != nil {
		return "", err
	}

	result, err := efClient.Create(context.TODO(), ef, metav1.CreateOptions{})

	if err != nil {
		return "", err
	}

	fmt.Printf("Create Envoy Filter %s \n", result.GetName())
	return result.GetName(), nil
}

func removeGateway(clientset *versioned.Clientset) {
	gtClient := clientset.NetworkingV1beta1().Gateways(NAMESPACE)
	err := gtClient.Delete(context.TODO(), "nonrtric-istio-"+appName+"-gateway", metav1.DeleteOptions{})
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted Gateway nonrtric-istio-" + appName + "-gateway")
	}
}

func removeVirtualService(clientset *versioned.Clientset) {
	vsClient := clientset.NetworkingV1beta1().VirtualServices(NAMESPACE)
	err := vsClient.Delete(context.TODO(), "nonrtric-istio-"+appName+"-vs", metav1.DeleteOptions{})
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted VirtualServices nonrtric-istio-" + appName + "-vs")
	}
}

func removeRequestAuthentication(clientset *versioned.Clientset) {
	raClient := clientset.SecurityV1beta1().RequestAuthentications(NAMESPACE)
	err := raClient.Delete(context.TODO(), "jwt-"+appName, metav1.DeleteOptions{})
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted RequestAuthentication jwt-" + appName)
	}
}

func removeAuthorizationPolicy(clientset *versioned.Clientset) {
	apClient := clientset.SecurityV1beta1().AuthorizationPolicies(NAMESPACE)
	err := apClient.Delete(context.TODO(), appName+"-policy", metav1.DeleteOptions{})
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted AuthorizationPolicy " + appName + "-policy")
	}
}

func removeEnvoyFilter(clientset *versioned.Clientset) {
	efClient := clientset.NetworkingV1alpha3().EnvoyFilters(NAMESPACE)
	err := efClient.Delete(context.TODO(), appName+"-outbound-filter", metav1.DeleteOptions{})
	if err != nil {
		fmt.Println(err)
	} else {
		fmt.Println("Deleted EnvoyFilter " + appName + "-outbound-filter")
	}
}

func createIstioPolicy(res http.ResponseWriter, req *http.Request) {
	query := req.URL.Query()
	realmName := query.Get("realm")
	appName := query.Get("name")
	roleName := query.Get("role")
	methodName := query.Get("method")
	inputs = TemplateConfig{Name: appName, Namespace: NAMESPACE, Realm: realmName, Role: roleName, Method: methodName}
	var msg string
	clientset := connectToK8s()
	_, err := createGateway(clientset)
	if err != nil {
		msg = err.Error()
		fmt.Println(err.Error())
	} else {
		_, err := createVirtualService(clientset)
		if err != nil {
			msg = err.Error()
			fmt.Println(err.Error())
		} else {
			_, err := createRequestAuthentication(clientset)
			if err != nil {
				msg = err.Error()
				fmt.Println(err.Error())
			} else {
				_, err := createAuthorizationPolicy(clientset)
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

func createIstioFilter(res http.ResponseWriter, req *http.Request) {
	query := req.URL.Query()
	realmName := query.Get("realm")
	clientId := query.Get("client")
	appName := query.Get("name")
	authType := query.Get("authType")
	tlsCrt := query.Get("tlsCrt")
	tlsKey := query.Get("tlsKey")
	caCrt := query.Get("caCrt")
	inputs = TemplateConfig{Name: appName, Namespace: NAMESPACE, Realm: realmName, Client: clientId,
		Authenticator: authType, TlsCrt: tlsCrt, TlsKey: tlsKey, CaCrt: caCrt}
	var msg string
	clientset := connectToK8s()
	_, err := createEnvoyFilter(clientset)
	if err != nil {
		msg = err.Error()
		fmt.Println(err.Error())
	}
	// create response binary data
	data := []byte(msg) // slice of bytes
	// write `data` to response
	res.Write(data)
}

func removeIstioPolicy(res http.ResponseWriter, req *http.Request) {
	query := req.URL.Query()
	appName = query.Get("name")
	clientset := connectToK8s()
	removeAuthorizationPolicy(clientset)
	removeRequestAuthentication(clientset)
	removeVirtualService(clientset)
	removeGateway(clientset)
}

func removeIstioFilter(res http.ResponseWriter, req *http.Request) {
	query := req.URL.Query()
	appName = query.Get("name")
	clientset := connectToK8s()
	removeEnvoyFilter(clientset)
}

func main() {
	createIstioPolicyHandler := http.HandlerFunc(createIstioPolicy)
	http.Handle("/create-policy", createIstioPolicyHandler)
	removeIstioPolicyHandler := http.HandlerFunc(removeIstioPolicy)
	http.Handle("/remove-policy", removeIstioPolicyHandler)
	createIstioFilterHandler := http.HandlerFunc(createIstioFilter)
	http.Handle("/create-filter", createIstioFilterHandler)
	removeIstioFilterHandler := http.HandlerFunc(removeIstioFilter)
	http.Handle("/remove-filter", removeIstioFilterHandler)
	http.ListenAndServe(":9000", nil)
}
