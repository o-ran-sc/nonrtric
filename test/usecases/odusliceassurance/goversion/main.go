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

	log "github.com/sirupsen/logrus"
	"oransc.org/usecase/oduclosedloop/internal/config"
	"oransc.org/usecase/oduclosedloop/internal/sliceassurance"
)

const TOPIC string = "unauthenticated.VES_O_RAN_SC_HELLO_WORLD_PM_STREAMING_OUTPUT"

var configuration *config.Config

func main() {
	configuration = config.New()

	log.SetLevel(configuration.LogLevel)
	log.SetFormatter(&log.JSONFormatter{})

	log.Debug("Using configuration: ", configuration)

	dmaapUrl := configuration.MRHost + ":" + configuration.MRPort

	if err := validateConfiguration(configuration); err != nil {
		log.Fatalf("Unable to start consumer due to configuration error: %v", err)
	}

	a := sliceassurance.App{}
	a.Initialize(dmaapUrl, configuration.SDNRAddress)
	go a.Run(TOPIC, configuration.Polltime)

	http.HandleFunc("/status", statusHandler)

	log.Fatal(http.ListenAndServe(":40936", nil))
}

func validateConfiguration(configuration *config.Config) error {
	if configuration.MRHost == "" || configuration.MRPort == "" {
		return fmt.Errorf("message router host and port must be provided")
	}
	return nil
}

func statusHandler(w http.ResponseWriter, r *http.Request) {
	// Just respond OK to show the service is alive for now. Might be extended later.
}
