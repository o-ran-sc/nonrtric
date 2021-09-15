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

package server

import (
	"io"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestStatusHandler(t *testing.T) {
	assertions := require.New(t)
	type args struct {
		responseRecorder *httptest.ResponseRecorder
		r                *http.Request
	}
	tests := []struct {
		name         string
		args         args
		wantedStatus int
		wantedBody   string
	}{
		{
			name: "StatusHandler with correct path and method, should return OK",
			args: args{
				responseRecorder: httptest.NewRecorder(),
				r:                newRequest("GET", "/", nil, t),
			},
			wantedStatus: http.StatusOK,
			wantedBody:   "All is well!",
		},
		{
			name: "StatusHandler with incorrect path, should return NotFound",
			args: args{
				responseRecorder: httptest.NewRecorder(),
				r:                newRequest("GET", "/wrong", nil, t),
			},
			wantedStatus: http.StatusNotFound,
			wantedBody:   "404 not found.\n",
		},
		{
			name: "StatusHandler with incorrect method, should return NotFound",
			args: args{
				responseRecorder: httptest.NewRecorder(),
				r:                newRequest("PUT", "/", nil, t),
			},
			wantedStatus: http.StatusNotFound,
			wantedBody:   "Method is not supported.\n",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			handler := http.HandlerFunc(StatusHandler)
			handler.ServeHTTP(tt.args.responseRecorder, tt.args.r)
			assertions.Equal(tt.wantedStatus, tt.args.responseRecorder.Code)

			assertions.Equal(tt.wantedBody, tt.args.responseRecorder.Body.String())
		})
	}
}

func newRequest(method string, url string, body io.Reader, t *testing.T) *http.Request {
	if req, err := http.NewRequest(method, url, body); err == nil {
		return req
	} else {
		t.Fatalf("Could not create request due to: %v", err)
		return nil
	}
}
