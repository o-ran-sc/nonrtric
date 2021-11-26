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
	"path/filepath"
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
		ProducerCertPath:       "cert",
		ProducerKeyPath:        "key",
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
		ProducerCertPath:       "security/producer.crt",
		ProducerKeyPath:        "security/producer.key",
	}
	got := New()
	assertions.Equal(&wantConfig, got)
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
		ProducerCertPath:       "security/producer.crt",
		ProducerKeyPath:        "security/producer.key",
	}

	got := New()

	assertions.Equal(&wantConfig, got)
	logString := buf.String()
	assertions.Contains(logString, "Invalid log level: wrong. Log level will be Info!")
}

const typeDefinition = `{"types": [{"id": "type1", "dmaapTopicUrl": "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/type1"}]}`

func TestGetTypesFromConfiguration_fileOkShouldReturnSliceOfTypeDefinitions(t *testing.T) {
	assertions := require.New(t)
	typesDir, err := os.MkdirTemp("", "configs")
	if err != nil {
		t.Errorf("Unable to create temporary directory for types due to: %v", err)
	}
	fname := filepath.Join(typesDir, "type_config.json")
	t.Cleanup(func() {
		os.RemoveAll(typesDir)
	})
	if err = os.WriteFile(fname, []byte(typeDefinition), 0666); err != nil {
		t.Errorf("Unable to create temporary config file for types due to: %v", err)
	}

	types, err := GetJobTypesFromConfiguration(fname)

	wantedType := TypeDefinition{
		Id:            "type1",
		DmaapTopicURL: "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/type1",
	}
	wantedTypes := []TypeDefinition{wantedType}
	assertions.EqualValues(wantedTypes, types)
	assertions.Nil(err)
}
