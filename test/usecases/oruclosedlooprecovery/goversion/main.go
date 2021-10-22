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
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gorilla/mux"
	log "github.com/sirupsen/logrus"
	"oransc.org/usecase/oruclosedloop/internal/config"
	"oransc.org/usecase/oruclosedloop/internal/linkfailure"
	"oransc.org/usecase/oruclosedloop/internal/repository"
	"oransc.org/usecase/oruclosedloop/internal/restclient"
)

type Server interface {
	ListenAndServe() error
}

const timeoutHTTPClient = time.Second * 5
const jobId = "14e7bb84-a44d-44c1-90b7-6995a92ad43c"

var jobRegistrationInfo = struct {
	InfoTypeId    string      `json:"info_type_id"`
	JobResultUri  string      `json:"job_result_uri"`
	JobOwner      string      `json:"job_owner"`
	JobDefinition interface{} `json:"job_definition"`
}{
	InfoTypeId:    "STD_Fault_Messages",
	JobResultUri:  "",
	JobOwner:      "O-RU Closed Loop Usecase",
	JobDefinition: "{}",
}

var client restclient.HTTPClient
var configuration *config.Config
var linkfailureConfig linkfailure.Configuration
var lookupService repository.LookupService
var consumerPort string

func init() {
	doInit()
}

func doInit() {
	configuration = config.New()

	log.SetLevel(configuration.LogLevel)

	client = &http.Client{
		Timeout: timeoutHTTPClient,
	}

	consumerPort = fmt.Sprint(configuration.ConsumerPort)
	jobRegistrationInfo.JobResultUri = configuration.ConsumerHost + ":" + consumerPort

	linkfailureConfig = linkfailure.Configuration{
		SDNRAddress:  configuration.SDNRHost + ":" + fmt.Sprint(configuration.SDNRPort),
		SDNRUser:     configuration.SDNRUser,
		SDNRPassword: configuration.SDNPassword,
	}
}

func main() {
	if err := validateConfiguration(configuration); err != nil {
		log.Fatalf("Unable to start consumer due to configuration error: %v", err)
	}

	csvFileHelper := repository.NewCsvFileHelperImpl()
	if initErr := initializeLookupService(csvFileHelper, configuration.ORUToODUMapFile); initErr != nil {
		log.Fatalf("Unable to create LookupService due to inability to get O-RU-ID to O-DU-ID map. Cause: %v", initErr)
	}

	go func() {
		startServer(&http.Server{
			Addr:    ":" + consumerPort,
			Handler: getRouter(),
		})
		os.Exit(1) // If the startServer function exits, it is because there has been a failure in the server, so we exit.
	}()

	go func() {
		deleteOnShutdown(make(chan os.Signal, 1))
		os.Exit(0)
	}()

	keepConsumerAlive()
}

func validateConfiguration(configuration *config.Config) error {
	if configuration.ConsumerHost == "" || configuration.ConsumerPort == 0 {
		return fmt.Errorf("consumer host and port must be provided")
	}
	return nil
}

func initializeLookupService(csvFileHelper repository.CsvFileHelper, csvFile string) error {
	lookupService = repository.NewLookupServiceImpl(csvFileHelper, csvFile)
	return lookupService.Init()
}

func getRouter() *mux.Router {
	messageHandler := linkfailure.NewLinkFailureHandler(lookupService, linkfailureConfig, client)

	r := mux.NewRouter()
	r.HandleFunc("/", messageHandler.MessagesHandler).Methods(http.MethodPost).Name("messageHandler")
	r.HandleFunc("/admin/start", startHandler).Methods(http.MethodPost).Name("start")
	r.HandleFunc("/admin/stop", stopHandler).Methods(http.MethodPost).Name("stop")

	return r
}

func startServer(server Server) {
	if err := server.ListenAndServe(); err != nil {
		log.Errorf("Server stopped unintentionally due to: %v. Deleteing job.", err)
		if deleteErr := deleteJob(); deleteErr != nil {
			log.Error(fmt.Sprintf("Unable to delete consumer job due to: %v. Please remove job %v manually.", deleteErr, jobId))
		}
	}
}

func keepConsumerAlive() {
	forever := make(chan int)
	<-forever
}

func startHandler(w http.ResponseWriter, r *http.Request) {
	body, _ := json.Marshal(jobRegistrationInfo)
	putErr := restclient.PutWithoutAuth(configuration.InfoCoordinatorAddress+"/data-consumer/v1/info-jobs/"+jobId, body, client)
	if putErr != nil {
		http.Error(w, fmt.Sprintf("Unable to register consumer job due to: %v.", putErr), http.StatusBadRequest)
		return
	}
	log.Debug("Registered job.")
}

func stopHandler(w http.ResponseWriter, r *http.Request) {
	deleteErr := deleteJob()
	if deleteErr != nil {
		http.Error(w, fmt.Sprintf("Unable to delete consumer job due to: %v. Please remove job %v manually.", deleteErr, jobId), http.StatusBadRequest)
		return
	}
	log.Debug("Deleted job.")
}

func deleteOnShutdown(s chan os.Signal) {
	signal.Notify(s, os.Interrupt)
	signal.Notify(s, syscall.SIGTERM)
	<-s
	log.Info("Shutting down gracefully.")
	if err := deleteJob(); err != nil {
		log.Error(fmt.Sprintf("Unable to delete job on shutdown due to: %v. Please remove job %v manually.", err, jobId))
	}
}

func deleteJob() error {
	return restclient.Delete(configuration.InfoCoordinatorAddress+"/data-consumer/v1/info-jobs/"+jobId, client)
}
