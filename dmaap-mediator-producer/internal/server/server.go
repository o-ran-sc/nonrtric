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
const DeleteJobPath = AddJobPath + "/{" + jobIdToken + "}"

func StatusHandler(w http.ResponseWriter, r *http.Request) {
	if r.URL.Path != StatusPath {
		http.Error(w, "404 not found.", http.StatusNotFound)
		return
	}

	if r.Method != "GET" {
		http.Error(w, "Method is not supported.", http.StatusMethodNotAllowed)
		return
	}

	fmt.Fprintf(w, "All is well!")
}

func AddInfoJobHandler(w http.ResponseWriter, r *http.Request) {
	if r.URL.Path != AddJobPath {
		http.Error(w, "404 not found.", http.StatusNotFound)
		return
	}

	if r.Method != "POST" {
		http.Error(w, "Method is not supported.", http.StatusMethodNotAllowed)
		return
	}

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
	if err := jobs.AddJob(jobInfo); err != nil {
		http.Error(w, fmt.Sprintf("Invalid job info. Cause: %v", err), http.StatusBadRequest)
	}
}

func DeleteInfoJobHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "DELETE" {
		http.Error(w, "Method is not supported.", http.StatusMethodNotAllowed)
		return
	}

	vars := mux.Vars(r)
	id, ok := vars[jobIdToken]
	if !ok {
		http.Error(w, "Must provide infoJobId.", http.StatusBadRequest)
		return
	}

	jobs.DeleteJob(id)
}
