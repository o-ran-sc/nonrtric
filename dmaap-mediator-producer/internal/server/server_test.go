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
	"bytes"
	"encoding/json"
	"errors"
	"io"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gorilla/mux"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/jobs"
	"oransc.org/nonrtric/dmaapmediatorproducer/mocks/jobhandler"
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
				r:                newRequest("GET", "/status", nil, t),
			},
			wantedStatus: http.StatusOK,
			wantedBody:   "",
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
			name: "StatusHandler with incorrect method, should return MethodNotAllowed",
			args: args{
				responseRecorder: httptest.NewRecorder(),
				r:                newRequest("PUT", "/status", nil, t),
			},
			wantedStatus: http.StatusMethodNotAllowed,
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

func TestAddInfoJobHandler(t *testing.T) {
	assertions := require.New(t)
	jobHandlerMock := jobhandler.JobHandler{}

	goodJobInfo := jobs.JobInfo{
		Owner:            "owner",
		LastUpdated:      "now",
		InfoJobIdentity:  "jobId",
		TargetUri:        "target",
		InfoJobData:      "{}",
		InfoTypeIdentity: "type",
	}
	badJobInfo := jobs.JobInfo{
		Owner: "bad",
	}
	jobHandlerMock.On("AddJob", goodJobInfo).Return(nil)
	jobHandlerMock.On("AddJob", badJobInfo).Return(errors.New("error"))
	jobs.Handler = &jobHandlerMock

	type args struct {
		responseRecorder *httptest.ResponseRecorder
		r                *http.Request
	}
	tests := []struct {
		name         string
		args         args
		wantedStatus int
		wantedBody   string
		assertFunc   assertMockFunk
	}{
		{
			name: "AddInfoJobHandler with correct path and method, should return OK",
			args: args{
				responseRecorder: httptest.NewRecorder(),
				r:                newRequest("POST", "/jobs", &goodJobInfo, t),
			},
			wantedStatus: http.StatusOK,
			wantedBody:   "",
			assertFunc: func(mock *jobhandler.JobHandler) {
				mock.AssertCalled(t, "AddJob", goodJobInfo)
			},
		},
		{
			name: "AddInfoJobHandler with incorrect job info, should return BadRequest",
			args: args{
				responseRecorder: httptest.NewRecorder(),
				r:                newRequest("POST", "/jobs", &badJobInfo, t),
			},
			wantedStatus: http.StatusBadRequest,
			wantedBody:   "Invalid job info. Cause: error",
		},
		{
			name: "AddInfoJobHandler with incorrect path, should return NotFound",
			args: args{
				responseRecorder: httptest.NewRecorder(),
				r:                newRequest("GET", "/wrong", nil, t),
			},
			wantedStatus: http.StatusNotFound,
			wantedBody:   "404 not found.",
		},
		{
			name: "AddInfoJobHandler with incorrect method, should return MethodNotAllowed",
			args: args{
				responseRecorder: httptest.NewRecorder(),
				r:                newRequest("PUT", "/jobs", nil, t),
			},
			wantedStatus: http.StatusMethodNotAllowed,
			wantedBody:   "Method is not supported.",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			handler := http.HandlerFunc(AddInfoJobHandler)
			handler.ServeHTTP(tt.args.responseRecorder, tt.args.r)
			assertions.Equal(tt.wantedStatus, tt.args.responseRecorder.Code, tt.name)

			assertions.Contains(tt.args.responseRecorder.Body.String(), tt.wantedBody, tt.name)

			if tt.assertFunc != nil {
				tt.assertFunc(&jobHandlerMock)
			}
		})
	}
}

func TestDeleteJob(t *testing.T) {
	assertions := require.New(t)
	jobHandlerMock := jobhandler.JobHandler{}

	jobHandlerMock.On("DeleteJob", mock.Anything).Return(nil)
	jobs.Handler = &jobHandlerMock

	type args struct {
		responseRecorder *httptest.ResponseRecorder
		r                *http.Request
	}
	tests := []struct {
		name         string
		args         args
		wantedStatus int
		wantedBody   string
		assertFunc   assertMockFunk
	}{
		{
			name: "Delete job with correct path and method, should return OK",
			args: args{
				responseRecorder: httptest.NewRecorder(),
				r:                mux.SetURLVars(newRequest("DELETE", "/jobs/", nil, t), map[string]string{"infoJobId": "job1"}),
			},
			wantedStatus: http.StatusOK,
			wantedBody:   "",
			assertFunc: func(mock *jobhandler.JobHandler) {
				mock.AssertCalled(t, "DeleteJob", "job1")
			},
		},
		{
			name: "Delete job with incorrect path, should return BadRequest",
			args: args{
				responseRecorder: httptest.NewRecorder(),
				r:                newRequest("DELETE", "/jobs", nil, t),
			},
			wantedStatus: http.StatusBadRequest,
			wantedBody:   "Must provide infoJobId.\n",
		},
		{
			name: "StatusHandler with incorrect method, should return MethodNotAllowed",
			args: args{
				responseRecorder: httptest.NewRecorder(),
				r:                newRequest("PUT", "/jobs/gurka", nil, t),
			},
			wantedStatus: http.StatusMethodNotAllowed,
			wantedBody:   "Method is not supported.\n",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			handler := http.HandlerFunc(DeleteInfoJobHandler)
			handler.ServeHTTP(tt.args.responseRecorder, tt.args.r)
			assertions.Equal(tt.wantedStatus, tt.args.responseRecorder.Code, tt.name)

			assertions.Equal(tt.wantedBody, tt.args.responseRecorder.Body.String(), tt.name)

			if tt.assertFunc != nil {
				tt.assertFunc(&jobHandlerMock)
			}
		})
	}
}

type assertMockFunk func(mock *jobhandler.JobHandler)

func newRequest(method string, url string, jobInfo *jobs.JobInfo, t *testing.T) *http.Request {
	var body io.Reader
	if jobInfo != nil {
		bodyAsBytes, _ := json.Marshal(jobInfo)
		body = ioutil.NopCloser(bytes.NewReader(bodyAsBytes))
	}
	if req, err := http.NewRequest(method, url, body); err == nil {
		return req
	} else {
		t.Fatalf("Could not create request due to: %v", err)
		return nil
	}
}
