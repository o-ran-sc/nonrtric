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
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"io/ioutil"
	"k8s.io/api/admission/v1beta1"
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/serializer"
	"log"
	"net/http"
)

type ServerParameters struct {
	port     string // webhook server port
	certFile string // path to the x509 cert
	keyFile  string // path to the x509 private key
	secret   string
}

type patchOperation struct {
	Op    string      `json:"op"`
	Path  string      `json:"path"`
	Value interface{} `json:"value,omitempty"`
}

var parameters ServerParameters

var (
	universalDeserializer = serializer.NewCodecFactory(runtime.NewScheme()).UniversalDeserializer()
)

func main() {
	flag.StringVar(&parameters.port, "port", "8443", "Webhook server port.")
	flag.StringVar(&parameters.certFile, "tlsCertFile", "/certs/tls.crt", "File containing the x509 certificate")
	flag.StringVar(&parameters.keyFile, "tlsKeyFile", "/certs/tls.key", "File containing the x509 private key")
	flag.StringVar(&parameters.secret, "secret", "cm-keycloak-client-certs", "Secret containing rapp cert files")
	flag.Parse()

	http.HandleFunc("/inject-sidecar", HandleSideCarInjection)
	log.Fatal(http.ListenAndServeTLS(":"+parameters.port, parameters.certFile, parameters.keyFile, nil))
}

func HandleSideCarInjection(w http.ResponseWriter, r *http.Request) {

	body, err := ioutil.ReadAll(r.Body)
	err = ioutil.WriteFile("/tmp/request", body, 0644)
	if err != nil {
		panic(err.Error())
	}

	var admissionReviewReq v1beta1.AdmissionReview

	if _, _, err := universalDeserializer.Decode(body, nil, &admissionReviewReq); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Errorf("Could not deserialize request: %v", err)
	} else if admissionReviewReq.Request == nil {
		w.WriteHeader(http.StatusBadRequest)
		errors.New("Malformed admission review - request is empty")
	}

	fmt.Printf("Received Admission Review Request - Type: %v \t Event: %v \t Name: %v \n",
		admissionReviewReq.Request.Kind,
		admissionReviewReq.Request.Operation,
		admissionReviewReq.Request.Name,
	)

	var pod v1.Pod

	err = json.Unmarshal(admissionReviewReq.Request.Object.Raw, &pod)

	if err != nil {
		fmt.Errorf("Could not unmarshal pod from admission request: %v", err)
	}

	var patches []patchOperation

	labels := pod.ObjectMeta.Labels
	labels["sidecar-injection-webhook"] = "jwt-proxy"

	patches = append(patches, patchOperation{
		Op:    "add",
		Path:  "/metadata/labels",
		Value: labels,
	})

	var containers []v1.Container
	containers = append(containers, pod.Spec.Containers...)
	container := v1.Container{
		Name:            "jwt-proxy",
		Image:           "ktimoney/rapps-jwt",
		ImagePullPolicy: v1.PullIfNotPresent,
		Ports: []v1.ContainerPort{
			{
				Name:          "http",
				Protocol:      v1.ProtocolTCP,
				ContainerPort: 8888,
			},
		},
		VolumeMounts: []v1.VolumeMount{
			{
				Name:      "certsdir",
				MountPath: "/certs",
				ReadOnly:  true,
			},
		},
	}

	containers = append(containers, container)
	fmt.Println(containers)

	patches = append(patches, patchOperation{
		Op:    "add",
		Path:  "/spec/containers",
		Value: containers,
	})

	var volumes []v1.Volume
	volumes = append(volumes, pod.Spec.Volumes...)
	volume := v1.Volume{
		Name: "certsdir",
		VolumeSource: v1.VolumeSource{
			Secret: &v1.SecretVolumeSource{
				SecretName: parameters.secret,
			},
		},
	}
	volumes = append(volumes, volume)
	fmt.Println(volumes)

	patches = append(patches, patchOperation{
		Op:    "add",
		Path:  "/spec/volumes",
		Value: volumes,
	})
	fmt.Println(patches)

	patchBytes, err := json.Marshal(patches)

	if err != nil {
		fmt.Errorf("Error occurred when trying to marshal JSON patch: %v", err)
	}

	admissionReviewResponse := v1beta1.AdmissionReview{
		Response: &v1beta1.AdmissionResponse{
			UID:     admissionReviewReq.Request.UID,
			Allowed: true,
		},
	}

	admissionReviewResponse.Response.Patch = patchBytes

	bytes, err := json.Marshal(&admissionReviewResponse)
	if err != nil {
		fmt.Errorf("Error occurred when trying to marshal Aadmission Review response: %v", err)
	}

	w.Write(bytes)

}
