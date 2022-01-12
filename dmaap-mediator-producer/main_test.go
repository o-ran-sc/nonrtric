// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2022: Nordix Foundation
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

package main

import (
	"bytes"
	"io/ioutil"
	"net/http"
	"os/exec"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/config"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/jobs"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/kafkaclient"
)

// This is not a real test, just a way to get the Swagger documentation generated automatically.
// Hence there are no assertions in this test.
func TestGenerateSwaggerDocs(t *testing.T) {
	cmd := exec.Command("./generate_swagger_docs.sh")

	cmd.Run()
}

func TestValidateConfiguration(t *testing.T) {
	assertions := require.New(t)

	validConfig := config.Config{
		InfoProducerHost:      "host",
		DMaaPMRAddress:        "address",
		KafkaBootstrapServers: "servers",
		ProducerCertPath:      "path",
		ProducerKeyPath:       "path",
	}
	assertions.Nil(validateConfiguration(&validConfig))

	missingProducerHost := config.Config{
		DMaaPMRAddress:        "address",
		KafkaBootstrapServers: "servers",
		ProducerCertPath:      "path",
		ProducerKeyPath:       "path",
	}
	assertions.Contains(validateConfiguration(&missingProducerHost).Error(), "INFO_PRODUCER_HOST")

	missingCert := config.Config{
		InfoProducerHost:      "host",
		DMaaPMRAddress:        "address",
		KafkaBootstrapServers: "servers",
		ProducerKeyPath:       "path",
	}
	assertions.Contains(validateConfiguration(&missingCert).Error(), "PRODUCER_CERT")

	missingCertKey := config.Config{
		InfoProducerHost:      "host",
		DMaaPMRAddress:        "address",
		KafkaBootstrapServers: "servers",
		ProducerCertPath:      "path",
	}
	assertions.Contains(validateConfiguration(&missingCertKey).Error(), "PRODUCER_KEY")

	missingMRAddress := config.Config{
		InfoProducerHost:      "host",
		KafkaBootstrapServers: "servers",
		ProducerCertPath:      "path",
		ProducerKeyPath:       "path",
	}
	assertions.Nil(validateConfiguration(&missingMRAddress))

	missingKafkaServers := config.Config{
		InfoProducerHost: "host",
		DMaaPMRAddress:   "address",
		ProducerCertPath: "path",
		ProducerKeyPath:  "path",
	}
	assertions.Nil(validateConfiguration(&missingKafkaServers))

	missingMRAddressdAndKafkaServers := config.Config{
		InfoProducerHost: "host",
		ProducerCertPath: "path",
		ProducerKeyPath:  "path",
	}
	assertions.Contains(validateConfiguration(&missingMRAddressdAndKafkaServers).Error(), "DMAAP_MR_ADDR")
	assertions.Contains(validateConfiguration(&missingMRAddressdAndKafkaServers).Error(), "KAFKA_BOOTSRAP_SERVERS")
}

func TestRegisterTypesAndProducer(t *testing.T) {
	assertions := require.New(t)

	wg := sync.WaitGroup{}
	clientMock := NewTestClient(func(req *http.Request) *http.Response {
		if req.URL.String() == configuration.InfoCoordinatorAddress+"/data-producer/v1/info-types/STD_Fault_Messages" {
			assertions.Equal(req.Method, "PUT")
			body := getBodyAsString(req, t)
			assertions.Contains(body, "info_job_data_schema")
			assertions.Equal("application/json", req.Header.Get("Content-Type"))
			wg.Done()
			return &http.Response{
				StatusCode: 200,
				Body:       ioutil.NopCloser(bytes.NewBufferString(`OK`)),
				Header:     make(http.Header), // Must be set to non-nil value or it panics
			}
		} else if req.URL.String() == configuration.InfoCoordinatorAddress+"/data-producer/v1/info-types/Kafka_TestTopic" {
			assertions.Equal(req.Method, "PUT")
			body := getBodyAsString(req, t)
			assertions.Contains(body, "info_job_data_schema")
			assertions.Equal("application/json", req.Header.Get("Content-Type"))
			wg.Done()
			return &http.Response{
				StatusCode: 200,
				Body:       ioutil.NopCloser(bytes.NewBufferString(`OK`)),
				Header:     make(http.Header), // Must be set to non-nil value or it panics
			}
		} else if req.URL.String() == configuration.InfoCoordinatorAddress+"/data-producer/v1/info-producers/DMaaP_Mediator_Producer" {
			assertions.Equal(req.Method, "PUT")
			body := getBodyAsString(req, t)
			assertions.Contains(body, "callbackAddress/health_check")
			assertions.Contains(body, "callbackAddress/info_job")
			assertions.Contains(body, "Kafka_TestTopic")
			assertions.Contains(body, "STD_Fault_Messages")
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
	jobsManager := jobs.NewJobsManagerImpl(clientMock, configuration.DMaaPMRAddress, kafkaclient.KafkaFactoryImpl{}, nil)

	wg.Add(3)
	err := registerTypesAndProducer(jobsManager, configuration.InfoCoordinatorAddress, "callbackAddress", clientMock)

	assertions.Nil(err)

	if waitTimeout(&wg, 2*time.Second) {
		t.Error("Not all calls to server were made")
		t.Fail()
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

func getBodyAsString(req *http.Request, t *testing.T) string {
	buf := new(bytes.Buffer)
	if _, err := buf.ReadFrom(req.Body); err != nil {
		t.Fail()
	}
	return buf.String()
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
