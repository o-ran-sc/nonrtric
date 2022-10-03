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
	"flag"
	"fmt"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"io/ioutil"
	"net/http"
	"strings"
	"time"
)


var gatewayHost string
var gatewayPort string
var securityEnabled string
var useGateway string
var rapp string
var methods string
var healthy bool = true
var ttime time.Time

const (
	namespace             = "istio-nonrtric"
	scope                 = "email"
	client_assertion_type = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
)

var (
	reqDuration = prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "rapp_http_request_duration_seconds",
		Help:    "Duration of the last request call.",
		Buckets: []float64{0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10},
	}, []string{"app", "func", "handler", "method", "code"})
	reqBytes = prometheus.NewSummaryVec(prometheus.SummaryOpts{
		Name: "rapp_bytes_summary",
		Help: "Summary of bytes transferred over http",
	}, []string{"app", "func", "handler", "method", "code"})
)


func MakeRequest(client *http.Client, prefix string, method string, ch chan string) {
	var service = strings.Split(prefix, "/")[1]
	var gatewayUrl = "http://" + gatewayHost + ":" + gatewayPort
	var jsonValue []byte = []byte{}
	var restUrl string = ""

	if securityEnabled != "true" {
		gatewayUrl = "http://" + service + "." + namespace + ":80"
		prefix = ""
	}

	restUrl = gatewayUrl + prefix
	resp := &http.Response{}

	timer := prometheus.NewTimer(prometheus.ObserverFunc(func(v float64) {
		reqDuration.WithLabelValues("rapp-helloworld-invoker2", "MakeRequest", resp.Request.URL.Path, resp.Request.Method,
			resp.Status).Observe(v)
	}))
	defer timer.ObserveDuration()
	fmt.Printf("Making get request to %s\n", restUrl)
	req, err := http.NewRequest(method, restUrl, bytes.NewBuffer(jsonValue))
	if err != nil {
		fmt.Printf("Got error %s", err.Error())
	}
	req.Header.Set("Content-type", "application/json")

	resp, err = client.Do(req)
	if err != nil {
		fmt.Printf("Got error %s", err.Error())
	}

	defer resp.Body.Close()
	body, _ := ioutil.ReadAll(resp.Body)
	reqBytes.WithLabelValues("rapp-helloworld-invoker2", "MakeRequest", req.URL.Path, req.Method,
		resp.Status).Observe(float64(resp.ContentLength))

	respString := string(body[:])
	if respString == "RBAC: access denied" {
		respString += " for " + service + " " + strings.ToLower(method) + " request"
	}
	fmt.Printf("Received response for %s %s request - %s\n", service, strings.ToLower(method), respString)
	ch <- prefix + "," + method
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
	time.Sleep(4 * time.Second)
	prometheus.Register(reqDuration)
	prometheus.Register(reqBytes)

	flag.StringVar(&gatewayHost, "gatewayHost", "istio-ingressgateway.istio-system", "Gateway Host")
	flag.StringVar(&gatewayPort, "gatewayPort", "80", "Gateway Port")
	flag.StringVar(&useGateway, "useGateway", "Y", "Connect to services through API gateway")
	flag.StringVar(&securityEnabled, "securityEnabled", "true", "Security is required to use this application")
	flag.StringVar(&rapp, "rapp", "rapp-helloworld-provider", "Name of rapp to invoke")
	flag.StringVar(&methods, "methods", "GET", "Methods to access application")
	flag.Parse()

	healthHandler := http.HandlerFunc(health)
	http.Handle("/health", healthHandler)
	http.Handle("/metrics", promhttp.Handler())
	go func() {
		http.ListenAndServe(":9000", nil)
	}()

	ioutil.WriteFile("init.txt", []byte("Initialization done."), 0644)

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
