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
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestNewRequest(t *testing.T) {
	assertions := require.New(t)

	bodyBytes, _ := json.Marshal("body")
	succesfullReq, _ := http.NewRequest(http.MethodGet, "url", bytes.NewReader(bodyBytes))

	type args struct {
		method  string
		path    string
		payload interface{}
	}
	tests := []struct {
		name    string
		args    args
		want    *http.Request
		wantErr error
	}{
		{
			name: "succesfull newRequest",
			args: args{
				method:  http.MethodGet,
				path:    "url",
				payload: "body",
			},
			want:    succesfullReq,
			wantErr: nil,
		},
		{
			name: "request failed json marshal",
			args: args{
				method: http.MethodGet,
				path:   "url",
				payload: map[string]interface{}{
					"foo": make(chan int),
				},
			},
			want:    nil,
			wantErr: fmt.Errorf("failed to marshal request body: json: unsupported type: chan int"),
		},
		{
			name: "request failed calling newRequest",
			args: args{
				method:  "*?",
				path:    "url",
				payload: "body",
			},
			want:    nil,
			wantErr: fmt.Errorf("failed to create HTTP request: net/http: invalid method \"*?\""),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			client := New(&http.Client{})

			req, err := client.newRequest(tt.args.method, tt.args.path, tt.args.payload)
			if tt.wantErr != nil {
				assertions.Equal(tt.want, req)
				assertions.EqualError(tt.wantErr, err.Error())
			} else {
				assertions.Equal("url", req.URL.Path)
				assertions.Equal("application/json; charset=utf-8", req.Header.Get("Content-Type"))
				assertions.Empty(req.Header.Get("Authorization"))
				assertions.Nil(err)
			}

		})
	}
}

func TestGet(t *testing.T) {
	assertions := require.New(t)
	type args struct {
		header   string
		respCode int
		resp     interface{}
	}
	tests := []struct {
		name    string
		args    args
		wantErr string
	}{
		{
			name: "successful GET request",
			args: args{
				header:   "application/json",
				respCode: http.StatusOK,
				resp:     "Success!",
			},
			wantErr: "",
		},
		{
			name: "error GET request",
			args: args{
				header:   "application/json",
				respCode: http.StatusBadRequest,
				resp:     nil,
			},
			wantErr: "failed to do request, 400 status code received",
		},
	}

	for _, tt := range tests {

		t.Run(tt.name, func(t *testing.T) {
			srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				response, _ := json.Marshal(tt.args.resp)
				w.Header().Set("Content-Type", tt.args.header)
				w.WriteHeader(tt.args.respCode)
				w.Write(response)
			}))
			defer srv.Close()

			client := New(&http.Client{})
			var res interface{}
			err := client.Get(srv.URL, &res)

			if err != nil {
				assertions.Equal(tt.wantErr, err.Error())
			}
			assertions.Equal(tt.args.resp, res)
		})
	}
}

func TestPost(t *testing.T) {
	assertions := require.New(t)
	type args struct {
		header   string
		respCode int
		resp     interface{}
	}
	tests := []struct {
		name    string
		args    args
		wantErr string
	}{
		{
			name: "successful Post request",
			args: args{
				header:   "application/json",
				respCode: http.StatusOK,
				resp:     "Success!",
			},
			wantErr: "",
		},
	}

	for _, tt := range tests {

		t.Run(tt.name, func(t *testing.T) {
			srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {

				assertions.Equal(http.MethodPost, r.Method)
				assertions.Contains(r.Header.Get("Content-Type"), "application/json")

				var reqBody interface{}
				decoder := json.NewDecoder(r.Body)
				decoder.Decode(&reqBody)
				assertions.Equal(reqBody, `json:"example"`)

				response, _ := json.Marshal(tt.args.resp)
				w.Header().Set("Content-Type", tt.args.header)
				w.WriteHeader(tt.args.respCode)
				w.Write(response)
			}))
			defer srv.Close()

			client := New(&http.Client{})
			payload := `json:"example"`
			err := client.Post(srv.URL, payload, nil)

			if err != nil {
				assertions.Equal(tt.wantErr, err.Error())
			}
		})
	}
}
