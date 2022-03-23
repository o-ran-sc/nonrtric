// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2022: Nordix Foundation
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
	"os"

	"strconv"

	"fmt"

	log "github.com/sirupsen/logrus"
)

type Config struct {
	LogLevel                log.Level
	CertPath                string
	CACertsPath             string
	KeyPath                 string
	AuthServiceUrl          string
	GrantType               string
	ClientSecret            string
	ClientId                string
	AuthTokenOutputFileName string
	RefreshMarginSeconds    int
}

func NewConfig() *Config {
	return &Config{
		CertPath:                getEnv("CERT_PATH", "security/tls.crt", false),
		KeyPath:                 getEnv("CERT_KEY_PATH", "security/tls.key", false),
		CACertsPath:             getEnv("ROOT_CA_CERTS_PATH", "", false),
		LogLevel:                getLogLevel(),
		GrantType:               getEnv("CREDS_GRANT_TYPE", "", false),
		ClientSecret:            getEnv("CREDS_CLIENT_SECRET", "", true),
		ClientId:                getEnv("CREDS_CLIENT_ID", "", false),
		AuthTokenOutputFileName: getEnv("OUTPUT_FILE", "/tmp/authToken.txt", false),
		AuthServiceUrl:          getEnv("AUTH_SERVICE_URL", "https://localhost:39687/example-singlelogin-sever/login", false),
		RefreshMarginSeconds:    getEnvAsInt("REFRESH_MARGIN_SECONDS", 5, 1, 3600),
	}
}

func validateConfiguration(configuration *Config) error {
	if configuration.CertPath == "" || configuration.KeyPath == "" {
		return fmt.Errorf("missing CERT_PATH and/or CERT_KEY_PATH")
	}

	if configuration.CACertsPath == "" {
		log.Warn("No Root CA certs loaded, no trust validation may be performed")
	}

	return nil
}

func getEnv(key string, defaultVal string, secret bool) string {
	if value, exists := os.LookupEnv(key); exists {
		if !secret {
			log.Debugf("Using value: '%v' for '%v'", value, key)
		}
		return value
	} else {
		if !secret {
			log.Debugf("Using default value: '%v' for '%v'", defaultVal, key)
		}
		return defaultVal
	}
}

func getEnvAsInt(name string, defaultVal int, min int, max int) int {
	valueStr := getEnv(name, fmt.Sprint(defaultVal), false)
	if value, err := strconv.Atoi(valueStr); err == nil {
		if value < min || value > max {
			log.Warnf("Value out of range: '%v' for variable: '%v'. Default value: '%v' will be used", valueStr, name, defaultVal)
			return defaultVal
		}
		return value
	} else if valueStr != "" {
		log.Warnf("Invalid int value: '%v' for variable: '%v'. Default value: '%v' will be used", valueStr, name, defaultVal)
	}
	return defaultVal

}

func getLogLevel() log.Level {
	logLevelStr := getEnv("LOG_LEVEL", "Info", false)
	if loglevel, err := log.ParseLevel(logLevelStr); err == nil {
		return loglevel
	} else {
		log.Warnf("Invalid log level: %v. Log level will be Info!", logLevelStr)
		return log.InfoLevel
	}
}
