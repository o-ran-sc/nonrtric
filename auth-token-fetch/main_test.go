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
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"testing"
	"time"

	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/require"
)

func createHttpClientMock(t *testing.T, configuration *Config, token JwtToken) *http.Client {
	assertions := require.New(t)
	clientMock := NewTestClient(func(req *http.Request) *http.Response {
		if req.URL.String() == configuration.AuthServiceUrl {
			assertions.Equal(req.Method, "POST")
			body := getBodyAsString(req, t)
			assertions.Contains(body, "client_id="+configuration.ClientId)
			assertions.Contains(body, "secret="+configuration.ClientSecret)
			assertions.Contains(body, "grant_type="+configuration.GrantType)
			contentType := req.Header.Get("content-type")
			assertions.Equal("application/x-www-form-urlencoded", contentType)

			return &http.Response{
				StatusCode: 200,
				Body:       ioutil.NopCloser(bytes.NewBuffer(toBody(token))),
				Header:     make(http.Header), // Must be set to non-nil value or it panics
			}
		}
		t.Error("Wrong call to client: ", req)
		t.Fail()
		return nil
	})
	return clientMock
}

func TestFetchAndStoreToken(t *testing.T) {
	log.SetLevel(log.TraceLevel)
	assertions := require.New(t)
	configuration := NewConfig()
	configuration.AuthTokenOutputFileName = "/tmp/authToken" + fmt.Sprint(time.Now().UnixNano())
	configuration.ClientId = "testClientId"
	configuration.ClientSecret = "testClientSecret"
	configuration.RefreshMarginSeconds = 1
	context := NewContext(configuration)

	t.Cleanup(func() {
		os.Remove(configuration.AuthTokenOutputFileName)
	})

	accessToken := "Access_token" + fmt.Sprint(time.Now().UnixNano())
	token := JwtToken{Access_token: accessToken, Expires_in: 7, Token_type: "Token_type"}

	clientMock := createHttpClientMock(t, configuration, token)

	go periodicRefreshIwtToken(clientMock, context)

	await(func() bool { return fileExists(configuration.AuthTokenOutputFileName) }, t)

	tokenFileContent, err := ioutil.ReadFile(configuration.AuthTokenOutputFileName)
	check(err)

	assertions.Equal(accessToken, string(tokenFileContent))

	context.Running = false
}

func fileExists(fileName string) bool {
	if _, err := os.Stat(fileName); err == nil {
		return true
	}
	log.Debug("Waiting for file: " + fileName)
	return false
}

func await(predicate func() bool, t *testing.T) {
	MAX_TIME_SECONDS := 30
	for i := 1; i < MAX_TIME_SECONDS; i++ {
		if predicate() {
			return
		}
		time.Sleep(time.Second)
	}
	t.Error("Predicate not fulfilled")
	t.Fail()
}

func TestStart(t *testing.T) {
	assertions := require.New(t)
	log.SetLevel(log.TraceLevel)

	configuration := NewConfig()
	configuration.AuthTokenOutputFileName = "/tmp/authToken" + fmt.Sprint(time.Now().UnixNano())
	configuration.CACertsPath = configuration.CertPath
	context := NewContext(configuration)
	t.Cleanup(func() {
		os.Remove(configuration.AuthTokenOutputFileName)
	})

	start(context)

	time.Sleep(time.Second * 5)

	_, err := os.Stat(configuration.AuthTokenOutputFileName)

	assertions.True(errors.Is(err, os.ErrNotExist))
	context.Running = false
}

func toBody(token JwtToken) []byte {
	body, err := json.Marshal(token)
	check(err)
	return body
}

type RoundTripFunc func(req *http.Request) *http.Response

func (f RoundTripFunc) RoundTrip(req *http.Request) (*http.Response, error) {
	return f(req), nil
}

//NewTestClient returns *http.Client with Transport replaced to avoid making real calls
func NewTestClient(fn RoundTripFunc) *http.Client {
	return &http.Client{
		Transport: RoundTripFunc(fn),
	}
}

func getBodyAsString(req *http.Request, t *testing.T) string {
	buf := new(bytes.Buffer)
	if _, err := buf.ReadFrom(req.Body); err != nil {
		t.Fail()
	}
	return buf.String()
}
