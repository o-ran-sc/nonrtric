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
	jobHandler    *jobHandler
}

type JobInfo struct {
	Owner            string      `json:"owner"`
	LastUpdated      string      `json:"last_updated"`
	InfoJobIdentity  string      `json:"info_job_identity"`
	TargetUri        string      `json:"target_uri"`
	InfoJobData      interface{} `json:"info_job_data"`
	InfoTypeIdentity string      `json:"info_type_identity"`
}

type JobTypesManager interface {
	LoadTypesFromConfiguration() ([]config.TypeDefinition, error)
	GetSupportedTypes() []string
}

type JobsManager interface {
	AddJob(JobInfo) error
	DeleteJob(jobId string)
}

type JobsManagerImpl struct {
	configFile       string
	allTypes         map[string]TypeData
	pollClient       restclient.HTTPClient
	mrAddress        string
	distributeClient restclient.HTTPClient
}

type jobHandler struct {
	mu               sync.Mutex
	typeId           string
	topicUrl         string
	jobs             map[string]JobInfo
	addJobCh         chan JobInfo
	deleteJobCh      chan string
	pollClient       restclient.HTTPClient
	distributeClient restclient.HTTPClient
}

func NewJobsManagerImpl(typeConfigFilePath string, pollClient restclient.HTTPClient, mrAddr string, distributeClient restclient.HTTPClient) *JobsManagerImpl {
	return &JobsManagerImpl{
		configFile:       typeConfigFilePath,
		allTypes:         make(map[string]TypeData),
		pollClient:       pollClient,
		mrAddress:        mrAddr,
		distributeClient: distributeClient,
	}
}

func (jm *JobsManagerImpl) AddJob(ji JobInfo) error {
	if err := jm.validateJobInfo(ji); err == nil {
		typeData := jm.allTypes[ji.InfoTypeIdentity]
		typeData.jobHandler.addJobCh <- ji
		log.Debug("Added job: ", ji)
		return nil
	} else {
		return err
	}
}

func (jm *JobsManagerImpl) DeleteJob(jobId string) {
	for _, typeData := range jm.allTypes {
		log.Debugf("Deleting job %v from type %v", jobId, typeData.TypeId)
		typeData.jobHandler.deleteJobCh <- jobId
	}
	log.Debug("Deleted job: ", jobId)
}

func (jm *JobsManagerImpl) validateJobInfo(ji JobInfo) error {
	if _, ok := jm.allTypes[ji.InfoTypeIdentity]; !ok {
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

func (jm *JobsManagerImpl) LoadTypesFromConfiguration() ([]config.TypeDefinition, error) {
	typeDefsByte, err := os.ReadFile(jm.configFile)
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
		addCh := make(chan JobInfo)
		deleteCh := make(chan string)
		jh := jobHandler{
			typeId:           typeDef.Id,
			topicUrl:         typeDef.DmaapTopicURL,
			jobs:             make(map[string]JobInfo),
			addJobCh:         addCh,
			deleteJobCh:      deleteCh,
			pollClient:       jm.pollClient,
			distributeClient: jm.distributeClient,
		}
		jm.allTypes[typeDef.Id] = TypeData{
			TypeId:        typeDef.Id,
			DMaaPTopicURL: typeDef.DmaapTopicURL,
			jobHandler:    &jh,
		}
	}
	return typeDefs.Types, nil
}

func (jm *JobsManagerImpl) GetSupportedTypes() []string {
	supportedTypes := []string{}
	for k := range jm.allTypes {
		supportedTypes = append(supportedTypes, k)
	}
	return supportedTypes
}

func (jm *JobsManagerImpl) StartJobs() {
	for _, jobType := range jm.allTypes {

		go jobType.jobHandler.start(jm.mrAddress)

	}
}

func (jh *jobHandler) start(mRAddress string) {
	go func() {
		for {
			jh.pollAndDistributeMessages(mRAddress)
		}
	}()

	go func() {
		for {
			jh.monitorManagementChannels()
		}
	}()
}

func (jh *jobHandler) pollAndDistributeMessages(mRAddress string) {
	jh.mu.Lock()
	defer jh.mu.Unlock()
	log.Debugf("Processing jobs for type: %v", jh.typeId)
	messagesBody, error := restclient.Get(mRAddress+jh.topicUrl, jh.pollClient)
	if error != nil {
		log.Warnf("Error getting data from MR. Cause: %v", error)
	}
	log.Debugf("Received messages: %v", string(messagesBody))
	jh.distributeMessages(messagesBody)
}

func (jh *jobHandler) distributeMessages(messages []byte) {
	if len(messages) > 2 {
		for _, jobInfo := range jh.jobs {
			go jh.sendMessagesToConsumer(messages, jobInfo)
		}
	}
}

func (jh *jobHandler) sendMessagesToConsumer(messages []byte, jobInfo JobInfo) {
	log.Debugf("Processing job: %v", jobInfo.InfoJobIdentity)
	if postErr := restclient.Post(jobInfo.TargetUri, messages, jh.distributeClient); postErr != nil {
		log.Warnf("Error posting data for job: %v. Cause: %v", jobInfo, postErr)
	}
	log.Debugf("Messages distributed to consumer: %v.", jobInfo.Owner)
}

func (jh *jobHandler) monitorManagementChannels() {
	select {
	case addedJob := <-jh.addJobCh:
		jh.mu.Lock()
		log.Debugf("received %v from addJobCh\n", addedJob)
		jh.jobs[addedJob.InfoJobIdentity] = addedJob
		jh.mu.Unlock()
	case deletedJob := <-jh.deleteJobCh:
		jh.mu.Lock()
		log.Debugf("received %v from deleteJobCh\n", deletedJob)
		delete(jh.jobs, deletedJob)
		jh.mu.Unlock()
	}
}
