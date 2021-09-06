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
	os.Setenv("INFO_JOB_CALLBACK_URL", "jobCallbackUrl")
	os.Setenv("INFO_COORD_ADDR", "infoCoordAddr")
	os.Setenv("INFO_PRODUCER_SUPERVISION_CALLBACK_URL", "supervisionCallbackUrl")
	defer os.Clearenv()
	wantConfig := Config{
		LogLevel:                           "Debug",
		InfoJobCallbackUrl:                 "jobCallbackUrl",
		InfoCoordinatorAddress:             "infoCoordAddr",
		InfoProducerSupervisionCallbackUrl: "supervisionCallbackUrl",
	}
	if got := New(); !reflect.DeepEqual(got, &wantConfig) {
		t.Errorf("New() = %v, want %v", got, &wantConfig)
	}
}

func TestNew_envVarsNotSetConfigContainDefaultValues(t *testing.T) {
	wantConfig := Config{
		LogLevel:                           "Info",
		InfoJobCallbackUrl:                 "",
		InfoCoordinatorAddress:             "http://enrichmentservice:8083",
		InfoProducerSupervisionCallbackUrl: "",
	}
	if got := New(); !reflect.DeepEqual(got, &wantConfig) {
		t.Errorf("New() = %v, want %v", got, &wantConfig)
	}
}
