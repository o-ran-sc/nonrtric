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
	"fmt"
	"os"
	"strconv"

	log "github.com/sirupsen/logrus"
)

type Config struct {
	MRHost      string
	MRPort      string
	SDNRAddress string
	SDNRUser    string
	SDNPassword string
	Polltime    int
	LogLevel    log.Level
}

func New() *Config {
	return &Config{
		MRHost:      getEnv("MR_HOST", ""),
		MRPort:      getEnv("MR_PORT", ""),
		SDNRAddress: getEnv("SDNR_ADDR", "http://localhost:3904"),
		SDNRUser:    getEnv("SDNR_USER", "admin"),
		SDNPassword: getEnv("SDNR_PASSWORD", "Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U"),
		Polltime:    getEnvAsInt("Polltime", 30),
		LogLevel:    getLogLevel(),
	}
}

func (c Config) String() string {
	return fmt.Sprintf("[MRHost: %v, MRPort: %v, SDNRAddress: %v, SDNRUser: %v, SDNRPassword: %v, PollTime: %v, LogLevel: %v]", c.MRHost, c.MRPort, c.SDNRAddress, c.SDNRUser, c.SDNPassword, c.Polltime, c.LogLevel)
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
