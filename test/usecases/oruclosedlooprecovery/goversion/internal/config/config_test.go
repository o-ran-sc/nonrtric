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
	"testing"

	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/require"
)

func TestNew_envVarsSetConfigContainSetValues(t *testing.T) {
	assertions := require.New(t)
	os.Setenv("LOG_LEVEL", "Debug")
	os.Setenv("CONSUMER_HOST", "consumerHost")
	os.Setenv("CONSUMER_PORT", "8095")
	os.Setenv("INFO_COORD_ADDR", "infoCoordAddr")
	os.Setenv("SDNR_HOST", "sdnrHost")
	os.Setenv("SDNR_PORT", "3908")
	os.Setenv("SDNR_USER", "admin")
	os.Setenv("SDNR_PASSWORD", "pwd")
	os.Setenv("ORU_TO_ODU_MAP_FILE", "file")
	t.Cleanup(func() {
		os.Clearenv()
	})
	wantConfig := Config{
		LogLevel:               log.DebugLevel,
		ConsumerHost:           "consumerHost",
		ConsumerPort:           8095,
		InfoCoordinatorAddress: "infoCoordAddr",
		SDNRHost:               "sdnrHost",
		SDNRPort:               3908,
		SDNRUser:               "admin",
		SDNPassword:            "pwd",
		ORUToODUMapFile:        "file",
	}

	got := New()
	assertions.Equal(&wantConfig, got)
}

func TestNew_faultyIntValueSetConfigContainDefaultValueAndWarnInLog(t *testing.T) {
	assertions := require.New(t)
	var buf bytes.Buffer
	log.SetOutput(&buf)

	os.Setenv("CONSUMER_PORT", "wrong")
	t.Cleanup(func() {
		log.SetOutput(os.Stderr)
		os.Clearenv()
	})
	wantConfig := Config{
		LogLevel:               log.InfoLevel,
		ConsumerHost:           "",
		ConsumerPort:           0,
		InfoCoordinatorAddress: "http://enrichmentservice:8083",
		SDNRHost:               "http://localhost",
		SDNRPort:               3904,
		SDNRUser:               "admin",
		SDNPassword:            "Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U",
		ORUToODUMapFile:        "o-ru-to-o-du-map.csv",
	}

	got := New()
	assertions.Equal(&wantConfig, got)

	logString := buf.String()
	assertions.Contains(logString, "Invalid int value: wrong for variable: CONSUMER_PORT. Default value: 0 will be used")
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
		ConsumerHost:           "",
		ConsumerPort:           0,
		InfoCoordinatorAddress: "http://enrichmentservice:8083",
		SDNRHost:               "http://localhost",
		SDNRPort:               3904,
		SDNRUser:               "admin",
		SDNPassword:            "Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U",
		ORUToODUMapFile:        "o-ru-to-o-du-map.csv",
	}
	got := New()
	assertions.Equal(&wantConfig, got)
	logString := buf.String()
	assertions.Contains(logString, "Invalid log level: wrong. Log level will be Info!")
}
