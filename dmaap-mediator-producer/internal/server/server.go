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
	log "github.com/sirupsen/logrus"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/jobs"
)

const HealthCheckPath = "/health_check"
const AddJobPath = "/info_job"
const jobIdToken = "infoJobId"
const deleteJobPath = AddJobPath + "/{" + jobIdToken + "}"
const logLevelToken = "level"
const logAdminPath = "/admin/log"

type ProducerCallbackHandler struct {
	jobsManager jobs.JobsManager
}

func NewProducerCallbackHandler(jm jobs.JobsManager) *ProducerCallbackHandler {
	return &ProducerCallbackHandler{
		jobsManager: jm,
	}
}

func NewRouter(jm jobs.JobsManager, hcf func(http.ResponseWriter, *http.Request)) *mux.Router {
	callbackHandler := NewProducerCallbackHandler(jm)
	r := mux.NewRouter()
	r.HandleFunc(HealthCheckPath, hcf).Methods(http.MethodGet).Name("health_check")
	r.HandleFunc(AddJobPath, callbackHandler.addInfoJobHandler).Methods(http.MethodPost).Name("add")
	r.HandleFunc(deleteJobPath, callbackHandler.deleteInfoJobHandler).Methods(http.MethodDelete).Name("delete")
	r.HandleFunc(logAdminPath, callbackHandler.setLogLevel).Methods(http.MethodPut).Name("setLogLevel")
	r.NotFoundHandler = &notFoundHandler{}
	r.MethodNotAllowedHandler = &methodNotAllowedHandler{}
	return r
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
	if err := h.jobsManager.AddJobFromRESTCall(jobInfo); err != nil {
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

	h.jobsManager.DeleteJobFromRESTCall(id)
}

func (h *ProducerCallbackHandler) setLogLevel(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query()
	logLevelStr := query.Get(logLevelToken)
	if loglevel, err := log.ParseLevel(logLevelStr); err == nil {
		log.SetLevel(loglevel)
	} else {
		http.Error(w, fmt.Sprintf("Invalid log level: %v. Log level will not be changed!", logLevelStr), http.StatusBadRequest)
		return
	}
}

type notFoundHandler struct{}

func (h *notFoundHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	http.Error(w, "404 not found.", http.StatusNotFound)
}

type methodNotAllowedHandler struct{}

func (h *methodNotAllowedHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	http.Error(w, "Method is not supported.", http.StatusMethodNotAllowed)
}
