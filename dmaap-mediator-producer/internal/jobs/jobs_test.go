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

package jobs

import (
	"bytes"
	"io/ioutil"
	"net/http"
	"os"
	"path/filepath"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/config"
)

const typeDefinition = `{"types": [{"id": "type1", "dmaapTopicUrl": "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/type1"}]}`

func TestGetTypes_filesOkShouldReturnSliceOfTypesAndProvideSupportedTypes(t *testing.T) {
	assertions := require.New(t)
	typesDir, err := os.MkdirTemp("", "configs")
	if err != nil {
		t.Errorf("Unable to create temporary directory for types due to: %v", err)
	}
	fname := filepath.Join(typesDir, "type_config.json")
	managerUnderTest := NewJobsManagerImpl(fname, nil, "", nil)
	t.Cleanup(func() {
		os.RemoveAll(typesDir)
	})
	if err = os.WriteFile(fname, []byte(typeDefinition), 0666); err != nil {
		t.Errorf("Unable to create temporary config file for types due to: %v", err)
	}
	types, err := managerUnderTest.LoadTypesFromConfiguration()
	wantedType := config.TypeDefinition{
		Id:            "type1",
		DmaapTopicURL: "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/type1",
	}
	wantedTypes := []config.TypeDefinition{wantedType}
	assertions.EqualValues(wantedTypes, types)
	assertions.Nil(err)

	supportedTypes := managerUnderTest.GetSupportedTypes()
	assertions.EqualValues([]string{"type1"}, supportedTypes)
}

func TestManagerAddJobWhenTypeIsSupported_shouldAddJobToChannel(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl("", nil, "", nil)
	wantedJob := JobInfo{
		Owner:            "owner",
		LastUpdated:      "now",
		InfoJobIdentity:  "job1",
		TargetUri:        "target",
		InfoJobData:      "{}",
		InfoTypeIdentity: "type1",
	}
	jobHandler := jobsHandler{
		addJobCh: make(chan JobInfo)}
	managerUnderTest.allTypes["type1"] = TypeData{
		TypeId:      "type1",
		jobsHandler: &jobHandler,
	}

	var err error
	go func() {
		err = managerUnderTest.AddJob(wantedJob)
	}()

	assertions.Nil(err)
	addedJob := <-jobHandler.addJobCh
	assertions.Equal(wantedJob, addedJob)
}

func TestManagerAddJobWhenTypeIsNotSupported_shouldReturnError(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl("", nil, "", nil)
	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
	}

	err := managerUnderTest.AddJob(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("type not supported: type1", err.Error())
}

func TestManagerAddJobWhenJobIdMissing_shouldReturnError(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl("", nil, "", nil)
	managerUnderTest.allTypes["type1"] = TypeData{
		TypeId: "type1",
	}

	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
	}
	err := managerUnderTest.AddJob(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("missing required job identity: {    <nil> type1}", err.Error())
}

func TestManagerAddJobWhenTargetUriMissing_shouldReturnError(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl("", nil, "", nil)
	managerUnderTest.allTypes["type1"] = TypeData{
		TypeId: "type1",
	}

	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
		InfoJobIdentity:  "job1",
	}
	err := managerUnderTest.AddJob(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("missing required target URI: {  job1  <nil> type1}", err.Error())
}

func TestManagerDeleteJob(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl("", nil, "", nil)
	jobHandler := jobsHandler{
		deleteJobCh: make(chan string)}
	managerUnderTest.allTypes["type1"] = TypeData{
		TypeId:      "type1",
		jobsHandler: &jobHandler,
	}

	go managerUnderTest.DeleteJob("job2")

	assertions.Equal("job2", <-jobHandler.deleteJobCh)
}

func TestHandlerPollAndDistributeMessages(t *testing.T) {
	assertions := require.New(t)

	wg := sync.WaitGroup{}
	messages := `[{"message": {"data": "data"}}]`
	pollClientMock := NewTestClient(func(req *http.Request) *http.Response {
		if req.URL.String() == "http://mrAddr/topicUrl" {
			assertions.Equal(req.Method, "GET")
			wg.Done() // Signal that the poll call has been made
			return &http.Response{
				StatusCode: 200,
				Body:       ioutil.NopCloser(bytes.NewReader([]byte(messages))),
				Header:     make(http.Header), // Must be set to non-nil value or it panics
			}
		}
		t.Error("Wrong call to client: ", req)
		t.Fail()
		return nil
	})
	distributeClientMock := NewTestClient(func(req *http.Request) *http.Response {
		if req.URL.String() == "http://consumerHost/target" {
			assertions.Equal(req.Method, "POST")
			assertions.Equal(messages, getBodyAsString(req))
			assertions.Equal("application/json", req.Header.Get("Content-Type"))
			wg.Done() // Signal that the distribution call has been made
			return &http.Response{
				StatusCode: 200,
				Body:       ioutil.NopCloser(bytes.NewBufferString(`OK`)),
				Header:     make(http.Header), // Must be set to non-nil value or it panics
			}
		}
		t.Error("Wrong call to client: ", req)
		t.Fail()
		return nil
	})

	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
		InfoJobIdentity:  "job1",
		TargetUri:        "http://consumerHost/target",
	}
	handlerUnderTest := jobsHandler{
		topicUrl:         "/topicUrl",
		jobs:             map[string]JobInfo{jobInfo.InfoJobIdentity: jobInfo},
		pollClient:       pollClientMock,
		distributeClient: distributeClientMock,
	}

	wg.Add(2) // Two calls should be made to the server, one to poll and one to distribute
	handlerUnderTest.pollAndDistributeMessages("http://mrAddr")

	if waitTimeout(&wg, 100*time.Millisecond) {
		t.Error("Not all calls to server were made")
		t.Fail()
	}
}

func TestHandlerAddJob_shouldAddJobToJobsMap(t *testing.T) {
	assertions := require.New(t)

	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
		InfoJobIdentity:  "job1",
		TargetUri:        "http://consumerHost/target",
	}

	addCh := make(chan JobInfo)
	handlerUnderTest := jobsHandler{
		mu:       sync.Mutex{},
		jobs:     map[string]JobInfo{},
		addJobCh: addCh,
	}

	go func() {
		addCh <- jobInfo
	}()

	handlerUnderTest.monitorManagementChannels()

	assertions.Len(handlerUnderTest.jobs, 1)
	assertions.Equal(jobInfo, handlerUnderTest.jobs["job1"])
}

func TestHandlerDeleteJob_shouldDeleteJobFromJobsMap(t *testing.T) {
	assertions := require.New(t)

	deleteCh := make(chan string)
	handlerUnderTest := jobsHandler{
		mu: sync.Mutex{},
		jobs: map[string]JobInfo{"job1": {
			InfoJobIdentity: "job1",
		}},
		deleteJobCh: deleteCh,
	}

	go func() {
		deleteCh <- "job1"
	}()

	handlerUnderTest.monitorManagementChannels()

	assertions.Len(handlerUnderTest.jobs, 0)
}

type RoundTripFunc func(req *http.Request) *http.Response

func (f RoundTripFunc) RoundTrip(req *http.Request) (*http.Response, error) {
	return f(req), nil
}

//NewTestClient returns *http.Client with Transport replaced to avoid making real calls
func NewTestClient(fn RoundTripFunc) *http.Client {
	return &http.Client{
		Transport: RoundTripFunc(fn),
	}
}

// waitTimeout waits for the waitgroup for the specified max timeout.
// Returns true if waiting timed out.
func waitTimeout(wg *sync.WaitGroup, timeout time.Duration) bool {
	c := make(chan struct{})
	go func() {
		defer close(c)
		wg.Wait()
	}()
	select {
	case <-c:
		return false // completed normally
	case <-time.After(timeout):
		return true // timed out
	}
}

func getBodyAsString(req *http.Request) string {
	buf := new(bytes.Buffer)
	buf.ReadFrom(req.Body)
	return buf.String()
}
