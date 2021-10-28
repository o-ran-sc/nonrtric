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
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/config"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/restclient"
)

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

type JobTypeHandler interface {
	GetTypes() ([]config.TypeDefinition, error)
	GetSupportedTypes() []string
}

type JobHandler interface {
	AddJob(JobInfo) error
	DeleteJob(jobId string)
}

type JobHandlerImpl struct {
	mu               sync.Mutex
	configFile       string
	allTypes         map[string]TypeData
	pollClient       restclient.HTTPClient
	distributeClient restclient.HTTPClient
}

func NewJobHandlerImpl(typeConfigFilePath string, pollClient restclient.HTTPClient, distributeClient restclient.HTTPClient) *JobHandlerImpl {
	return &JobHandlerImpl{
		configFile:       typeConfigFilePath,
		allTypes:         make(map[string]TypeData),
		pollClient:       pollClient,
		distributeClient: distributeClient,
	}
}

func (jh *JobHandlerImpl) AddJob(ji JobInfo) error {
	jh.mu.Lock()
	defer jh.mu.Unlock()
	if err := jh.validateJobInfo(ji); err == nil {
		jobs := jh.allTypes[ji.InfoTypeIdentity].Jobs
		jobs[ji.InfoJobIdentity] = ji
		log.Debug("Added job: ", ji)
		return nil
	} else {
		return err
	}
}

func (jh *JobHandlerImpl) DeleteJob(jobId string) {
	jh.mu.Lock()
	defer jh.mu.Unlock()
	for _, typeData := range jh.allTypes {
		delete(typeData.Jobs, jobId)
	}
	log.Debug("Deleted job: ", jobId)
}

func (jh *JobHandlerImpl) validateJobInfo(ji JobInfo) error {
	if _, ok := jh.allTypes[ji.InfoTypeIdentity]; !ok {
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

func (jh *JobHandlerImpl) GetTypes() ([]config.TypeDefinition, error) {
	jh.mu.Lock()
	defer jh.mu.Unlock()
	typeDefsByte, err := os.ReadFile(jh.configFile)
	if err != nil {
		return nil, err
	}
	typeDefs := struct {
		Types []config.TypeDefinition `json:"types"`
	}{}
	err = json.Unmarshal(typeDefsByte, &typeDefs)
	if err != nil {
		return nil, err
	}
	for _, typeDef := range typeDefs.Types {
		jh.allTypes[typeDef.Id] = TypeData{
			TypeId:        typeDef.Id,
			DMaaPTopicURL: typeDef.DmaapTopicURL,
			Jobs:          make(map[string]JobInfo),
		}
	}
	return typeDefs.Types, nil
}

func (jh *JobHandlerImpl) GetSupportedTypes() []string {
	jh.mu.Lock()
	defer jh.mu.Unlock()
	supportedTypes := []string{}
	for k := range jh.allTypes {
		supportedTypes = append(supportedTypes, k)
	}
	return supportedTypes
}

func (jh *JobHandlerImpl) RunJobs(mRAddress string) {
	for {
		jh.pollAndDistributeMessages(mRAddress)
	}
}

func (jh *JobHandlerImpl) pollAndDistributeMessages(mRAddress string) {
	jh.mu.Lock()
	defer jh.mu.Unlock()
	for typeId, typeInfo := range jh.allTypes {
		log.Debugf("Processing jobs for type: %v", typeId)
		messagesBody, error := restclient.Get(fmt.Sprintf("%v/%v", mRAddress, typeInfo.DMaaPTopicURL), jh.pollClient)
		if error != nil {
			log.Warnf("Error getting data from MR. Cause: %v", error)
			continue
		}
		jh.distributeMessages(messagesBody, typeInfo)
	}
}

func (jh *JobHandlerImpl) distributeMessages(messages []byte, typeInfo TypeData) {
	if len(messages) > 2 {
		for _, jobInfo := range typeInfo.Jobs {
			go jh.sendMessagesToConsumer(messages, jobInfo)
		}
	}
}

func (jh *JobHandlerImpl) sendMessagesToConsumer(messages []byte, jobInfo JobInfo) {
	log.Debugf("Processing job: %v", jobInfo.InfoJobIdentity)
	if postErr := restclient.Post(jobInfo.TargetUri, messages, jh.distributeClient); postErr != nil {
		log.Warnf("Error posting data for job: %v. Cause: %v", jobInfo, postErr)
	}
	log.Debugf("Messages distributed to consumer: %v.", jobInfo.Owner)
}

func (jh *JobHandlerImpl) clearAll() {
	jh.allTypes = make(map[string]TypeData)
}
