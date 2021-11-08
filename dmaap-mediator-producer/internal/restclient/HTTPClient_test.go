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
	"crypto/tls"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"reflect"
	"testing"
	"time"

	"github.com/hashicorp/go-retryablehttp"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"oransc.org/nonrtric/dmaapmediatorproducer/mocks/httpclient"
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
		mockReturnBody   string
		mockReturnError  error
	}
	tests := []struct {
		name        string
		args        args
		want        []byte
		wantedError error
	}{
		{
			name: "Test Get with OK response",
			args: args{
				url:              "http://testOk",
				mockReturnStatus: http.StatusOK,
				mockReturnBody:   "Response",
			},
			want: []byte("Response"),
		},
		{
			name: "Test Get with Not OK response",
			args: args{
				url:              "http://testNotOk",
				mockReturnStatus: http.StatusBadRequest,
				mockReturnBody:   "Bad Response",
			},
			want: nil,
			wantedError: RequestError{
				StatusCode: http.StatusBadRequest,
				Body:       []byte("Bad Response"),
			},
		},
		{
			name: "Test Get with error",
			args: args{
				url:             "http://testError",
				mockReturnError: errors.New("Failed Request"),
			},
			want:        nil,
			wantedError: errors.New("Failed Request"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			clientMock := httpclient.HTTPClient{}
			clientMock.On("Get", tt.args.url).Return(&http.Response{
				StatusCode: tt.args.mockReturnStatus,
				Body:       ioutil.NopCloser(bytes.NewReader([]byte(tt.args.mockReturnBody))),
			}, tt.args.mockReturnError)

			got, err := Get(tt.args.url, &clientMock)
			assertions.Equal(tt.wantedError, err, tt.name)
			assertions.Equal(tt.want, got, tt.name)
			clientMock.AssertCalled(t, "Get", tt.args.url)
		})
	}
}

func TestPutOk(t *testing.T) {
	assertions := require.New(t)
	clientMock := httpclient.HTTPClient{}

	clientMock.On("Do", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusOK,
	}, nil)

	if err := Put("http://localhost:9990", []byte("body"), &clientMock); err != nil {
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
	assertions.Equal("application/json", actualRequest.Header.Get("Content-Type"))
	body, _ := ioutil.ReadAll(actualRequest.Body)
	expectedBody := []byte("body")
	assertions.Equal(expectedBody, body)
	clientMock.AssertNumberOfCalls(t, "Do", 1)
}

func TestPostOk(t *testing.T) {
	assertions := require.New(t)
	clientMock := httpclient.HTTPClient{}

	clientMock.On("Do", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusOK,
	}, nil)

	if err := Post("http://localhost:9990", []byte("body"), &clientMock); err != nil {
		t.Errorf("Put() error = %v, did not want error", err)
	}
	var actualRequest *http.Request
	clientMock.AssertCalled(t, "Do", mock.MatchedBy(func(req *http.Request) bool {
		actualRequest = req
		return true
	}))
	assertions.Equal(http.MethodPost, actualRequest.Method)
	assertions.Equal("http", actualRequest.URL.Scheme)
	assertions.Equal("localhost:9990", actualRequest.URL.Host)
	assertions.Equal("application/json", actualRequest.Header.Get("Content-Type"))
	body, _ := ioutil.ReadAll(actualRequest.Body)
	expectedBody := []byte("body")
	assertions.Equal(expectedBody, body)
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
			clientMock := httpclient.HTTPClient{}
			clientMock.On("Do", mock.Anything).Return(&http.Response{
				StatusCode: tt.args.mockReturnStatus,
				Body:       ioutil.NopCloser(bytes.NewReader(tt.args.mockReturnBody)),
			}, tt.args.mockReturnError)
			err := do("PUT", tt.args.url, nil, &clientMock)
			assertions.Equal(tt.wantErr, err, tt.name)
		})
	}
}

func Test_createClientCertificate(t *testing.T) {
	assertions := require.New(t)
	wantedCert, _ := tls.LoadX509KeyPair("../../security/producer.crt", "../../security/producer.key")
	type args struct {
		certPath string
		keyPath  string
	}
	tests := []struct {
		name     string
		args     args
		wantCert *tls.Certificate
		wantErr  error
	}{
		{
			name: "Paths to cert info ok should return cerftificate",
			args: args{
				certPath: "../../security/producer.crt",
				keyPath:  "../../security/producer.key",
			},
			wantCert: &wantedCert,
		},
		{
			name: "Paths to cert info not ok should return error with info about error",
			args: args{
				certPath: "wrong_cert",
				keyPath:  "wrong_key",
			},
			wantErr: fmt.Errorf("cannot create x509 keypair from cert file wrong_cert and key file wrong_key due to: open wrong_cert: no such file or directory"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cert, err := CreateClientCertificate(tt.args.certPath, tt.args.keyPath)
			assertions.Equal(tt.wantCert, cert, tt.name)
			assertions.Equal(tt.wantErr, err, tt.name)
		})
	}
}

func Test_CreateRetryClient(t *testing.T) {
	assertions := require.New(t)

	client := CreateRetryClient(&tls.Certificate{}, 5*time.Second)

	transport := client.Transport
	assertions.Equal("*retryablehttp.RoundTripper", reflect.TypeOf(transport).String())
	retryableTransport := transport.(*retryablehttp.RoundTripper)
	retryableClient := retryableTransport.Client
	assertions.Equal(time.Minute, retryableClient.RetryWaitMax)
	assertions.Equal(int(^uint(0)>>1), retryableClient.RetryMax)
	assertions.Equal(5*time.Second, client.Timeout)
}

func Test_CreateClientWithoutRetry(t *testing.T) {
	assertions := require.New(t)

	client := CreateClientWithoutRetry(&tls.Certificate{}, 5*time.Second)

	transport := client.Transport
	assertions.Equal("*http.Transport", reflect.TypeOf(transport).String())
	assertions.Equal(5*time.Second, client.Timeout)
}

func TestIsUrlSecured(t *testing.T) {
	assertions := require.New(t)

	assertions.True(IsUrlSecure("https://url"))

	assertions.False(IsUrlSecure("http://url"))
}
