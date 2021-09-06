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
	"sync"

	log "github.com/sirupsen/logrus"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/config"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/jobs"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/server"
)

var configuration *config.Config
var supervisionCallbackAddress string
var jobInfoCallbackAddress string

func init() {
	configuration = config.New()
	if loglevel, err := log.ParseLevel(configuration.LogLevel); err == nil {
		log.SetLevel(loglevel)
	} else {
		log.Warnf("Invalid log level: %v. Log level will be Info!", configuration.LogLevel)
	}

	log.Debug("Initializing DMaaP Mediator Producer")
	if configuration.InfoProducerSupervisionCallbackHost == "" {
		log.Fatal("Missing INFO_PRODUCER_SUPERVISION_CALLBACK_HOST")
	}
	supervisionCallbackAddress = fmt.Sprintf("%v:%v", configuration.InfoProducerSupervisionCallbackHost, configuration.InfoProducerSupervisionCallbackPort)

	if configuration.InfoJobCallbackHost == "" {
		log.Fatal("Missing INFO_JOB_CALLBACK_HOST")
	}
	jobInfoCallbackAddress = fmt.Sprintf("%v:%v", configuration.InfoJobCallbackHost, configuration.InfoJobCallbackPort)

	registrator := config.NewRegistratorImpl(configuration.InfoCoordinatorAddress)
	if types, err := jobtypes.GetTypes(); err == nil {
		if regErr := registrator.RegisterTypes(types); regErr != nil {
			log.Fatalf("Unable to register all types due to: %v", regErr)
		}
	} else {
		log.Fatalf("Unable to get types to register due to: %v", err)
	}
	producer := config.ProducerRegistrationInfo{
		InfoProducerSupervisionCallbackUrl: supervisionCallbackAddress,
		SupportedInfoTypes:                 jobs.GetSupportedTypes(),
		InfoJobCallbackUrl:                 jobInfoCallbackAddress,
	}
	if err := registrator.RegisterProducer("DMaaP_Mediator_Producer", &producer); err != nil {
		log.Fatalf("Unable to register producer due to: %v", err)
	}
}

func main() {
	log.Debug("Starting DMaaP Mediator Producer")
	wg := new(sync.WaitGroup)

	// add two goroutines to `wg` WaitGroup, one for each avilable server
	wg.Add(2)

	log.Debugf("Starting status callback server at port %v", configuration.InfoProducerSupervisionCallbackPort)
	go func() {
		server := server.CreateServer(configuration.InfoProducerSupervisionCallbackPort, server.StatusHandler)
		log.Warn(server.ListenAndServe())
		wg.Done()
	}()

	go func() {
		server := server.CreateServer(configuration.InfoJobCallbackPort, server.CreateInfoJobHandler)
		log.Warn(server.ListenAndServe())
		wg.Done()
	}()

	// wait until WaitGroup is done
	wg.Wait()
	log.Debug("Stopping DMaaP Mediator Producer")
}
