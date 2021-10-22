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

func TestNewRouter(t *testing.T) {
	assertions := require.New(t)

	r := NewRouter(nil)
	statusRoute := r.Get("status")
	assertions.NotNil(statusRoute)
	supportedMethods, err := statusRoute.GetMethods()
	assertions.Equal([]string{http.MethodGet}, supportedMethods)
	assertions.Nil(err)
	path, _ := statusRoute.GetPathTemplate()
	assertions.Equal("/status", path)

	addJobRoute := r.Get("add")
	assertions.NotNil(addJobRoute)
	supportedMethods, err = addJobRoute.GetMethods()
	assertions.Equal([]string{http.MethodPost}, supportedMethods)
	assertions.Nil(err)
	path, _ = addJobRoute.GetPathTemplate()
	assertions.Equal("/jobs", path)

	deleteJobRoute := r.Get("delete")
	assertions.NotNil(deleteJobRoute)
	supportedMethods, err = deleteJobRoute.GetMethods()
	assertions.Equal([]string{http.MethodDelete}, supportedMethods)
	assertions.Nil(err)
	path, _ = deleteJobRoute.GetPathTemplate()
	assertions.Equal("/jobs/{infoJobId}", path)

	notFoundHandler := r.NotFoundHandler
	handler := http.HandlerFunc(notFoundHandler.ServeHTTP)
	responseRecorder := httptest.NewRecorder()
	handler.ServeHTTP(responseRecorder, newRequest("GET", "/wrong", nil, t))
	assertions.Equal(http.StatusNotFound, responseRecorder.Code)
	assertions.Contains(responseRecorder.Body.String(), "404 not found.")

	methodNotAllowedHandler := r.MethodNotAllowedHandler
	handler = http.HandlerFunc(methodNotAllowedHandler.ServeHTTP)
	responseRecorder = httptest.NewRecorder()
	handler.ServeHTTP(responseRecorder, newRequest(http.MethodPut, "/status", nil, t))
	assertions.Equal(http.StatusMethodNotAllowed, responseRecorder.Code)
	assertions.Contains(responseRecorder.Body.String(), "Method is not supported.")
}

func TestStatusHandler(t *testing.T) {
	assertions := require.New(t)

	handler := http.HandlerFunc(statusHandler)
	responseRecorder := httptest.NewRecorder()
	r := newRequest(http.MethodGet, "/status", nil, t)

	handler.ServeHTTP(responseRecorder, r)

	assertions.Equal(http.StatusOK, responseRecorder.Code)
	assertions.Equal("", responseRecorder.Body.String())
}

func TestAddInfoJobHandler(t *testing.T) {
	assertions := require.New(t)

	type args struct {
		job        jobs.JobInfo
		mockReturn error
	}
	tests := []struct {
		name         string
		args         args
		wantedStatus int
		wantedBody   string
	}{
		{
			name: "AddInfoJobHandler with correct job, should return OK",
			args: args{
				job: jobs.JobInfo{
					Owner:            "owner",
					LastUpdated:      "now",
					InfoJobIdentity:  "jobId",
					TargetUri:        "target",
					InfoJobData:      "{}",
					InfoTypeIdentity: "type",
				},
			},
			wantedStatus: http.StatusOK,
			wantedBody:   "",
		},
		{
			name: "AddInfoJobHandler with incorrect job info, should return BadRequest",
			args: args{
				job: jobs.JobInfo{
					Owner: "bad",
				},
				mockReturn: errors.New("error"),
			},
			wantedStatus: http.StatusBadRequest,
			wantedBody:   "Invalid job info. Cause: error",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			jobHandlerMock := jobhandler.JobHandler{}
			jobHandlerMock.On("AddJob", tt.args.job).Return(tt.args.mockReturn)

			callbackHandlerUnderTest := NewProducerCallbackHandler(&jobHandlerMock)

			handler := http.HandlerFunc(callbackHandlerUnderTest.addInfoJobHandler)
			responseRecorder := httptest.NewRecorder()
			r := newRequest(http.MethodPost, "/jobs", &tt.args.job, t)

			handler.ServeHTTP(responseRecorder, r)

			assertions.Equal(tt.wantedStatus, responseRecorder.Code, tt.name)
			assertions.Contains(responseRecorder.Body.String(), tt.wantedBody, tt.name)
			jobHandlerMock.AssertCalled(t, "AddJob", tt.args.job)
		})
	}
}

func TestDeleteJob(t *testing.T) {
	assertions := require.New(t)
	jobHandlerMock := jobhandler.JobHandler{}
	jobHandlerMock.On("DeleteJob", mock.Anything).Return(nil)

	callbackHandlerUnderTest := NewProducerCallbackHandler(&jobHandlerMock)

	responseRecorder := httptest.NewRecorder()
	r := mux.SetURLVars(newRequest(http.MethodDelete, "/jobs/", nil, t), map[string]string{"infoJobId": "job1"})
	handler := http.HandlerFunc(callbackHandlerUnderTest.deleteInfoJobHandler)
	handler.ServeHTTP(responseRecorder, r)
	assertions.Equal(http.StatusOK, responseRecorder.Result().StatusCode)

	assertions.Equal("", responseRecorder.Body.String())

	jobHandlerMock.AssertCalled(t, "DeleteJob", "job1")
}

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
