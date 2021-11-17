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

func TestJobsManagerGetTypes_filesOkShouldReturnSliceOfTypesAndProvideSupportedTypes(t *testing.T) {
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

func TestJobsManagerAddJobWhenTypeIsSupported_shouldAddJobToChannel(t *testing.T) {
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
	jobsHandler := jobsHandler{
		addJobCh: make(chan JobInfo)}
	managerUnderTest.allTypes["type1"] = TypeData{
		TypeId:      "type1",
		jobsHandler: &jobsHandler,
	}

	var err error
	go func() {
		err = managerUnderTest.AddJobFromRESTCall(wantedJob)
	}()

	assertions.Nil(err)
	addedJob := <-jobsHandler.addJobCh
	assertions.Equal(wantedJob, addedJob)
}

func TestJobsManagerAddJobWhenTypeIsNotSupported_shouldReturnError(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl("", nil, "", nil)
	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
	}

	err := managerUnderTest.AddJobFromRESTCall(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("type not supported: type1", err.Error())
}

func TestJobsManagerAddJobWhenJobIdMissing_shouldReturnError(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl("", nil, "", nil)
	managerUnderTest.allTypes["type1"] = TypeData{
		TypeId: "type1",
	}

	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
	}
	err := managerUnderTest.AddJobFromRESTCall(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("missing required job identity: {    <nil> type1}", err.Error())
}

func TestJobsManagerAddJobWhenTargetUriMissing_shouldReturnError(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl("", nil, "", nil)
	managerUnderTest.allTypes["type1"] = TypeData{
		TypeId: "type1",
	}

	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
		InfoJobIdentity:  "job1",
	}
	err := managerUnderTest.AddJobFromRESTCall(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("missing required target URI: {  job1  <nil> type1}", err.Error())
}

func TestJobsManagerDeleteJob_shouldSendDeleteToChannel(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl("", nil, "", nil)
	jobsHandler := jobsHandler{
		deleteJobCh: make(chan string)}
	managerUnderTest.allTypes["type1"] = TypeData{
		TypeId:      "type1",
		jobsHandler: &jobsHandler,
	}

	go managerUnderTest.DeleteJobFromRESTCall("job2")

	assertions.Equal("job2", <-jobsHandler.deleteJobCh)
}

func TestAddJobToJobsManager_shouldStartPollAndDistributeMessages(t *testing.T) {
	assertions := require.New(t)

	called := false
	messages := `[{"message": {"data": "data"}}]`
	pollClientMock := NewTestClient(func(req *http.Request) *http.Response {
		if req.URL.String() == "http://mrAddr/topicUrl" {
			assertions.Equal(req.Method, "GET")
			body := "[]"
			if !called {
				called = true
				body = messages
			}
			return &http.Response{
				StatusCode: 200,
				Body:       ioutil.NopCloser(bytes.NewReader([]byte(body))),
				Header:     make(http.Header), // Must be set to non-nil value or it panics
			}
		}
		t.Error("Wrong call to client: ", req)
		t.Fail()
		return nil
	})

	wg := sync.WaitGroup{}
	distributeClientMock := NewTestClient(func(req *http.Request) *http.Response {
		if req.URL.String() == "http://consumerHost/target" {
			assertions.Equal(req.Method, "POST")
			assertions.Equal(messages, getBodyAsString(req))
			assertions.Equal("application/json", req.Header.Get("Content-Type"))
			wg.Done()
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
	jobsHandler := newJobsHandler("type1", "/topicUrl", pollClientMock, distributeClientMock)

	jobsManager := NewJobsManagerImpl("", pollClientMock, "http://mrAddr", distributeClientMock)
	jobsManager.allTypes["type1"] = TypeData{
		DMaaPTopicURL: "/topicUrl",
		TypeId:        "type1",
		jobsHandler:   jobsHandler,
	}

	jobsManager.StartJobsForAllTypes()

	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
		InfoJobIdentity:  "job1",
		TargetUri:        "http://consumerHost/target",
	}

	wg.Add(1) // Wait till the distribution has happened
	jobsManager.AddJobFromRESTCall(jobInfo)

	if waitTimeout(&wg, 2*time.Second) {
		t.Error("Not all calls to server were made")
		t.Fail()
	}
}

func TestJobsHandlerDeleteJob_shouldDeleteJobFromJobsMap(t *testing.T) {
	jobToDelete := newJob(JobInfo{}, nil)
	go jobToDelete.start()
	jobsHandler := newJobsHandler("type1", "/topicUrl", nil, nil)
	jobsHandler.jobs["job1"] = jobToDelete

	go jobsHandler.monitorManagementChannels()

	jobsHandler.deleteJobCh <- "job1"

	deleted := false
	for i := 0; i < 100; i++ {
		if len(jobsHandler.jobs) == 0 {
			deleted = true
			break
		}
		time.Sleep(time.Microsecond) // Need to drop control to let the job's goroutine do the job
	}
	require.New(t).True(deleted, "Job not deleted")
}

func TestJobsHandlerEmptyJobMessageBufferWhenItIsFull(t *testing.T) {
	job := newJob(JobInfo{
		InfoJobIdentity: "job",
	}, nil)

	jobsHandler := newJobsHandler("type1", "/topicUrl", nil, nil)
	jobsHandler.jobs["job1"] = job

	fillMessagesBuffer(job.messagesChannel)

	jobsHandler.distributeMessages([]byte("sent msg"))

	require.New(t).Len(job.messagesChannel, 0)
}

func fillMessagesBuffer(mc chan []byte) {
	for i := 0; i < cap(mc); i++ {
		mc <- []byte("msg")
	}
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
