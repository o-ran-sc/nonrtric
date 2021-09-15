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
	"os"
	"reflect"
	"testing"
)

func TestNew_envVarsSetConfigContainSetValues(t *testing.T) {
	os.Setenv("LOG_LEVEL", "Debug")
	os.Setenv("INFO_PRODUCER_SUPERVISION_CALLBACK_HOST", "supervisionCallbackHost")
	os.Setenv("INFO_PRODUCER_SUPERVISION_CALLBACK_PORT", "supervisionCallbackPort")
	os.Setenv("INFO_JOB_CALLBACK_HOST", "jobCallbackHost")
	os.Setenv("INFO_JOB_CALLBACK_PORT", "jobCallbackPort")
	os.Setenv("INFO_COORD_ADDR", "infoCoordAddr")
	defer os.Clearenv()
	wantConfig := Config{
		LogLevel:                            "Debug",
		InfoProducerSupervisionCallbackHost: "supervisionCallbackHost",
		InfoProducerSupervisionCallbackPort: "supervisionCallbackPort",
		InfoJobCallbackHost:                 "jobCallbackHost",
		InfoJobCallbackPort:                 "jobCallbackPort",
		InfoCoordinatorAddress:              "infoCoordAddr",
	}
	if got := New(); !reflect.DeepEqual(got, &wantConfig) {
		t.Errorf("New() = %v, want %v", got, &wantConfig)
	}
}

func TestNew_envVarsNotSetConfigContainDefaultValues(t *testing.T) {
	wantConfig := Config{
		LogLevel:                            "Info",
		InfoProducerSupervisionCallbackHost: "",
		InfoProducerSupervisionCallbackPort: "8085",
		InfoJobCallbackHost:                 "",
		InfoJobCallbackPort:                 "8086",
		InfoCoordinatorAddress:              "http://enrichmentservice:8083",
	}
	if got := New(); !reflect.DeepEqual(got, &wantConfig) {
		t.Errorf("New() = %v, want %v", got, &wantConfig)
	}
}
