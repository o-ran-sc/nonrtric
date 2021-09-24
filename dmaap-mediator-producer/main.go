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
	"fmt"
	"net/http"
	"sync"

	"github.com/gorilla/mux"
	log "github.com/sirupsen/logrus"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/config"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/jobs"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/server"
)

var configuration *config.Config
var callbackAddress string

func init() {
	configuration = config.New()
	if loglevel, err := log.ParseLevel(configuration.LogLevel); err == nil {
		log.SetLevel(loglevel)
	} else {
		log.Warnf("Invalid log level: %v. Log level will be Info!", configuration.LogLevel)
	}

	log.Debug("Initializing DMaaP Mediator Producer")
	if configuration.InfoProducerHost == "" {
		log.Fatal("Missing INFO_PRODUCER_SUPERVISION_CALLBACK_HOST")
	}
	callbackAddress = fmt.Sprintf("%v:%v", configuration.InfoProducerHost, configuration.InfoProducerPort)

	registrator := config.NewRegistratorImpl(configuration.InfoCoordinatorAddress)
	if types, err := jobs.GetTypes(); err == nil {
		if regErr := registrator.RegisterTypes(types); regErr != nil {
			log.Fatalf("Unable to register all types due to: %v", regErr)
		}
	} else {
		log.Fatalf("Unable to get types to register due to: %v", err)
	}
	producer := config.ProducerRegistrationInfo{
		InfoProducerSupervisionCallbackUrl: callbackAddress + server.StatusPath,
		SupportedInfoTypes:                 jobs.GetSupportedTypes(),
		InfoJobCallbackUrl:                 callbackAddress + server.AddJobPath,
	}
	if err := registrator.RegisterProducer("DMaaP_Mediator_Producer", &producer); err != nil {
		log.Fatalf("Unable to register producer due to: %v", err)
	}
}

func main() {
	log.Debug("Starting DMaaP Mediator Producer")
	wg := new(sync.WaitGroup)

	// add two goroutines to `wg` WaitGroup, one for each running go routine
	wg.Add(2)

	log.Debugf("Starting callback server at port %v", configuration.InfoProducerPort)
	go func() {
		r := mux.NewRouter()
		r.HandleFunc(server.StatusPath, server.StatusHandler)
		r.HandleFunc(server.AddJobPath, server.AddInfoJobHandler)
		r.HandleFunc(server.DeleteJobPath, server.DeleteInfoJobHandler)
		log.Warn(http.ListenAndServe(fmt.Sprintf(":%v", configuration.InfoProducerPort), r))
		wg.Done()
	}()

	go func() {
		jobs.RunJobs(fmt.Sprintf("%v:%v", configuration.MRHost, configuration.MRPort))
		wg.Done()
	}()

	// wait until WaitGroup is done
	wg.Wait()
	log.Debug("Stopping DMaaP Mediator Producer")
}
