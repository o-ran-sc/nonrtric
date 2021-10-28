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

package config

import (
	"bytes"
	"os"
	"reflect"
	"testing"

	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/require"
)

func TestNew_envVarsSetConfigContainSetValues(t *testing.T) {
	assertions := require.New(t)
	os.Setenv("LOG_LEVEL", "Debug")
	os.Setenv("INFO_PRODUCER_HOST", "producerHost")
	os.Setenv("INFO_PRODUCER_PORT", "8095")
	os.Setenv("INFO_COORD_ADDR", "infoCoordAddr")
	os.Setenv("DMAAP_MR_ADDR", "mrHost:3908")
	os.Setenv("PRODUCER_CERT_PATH", "cert")
	os.Setenv("PRODUCER_KEY_PATH", "key")
	t.Cleanup(func() {
		os.Clearenv()
	})
	wantConfig := Config{
		LogLevel:               log.DebugLevel,
		InfoProducerHost:       "producerHost",
		InfoProducerPort:       8095,
		InfoCoordinatorAddress: "infoCoordAddr",
		DMaaPMRAddress:         "mrHost:3908",
		ProducerCert:           "cert",
		ProducerKey:            "key",
	}
	got := New()

	assertions.Equal(&wantConfig, got)
}

func TestNew_faultyIntValueSetConfigContainDefaultValueAndWarnInLog(t *testing.T) {
	assertions := require.New(t)
	var buf bytes.Buffer
	log.SetOutput(&buf)

	os.Setenv("INFO_PRODUCER_PORT", "wrong")
	t.Cleanup(func() {
		log.SetOutput(os.Stderr)
		os.Clearenv()
	})
	wantConfig := Config{
		LogLevel:               log.InfoLevel,
		InfoProducerHost:       "",
		InfoProducerPort:       8085,
		InfoCoordinatorAddress: "https://enrichmentservice:8434",
		DMaaPMRAddress:         "https://message-router.onap:3905",
		ProducerCert:           "configs/producer.crt",
		ProducerKey:            "configs/producer.key",
	}
	if got := New(); !reflect.DeepEqual(got, &wantConfig) {
		t.Errorf("New() = %v, want %v", got, &wantConfig)
	}
	logString := buf.String()
	assertions.Contains(logString, "Invalid int value: wrong for variable: INFO_PRODUCER_PORT. Default value: 8085 will be used")
}

func TestNew_envFaultyLogLevelConfigContainDefaultValues(t *testing.T) {
	assertions := require.New(t)
	var buf bytes.Buffer
	log.SetOutput(&buf)

	os.Setenv("LOG_LEVEL", "wrong")
	t.Cleanup(func() {
		log.SetOutput(os.Stderr)
		os.Clearenv()
	})

	wantConfig := Config{
		LogLevel:               log.InfoLevel,
		InfoProducerHost:       "",
		InfoProducerPort:       8085,
		InfoCoordinatorAddress: "https://enrichmentservice:8434",
		DMaaPMRAddress:         "https://message-router.onap:3905",
		ProducerCert:           "configs/producer.crt",
		ProducerKey:            "configs/producer.key",
	}

	got := New()

	assertions.Equal(&wantConfig, got)
	logString := buf.String()
	assertions.Contains(logString, "Invalid log level: wrong. Log level will be Info!")
}
