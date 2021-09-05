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

package restclient

import (
	"bytes"
	"errors"
	"io/ioutil"
	"net/http"
	"reflect"
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"oransc.org/nonrtric/dmaapmediatorproducer/mocks"
)

func TestGet(t *testing.T) {
	clientMock := mocks.HTTPClient{}

	clientMock.On("Get", "http://testOk").Return(&http.Response{
		StatusCode: http.StatusOK,
		Body:       ioutil.NopCloser(bytes.NewReader([]byte("Response"))),
	}, nil)

	clientMock.On("Get", "http://testNotOk").Return(&http.Response{
		StatusCode: http.StatusBadRequest,
		Body:       ioutil.NopCloser(bytes.NewReader([]byte("Bad Response"))),
	}, nil)

	clientMock.On("Get", "http://testError").Return(nil, errors.New("Failed Request"))

	Client = &clientMock

	type args struct {
		url string
	}
	tests := []struct {
		name        string
		args        args
		want        []byte
		wantErr     bool
		wantedError error
	}{
		{
			name: "Test Get with OK response",
			args: args{
				url: "http://testOk",
			},
			want:    []byte("Response"),
			wantErr: false,
		},
		{
			name: "Test Get with Not OK response",
			args: args{
				url: "http://testNotOk",
			},
			want:    nil,
			wantErr: true,
			wantedError: RequestError{
				StatusCode: http.StatusBadRequest,
				Body:       []byte("Bad Response"),
			},
		},
		{
			name: "Test Get with error",
			args: args{
				url: "http://testError",
			},
			want:        nil,
			wantErr:     true,
			wantedError: errors.New("Failed Request"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := Get(tt.args.url)
			if (err != nil) != tt.wantErr {
				t.Errorf("Get() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Get() = %v, want %v", got, tt.want)
			}
			if tt.wantErr && err.Error() != tt.wantedError.Error() {
				t.Errorf("Get() error = %v, wantedError % v", err, tt.wantedError.Error())
			}
		})
	}
}

func TestPutOk(t *testing.T) {
	assertions := require.New(t)
	clientMock := mocks.HTTPClient{}

	clientMock.On("Do", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusOK,
	}, nil)

	Client = &clientMock
	if err := Put("http://localhost:9990", []byte("body")); err != nil {
		t.Errorf("Put() error = %v, did not want error", err)
	}
	var actualRequest *http.Request
	clientMock.AssertCalled(t, "Do", mock.MatchedBy(func(req *http.Request) bool {
		actualRequest = req
		return true
	}))
	assertions.Equal(http.MethodPut, actualRequest.Method)
	assertions.Equal("http", actualRequest.URL.Scheme)
	assertions.Equal("localhost:9990", actualRequest.URL.Host)
	assertions.Equal("application/json; charset=utf-8", actualRequest.Header.Get("Content-Type"))
	body, _ := ioutil.ReadAll(actualRequest.Body)
	expectedBody := []byte("body")
	assertions.Equal(expectedBody, body)
	clientMock.AssertNumberOfCalls(t, "Do", 1)
}

func TestPutBadResponse(t *testing.T) {
	assertions := require.New(t)
	clientMock := mocks.HTTPClient{}

	clientMock.On("Do", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusBadRequest,
		Body:       ioutil.NopCloser(bytes.NewReader([]byte("Bad Request"))),
	}, nil)

	Client = &clientMock
	err := Put("url", []byte("body"))
	assertions.NotNil("Put() error = %v, wanted error", err)
	expectedErrorMessage := "Request failed due to error response with status: 400 and body: Bad Request"
	assertions.Equal(expectedErrorMessage, err.Error())
}

func TestPutError(t *testing.T) {
	assertions := require.New(t)
	clientMock := mocks.HTTPClient{}

	clientMock.On("Do", mock.Anything).Return(nil, errors.New("Failed Request"))

	Client = &clientMock
	err := Put("url", []byte("body"))
	assertions.NotNil("Put() error = %v, wanted error", err)
	expectedErrorMessage := "Failed Request"
	assertions.Equal(expectedErrorMessage, err.Error())
}
