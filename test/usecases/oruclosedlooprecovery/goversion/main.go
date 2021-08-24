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
	"fmt"
	"net/http"

	"github.com/gorilla/mux"
	log "github.com/sirupsen/logrus"
	"oransc.org/usecase/oruclosedloop/internal/config"
	"oransc.org/usecase/oruclosedloop/internal/linkfailure"
	"oransc.org/usecase/oruclosedloop/internal/repository"
	"oransc.org/usecase/oruclosedloop/internal/restclient"
)

var consumerConfig linkfailure.Configuration
var lookupService repository.LookupService
var host string
var port string

const jobId = "14e7bb84-a44d-44c1-90b7-6995a92ad43c"

func init() {
	configuration := config.New()

	log.SetLevel(configuration.LogLevel)

	if configuration.ConsumerHost == "" || configuration.ConsumerPort == 0 {
		log.Fatal("Consumer host and port must be provided!")
	}
	host = configuration.ConsumerHost
	port = fmt.Sprint(configuration.ConsumerPort)

	csvFileHelper := repository.NewCsvFileHelper()
	lookupService = repository.NewLookupServiceImpl(&csvFileHelper, configuration.ORUToODUMapFile)
	if initErr := lookupService.Init(); initErr != nil {
		log.Fatalf("Unable to create LookupService due to inability to get O-RU-ID to O-DU-ID map. Cause: %v", initErr)
	}
	consumerConfig = linkfailure.Configuration{
		InfoCoordAddress: configuration.InfoCoordinatorAddress,
		SDNRAddress:      configuration.SDNRHost + ":" + fmt.Sprint(configuration.SDNRPort),
		SDNRUser:         configuration.SDNRUser,
		SDNRPassword:     configuration.SDNPassword,
	}
}

func main() {
	defer deleteJob()
	messageHandler := linkfailure.NewLinkFailureHandler(lookupService, consumerConfig)
	r := mux.NewRouter()
	r.HandleFunc("/", messageHandler.MessagesHandler).Methods(http.MethodPost)
	r.HandleFunc("/admin/start", startHandler).Methods(http.MethodPost)
	r.HandleFunc("/admin/stop", stopHandler).Methods(http.MethodPost)
	log.Error(http.ListenAndServe(":"+port, r))
}

func startHandler(w http.ResponseWriter, r *http.Request) {
	jobRegistrationInfo := struct {
		InfoTypeId    string      `json:"info_type_id"`
		JobResultUri  string      `json:"job_result_uri"`
		JobOwner      string      `json:"job_owner"`
		JobDefinition interface{} `json:"job_definition"`
	}{
		InfoTypeId:    "STD_Fault_Messages",
		JobResultUri:  host + ":" + port,
		JobOwner:      "O-RU Closed Loop Usecase",
		JobDefinition: "{}",
	}
	body, _ := json.Marshal(jobRegistrationInfo)
	putErr := restclient.PutWithoutAuth(consumerConfig.InfoCoordAddress+"/data-consumer/v1/info-jobs/"+jobId, body)
	if putErr != nil {
		http.Error(w, fmt.Sprintf("Unable to register consumer job: %v", putErr), http.StatusBadRequest)
		return
	}
	log.Debug("Registered job.")
}

func stopHandler(w http.ResponseWriter, r *http.Request) {
	deleteErr := deleteJob()
	if deleteErr != nil {
		http.Error(w, fmt.Sprintf("Unable to delete consumer job: %v", deleteErr), http.StatusBadRequest)
		return
	}
	log.Debug("Deleted job.")
}

func deleteJob() error {
	return restclient.Delete(consumerConfig.InfoCoordAddress + "/data-consumer/v1/info-jobs/" + jobId)
}
