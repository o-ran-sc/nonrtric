// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2021: Nordix Foundation
//   %%
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
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
	"fmt"
	"io/ioutil"
	"net/http"
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"oransc.org/usecase/oruclosedloop/mocks"
)

func TestRequestError_Error(t *testing.T) {
	assertions := require.New(t)

	actualError := RequestError{
		StatusCode: http.StatusBadRequest,
		Body:       []byte("error"),
	}
	assertions.Equal("Request failed due to error response with status: 400 and body: error", actualError.Error())
}

func TestGet(t *testing.T) {
	assertions := require.New(t)

	type args struct {
		url              string
		mockReturnStatus int
		mockReturnBody   []byte
		mockReturnError  error
	}
	tests := []struct {
		name    string
		args    args
		want    []byte
		wantErr error
	}{
		{
			name: "Ok response",
			args: args{
				url:              "ok",
				mockReturnStatus: http.StatusOK,
				mockReturnBody:   []byte("body"),
				mockReturnError:  nil,
			},
			want:    []byte("body"),
			wantErr: nil,
		},
		{
			name: "Bad request should get RequestError",
			args: args{
				url:              "badRequest",
				mockReturnStatus: http.StatusBadRequest,
				mockReturnBody:   []byte("bad request"),
				mockReturnError:  nil,
			},
			want: nil,
			wantErr: RequestError{
				StatusCode: http.StatusBadRequest,
				Body:       []byte("bad request"),
			},
		},
		{
			name: "Server unavailable should get error",
			args: args{
				url:             "serverUnavailable",
				mockReturnError: fmt.Errorf("Server unavailable"),
			},
			want:    nil,
			wantErr: fmt.Errorf("Server unavailable"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			clientMock := mocks.HTTPClient{}
			clientMock.On("Get", tt.args.url).Return(&http.Response{
				StatusCode: tt.args.mockReturnStatus,
				Body:       ioutil.NopCloser(bytes.NewReader(tt.args.mockReturnBody)),
			}, tt.args.mockReturnError)
			Client = &clientMock

			got, err := Get(tt.args.url)
			assertions.Equal(tt.wantErr, err, tt.name)
			assertions.Equal(tt.want, got, tt.name)
			clientMock.AssertCalled(t, "Get", tt.args.url)
		})
	}
}

func TestPutWithoutAuth(t *testing.T) {
	assertions := require.New(t)

	clientMock := mocks.HTTPClient{}
	clientMock.On("Do", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusOK,
	}, nil)
	Client = &clientMock

	error := PutWithoutAuth("url", []byte("body"))

	assertions.Nil(error)
	var actualRequest *http.Request
	clientMock.AssertCalled(t, "Do", mock.MatchedBy(func(req *http.Request) bool {
		actualRequest = req
		return true
	}))
	assertions.Equal(http.MethodPut, actualRequest.Method)
	assertions.Equal("url", actualRequest.URL.Path)
	assertions.Equal("application/json; charset=utf-8", actualRequest.Header.Get("Content-Type"))
	assertions.Empty(actualRequest.Header.Get("Authorization"))
	body, _ := ioutil.ReadAll(actualRequest.Body)
	expectedBody := []byte("body")
	assertions.Equal(expectedBody, body)
	clientMock.AssertNumberOfCalls(t, "Do", 1)
}

func TestPut(t *testing.T) {
	assertions := require.New(t)

	clientMock := mocks.HTTPClient{}
	clientMock.On("Do", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusOK,
	}, nil)
	Client = &clientMock

	error := Put("url", "body", "admin", "pwd")

	assertions.Nil(error)
	var actualRequest *http.Request
	clientMock.AssertCalled(t, "Do", mock.MatchedBy(func(req *http.Request) bool {
		actualRequest = req
		return true
	}))
	assertions.Equal(http.MethodPut, actualRequest.Method)
	assertions.Equal("url", actualRequest.URL.Path)
	assertions.Equal("application/json; charset=utf-8", actualRequest.Header.Get("Content-Type"))
	tempRequest, _ := http.NewRequest("", "", nil)
	tempRequest.SetBasicAuth("admin", "pwd")
	assertions.Equal(tempRequest.Header.Get("Authorization"), actualRequest.Header.Get("Authorization"))
	body, _ := ioutil.ReadAll(actualRequest.Body)
	expectedBody := []byte("body")
	assertions.Equal(expectedBody, body)
	clientMock.AssertNumberOfCalls(t, "Do", 1)
}

func TestDelete(t *testing.T) {
	assertions := require.New(t)

	clientMock := mocks.HTTPClient{}
	clientMock.On("Do", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusOK,
	}, nil)
	Client = &clientMock

	error := Delete("url")

	assertions.Nil(error)
	var actualRequest *http.Request
	clientMock.AssertCalled(t, "Do", mock.MatchedBy(func(req *http.Request) bool {
		actualRequest = req
		return true
	}))
	assertions.Equal(http.MethodDelete, actualRequest.Method)
	assertions.Equal("url", actualRequest.URL.Path)
	assertions.Empty(actualRequest.Header.Get("Content-Type"))
	assertions.Empty(actualRequest.Header.Get("Authorization"))
	assertions.Equal(http.NoBody, actualRequest.Body)
	clientMock.AssertNumberOfCalls(t, "Do", 1)
}

func Test_doErrorCases(t *testing.T) {
	assertions := require.New(t)

	type args struct {
		url              string
		mockReturnStatus int
		mockReturnBody   []byte
		mockReturnError  error
	}
	tests := []struct {
		name    string
		args    args
		wantErr error
	}{
		{
			name: "Bad request should get RequestError",
			args: args{
				url:              "badRequest",
				mockReturnStatus: http.StatusBadRequest,
				mockReturnBody:   []byte("bad request"),
				mockReturnError:  nil,
			},
			wantErr: RequestError{
				StatusCode: http.StatusBadRequest,
				Body:       []byte("bad request"),
			},
		},
		{
			name: "Server unavailable should get error",
			args: args{
				url:             "serverUnavailable",
				mockReturnError: fmt.Errorf("Server unavailable"),
			},
			wantErr: fmt.Errorf("Server unavailable"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			clientMock := mocks.HTTPClient{}
			clientMock.On("Do", mock.Anything).Return(&http.Response{
				StatusCode: tt.args.mockReturnStatus,
				Body:       ioutil.NopCloser(bytes.NewReader(tt.args.mockReturnBody)),
			}, tt.args.mockReturnError)
			Client = &clientMock

			err := do("PUT", tt.args.url, nil)
			assertions.Equal(tt.wantErr, err, tt.name)
		})
	}
}
