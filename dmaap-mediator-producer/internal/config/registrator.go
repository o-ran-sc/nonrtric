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

package config

import (
	"encoding/json"
	"fmt"
	"net/url"

	log "github.com/sirupsen/logrus"

	"oransc.org/nonrtric/dmaapmediatorproducer/internal/restclient"
)

const registerTypePath = "/data-producer/v1/info-types/"
const registerProducerPath = "/data-producer/v1/info-producers/"

type TypeDefinition struct {
	Identity        string `json:"id"`
	DMaaPTopicURL   string `json:"dmaapTopicUrl"`
	KafkaInputTopic string `json:"kafkaInputTopic"`
	TypeSchema      interface{}
}

func (td TypeDefinition) IsKafkaType() bool {
	return td.KafkaInputTopic != ""
}

func (td TypeDefinition) IsDMaaPType() bool {
	return td.DMaaPTopicURL != ""
}

type ProducerRegistrationInfo struct {
	InfoProducerSupervisionCallbackUrl string   `json:"info_producer_supervision_callback_url"`
	SupportedInfoTypes                 []string `json:"supported_info_types"`
	InfoJobCallbackUrl                 string   `json:"info_job_callback_url"`
}

type Registrator interface {
	RegisterTypes(types []TypeDefinition) error
	RegisterProducer(producerId string, producerInfo *ProducerRegistrationInfo)
}

type RegistratorImpl struct {
	infoCoordinatorAddress string
	httpClient             restclient.HTTPClient
}

func NewRegistratorImpl(infoCoordAddr string, client restclient.HTTPClient) *RegistratorImpl {
	return &RegistratorImpl{
		infoCoordinatorAddress: infoCoordAddr,
		httpClient:             client,
	}
}

func (r RegistratorImpl) RegisterTypes(jobTypes []TypeDefinition) error {
	for _, jobType := range jobTypes {
		body := fmt.Sprintf(`{"info_job_data_schema": %v}`, jobType.TypeSchema)
		if error := restclient.Put(r.infoCoordinatorAddress+registerTypePath+url.PathEscape(jobType.Identity), []byte(body), r.httpClient); error != nil {
			return error
		}
		log.Debugf("Registered type: %v", jobType)
	}
	return nil
}

func (r RegistratorImpl) RegisterProducer(producerId string, producerInfo *ProducerRegistrationInfo) error {
	if body, marshalErr := json.Marshal(producerInfo); marshalErr == nil {
		if putErr := restclient.Put(r.infoCoordinatorAddress+registerProducerPath+url.PathEscape(producerId), []byte(body), r.httpClient); putErr != nil {
			return putErr
		}
		log.Debugf("Registered producer: %v", producerId)
		return nil
	} else {
		return marshalErr
	}
}
