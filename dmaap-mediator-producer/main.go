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
	"time"

	log "github.com/sirupsen/logrus"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/config"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/jobtypes"
)

var configuration *config.Config

func init() {
	configuration = config.New()
	if loglevel, err := log.ParseLevel(configuration.LogLevel); err == nil {
		log.SetLevel(loglevel)
	} else {
		log.Warnf("Invalid log level: %v. Log level will be Info!", configuration.LogLevel)
	}

	log.Debug("Initializing DMaaP Mediator Producer")
	if configuration.JobResultUri == "" {
		log.Fatal("Missing JOB_RESULT_URI")
	}

	registrator := config.NewRegistratorImpl(configuration.InfoCoordinatorAddress)
	if types, err := jobtypes.GetTypes(); err == nil {
		if regErr := registrator.RegisterTypes(types); regErr != nil {
			log.Fatalf("Unable to register all types due to: %v", regErr)
		}
	} else {
		log.Fatalf("Unable to get types to register due to: %v", err)
	}
}

func main() {
	log.Debug("Starting DMaaP Mediator Producer")
	time.Sleep(1000 * time.Millisecond)
}
