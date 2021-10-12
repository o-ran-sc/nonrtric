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
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"

	"github.com/gorilla/mux"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/jobs"
)

const StatusPath = "/status"
const AddJobPath = "/jobs"
const jobIdToken = "infoJobId"
const deleteJobPath = AddJobPath + "/{" + jobIdToken + "}"

type ProducerCallbackHandler struct {
	jobHandler jobs.JobHandler
}

func NewProducerCallbackHandler(jh jobs.JobHandler) *ProducerCallbackHandler {
	return &ProducerCallbackHandler{
		jobHandler: jh,
	}
}

func NewRouter(jh jobs.JobHandler) *mux.Router {
	callbackHandler := NewProducerCallbackHandler(jh)
	r := mux.NewRouter()
	r.HandleFunc(StatusPath, statusHandler).Methods(http.MethodGet).Name("status")
	r.HandleFunc(AddJobPath, callbackHandler.addInfoJobHandler).Methods(http.MethodPost).Name("add")
	r.HandleFunc(deleteJobPath, callbackHandler.deleteInfoJobHandler).Methods(http.MethodDelete).Name("delete")
	r.NotFoundHandler = &notFoundHandler{}
	r.MethodNotAllowedHandler = &methodNotAllowedHandler{}
	return r
}

func statusHandler(w http.ResponseWriter, r *http.Request) {
	// Just respond OK to show the server is alive for now. Might be extended later.
}

func (h *ProducerCallbackHandler) addInfoJobHandler(w http.ResponseWriter, r *http.Request) {
	b, readErr := ioutil.ReadAll(r.Body)
	if readErr != nil {
		http.Error(w, fmt.Sprintf("Unable to read body due to: %v", readErr), http.StatusBadRequest)
		return
	}
	jobInfo := jobs.JobInfo{}
	if unmarshalErr := json.Unmarshal(b, &jobInfo); unmarshalErr != nil {
		http.Error(w, fmt.Sprintf("Invalid json body. Cause: %v", unmarshalErr), http.StatusBadRequest)
		return
	}
	if err := h.jobHandler.AddJob(jobInfo); err != nil {
		http.Error(w, fmt.Sprintf("Invalid job info. Cause: %v", err), http.StatusBadRequest)
	}
}

func (h *ProducerCallbackHandler) deleteInfoJobHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, ok := vars[jobIdToken]
	if !ok {
		http.Error(w, "Must provide infoJobId.", http.StatusBadRequest)
		return
	}

	h.jobHandler.DeleteJob(id)
}

type notFoundHandler struct{}

func (h *notFoundHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	http.Error(w, "404 not found.", http.StatusNotFound)
}

type methodNotAllowedHandler struct{}

func (h *methodNotAllowedHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	http.Error(w, "Method is not supported.", http.StatusMethodNotAllowed)
}
