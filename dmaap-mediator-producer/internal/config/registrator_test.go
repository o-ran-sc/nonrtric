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
	"io/ioutil"
	"net/http"
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"oransc.org/nonrtric/dmaapmediatorproducer/mocks/httpclient"
)

func TestRegisterTypes(t *testing.T) {
	assertions := require.New(t)

	clientMock := httpclient.HTTPClient{}

	clientMock.On("Do", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusCreated,
	}, nil)

	type1 := TypeDefinition{
		Id: "Type1",
	}
	types := []TypeDefinition{type1}

	r := NewRegistratorImpl("http://localhost:9990", &clientMock)
	err := r.RegisterTypes(types)

	assertions.Nil(err)
	var actualRequest *http.Request
	clientMock.AssertCalled(t, "Do", mock.MatchedBy(func(req *http.Request) bool {
		actualRequest = req
		return true
	}))
	assertions.Equal(http.MethodPut, actualRequest.Method)
	assertions.Equal("http", actualRequest.URL.Scheme)
	assertions.Equal("localhost:9990", actualRequest.URL.Host)
	assertions.Equal("/data-producer/v1/info-types/Type1", actualRequest.URL.Path)
	assertions.Equal("application/json", actualRequest.Header.Get("Content-Type"))
	body, _ := ioutil.ReadAll(actualRequest.Body)
	expectedBody := []byte(`{"info_job_data_schema": {"type": "object","properties": {},"additionalProperties": false}}`)
	assertions.Equal(expectedBody, body)
	clientMock.AssertNumberOfCalls(t, "Do", 1)
}

func TestRegisterProducer(t *testing.T) {
	assertions := require.New(t)

	clientMock := httpclient.HTTPClient{}

	clientMock.On("Do", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusCreated,
	}, nil)

	producer := ProducerRegistrationInfo{
		InfoProducerSupervisionCallbackUrl: "supervisionCallbackUrl",
		SupportedInfoTypes:                 []string{"type1"},
		InfoJobCallbackUrl:                 "jobCallbackUrl",
	}

	r := NewRegistratorImpl("http://localhost:9990", &clientMock)
	err := r.RegisterProducer("Producer1", &producer)

	assertions.Nil(err)
	var actualRequest *http.Request
	clientMock.AssertCalled(t, "Do", mock.MatchedBy(func(req *http.Request) bool {
		actualRequest = req
		return true
	}))
	assertions.Equal(http.MethodPut, actualRequest.Method)
	assertions.Equal("http", actualRequest.URL.Scheme)
	assertions.Equal("localhost:9990", actualRequest.URL.Host)
	assertions.Equal("/data-producer/v1/info-producers/Producer1", actualRequest.URL.Path)
	assertions.Equal("application/json", actualRequest.Header.Get("Content-Type"))
	body, _ := ioutil.ReadAll(actualRequest.Body)
	expectedBody := []byte(`{"info_producer_supervision_callback_url":"supervisionCallbackUrl","supported_info_types":["type1"],"info_job_callback_url":"jobCallbackUrl"}`)
	assertions.Equal(expectedBody, body)
	clientMock.AssertNumberOfCalls(t, "Do", 1)
}
