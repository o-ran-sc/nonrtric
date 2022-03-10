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
	"testing"

	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/require"
)

func TestNew_envVarsSetConfigContainSetValues(t *testing.T) {
	assertions := require.New(t)
	os.Setenv("LOG_LEVEL", "Debug")
	os.Setenv("CERT_PATH", "CERT_PATH")
	os.Setenv("CERT_KEY_PATH", "CERT_KEY_PATH")
	os.Setenv("CREDS_GRANT_TYPE", "CREDS_GRANT_TYPE")
	os.Setenv("CREDS_CLIENT_SECRET", "CREDS_CLIENT_SECRET")
	os.Setenv("CREDS_CLIENT_ID", "CREDS_CLIENT_ID")
	os.Setenv("OUTPUT_FILE", "OUTPUT_FILE")
	os.Setenv("AUTH_SERVICE_URL", "AUTH_SERVICE_URL")

	t.Cleanup(func() {
		os.Clearenv()
	})
	wantConfig := Config{
		LogLevel:                log.DebugLevel,
		CertPath:                "CERT_PATH",
		KeyPath:                 "CERT_KEY_PATH",
		AuthServiceUrl:          "AUTH_SERVICE_URL",
		GrantType:               "CREDS_GRANT_TYPE",
		ClientSecret:            "CREDS_CLIENT_SECRET",
		ClientId:                "CREDS_CLIENT_ID",
		AuthTokenOutputFileName: "OUTPUT_FILE",
	}
	got := NewConfig()

	assertions.Equal(&wantConfig, got)
}
