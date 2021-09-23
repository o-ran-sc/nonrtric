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
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/restclient"
	"oransc.org/nonrtric/dmaapmediatorproducer/mocks"
)

const typeDefinition = `[{"id": "type1", "dmaapTopicUrl": "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/type1"}]`

func TestGetTypes_filesOkShouldReturnSliceOfTypesAndProvideSupportedTypes(t *testing.T) {
	assertions := require.New(t)
	typesDir, err := os.MkdirTemp("", "configs")
	if err != nil {
		t.Errorf("Unable to create temporary directory for types due to: %v", err)
	}
	t.Cleanup(func() {
		os.RemoveAll(typesDir)
		clearAll()
	})
	fname := filepath.Join(typesDir, "type_config.json")
	configFile = fname
	if err = os.WriteFile(fname, []byte(typeDefinition), 0666); err != nil {
		t.Errorf("Unable to create temporary config file for types due to: %v", err)
	}
	types, err := GetTypes()
	wantedType := TypeData{
		TypeId:        "type1",
		DMaaPTopicURL: "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/type1",
		Jobs:          make(map[string]JobInfo),
	}
	wantedTypes := []TypeData{wantedType}
	assertions.EqualValues(wantedTypes, types)
	assertions.Nil(err)

	supportedTypes := GetSupportedTypes()
	assertions.EqualValues([]string{"type1"}, supportedTypes)
}

func TestAddJobWhenTypeIsSupported_shouldAddJobToAllJobsMap(t *testing.T) {
	assertions := require.New(t)
	wantedJob := JobInfo{
		Owner:            "owner",
		LastUpdated:      "now",
		InfoJobIdentity:  "job1",
		TargetUri:        "target",
		InfoJobData:      "{}",
		InfoTypeIdentity: "type1",
	}
	allTypes["type1"] = TypeData{
		TypeId: "type1",
		Jobs:   map[string]JobInfo{"job1": wantedJob},
	}
	t.Cleanup(func() {
		clearAll()
	})

	err := AddJob(wantedJob)
	assertions.Nil(err)
	assertions.Equal(1, len(allTypes["type1"].Jobs))
	assertions.Equal(wantedJob, allTypes["type1"].Jobs["job1"])
}

func TestAddJobWhenTypeIsNotSupported_shouldReturnError(t *testing.T) {
	assertions := require.New(t)
	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
	}

	err := AddJob(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("type not supported: type1", err.Error())
}

func TestAddJobWhenJobIdMissing_shouldReturnError(t *testing.T) {
	assertions := require.New(t)
	allTypes["type1"] = TypeData{
		TypeId: "type1",
	}
	t.Cleanup(func() {
		clearAll()
	})
	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
	}

	err := AddJob(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("missing required job identity: {    <nil> type1}", err.Error())
}

func TestAddJobWhenTargetUriMissing_shouldReturnError(t *testing.T) {
	assertions := require.New(t)
	allTypes["type1"] = TypeData{
		TypeId: "type1",
	}
	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
		InfoJobIdentity:  "job1",
	}

	err := AddJob(jobInfo)
	assertions.NotNil(err)
	assertions.Equal("missing required target URI: {  job1  <nil> type1}", err.Error())
	clearAll()
}

func TestPollAndDistributeMessages(t *testing.T) {
	assertions := require.New(t)
	jobInfo := JobInfo{
		InfoTypeIdentity: "type1",
		InfoJobIdentity:  "job1",
		TargetUri:        "http://consumerHost/target",
	}
	allTypes["type1"] = TypeData{
		TypeId:        "type1",
		DMaaPTopicURL: "topicUrl",
		Jobs:          map[string]JobInfo{"job1": jobInfo},
	}
	t.Cleanup(func() {
		clearAll()
	})

	body := ioutil.NopCloser(bytes.NewReader([]byte(`[{"message": {"data": "data"}}]`)))
	clientMock := mocks.HTTPClient{}
	clientMock.On("Get", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusOK,
		Body:       body,
	}, nil)

	clientMock.On("Do", mock.Anything).Return(&http.Response{
		StatusCode: http.StatusOK,
	}, nil)

	restclient.Client = &clientMock

	pollAndDistributeMessages("http://mrAddr")

	time.Sleep(100 * time.Millisecond)

	var actualRequest *http.Request
	clientMock.AssertCalled(t, "Get", "http://mrAddr/topicUrl")
	clientMock.AssertNumberOfCalls(t, "Get", 1)

	clientMock.AssertCalled(t, "Do", mock.MatchedBy(func(req *http.Request) bool {
		actualRequest = req
		return true
	}))
	assertions.Equal(http.MethodPost, actualRequest.Method)
	assertions.Equal("consumerHost", actualRequest.URL.Host)
	assertions.Equal("/target", actualRequest.URL.Path)
	assertions.Equal("application/json; charset=utf-8", actualRequest.Header.Get("Content-Type"))
	actualBody, _ := ioutil.ReadAll(actualRequest.Body)
	assertions.Equal([]byte(`[{"message": {"data": "data"}}]`), actualBody)
	clientMock.AssertNumberOfCalls(t, "Do", 1)
}
