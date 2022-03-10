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

	log "github.com/sirupsen/logrus"
)

type Config struct {
	LogLevel                log.Level
	CertPath                string
	KeyPath                 string
	AuthServiceUrl          string
	GrantType               string
	ClientSecret            string
	ClientId                string
	AuthTokenOutputFileName string
}

func NewConfig() *Config {
	return &Config{
		CertPath:                getEnv("CERT_PATH", "security/tls.crt"),
		KeyPath:                 getEnv("CERT_KEY_PATH", "security/tls.key"),
		LogLevel:                getLogLevel(),
		GrantType:               getEnv("CREDS_GRANT_TYPE", ""),
		ClientSecret:            getEnv("CREDS_CLIENT_SECRET", ""),
		ClientId:                getEnv("CREDS_CLIENT_ID", ""),
		AuthTokenOutputFileName: getEnv("OUTPUT_FILE", "/tmp/authToken.txt"),
		AuthServiceUrl:          getEnv("AUTH_SERVICE_URL", "https://localhost:39687/example-singlelogin-sever/login"),
	}
}

func getEnv(key string, defaultVal string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
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
