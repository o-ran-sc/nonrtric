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
	"time"

	"github.com/gorilla/mux"
	log "github.com/sirupsen/logrus"
	"oransc.org/usecase/oruclosedloop/internal/config"
	"oransc.org/usecase/oruclosedloop/internal/linkfailure"
	"oransc.org/usecase/oruclosedloop/internal/repository"
	"oransc.org/usecase/oruclosedloop/internal/restclient"
)

const timeoutHTTPClient = time.Second * 5
const jobId = "14e7bb84-a44d-44c1-90b7-6995a92ad43c"

var infoCoordAddress string
var linkfailureConfig linkfailure.Configuration
var lookupService repository.LookupService
var host string
var port string
var client restclient.HTTPClient

func init() {
	configuration := config.New()

	client = &http.Client{
		Timeout: timeoutHTTPClient,
	}

	log.SetLevel(configuration.LogLevel)

	if err := validateConfiguration(configuration); err != nil {
		log.Fatalf("Unable to start consumer due to: %v", err)
	}
	host = configuration.ConsumerHost
	port = fmt.Sprint(configuration.ConsumerPort)

	csvFileHelper := repository.NewCsvFileHelperImpl()
	if initErr := initializeLookupService(csvFileHelper, configuration); initErr != nil {
		log.Fatalf("Unable to create LookupService due to inability to get O-RU-ID to O-DU-ID map. Cause: %v", initErr)
	}

	infoCoordAddress = configuration.InfoCoordinatorAddress

	linkfailureConfig = linkfailure.Configuration{
		SDNRAddress:  configuration.SDNRHost + ":" + fmt.Sprint(configuration.SDNRPort),
		SDNRUser:     configuration.SDNRUser,
		SDNRPassword: configuration.SDNPassword,
	}
}

func validateConfiguration(configuration *config.Config) error {
	if configuration.ConsumerHost == "" || configuration.ConsumerPort == 0 {
		return fmt.Errorf("consumer host and port must be provided")
	}
	return nil
}

func initializeLookupService(csvFileHelper repository.CsvFileHelper, configuration *config.Config) error {
	lookupService = repository.NewLookupServiceImpl(csvFileHelper, configuration.ORUToODUMapFile)
	if initErr := lookupService.Init(); initErr != nil {
		return initErr
	}
	return nil
}

func main() {
	defer deleteJob()
	messageHandler := linkfailure.NewLinkFailureHandler(lookupService, linkfailureConfig, client)
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
	putErr := restclient.PutWithoutAuth(infoCoordAddress+"/data-consumer/v1/info-jobs/"+jobId, body, client)
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
	return restclient.Delete(infoCoordAddress+"/data-consumer/v1/info-jobs/"+jobId, client)
}
