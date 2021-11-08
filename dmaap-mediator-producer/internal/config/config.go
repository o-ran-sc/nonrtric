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
	"strconv"

	log "github.com/sirupsen/logrus"
)

type Config struct {
	LogLevel               log.Level
	InfoProducerHost       string
	InfoProducerPort       int
	InfoCoordinatorAddress string
	DMaaPMRAddress         string
	ProducerCertPath       string
	ProducerKeyPath        string
}

func New() *Config {
	return &Config{
		LogLevel:               getLogLevel(),
		InfoProducerHost:       getEnv("INFO_PRODUCER_HOST", ""),
		InfoProducerPort:       getEnvAsInt("INFO_PRODUCER_PORT", 8085),
		InfoCoordinatorAddress: getEnv("INFO_COORD_ADDR", "https://enrichmentservice:8434"),
		DMaaPMRAddress:         getEnv("DMAAP_MR_ADDR", "https://message-router.onap:3905"),
		ProducerCertPath:       getEnv("PRODUCER_CERT_PATH", "security/producer.crt"),
		ProducerKeyPath:        getEnv("PRODUCER_KEY_PATH", "security/producer.key"),
	}
}

func getEnv(key string, defaultVal string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}

	return defaultVal
}

func getEnvAsInt(name string, defaultVal int) int {
	valueStr := getEnv(name, "")
	if value, err := strconv.Atoi(valueStr); err == nil {
		return value
	} else if valueStr != "" {
		log.Warnf("Invalid int value: %v for variable: %v. Default value: %v will be used", valueStr, name, defaultVal)
	}

	return defaultVal
}

func getLogLevel() log.Level {
	logLevelStr := getEnv("LOG_LEVEL", "Info")
	if loglevel, err := log.ParseLevel(logLevelStr); err == nil {
		return loglevel
	} else {
		log.Warnf("Invalid log level: %v. Log level will be Info!", logLevelStr)
		return log.InfoLevel
	}
}
