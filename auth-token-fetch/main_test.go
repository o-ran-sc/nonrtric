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
	"sync"
	"testing"
	"time"

	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/require"
)

func createHttpClientMock(t *testing.T, configuration *Config, wg *sync.WaitGroup, token JwtToken) *http.Client {
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
			wg.Done()
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

	wg := sync.WaitGroup{}
	wg.Add(2) // Get token two times
	clientMock := createHttpClientMock(t, configuration, &wg, token)

	go periodicRefreshIwtToken(clientMock, context)

	if waitTimeout(&wg, 12*time.Second) {
		t.Error("Not all calls to server were made")
		t.Fail()
	}

	tokenFileContent, err := ioutil.ReadFile(configuration.AuthTokenOutputFileName)
	check(err)

	assertions.Equal(accessToken, string(tokenFileContent))

	context.Running = false
}

func TestStart(t *testing.T) {
	assertions := require.New(t)
	log.SetLevel(log.TraceLevel)

	configuration := NewConfig()
	configuration.AuthTokenOutputFileName = "/tmp/authToken" + fmt.Sprint(time.Now().UnixNano())
	context := NewContext(configuration)

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

// waitTimeout waits for the waitgroup for the specified max timeout.
// Returns true if waiting timed out.
func waitTimeout(wg *sync.WaitGroup, timeout time.Duration) bool {
	c := make(chan struct{})
	go func() {
		defer close(c)
		wg.Wait()
	}()
	select {
	case <-c:
		return false // completed normally
	case <-time.After(timeout):
		return true // timed out
	}
}

func getBodyAsString(req *http.Request, t *testing.T) string {
	buf := new(bytes.Buffer)
	if _, err := buf.ReadFrom(req.Body); err != nil {
		t.Fail()
	}
	return buf.String()
}
