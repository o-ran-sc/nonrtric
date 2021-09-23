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
	"sync"

	log "github.com/sirupsen/logrus"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/restclient"
)

type TypeDefinitions struct {
	Types []TypeDefinition `json:"types"`
}
type TypeDefinition struct {
	Id            string `json:"id"`
	DmaapTopicURL string `json:"dmaapTopicUrl"`
}

type TypeData struct {
	TypeId        string `json:"id"`
	DMaaPTopicURL string `json:"dmaapTopicUrl"`
	Jobs          map[string]JobInfo
}

type JobInfo struct {
	Owner            string      `json:"owner"`
	LastUpdated      string      `json:"last_updated"`
	InfoJobIdentity  string      `json:"info_job_identity"`
	TargetUri        string      `json:"target_uri"`
	InfoJobData      interface{} `json:"info_job_data"`
	InfoTypeIdentity string      `json:"info_type_identity"`
}

type JobHandler interface {
	AddJob(JobInfo) error
}

var (
	mu         sync.Mutex
	configFile = "configs/type_config.json"
	Handler    JobHandler
	allTypes   = make(map[string]TypeData)
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
		jobs := allTypes[ji.InfoTypeIdentity].Jobs
		jobs[ji.InfoJobIdentity] = ji
		log.Debug("Added job: ", ji)
		return nil
	} else {
		return err
	}
}

func validateJobInfo(ji JobInfo) error {
	if _, ok := allTypes[ji.InfoTypeIdentity]; !ok {
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

func GetTypes() ([]TypeData, error) {
	mu.Lock()
	defer mu.Unlock()
	types := make([]TypeData, 0, 1)
	typeDefsByte, err := os.ReadFile(configFile)
	if err != nil {
		return nil, err
	}
	typeDefs := TypeDefinitions{}
	err = json.Unmarshal(typeDefsByte, &typeDefs)
	if err != nil {
		return nil, err
	}
	for _, typeDef := range typeDefs.Types {
		typeInfo := TypeData{
			TypeId:        typeDef.Id,
			DMaaPTopicURL: typeDef.DmaapTopicURL,
			Jobs:          make(map[string]JobInfo),
		}
		if _, ok := allTypes[typeInfo.TypeId]; !ok {
			allTypes[typeInfo.TypeId] = typeInfo
		}
		types = append(types, typeInfo)
	}
	return types, nil
}

func GetSupportedTypes() []string {
	mu.Lock()
	defer mu.Unlock()
	supportedTypes := []string{}
	for k := range allTypes {
		supportedTypes = append(supportedTypes, k)
	}
	return supportedTypes
}

func AddJob(job JobInfo) error {
	return Handler.AddJob(job)
}

func RunJobs(mRAddress string) {
	for {
		pollAndDistributeMessages(mRAddress)
	}
}

func pollAndDistributeMessages(mRAddress string) {
	for typeId, typeInfo := range allTypes {
		log.Debugf("Processing jobs for type: %v", typeId)
		messagesBody, error := restclient.Get(fmt.Sprintf("%v/%v", mRAddress, typeInfo.DMaaPTopicURL))
		if error != nil {
			log.Warnf("Error getting data from MR. Cause: %v", error)
			continue
		}
		distributeMessages(messagesBody, typeInfo)
	}
}

func distributeMessages(messages []byte, typeInfo TypeData) {
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
	allTypes = make(map[string]TypeData)
}
