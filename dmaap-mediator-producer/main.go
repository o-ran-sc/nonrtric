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
	"crypto/tls"
	"fmt"
	"net/http"
	"time"

	"github.com/gorilla/mux"
	log "github.com/sirupsen/logrus"
	_ "oransc.org/nonrtric/dmaapmediatorproducer/docs"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/config"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/jobs"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/kafkaclient"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/restclient"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/server"

	httpSwagger "github.com/swaggo/http-swagger"
)

var configuration *config.Config
var registered bool

func init() {
	configuration = config.New()
}

// @title DMaaP Mediator Producer
// @version 1.1.0

// @license.name  Apache 2.0
// @license.url   http://www.apache.org/licenses/LICENSE-2.0.html

func main() {
	log.SetLevel(configuration.LogLevel)
	log.Debug("Initializing DMaaP Mediator Producer")
	log.Debug("Using configuration: ", configuration)
	if err := validateConfiguration(configuration); err != nil {
		log.Fatalf("Stopping producer due to error: %v", err)
	}
	callbackAddress := fmt.Sprintf("%v:%v", configuration.InfoProducerHost, configuration.InfoProducerPort)

	var cert tls.Certificate
	if c, err := restclient.CreateClientCertificate(configuration.ProducerCertPath, configuration.ProducerKeyPath); err == nil {
		cert = c
	} else {
		log.Fatalf("Stopping producer due to error: %v", err)
	}

	retryClient := restclient.CreateRetryClient(cert)
	kafkaFactory := kafkaclient.KafkaFactoryImpl{BootstrapServer: configuration.KafkaBootstrapServers}
	distributionClient := restclient.CreateClientWithoutRetry(cert, 10*time.Second)

	jobsManager := jobs.NewJobsManagerImpl(retryClient, configuration.DMaaPMRAddress, kafkaFactory, distributionClient)
	go startCallbackServer(jobsManager, callbackAddress)

	if err := registerTypesAndProducer(jobsManager, configuration.InfoCoordinatorAddress, callbackAddress, retryClient); err != nil {
		log.Fatalf("Stopping producer due to: %v", err)
	}
	registered = true
	jobsManager.StartJobsForAllTypes()

	log.Debug("Starting DMaaP Mediator Producer")

	keepProducerAlive()
}

func validateConfiguration(configuration *config.Config) error {
	if configuration.InfoProducerHost == "" {
		return fmt.Errorf("missing INFO_PRODUCER_HOST")
	}
	if configuration.ProducerCertPath == "" || configuration.ProducerKeyPath == "" {
		return fmt.Errorf("missing PRODUCER_CERT and/or PRODUCER_KEY")
	}
	if configuration.DMaaPMRAddress == "" && configuration.KafkaBootstrapServers == "" {
		return fmt.Errorf("at least one of DMAAP_MR_ADDR or KAFKA_BOOTSRAP_SERVERS must be provided")
	}
	return nil
}
func registerTypesAndProducer(jobTypesHandler jobs.JobTypesManager, infoCoordinatorAddress string, callbackAddress string, client restclient.HTTPClient) error {
	registrator := config.NewRegistratorImpl(infoCoordinatorAddress, client)
	configTypes, err := config.GetJobTypesFromConfiguration("configs")
	if err != nil {
		return fmt.Errorf("unable to register all types due to: %v", err)
	}
	regErr := registrator.RegisterTypes(jobTypesHandler.LoadTypesFromConfiguration(configTypes))
	if regErr != nil {
		return fmt.Errorf("unable to register all types due to: %v", regErr)
	}

	producer := config.ProducerRegistrationInfo{
		InfoProducerSupervisionCallbackUrl: callbackAddress + server.HealthCheckPath,
		SupportedInfoTypes:                 jobTypesHandler.GetSupportedTypes(),
		InfoJobCallbackUrl:                 callbackAddress + server.AddJobPath,
	}
	if err := registrator.RegisterProducer("DMaaP_Mediator_Producer", &producer); err != nil {
		return fmt.Errorf("unable to register producer due to: %v", err)
	}
	return nil
}

func startCallbackServer(jobsManager jobs.JobsManager, callbackAddress string) {
	log.Debugf("Starting callback server at port %v", configuration.InfoProducerPort)
	r := server.NewRouter(jobsManager, statusHandler)
	addSwaggerHandler(r)
	if restclient.IsUrlSecure(callbackAddress) {
		log.Fatalf("Server stopped: %v", http.ListenAndServeTLS(fmt.Sprintf(":%v", configuration.InfoProducerPort), configuration.ProducerCertPath, configuration.ProducerKeyPath, r))
	} else {
		log.Fatalf("Server stopped: %v", http.ListenAndServe(fmt.Sprintf(":%v", configuration.InfoProducerPort), r))
	}
}

// @Summary Get status
// @Description Get the status of the producer. Will show if the producer has registered in ICS.
// @Tags Data producer (callbacks)
// @Success 200
// @Router /health_check [get]
func statusHandler(w http.ResponseWriter, r *http.Request) {
	registeredStatus := "not registered"
	if registered {
		registeredStatus = "registered"
	}
	fmt.Fprintf(w, `{"status": "%v"}`, registeredStatus)
}

// @Summary Get Swagger Documentation
// @Description Get the Swagger API documentation for the producer.
// @Tags Admin
// @Success 200
// @Router /swagger [get]
func addSwaggerHandler(r *mux.Router) {
	r.PathPrefix("/swagger").Handler(httpSwagger.WrapHandler)
}

func keepProducerAlive() {
	forever := make(chan int)
	<-forever
}
