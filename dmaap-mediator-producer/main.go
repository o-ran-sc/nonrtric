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

	log "github.com/sirupsen/logrus"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/config"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/jobs"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/restclient"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/server"
)

var configuration *config.Config

func init() {
	configuration = config.New()
}

func main() {
	log.SetLevel(configuration.LogLevel)
	log.Debug("Initializing DMaaP Mediator Producer")
	if err := validateConfiguration(configuration); err != nil {
		log.Fatalf("Stopping producer due to error: %v", err)
	}
	callbackAddress := fmt.Sprintf("%v:%v", configuration.InfoProducerHost, configuration.InfoProducerPort)

	var cert *tls.Certificate
	if c, err := restclient.CreateClientCertificate(configuration.ProducerCertPath, configuration.ProducerKeyPath); err == nil {
		cert = c
	} else {
		log.Fatalf("Stopping producer due to error: %v", err)
	}
	retryClient := restclient.CreateRetryClient(cert)

	jobsManager := jobs.NewJobsManagerImpl("configs/type_config.json", retryClient, configuration.DMaaPMRAddress, restclient.CreateClientWithoutRetry(cert, 5*time.Second))
	if err := registerTypesAndProducer(jobsManager, configuration.InfoCoordinatorAddress, callbackAddress, retryClient); err != nil {
		log.Fatalf("Stopping producer due to: %v", err)
	}
	jobsManager.StartJobs()

	log.Debug("Starting DMaaP Mediator Producer")
	go func() {
		log.Debugf("Starting callback server at port %v", configuration.InfoProducerPort)
		r := server.NewRouter(jobsManager)
		if restclient.IsUrlSecure(callbackAddress) {
			log.Fatalf("Server stopped: %v", http.ListenAndServeTLS(fmt.Sprintf(":%v", configuration.InfoProducerPort), configuration.ProducerCertPath, configuration.ProducerKeyPath, r))
		} else {
			log.Fatalf("Server stopped: %v", http.ListenAndServe(fmt.Sprintf(":%v", configuration.InfoProducerPort), r))
		}
	}()

	keepProducerAlive()
}

func validateConfiguration(configuration *config.Config) error {
	if configuration.InfoProducerHost == "" {
		return fmt.Errorf("missing INFO_PRODUCER_HOST")
	}
	if configuration.ProducerCertPath == "" || configuration.ProducerKeyPath == "" {
		return fmt.Errorf("missing PRODUCER_CERT and/or PRODUCER_KEY")
	}
	return nil
}
func registerTypesAndProducer(jobTypesHandler jobs.JobTypesManager, infoCoordinatorAddress string, callbackAddress string, client restclient.HTTPClient) error {
	registrator := config.NewRegistratorImpl(infoCoordinatorAddress, client)
	if types, err := jobTypesHandler.LoadTypesFromConfiguration(); err == nil {
		if regErr := registrator.RegisterTypes(types); regErr != nil {
			return fmt.Errorf("unable to register all types due to: %v", regErr)
		}
	} else {
		return fmt.Errorf("unable to get types to register due to: %v", err)
	}
	producer := config.ProducerRegistrationInfo{
		InfoProducerSupervisionCallbackUrl: callbackAddress + server.StatusPath,
		SupportedInfoTypes:                 jobTypesHandler.GetSupportedTypes(),
		InfoJobCallbackUrl:                 callbackAddress + server.AddJobPath,
	}
	if err := registrator.RegisterProducer("DMaaP_Mediator_Producer", &producer); err != nil {
		return fmt.Errorf("unable to register producer due to: %v", err)
	}
	return nil
}

func keepProducerAlive() {
	forever := make(chan int)
	<-forever
}
