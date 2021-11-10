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
	jobsHandler   *jobsHandler
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

type jobsHandler struct {
	mu               sync.Mutex
	typeId           string
	topicUrl         string
	jobs             map[string]job
	addJobCh         chan JobInfo
	deleteJobCh      chan string
	pollClient       restclient.HTTPClient
	distributeClient restclient.HTTPClient
}

type job struct {
	jobInfo         JobInfo
	client          restclient.HTTPClient
	messagesChannel chan *[]byte
	controlChannel  chan struct{}
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

func newJobsHandler(typeId string, topicURL string, pollClient restclient.HTTPClient, distributeClient restclient.HTTPClient) *jobsHandler {
	return &jobsHandler{
		typeId:           typeId,
		topicUrl:         topicURL,
		jobs:             make(map[string]job),
		addJobCh:         make(chan JobInfo),
		deleteJobCh:      make(chan string),
		pollClient:       pollClient,
		distributeClient: distributeClient,
	}
}

func (jm *JobsManagerImpl) AddJob(ji JobInfo) error {
	if err := jm.validateJobInfo(ji); err == nil {
		typeData := jm.allTypes[ji.InfoTypeIdentity]
		typeData.jobsHandler.addJobCh <- ji
		log.Debug("Added job: ", ji)
		return nil
	} else {
		return err
	}
}

func (jm *JobsManagerImpl) DeleteJob(jobId string) {
	for _, typeData := range jm.allTypes {
		log.Debugf("Deleting job %v from type %v", jobId, typeData.TypeId)
		typeData.jobsHandler.deleteJobCh <- jobId
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
		jm.allTypes[typeDef.Id] = TypeData{
			TypeId:        typeDef.Id,
			DMaaPTopicURL: typeDef.DmaapTopicURL,
			jobsHandler:   newJobsHandler(typeDef.Id, typeDef.DmaapTopicURL, jm.pollClient, jm.distributeClient),
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

		go jobType.jobsHandler.start(jm.mrAddress)

	}
}

func (jh *jobsHandler) start(mRAddress string) {
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

func (jh *jobsHandler) pollAndDistributeMessages(mRAddress string) {
	log.Debugf("Processing jobs for type: %v", jh.typeId)
	messagesBody, error := restclient.Get(mRAddress+jh.topicUrl, jh.pollClient)
	if error != nil {
		log.Warnf("Error getting data from MR. Cause: %v", error)
	}
	log.Debugf("Received messages: %v", string(messagesBody))
	jh.distributeMessages(&messagesBody)
}

func (jh *jobsHandler) distributeMessages(messages *[]byte) {
	if len(*messages) > 2 {
		jh.mu.Lock()
		defer jh.mu.Unlock()
		for _, job := range jh.jobs {
			if len(job.messagesChannel) < cap(job.messagesChannel) {
				job.messagesChannel <- messages
			} else {
				log.Debug("Skip sending for job: ", job.jobInfo.InfoJobIdentity)
				for i := 0; i < cap(job.messagesChannel)-3; i++ {
					log.Debug("Emptying message queue for job: ", job.jobInfo.InfoJobIdentity)
					<-job.messagesChannel
				}
			}
		}
	}
}

func (jh *jobsHandler) monitorManagementChannels() {
	select {
	case addedJob := <-jh.addJobCh:
		jh.mu.Lock()
		log.Debugf("received %v from addJobCh\n", addedJob)
		newJob := job{
			jobInfo:         addedJob,
			client:          jh.distributeClient,
			messagesChannel: make(chan *[]byte, 10),
			controlChannel:  make(chan struct{}),
		}
		go newJob.start()
		jh.jobs[addedJob.InfoJobIdentity] = newJob
		jh.mu.Unlock()
	case deletedJob := <-jh.deleteJobCh:
		jh.mu.Lock()
		log.Debugf("received %v from deleteJobCh\n", deletedJob)
		j, exist := jh.jobs[deletedJob]
		if exist {
			j.controlChannel <- struct{}{}
			delete(jh.jobs, deletedJob)
		}
		jh.mu.Unlock()
	}
}

func (j *job) start() {
out:
	for {
		select {
		case <-j.controlChannel:
			log.Debug("Stop distribution for job: ", j.jobInfo.InfoJobIdentity)
			break out
		case msg := <-j.messagesChannel:
			j.sendMessagesToConsumer(msg)
		}
	}
}

func (j *job) sendMessagesToConsumer(messages *[]byte) {
	log.Debugf("Processing job: %v", j.jobInfo.InfoJobIdentity)
	if postErr := restclient.Post(j.jobInfo.TargetUri, messages, j.client); postErr != nil {
		log.Warnf("Error posting data for job: %v. Cause: %v", j.jobInfo, postErr)
	}
	log.Debugf("Messages distributed to consumer: %v.", j.jobInfo.Owner)
}
