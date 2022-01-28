// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2021: Nordix Foundation
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
	"encoding/json"
	"flag"
	"fmt"
	"io"
	http "net/http"
	"time"

	"oransc.org/nonrtric/dmaapmediatorproducer/internal/restclient"
)

var httpClient http.Client

func main() {
	httpClient = http.Client{
		Timeout: time.Second * 5,
	}
	port := flag.Int("port", 40935, "The port this consumer will listen on")
	flag.Parse()
	http.HandleFunc("/jobs", handleData)

	registerJob(*port)

	fmt.Println("Starting consumer on port: ", *port)
	fmt.Println(http.ListenAndServe(fmt.Sprintf(":%v", *port), nil))
}

func registerJob(port int) {
	jobInfo := struct {
		JobOwner      string `json:"job_owner"`
		JobResultUri  string `json:"job_result_uri"`
		InfoTypeId    string `json:"info_type_id"`
		JobDefinition string `json:"job_definition"`
	}{
		JobOwner:      fmt.Sprintf("test%v", port),
		JobResultUri:  fmt.Sprintf("http://localhost:%v/jobs", port),
		InfoTypeId:    "STD_Fault_Messages",
		JobDefinition: "{}",
	}
	fmt.Println("Registering consumer: ", jobInfo)
	body, _ := json.Marshal(jobInfo)
	putErr := restclient.Put(fmt.Sprintf("https://localhost:8083/data-consumer/v1/info-jobs/job%v", port), body, &httpClient)
	if putErr != nil {
		fmt.Println("Unable to register consumer: ", putErr)
	}
}

func handleData(w http.ResponseWriter, req *http.Request) {
	defer req.Body.Close()
	if reqData, err := io.ReadAll(req.Body); err == nil {
		fmt.Println("Consumer received body: ", string(reqData))
	}
}
