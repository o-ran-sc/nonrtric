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

package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"os"
	"sync"
	"syscall"
	"testing"
	"time"

	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"oransc.org/usecase/oruclosedloop/internal/config"
	"oransc.org/usecase/oruclosedloop/internal/linkfailure"
	"oransc.org/usecase/oruclosedloop/mocks"
)

func Test_init(t *testing.T) {
	assertions := require.New(t)

	os.Setenv("CONSUMER_HOST", "consumerHost")
	os.Setenv("CONSUMER_PORT", "8095")
	t.Cleanup(func() {
		os.Clearenv()
	})

	doInit()

	wantedConfiguration := &config.Config{
		ConsumerHost:           "consumerHost",
		ConsumerPort:           8095,
		InfoCoordinatorAddress: "http://enrichmentservice:8083",
		SDNRAddress:            "http://localhost:3904",
		SDNRUser:               "admin",
		SDNPassword:            "Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U",
		ORUToODUMapFile:        "o-ru-to-o-du-map.csv",
		ConsumerCertPath:       "security/consumer.crt",
		ConsumerKeyPath:        "security/consumer.key",
		LogLevel:               log.InfoLevel,
	}
	assertions.Equal(wantedConfiguration, configuration)

	assertions.Equal(fmt.Sprint(wantedConfiguration.ConsumerPort), consumerPort)
	assertions.Equal(wantedConfiguration.ConsumerHost+":"+fmt.Sprint(wantedConfiguration.ConsumerPort), jobRegistrationInfo.JobResultURI)

	wantedLinkFailureConfig := linkfailure.Configuration{
		SDNRAddress:  wantedConfiguration.SDNRAddress,
		SDNRUser:     wantedConfiguration.SDNRUser,
		SDNRPassword: wantedConfiguration.SDNPassword,
	}
	assertions.Equal(wantedLinkFailureConfig, linkfailureConfig)
}

func Test_validateConfiguration(t *testing.T) {
	assertions := require.New(t)

	type args struct {
		configuration *config.Config
	}
	tests := []struct {
		name    string
		args    args
		wantErr error
	}{
		{
			name: "Valid config, should return nil",
			args: args{
				configuration: &config.Config{
					ConsumerHost:     "host",
					ConsumerPort:     80,
					ConsumerCertPath: "security/consumer.crt",
					ConsumerKeyPath:  "security/consumer.key",
				},
			},
		},
		{
			name: "Invalid config, should return error",
			args: args{
				configuration: &config.Config{},
			},
			wantErr: fmt.Errorf("consumer host and port must be provided"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := validateConfiguration(tt.args.configuration)
			assertions.Equal(tt.wantErr, err)
		})
	}
}

func Test_initializeLookupService(t *testing.T) {
	assertions := require.New(t)
	type args struct {
		csvFile         string
		oRuId           string
		mockReturn      [][]string
		mockReturnError error
	}
	tests := []struct {
		name        string
		args        args
		wantODuId   string
		wantInitErr error
	}{
		{
			name: "Successful initialization, should return nil and lookup service should be initiated with data",
			args: args{
				csvFile:    "file",
				oRuId:      "1",
				mockReturn: [][]string{{"1", "2"}},
			},
			wantODuId: "2",
		},
		{
			name: "Unsuccessful initialization, should return error and lookup service should not be initiated with data",
			args: args{
				csvFile:         "file",
				mockReturnError: errors.New("Error"),
			},
			wantInitErr: errors.New("Error"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mockCsvFileHelper := &mocks.CsvFileHelper{}
			mockCsvFileHelper.On("GetCsvFromFile", mock.Anything).Return(tt.args.mockReturn, tt.args.mockReturnError)

			err := initializeLookupService(mockCsvFileHelper, tt.args.csvFile)
			oDuId, _ := lookupService.GetODuID(tt.args.oRuId)
			assertions.Equal(tt.wantODuId, oDuId)
			assertions.Equal(tt.wantInitErr, err)
			mockCsvFileHelper.AssertCalled(t, "GetCsvFromFile", tt.args.csvFile)
		})
	}
}

func Test_getRouter_shouldContainAllPathsWithHandlers(t *testing.T) {
	assertions := require.New(t)

	r := getRouter()
	messageHandlerRoute := r.Get("messageHandler")
	assertions.NotNil(messageHandlerRoute)
	supportedMethods, err := messageHandlerRoute.GetMethods()
	assertions.Equal([]string{http.MethodPost}, supportedMethods)
	assertions.Nil(err)
	path, _ := messageHandlerRoute.GetPathTemplate()
	assertions.Equal("/", path)

	startHandlerRoute := r.Get("start")
	assertions.NotNil(messageHandlerRoute)
	supportedMethods, err = startHandlerRoute.GetMethods()
	assertions.Equal([]string{http.MethodPost}, supportedMethods)
	assertions.Nil(err)
	path, _ = startHandlerRoute.GetPathTemplate()
	assertions.Equal("/admin/start", path)

	stopHandlerRoute := r.Get("stop")
	assertions.NotNil(stopHandlerRoute)
	supportedMethods, err = stopHandlerRoute.GetMethods()
	assertions.Equal([]string{http.MethodPost}, supportedMethods)
	assertions.Nil(err)
	path, _ = stopHandlerRoute.GetPathTemplate()
	assertions.Equal("/admin/stop", path)

	statusHandlerRoute := r.Get("status")
	assertions.NotNil(statusHandlerRoute)
	supportedMethods, err = statusHandlerRoute.GetMethods()
	assertions.Equal([]string{http.MethodGet}, supportedMethods)
	assertions.Nil(err)
	path, _ = statusHandlerRoute.GetPathTemplate()
	assertions.Equal("/status", path)
}

func Test_startHandler(t *testing.T) {
	assertions := require.New(t)

	jobRegistrationInfo.JobResultURI = "host:80"

	type args struct {
		mockReturnBody   []byte
		mockReturnStatus int
	}
	tests := []struct {
		name         string
		args         args
		wantedStatus int
		wantedBody   string
	}{
		{
			name: "Start with successful registration, should return ok",
			args: args{
				mockReturnBody:   []byte(""),
				mockReturnStatus: http.StatusOK,
			},
			wantedStatus: http.StatusOK,
		},
		{
			name: "Start with error response at registration, should return error",
			args: args{
				mockReturnBody:   []byte("error"),
				mockReturnStatus: http.StatusBadRequest,
			},
			wantedStatus: http.StatusBadRequest,
			wantedBody:   "Unable to register consumer job due to:",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			clientMock := setUpClientMock(tt.args.mockReturnBody, tt.args.mockReturnStatus)

			handler := http.HandlerFunc(startHandler)
			responseRecorder := httptest.NewRecorder()
			r, _ := http.NewRequest(http.MethodPost, "/start", nil)

			handler.ServeHTTP(responseRecorder, r)

			assertions.Equal(tt.wantedStatus, responseRecorder.Code, tt.name)
			assertions.Contains(responseRecorder.Body.String(), tt.wantedBody, tt.name)

			var wantedJobRegistrationInfo = struct {
				InfoTypeId    string      `json:"info_type_id"`
				JobResultUri  string      `json:"job_result_uri"`
				JobOwner      string      `json:"job_owner"`
				JobDefinition interface{} `json:"job_definition"`
			}{
				InfoTypeId:    "STD_Fault_Messages",
				JobResultUri:  "host:80",
				JobOwner:      "O-RU Closed Loop Usecase",
				JobDefinition: "{}",
			}
			wantedBody, _ := json.Marshal(wantedJobRegistrationInfo)

			var actualRequest *http.Request
			clientMock.AssertCalled(t, "Do", mock.MatchedBy(func(req *http.Request) bool {
				actualRequest = req
				return true
			}))
			assertions.Equal(http.MethodPut, actualRequest.Method)
			assertions.Equal("http", actualRequest.URL.Scheme)
			assertions.Equal("enrichmentservice:8083", actualRequest.URL.Host)
			assertions.Equal("/data-consumer/v1/info-jobs/14e7bb84-a44d-44c1-90b7-6995a92ad43c", actualRequest.URL.Path)
			assertions.Equal("application/json; charset=utf-8", actualRequest.Header.Get("Content-Type"))
			body, _ := ioutil.ReadAll(actualRequest.Body)
			expectedBody := wantedBody
			assertions.Equal(expectedBody, body)
			clientMock.AssertNumberOfCalls(t, "Do", 1)

			// Check that the running status is "started"
			statusHandler := http.HandlerFunc(statusHandler)
			statusResponseRecorder := httptest.NewRecorder()
			statusRequest, _ := http.NewRequest(http.MethodGet, "/status", nil)

			statusHandler.ServeHTTP(statusResponseRecorder, statusRequest)

			assertions.Equal(http.StatusOK, statusResponseRecorder.Code)
			assertions.Equal(`{"status": "started"}`, statusResponseRecorder.Body.String())
		})
	}
}

func Test_stopHandler(t *testing.T) {
	assertions := require.New(t)

	jobRegistrationInfo.JobResultURI = "host:80"

	type args struct {
		mockReturnBody   []byte
		mockReturnStatus int
	}
	tests := []struct {
		name         string
		args         args
		wantedStatus int
		wantedBody   string
	}{
		{
			name: "Stop with successful job deletion, should return ok",
			args: args{
				mockReturnBody:   []byte(""),
				mockReturnStatus: http.StatusOK,
			},
			wantedStatus: http.StatusOK,
		},
		{
			name: "Stop with error response at job deletion, should return error",
			args: args{
				mockReturnBody:   []byte("error"),
				mockReturnStatus: http.StatusBadRequest,
			},
			wantedStatus: http.StatusBadRequest,
			wantedBody:   "Please remove job 14e7bb84-a44d-44c1-90b7-6995a92ad43c manually",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			clientMock := setUpClientMock(tt.args.mockReturnBody, tt.args.mockReturnStatus)

			handler := http.HandlerFunc(stopHandler)
			responseRecorder := httptest.NewRecorder()
			r, _ := http.NewRequest(http.MethodPost, "/stop", nil)

			handler.ServeHTTP(responseRecorder, r)

			assertions.Equal(tt.wantedStatus, responseRecorder.Code, tt.name)
			assertions.Contains(responseRecorder.Body.String(), tt.wantedBody, tt.name)

			var actualRequest *http.Request
			clientMock.AssertCalled(t, "Do", mock.MatchedBy(func(req *http.Request) bool {
				actualRequest = req
				return true
			}))
			assertions.Equal(http.MethodDelete, actualRequest.Method)
			assertions.Equal("http", actualRequest.URL.Scheme)
			assertions.Equal("enrichmentservice:8083", actualRequest.URL.Host)
			assertions.Equal("/data-consumer/v1/info-jobs/14e7bb84-a44d-44c1-90b7-6995a92ad43c", actualRequest.URL.Path)
			clientMock.AssertNumberOfCalls(t, "Do", 1)

			// Check that the running status is "stopped"
			statusHandler := http.HandlerFunc(statusHandler)
			statusResponseRecorder := httptest.NewRecorder()
			statusRequest, _ := http.NewRequest(http.MethodGet, "/status", nil)

			statusHandler.ServeHTTP(statusResponseRecorder, statusRequest)

			assertions.Equal(http.StatusOK, statusResponseRecorder.Code)
			assertions.Equal(`{"status": "stopped"}`, statusResponseRecorder.Body.String())
		})
	}
}

func Test_deleteOnShutdown(t *testing.T) {
	assertions := require.New(t)

	var buf bytes.Buffer
	log.SetOutput(&buf)

	t.Cleanup(func() {
		log.SetOutput(os.Stderr)
	})

	type args struct {
		mockReturnBody   []byte
		mockReturnStatus int
	}
	tests := []struct {
		name      string
		args      args
		wantedLog string
	}{
		{
			name: "Delete with successful job deletion, should return ok",
			args: args{
				mockReturnBody:   []byte(""),
				mockReturnStatus: http.StatusOK,
			},
		},
		{
			name: "Stop with error response at job deletion, should return error",
			args: args{
				mockReturnBody:   []byte("error"),
				mockReturnStatus: http.StatusBadRequest,
			},
			wantedLog: "Please remove job 14e7bb84-a44d-44c1-90b7-6995a92ad43c manually",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			setUpClientMock(tt.args.mockReturnBody, tt.args.mockReturnStatus)

			c := make(chan os.Signal, 1)
			go deleteOnShutdown(c)
			c <- syscall.SIGTERM

			waitForLogToBeWritten(&buf)

			log := buf.String()
			if tt.wantedLog != "" {
				assertions.Contains(log, "level=error")
				assertions.Contains(log, "Unable to delete job on shutdown due to:")
				assertions.Contains(log, tt.wantedLog)
			}
		})
	}
}

func waitForLogToBeWritten(logBuf *bytes.Buffer) {
	wg := sync.WaitGroup{}
	wg.Add(1)
	for {
		if waitTimeout(&wg, 10*time.Millisecond) && logBuf.Len() != 0 {
			wg.Done()
			break
		}
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

func setUpClientMock(body []byte, status int) *mocks.HTTPClient {
	clientMock := mocks.HTTPClient{}
	clientMock.On("Do", mock.Anything).Return(&http.Response{
		Body:       ioutil.NopCloser(bytes.NewReader(body)),
		StatusCode: status,
	}, nil)
	client = &clientMock
	return &clientMock
}
