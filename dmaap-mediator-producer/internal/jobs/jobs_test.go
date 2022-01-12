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
	"fmt"
	"io/ioutil"
	"net/http"
	"strconv"
	"sync"
	"testing"
	"time"

	"github.com/confluentinc/confluent-kafka-go/kafka"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/config"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/kafkaclient"
	"oransc.org/nonrtric/dmaapmediatorproducer/mocks"
)

func TestJobsManagerLoadTypesFromConfiguration_shouldReturnSliceOfTypesAndProvideSupportedTypes(t *testing.T) {
	assertions := require.New(t)

	managerUnderTest := NewJobsManagerImpl(nil, "", kafkaclient.KafkaFactoryImpl{}, nil)

	wantedDMaaPType := config.TypeDefinition{
		Identity:      "type1",
		DMaaPTopicURL: "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/type1",
	}
	wantedKafkaType := config.TypeDefinition{
		Identity:        "type2",
		KafkaInputTopic: "topic",
	}
	wantedTypes := []config.TypeDefinition{wantedDMaaPType, wantedKafkaType}

	types := managerUnderTest.LoadTypesFromConfiguration(wantedTypes)

	assertions.EqualValues(wantedTypes, types)

	supportedTypes := managerUnderTest.GetSupportedTypes()
	assertions.ElementsMatch([]string{"type1", "type2"}, supportedTypes)
	assertions.Equal(dMaaPSource, managerUnderTest.allTypes["type1"].jobsHandler.sourceType)
	assertions.Equal(kafkaSource, managerUnderTest.allTypes["type2"].jobsHandler.sourceType)
}

func TestJobsManagerAddJobWhenTypeIsSupported_shouldAddJobToChannel(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl(nil, "", kafkaclient.KafkaFactoryImpl{}, nil)
	wantedJob := JobInfo{
		Owner:            "owner",
		LastUpdated:      "now",
		InfoJobIdentity:  "job1",
		TargetUri:        "target",
		InfoJobData:      Parameters{},
		InfoTypeIdentity: "type1",
	}
	jobsHandler := jobsHandler{
		addJobCh: make(chan JobInfo)}
	managerUnderTest.allTypes["type1"] = TypeData{
		Identity:    "type1",
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
	managerUnderTest := NewJobsManagerImpl(nil, "", kafkaclient.KafkaFactoryImpl{}, nil)
	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
	}

	err := managerUnderTest.AddJobFromRESTCall(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("type not supported: type1", err.Error())
}

func TestJobsManagerAddJobWhenJobIdMissing_shouldReturnError(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl(nil, "", kafkaclient.KafkaFactoryImpl{}, nil)
	managerUnderTest.allTypes["type1"] = TypeData{
		Identity: "type1",
	}

	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
	}
	err := managerUnderTest.AddJobFromRESTCall(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("missing required job identity: {    {{0 0}} type1 }", err.Error())
}

func TestJobsManagerAddJobWhenTargetUriMissing_shouldReturnError(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl(nil, "", kafkaclient.KafkaFactoryImpl{}, nil)
	managerUnderTest.allTypes["type1"] = TypeData{
		Identity: "type1",
	}

	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
		InfoJobIdentity:  "job1",
	}
	err := managerUnderTest.AddJobFromRESTCall(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("missing required target URI: {  job1  {{0 0}} type1 }", err.Error())
}

func TestJobsManagerDeleteJob_shouldSendDeleteToChannel(t *testing.T) {
	assertions := require.New(t)
	managerUnderTest := NewJobsManagerImpl(nil, "", kafkaclient.KafkaFactoryImpl{}, nil)
	jobsHandler := jobsHandler{
		deleteJobCh: make(chan string)}
	managerUnderTest.allTypes["type1"] = TypeData{
		Identity:    "type1",
		jobsHandler: &jobsHandler,
	}

	go managerUnderTest.DeleteJobFromRESTCall("job2")

	assertions.Equal("job2", <-jobsHandler.deleteJobCh)
}

func TestStartJobsManagerAddDMaaPJob_shouldStartPollAndDistributeMessages(t *testing.T) {
	assertions := require.New(t)

	called := false
	dMaaPMessages := `[{"message": {"data": "dmaap"}}]`
	pollClientMock := NewTestClient(func(req *http.Request) *http.Response {
		if req.URL.String() == "http://mrAddr/topicUrl" {
			assertions.Equal(req.Method, "GET")
			body := "[]"
			if !called {
				called = true
				body = dMaaPMessages
			}
			return &http.Response{
				StatusCode: http.StatusOK,
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
		if req.URL.String() == "http://consumerHost/dmaaptarget" {
			assertions.Equal(req.Method, "POST")
			assertions.Equal(dMaaPMessages, getBodyAsString(req, t))
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
	dMaaPTypeDef := config.TypeDefinition{
		Identity:      "type1",
		DMaaPTopicURL: "/topicUrl",
	}
	dMaaPJobsHandler := newJobsHandler(dMaaPTypeDef, "http://mrAddr", nil, pollClientMock, distributeClientMock)

	jobsManager := NewJobsManagerImpl(pollClientMock, "http://mrAddr", kafkaclient.KafkaFactoryImpl{}, distributeClientMock)
	jobsManager.allTypes["type1"] = TypeData{
		Identity:    "type1",
		jobsHandler: dMaaPJobsHandler,
	}
	jobsManager.StartJobsForAllTypes()

	dMaaPJobInfo := JobInfo{
		InfoTypeIdentity: "type1",
		InfoJobIdentity:  "job1",
		TargetUri:        "http://consumerHost/dmaaptarget",
	}

	wg.Add(1) // Wait till the distribution has happened
	err := jobsManager.AddJobFromRESTCall(dMaaPJobInfo)
	assertions.Nil(err)

	if waitTimeout(&wg, 2*time.Second) {
		t.Error("Not all calls to server were made")
		t.Fail()
	}
}

func TestStartJobsManagerAddKafkaJob_shouldStartPollAndDistributeMessages(t *testing.T) {
	assertions := require.New(t)

	kafkaMessages := `1`
	wg := sync.WaitGroup{}
	distributeClientMock := NewTestClient(func(req *http.Request) *http.Response {
		if req.URL.String() == "http://consumerHost/kafkatarget" {
			assertions.Equal(req.Method, "POST")
			assertions.Equal(kafkaMessages, getBodyAsString(req, t))
			assertions.Equal("text/plain", req.Header.Get("Content-Type"))
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

	kafkaTypeDef := config.TypeDefinition{
		Identity:        "type2",
		KafkaInputTopic: "topic",
	}
	kafkaFactoryMock := mocks.KafkaFactory{}
	kafkaConsumerMock := mocks.KafkaConsumer{}
	kafkaConsumerMock.On("Commit").Return([]kafka.TopicPartition{}, error(nil))
	kafkaConsumerMock.On("Subscribe", mock.Anything).Return(error(nil))
	kafkaConsumerMock.On("ReadMessage", mock.Anything).Return(&kafka.Message{
		Value: []byte(kafkaMessages),
	}, error(nil)).Once()
	kafkaConsumerMock.On("ReadMessage", mock.Anything).Return(nil, fmt.Errorf("Just to stop"))
	kafkaFactoryMock.On("NewKafkaConsumer", mock.Anything).Return(kafkaConsumerMock, nil)
	kafkaJobsHandler := newJobsHandler(kafkaTypeDef, "", kafkaFactoryMock, nil, distributeClientMock)

	jobsManager := NewJobsManagerImpl(nil, "", kafkaFactoryMock, distributeClientMock)
	jobsManager.allTypes["type2"] = TypeData{
		Identity:    "type2",
		jobsHandler: kafkaJobsHandler,
	}

	jobsManager.StartJobsForAllTypes()

	kafkaJobInfo := JobInfo{
		InfoTypeIdentity: "type2",
		InfoJobIdentity:  "job2",
		TargetUri:        "http://consumerHost/kafkatarget",
	}

	wg.Add(1) // Wait till the distribution has happened
	err := jobsManager.AddJobFromRESTCall(kafkaJobInfo)
	assertions.Nil(err)

	if waitTimeout(&wg, 2*time.Second) {
		t.Error("Not all calls to server were made")
		t.Fail()
	}
}

func TestJobsHandlerDeleteJob_shouldDeleteJobFromJobsMap(t *testing.T) {
	jobToDelete := newJob(JobInfo{}, nil)
	go jobToDelete.start()
	typeDef := config.TypeDefinition{
		Identity:      "type1",
		DMaaPTopicURL: "/topicUrl",
	}
	jobsHandler := newJobsHandler(typeDef, "http://mrAddr", kafkaclient.KafkaFactoryImpl{}, nil, nil)
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

	typeDef := config.TypeDefinition{
		Identity:      "type1",
		DMaaPTopicURL: "/topicUrl",
	}
	jobsHandler := newJobsHandler(typeDef, "http://mrAddr", kafkaclient.KafkaFactoryImpl{}, nil, nil)
	jobsHandler.jobs["job1"] = job

	fillMessagesBuffer(job.messagesChannel)

	jobsHandler.distributeMessages([]byte("sent msg"))

	require.New(t).Len(job.messagesChannel, 0)
}

func TestKafkaPollingAgentTimedOut_shouldResultInEMptyMessages(t *testing.T) {
	assertions := require.New(t)

	kafkaFactoryMock := mocks.KafkaFactory{}
	kafkaConsumerMock := mocks.KafkaConsumer{}
	kafkaConsumerMock.On("Commit").Return([]kafka.TopicPartition{}, error(nil))
	kafkaConsumerMock.On("Subscribe", mock.Anything).Return(error(nil))
	kafkaConsumerMock.On("ReadMessage", mock.Anything).Return(nil, kafka.NewError(kafka.ErrTimedOut, "", false))
	kafkaFactoryMock.On("NewKafkaConsumer", mock.Anything).Return(kafkaConsumerMock, nil)

	pollingAgentUnderTest := newKafkaPollingAgent(kafkaFactoryMock, "")
	messages, err := pollingAgentUnderTest.pollMessages()

	assertions.Equal([]byte(""), messages)
	assertions.Nil(err)
}

func TestJobWithoutParameters_shouldSendOneMessageAtATime(t *testing.T) {
	assertions := require.New(t)

	wg := sync.WaitGroup{}
	messageNo := 1
	distributeClientMock := NewTestClient(func(req *http.Request) *http.Response {
		if req.URL.String() == "http://consumerHost/target" {
			assertions.Equal(req.Method, "POST")
			assertions.Equal(fmt.Sprint("message", messageNo), getBodyAsString(req, t))
			messageNo++
			assertions.Equal("text/plain", req.Header.Get("Content-Type"))
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

	jobUnderTest := newJob(JobInfo{
		sourceType: kafkaSource,
		TargetUri:  "http://consumerHost/target",
	}, distributeClientMock)

	wg.Add(2)
	go jobUnderTest.start()

	jobUnderTest.messagesChannel <- []byte("message1")
	jobUnderTest.messagesChannel <- []byte("message2")

	if waitTimeout(&wg, 2*time.Second) {
		t.Error("Not all calls to server were made")
		t.Fail()
	}
}

func TestJobWithBufferedParameters_shouldSendMessagesTogether(t *testing.T) {
	assertions := require.New(t)

	wg := sync.WaitGroup{}
	distributeClientMock := NewTestClient(func(req *http.Request) *http.Response {
		if req.URL.String() == "http://consumerHost/target" {
			assertions.Equal(req.Method, "POST")
			assertions.Equal(`"[{\"data\": 1},{\"data\": 2}]"`, getBodyAsString(req, t))
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

	jobUnderTest := newJob(JobInfo{
		TargetUri: "http://consumerHost/target",
		InfoJobData: Parameters{
			BufferTimeout: BufferTimeout{
				MaxSize:            5,
				MaxTimeMiliseconds: 200,
			},
		},
	}, distributeClientMock)

	wg.Add(1)
	go jobUnderTest.start()

	go func() {
		jobUnderTest.messagesChannel <- []byte(`{"data": 1}`)
		jobUnderTest.messagesChannel <- []byte(`{"data": 2}`)
	}()

	if waitTimeout(&wg, 2*time.Second) {
		t.Error("Not all calls to server were made")
		t.Fail()
	}
}

func TestJobReadMoreThanBufferSizeMessages_shouldOnlyReturnMaxSizeNoOfMessages(t *testing.T) {
	assertions := require.New(t)

	jobUnderTest := newJob(JobInfo{}, nil)

	go func() {
		for i := 0; i < 4; i++ {
			jobUnderTest.messagesChannel <- []byte(strconv.Itoa(i))
		}
	}()

	msgs := jobUnderTest.read(BufferTimeout{
		MaxSize:            2,
		MaxTimeMiliseconds: 200,
	})

	assertions.Equal([]byte("\"[0,1]\""), msgs)
}
func TestJobReadBufferedWhenTimeout_shouldOnlyReturnMessagesSentBeforeTimeout(t *testing.T) {
	assertions := require.New(t)

	jobUnderTest := newJob(JobInfo{}, nil)

	go func() {
		for i := 0; i < 4; i++ {
			time.Sleep(10 * time.Millisecond)
			jobUnderTest.messagesChannel <- []byte(strconv.Itoa(i))
		}
	}()

	msgs := jobUnderTest.read(BufferTimeout{
		MaxSize:            2,
		MaxTimeMiliseconds: 30,
	})

	assertions.Equal([]byte("\"[0,1]\""), msgs)
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

func getBodyAsString(req *http.Request, t *testing.T) string {
	buf := new(bytes.Buffer)
	if _, err := buf.ReadFrom(req.Body); err != nil {
		t.Fail()
	}
	return buf.String()
}
