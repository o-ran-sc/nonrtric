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

package linkfailure

import (
	"bytes"
	"encoding/json"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"

	log "github.com/sirupsen/logrus"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"oransc.org/usecase/oruclosedloop/internal/repository"
	"oransc.org/usecase/oruclosedloop/internal/ves"
	"oransc.org/usecase/oruclosedloop/mocks"
)

func Test_MessagesHandlerWithLinkFailure(t *testing.T) {
	log.SetLevel(log.DebugLevel)
	assertions := require.New(t)

	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer func() {
		log.SetOutput(os.Stderr)
	}()

	clientMock := mocks.HTTPClient{}

	clientMock.On("Do", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusOK,
	}, nil)

	lookupServiceMock := mocks.LookupService{}

	lookupServiceMock.On("GetODuID", mock.Anything).Return("O-DU-1122", nil)

	handlerUnderTest := NewLinkFailureHandler(&lookupServiceMock, Configuration{
		SDNRAddress:  "http://localhost:9990",
		SDNRUser:     "admin",
		SDNRPassword: "pwd",
	}, &clientMock)

	responseRecorder := httptest.NewRecorder()
	r := newRequest(http.MethodPost, "/", getFaultMessage("ERICSSON-O-RU-11220", "CRITICAL"), t)
	handler := http.HandlerFunc(handlerUnderTest.MessagesHandler)
	handler.ServeHTTP(responseRecorder, r)
	assertions.Equal(http.StatusOK, responseRecorder.Result().StatusCode)

	var actualRequest *http.Request
	clientMock.AssertCalled(t, "Do", mock.MatchedBy(func(req *http.Request) bool {
		actualRequest = req
		return true
	}))
	assertions.Equal(http.MethodPut, actualRequest.Method)
	assertions.Equal("http", actualRequest.URL.Scheme)
	assertions.Equal("localhost:9990", actualRequest.URL.Host)
	expectedSdnrPath := "/rests/data/network-topology:network-topology/topology=topology-netconf/node=O-DU-1122/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions=O-DU-1122/radio-resource-management-policy-ratio=rrm-pol-1"
	assertions.Equal(expectedSdnrPath, actualRequest.URL.Path)
	assertions.Equal("application/json; charset=utf-8", actualRequest.Header.Get("Content-Type"))
	tempRequest, _ := http.NewRequest("", "", nil)
	tempRequest.SetBasicAuth("admin", "pwd")
	assertions.Equal(tempRequest.Header.Get("Authorization"), actualRequest.Header.Get("Authorization"))
	body, _ := ioutil.ReadAll(actualRequest.Body)
	expectedBody := []byte(`{"o-ran-sc-du-hello-world:radio-resource-management-policy-ratio":[{"id":"rrm-pol-1","radio-resource-management-policy-max-ratio":25,"radio-resource-management-policy-members":[{"mobile-country-code":"310","mobile-network-code":"150","slice-differentiator":1,"slice-service-type":1}],"radio-resource-management-policy-min-ratio":15,"user-label":"rrm-pol-1","resource-type":"prb","radio-resource-management-policy-dedicated-ratio":20,"administrative-state":"unlocked"}]}`)
	assertions.Equal(expectedBody, body)
	clientMock.AssertNumberOfCalls(t, "Do", 1)

	logString := buf.String()
	assertions.Contains(logString, "Sent unlock message")
	assertions.Contains(logString, "O-RU: ERICSSON-O-RU-11220")
	assertions.Contains(logString, "O-DU: O-DU-1122")
}

func newRequest(method string, url string, bodyAsBytes []byte, t *testing.T) *http.Request {
	body := ioutil.NopCloser(bytes.NewReader(bodyAsBytes))
	if req, err := http.NewRequest(method, url, body); err == nil {
		return req
	} else {
		t.Fatalf("Could not create request due to: %v", err)
		return nil
	}
}

func Test_MessagesHandlerWithClearLinkFailure(t *testing.T) {
	log.SetLevel(log.DebugLevel)
	assertions := require.New(t)

	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer func() {
		log.SetOutput(os.Stderr)
	}()

	lookupServiceMock := mocks.LookupService{}

	lookupServiceMock.On("GetODuID", mock.Anything).Return("O-DU-1122", nil)

	handlerUnderTest := NewLinkFailureHandler(&lookupServiceMock, Configuration{}, nil)

	responseRecorder := httptest.NewRecorder()
	r := newRequest(http.MethodPost, "/", getFaultMessage("ERICSSON-O-RU-11220", "NORMAL"), t)
	handler := http.HandlerFunc(handlerUnderTest.MessagesHandler)
	handler.ServeHTTP(responseRecorder, r)
	assertions.Equal(http.StatusOK, responseRecorder.Result().StatusCode)

	logString := buf.String()
	assertions.Contains(logString, "Cleared Link failure")
	assertions.Contains(logString, "O-RU ID: ERICSSON-O-RU-11220")
}

func Test_MessagesHandlerWithLinkFailureUnmappedORU(t *testing.T) {
	log.SetLevel(log.DebugLevel)
	assertions := require.New(t)

	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer func() {
		log.SetOutput(os.Stderr)
	}()

	lookupServiceMock := mocks.LookupService{}

	lookupServiceMock.On("GetODuID", mock.Anything).Return("", repository.IdNotMappedError{
		Id: "ERICSSON-O-RU-11220",
	})

	handlerUnderTest := NewLinkFailureHandler(&lookupServiceMock, Configuration{}, nil)

	responseRecorder := httptest.NewRecorder()
	r := newRequest(http.MethodPost, "/", getFaultMessage("ERICSSON-O-RU-11220", "CRITICAL"), t)
	handler := http.HandlerFunc(handlerUnderTest.MessagesHandler)
	handler.ServeHTTP(responseRecorder, r)
	assertions.Equal(http.StatusOK, responseRecorder.Result().StatusCode)

	logString := buf.String()
	assertions.Contains(logString, "O-RU-ID: ERICSSON-O-RU-11220 not mapped.")
}

func getFaultMessage(sourceName string, eventSeverity string) []byte {
	linkFailureMessage := ves.FaultMessage{
		Event: ves.Event{
			CommonEventHeader: ves.CommonEventHeader{
				Domain:     "fault",
				SourceName: sourceName,
			},
			FaultFields: ves.FaultFields{
				AlarmCondition: "28",
				EventSeverity:  eventSeverity,
			},
		},
	}
	messageAsByteArray, _ := json.Marshal(linkFailureMessage)
	response := [1]string{string(messageAsByteArray)}
	responseAsByteArray, _ := json.Marshal(response)
	return responseAsByteArray
}
