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
	"fmt"
	"sync"
	"time"

	"github.com/confluentinc/confluent-kafka-go/kafka"
	log "github.com/sirupsen/logrus"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/config"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/kafkaconsumer"
	"oransc.org/nonrtric/dmaapmediatorproducer/internal/restclient"
)

type TypeData struct {
	TypeId      string `json:"id"`
	jobsHandler *jobsHandler
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
	LoadTypesFromConfiguration(types []config.TypeDefinition) []config.TypeDefinition
	GetSupportedTypes() []string
}

type JobsManager interface {
	AddJobFromRESTCall(JobInfo) error
	DeleteJobFromRESTCall(jobId string)
}

type JobsManagerImpl struct {
	allTypes         map[string]TypeData
	pollClient       restclient.HTTPClient
	mrAddress        string
	kafkaFactory     kafkaconsumer.KafkaFactory
	distributeClient restclient.HTTPClient
}

func NewJobsManagerImpl(pollClient restclient.HTTPClient, mrAddr string, kafkaFactory kafkaconsumer.KafkaFactory, distributeClient restclient.HTTPClient) *JobsManagerImpl {
	return &JobsManagerImpl{
		allTypes:         make(map[string]TypeData),
		pollClient:       pollClient,
		mrAddress:        mrAddr,
		kafkaFactory:     kafkaFactory,
		distributeClient: distributeClient,
	}
}

func (jm *JobsManagerImpl) AddJobFromRESTCall(ji JobInfo) error {
	if err := jm.validateJobInfo(ji); err == nil {
		typeData := jm.allTypes[ji.InfoTypeIdentity]
		typeData.jobsHandler.addJobCh <- ji
		log.Debug("Added job: ", ji)
		return nil
	} else {
		return err
	}
}

func (jm *JobsManagerImpl) DeleteJobFromRESTCall(jobId string) {
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

func (jm *JobsManagerImpl) LoadTypesFromConfiguration(types []config.TypeDefinition) []config.TypeDefinition {
	for _, typeDef := range types {
		if typeDef.DMaaPTopicURL == "" && typeDef.KafkaInputTopic == "" {
			log.Fatal("DMaaPTopicURL or KafkaInputTopic must be defined for type: ", typeDef.ID)
		}
		jm.allTypes[typeDef.ID] = TypeData{
			TypeId:      typeDef.ID,
			jobsHandler: newJobsHandler(typeDef, jm.mrAddress, jm.kafkaFactory, jm.pollClient, jm.distributeClient),
		}
	}
	return types
}

func (jm *JobsManagerImpl) GetSupportedTypes() []string {
	supportedTypes := []string{}
	for k := range jm.allTypes {
		supportedTypes = append(supportedTypes, k)
	}
	return supportedTypes
}

func (jm *JobsManagerImpl) StartJobsForAllTypes() {
	for _, jobType := range jm.allTypes {

		go jobType.jobsHandler.startPollingAndDistribution(jm.mrAddress)

	}
}

type jobsHandler struct {
	mu               sync.Mutex
	typeId           string
	pollingAgent     pollingAgent
	jobs             map[string]job
	addJobCh         chan JobInfo
	deleteJobCh      chan string
	distributeClient restclient.HTTPClient
}

func newJobsHandler(typeDef config.TypeDefinition, mRAddress string, kafkaFactory kafkaconsumer.KafkaFactory, pollClient restclient.HTTPClient, distributeClient restclient.HTTPClient) *jobsHandler {
	pollingAgent := createPollingAgent(typeDef, mRAddress, pollClient, kafkaFactory, typeDef.KafkaInputTopic)
	return &jobsHandler{
		typeId:           typeDef.ID,
		pollingAgent:     pollingAgent,
		jobs:             make(map[string]job),
		addJobCh:         make(chan JobInfo),
		deleteJobCh:      make(chan string),
		distributeClient: distributeClient,
	}
}

func (jh *jobsHandler) startPollingAndDistribution(mRAddress string) {
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
	messagesBody, error := jh.pollingAgent.pollMessages()
	if error != nil {
		log.Warn("Error getting data from source. Cause: ", error)
		time.Sleep(time.Minute) // Must wait before trying to call MR again
		return
	}
	log.Debug("Received messages: ", string(messagesBody))
	jh.distributeMessages(messagesBody)
}

func (jh *jobsHandler) distributeMessages(messages []byte) {
	if len(messages) > 2 {
		jh.mu.Lock()
		defer jh.mu.Unlock()
		for _, job := range jh.jobs {
			if len(job.messagesChannel) < cap(job.messagesChannel) {
				job.messagesChannel <- messages
			} else {
				jh.emptyMessagesBuffer(job)
			}
		}
	}
}

func (jh *jobsHandler) emptyMessagesBuffer(job job) {
	log.Debug("Emptying message queue for job: ", job.jobInfo.InfoJobIdentity)
out:
	for {
		select {
		case <-job.messagesChannel:
		default:
			break out
		}
	}
}

func (jh *jobsHandler) monitorManagementChannels() {
	select {
	case addedJob := <-jh.addJobCh:
		jh.addJob(addedJob)
	case deletedJob := <-jh.deleteJobCh:
		jh.deleteJob(deletedJob)
	}
}

func (jh *jobsHandler) addJob(addedJob JobInfo) {
	jh.mu.Lock()
	log.Debug("Add job: ", addedJob)
	newJob := newJob(addedJob, jh.distributeClient)
	go newJob.start()
	jh.jobs[addedJob.InfoJobIdentity] = newJob
	jh.mu.Unlock()
}

func (jh *jobsHandler) deleteJob(deletedJob string) {
	jh.mu.Lock()
	log.Debug("Delete job: ", deletedJob)
	j, exist := jh.jobs[deletedJob]
	if exist {
		j.controlChannel <- struct{}{}
		delete(jh.jobs, deletedJob)
	}
	jh.mu.Unlock()
}

type pollingAgent interface {
	pollMessages() ([]byte, error)
}

func createPollingAgent(typeDef config.TypeDefinition, mRAddress string, pollClient restclient.HTTPClient, kafkaFactory kafkaconsumer.KafkaFactory, topicID string) pollingAgent {
	if typeDef.DMaaPTopicURL != "" {
		return dMaaPPollingAgent{
			mRURL:      mRAddress + typeDef.DMaaPTopicURL,
			pollClient: pollClient,
		}
	} else {
		return newKafkaPollingAgent(kafkaFactory, typeDef.KafkaInputTopic)
	}
}

type dMaaPPollingAgent struct {
	mRURL      string
	pollClient restclient.HTTPClient
}

func (pa dMaaPPollingAgent) pollMessages() ([]byte, error) {
	return restclient.Get(pa.mRURL, pa.pollClient)
}

type kafkaPollingAgent struct {
	kafkaConsumer kafkaconsumer.KafkaConsumer
}

func newKafkaPollingAgent(kafkaFactory kafkaconsumer.KafkaFactory, topicID string) kafkaPollingAgent {
	c, err := kafkaFactory.NewKafkaConsumer(topicID)
	if err != nil {
		log.Fatalf("Cannot create consumer for topic: %v, error details: %v\n", topicID, err)
	}
	c.Commit()
	err = c.Subscribe(topicID)
	if err != nil {
		log.Fatalf("Cannot subscribe to topic: : %v, error details: %v\n", topicID, err)
	}
	return kafkaPollingAgent{
		kafkaConsumer: c,
	}
}

func (pa kafkaPollingAgent) pollMessages() ([]byte, error) {
	maxDur := time.Second
	msg, err := pa.kafkaConsumer.ReadMessage(maxDur)
	if err == nil {
		return msg.Value, nil
	} else {
		if isKafkaTimedOutError(err) {
			return []byte(""), nil
		}
		return nil, err
	}
}

func isKafkaTimedOutError(err error) bool {
	kafkaErr, ok := err.(kafka.Error)
	return ok && kafkaErr.Code() == kafka.ErrTimedOut
}

type job struct {
	jobInfo         JobInfo
	client          restclient.HTTPClient
	messagesChannel chan []byte
	controlChannel  chan struct{}
}

func newJob(j JobInfo, c restclient.HTTPClient) job {
	return job{
		jobInfo:         j,
		client:          c,
		messagesChannel: make(chan []byte, 10),
		controlChannel:  make(chan struct{}),
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

func (j *job) sendMessagesToConsumer(messages []byte) {
	log.Debug("Processing job: ", j.jobInfo.InfoJobIdentity)
	if postErr := restclient.Post(j.jobInfo.TargetUri, messages, j.client); postErr != nil {
		log.Warnf("Error posting data for job: %v. Cause: %v", j.jobInfo, postErr)
	}
	log.Debugf("Messages for job: %v distributed to consumer: %v", j.jobInfo.InfoJobIdentity, j.jobInfo.Owner)
}
