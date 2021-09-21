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
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"

	log "github.com/sirupsen/logrus"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/restclient"
)

type Type struct {
	TypeId     string `json:"id"`
	DMaaPTopic string `json:"dmaapTopic"`
	Schema     string `json:"schema"`
	Jobs       map[string]JobInfo
}

type JobInfo struct {
	Owner            string `json:"owner"`
	LastUpdated      string `json:"last_updated"`
	InfoJobIdentity  string `json:"info_job_identity"`
	TargetUri        string `json:"target_uri"`
	InfoJobData      string `json:"info_job_data"`
	InfoTypeIdentity string `json:"info_type_identity"`
}

type JobHandler interface {
	AddJob(JobInfo) error
}

var (
	mu      sync.Mutex
	typeDir = "configs"
	Handler JobHandler
	allJobs = make(map[string]Type)
)

func init() {
	Handler = newJobHandlerImpl()
}

type jobHandlerImpl struct{}

func newJobHandlerImpl() *jobHandlerImpl {
	return &jobHandlerImpl{}
}

func (jh *jobHandlerImpl) AddJob(ji JobInfo) error {
	mu.Lock()
	defer mu.Unlock()
	if err := validateJobInfo(ji); err == nil {
		jobs := allJobs[ji.InfoTypeIdentity].Jobs
		jobs[ji.InfoJobIdentity] = ji
		return nil
	} else {
		return err
	}
}

func validateJobInfo(ji JobInfo) error {
	if _, ok := allJobs[ji.InfoTypeIdentity]; !ok {
		return fmt.Errorf("type not supported: %v", ji.InfoTypeIdentity)
	}
	if ji.InfoJobIdentity == "" {
		return fmt.Errorf("missing required job identity: %v", ji)
	}
	// Temporary for when there are only REST callbacks needed
	if ji.TargetUri == "" {
		return fmt.Errorf("missing required target URI: %v", ji)
	}
	return nil
}

func GetTypes() ([]*Type, error) {
	mu.Lock()
	defer mu.Unlock()
	types := make([]*Type, 0, 1)
	err := filepath.Walk(typeDir,
		func(path string, info os.FileInfo, err error) error {
			if err != nil {
				return err
			}
			if strings.Contains(path, ".json") {
				if jobType, err := getType(path); err == nil {
					types = append(types, jobType)
				}
			}
			return nil
		})
	if err != nil {
		return nil, err
	}
	return types, nil
}

func GetSupportedTypes() []string {
	mu.Lock()
	defer mu.Unlock()
	supportedTypes := []string{}
	for k := range allJobs {
		supportedTypes = append(supportedTypes, k)
	}
	return supportedTypes
}

func AddJob(job JobInfo) error {
	return Handler.AddJob(job)
}

func getType(path string) (*Type, error) {
	if typeDefinition, err := os.ReadFile(path); err == nil {
		var dat map[string]interface{}
		if marshalError := json.Unmarshal(typeDefinition, &dat); marshalError == nil {
			schema, _ := json.Marshal(dat["schema"])
			typeInfo := Type{
				TypeId:     dat["id"].(string),
				DMaaPTopic: dat["dmaapTopic"].(string),
				Schema:     string(schema),
				Jobs:       make(map[string]JobInfo),
			}
			if _, ok := allJobs[typeInfo.TypeId]; !ok {
				allJobs[typeInfo.TypeId] = typeInfo
			}
			return &typeInfo, nil
		} else {
			return nil, marshalError
		}
	} else {
		return nil, err
	}
}

func RunJobs(mRAddress string) {
	for {
		pollAndDistributeMessages(mRAddress)
	}
}

func pollAndDistributeMessages(mRAddress string) {
	for typeId, typeInfo := range allJobs {
		log.Debugf("Processing jobs for type: %v", typeId)
		messagesBody, error := restclient.Get(fmt.Sprintf("%v/events/%v/users/dmaapmediatorproducer", mRAddress, typeInfo.DMaaPTopic))
		if error != nil {
			log.Warnf("Error getting data from MR. Cause: %v", error)
			continue
		}
		distributeMessages(messagesBody, typeInfo)
	}
}

func distributeMessages(messages []byte, typeInfo Type) {
	if len(messages) > 2 {
		mu.Lock()
		for _, jobInfo := range typeInfo.Jobs {
			go sendMessagesToConsumer(messages, jobInfo)
		}
		mu.Unlock()
	}
}

func sendMessagesToConsumer(messages []byte, jobInfo JobInfo) {
	log.Debugf("Processing job: %v", jobInfo.InfoJobIdentity)
	if postErr := restclient.Post(jobInfo.TargetUri, messages); postErr != nil {
		log.Warnf("Error posting data for job: %v. Cause: %v", jobInfo, postErr)
	}
}

func clearAll() {
	allJobs = make(map[string]Type)
}
