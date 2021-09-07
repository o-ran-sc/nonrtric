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
	os.Setenv("LOG_LEVEL", "Debug")
	os.Setenv("INFO_PRODUCER_SUPERVISION_CALLBACK_HOST", "supervisionCallbackHost")
	os.Setenv("INFO_PRODUCER_SUPERVISION_CALLBACK_PORT", "8095")
	os.Setenv("INFO_JOB_CALLBACK_HOST", "jobCallbackHost")
	os.Setenv("INFO_JOB_CALLBACK_PORT", "8096")
	os.Setenv("INFO_COORD_ADDR", "infoCoordAddr")
	t.Cleanup(func() {
		os.Clearenv()
	})
	wantConfig := Config{
		LogLevel:                            "Debug",
		InfoProducerSupervisionCallbackHost: "supervisionCallbackHost",
		InfoProducerSupervisionCallbackPort: 8095,
		InfoJobCallbackHost:                 "jobCallbackHost",
		InfoJobCallbackPort:                 8096,
		InfoCoordinatorAddress:              "infoCoordAddr",
	}
	if got := New(); !reflect.DeepEqual(got, &wantConfig) {
		t.Errorf("New() = %v, want %v", got, &wantConfig)
	}
}

func TestNew_faultyIntValueSetConfigContainDefaultValueAndWarnInLog(t *testing.T) {
	assertions := require.New(t)
	var buf bytes.Buffer
	log.SetOutput(&buf)

	os.Setenv("INFO_PRODUCER_SUPERVISION_CALLBACK_PORT", "wrong")
	t.Cleanup(func() {
		log.SetOutput(os.Stderr)
		os.Clearenv()
	})
	wantConfig := Config{
		LogLevel:                            "Info",
		InfoProducerSupervisionCallbackHost: "",
		InfoProducerSupervisionCallbackPort: 8085,
		InfoJobCallbackHost:                 "",
		InfoJobCallbackPort:                 8086,
		InfoCoordinatorAddress:              "http://enrichmentservice:8083",
	}
	if got := New(); !reflect.DeepEqual(got, &wantConfig) {
		t.Errorf("New() = %v, want %v", got, &wantConfig)
	}
	logString := buf.String()
	assertions.Contains(logString, "Invalid int value: wrong for variable: INFO_PRODUCER_SUPERVISION_CALLBACK_PORT. Default value: 8085 will be used")
}

func TestNew_envVarsNotSetConfigContainDefaultValues(t *testing.T) {
	wantConfig := Config{
		LogLevel:                            "Info",
		InfoProducerSupervisionCallbackHost: "",
		InfoProducerSupervisionCallbackPort: 8085,
		InfoJobCallbackHost:                 "",
		InfoJobCallbackPort:                 8086,
		InfoCoordinatorAddress:              "http://enrichmentservice:8083",
	}
	if got := New(); !reflect.DeepEqual(got, &wantConfig) {
		t.Errorf("New() = %v, want %v", got, &wantConfig)
	}
}
