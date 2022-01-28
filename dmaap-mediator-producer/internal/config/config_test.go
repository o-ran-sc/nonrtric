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
	"encoding/json"
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
	os.Setenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9093")
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
		KafkaBootstrapServers:  "localhost:9093",
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
		InfoCoordinatorAddress: "https://informationservice:8434",
		DMaaPMRAddress:         "https://message-router.onap:3905",
		KafkaBootstrapServers:  "localhost:9092",
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
		InfoCoordinatorAddress: "https://informationservice:8434",
		DMaaPMRAddress:         "https://message-router.onap:3905",
		KafkaBootstrapServers:  "localhost:9092",
		ProducerCertPath:       "security/producer.crt",
		ProducerKeyPath:        "security/producer.key",
	}

	got := New()

	assertions.Equal(&wantConfig, got)
	logString := buf.String()
	assertions.Contains(logString, "Invalid log level: wrong. Log level will be Info!")
}

func TestGetJobTypesFromConfiguration_fileOkShouldReturnSliceOfTypeDefinitions(t *testing.T) {
	assertions := require.New(t)
	typesDir := CreateTypeConfigFiles(t)
	t.Cleanup(func() {
		os.RemoveAll(typesDir)
	})

	var typeSchemaObj interface{}
	json.Unmarshal([]byte(typeSchemaFileContent), &typeSchemaObj)

	types, err := GetJobTypesFromConfiguration(typesDir)

	wantedDMaaPType := TypeDefinition{
		Identity:      "type1",
		DMaaPTopicURL: "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/type1",
		TypeSchema:    typeSchemaObj,
	}
	wantedKafkaType := TypeDefinition{
		Identity:        "type2",
		KafkaInputTopic: "TestTopic",
		TypeSchema:      typeSchemaObj,
	}
	wantedTypes := []TypeDefinition{wantedDMaaPType, wantedKafkaType}
	assertions.EqualValues(wantedTypes, types)
	assertions.Nil(err)
}

const typeDefinition = `{"types": [{"id": "type1", "dmaapTopicUrl": "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/type1"}, {"id": "type2", "kafkaInputTopic": "TestTopic"}]}`
const typeSchemaFileContent = `{
	"$schema": "http://json-schema.org/draft-04/schema#",
	"type": "object",
	"properties": {
	  "filter": {
		 "type": "string"
	   }
	},
	"additionalProperties": false
  }`

func CreateTypeConfigFiles(t *testing.T) string {
	typesDir, err := os.MkdirTemp("", "configs")
	if err != nil {
		t.Errorf("Unable to create temporary directory for types due to: %v", err)
	}
	fname := filepath.Join(typesDir, "type_config.json")
	if err = os.WriteFile(fname, []byte(typeDefinition), 0666); err != nil {
		t.Errorf("Unable to create temporary config file for types due to: %v", err)
	}
	fname = filepath.Join(typesDir, "typeSchemaDmaap.json")
	if err = os.WriteFile(fname, []byte(typeSchemaFileContent), 0666); err != nil {
		t.Errorf("Unable to create temporary schema file for DMaaP type due to: %v", err)
	}
	fname = filepath.Join(typesDir, "typeSchemaKafka.json")
	if err = os.WriteFile(fname, []byte(typeSchemaFileContent), 0666); err != nil {
		t.Errorf("Unable to create temporary schema file for Kafka type due to: %v", err)
	}
	return typesDir
}
