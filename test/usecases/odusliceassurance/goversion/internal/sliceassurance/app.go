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

package sliceassurance

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"oransc.org/usecase/oduclosedloop/internal/config"
	"oransc.org/usecase/oduclosedloop/internal/restclient"
	"oransc.org/usecase/oduclosedloop/internal/structures"
	"oransc.org/usecase/oduclosedloop/messages"

	log "github.com/sirupsen/logrus"
)

const (
	THRESHOLD_TPUT          = 7000
	DEFAULT_DEDICATED_RATIO = 15
	NEW_DEDICATED_RATIO     = 25
	NODE_ID                 = "O-DU-1122"
)

type App struct {
	client          *restclient.Client
	metricsPolicies *structures.SliceAssuranceMeas
}

var dmaapMRUrl string
var sDNRUrl string
var sDNRUsername string
var sDNRPassword string

func (a *App) Initialize(config *config.Config) {
	dmaapMRUrl = config.MRHost + ":" + config.MRPort
	sDNRUrl = config.SDNRAddress
	sDNRUsername = config.SDNRUser
	sDNRPassword = config.SDNPassword

	a.client = restclient.New(&http.Client{}, false)
	a.metricsPolicies = structures.NewSliceAssuranceMeas()
}

func (a *App) Run(topic string, pollTime int) {
	for {
		a.getMessagesFromDmaap(dmaapMRUrl + topic)

		for key := range a.metricsPolicies.Metrics {
			a.getRRMInformation(key.Duid)
		}
		a.updateDedicatedRatio()

		time.Sleep(time.Second * time.Duration(pollTime))
	}
}

func (a *App) getMessagesFromDmaap(path string) {
	log.Infof("Polling new messages from DmaapMR %v", path)

	//Added to work with onap-Dmaap
	var messageStrings []string
	if error := a.client.Get(path, &messageStrings); error != nil {
		log.Warn("Send of Get messages from DmaapMR failed! ", error)
	}

	for _, msgString := range messageStrings {
		var message messages.StdDefinedMessage
		if err := json.Unmarshal([]byte(msgString), &message); err == nil {
			for _, meas := range message.GetMeasurements() {
				log.Infof("Create sliceMetric and check if metric exist and update existing one or create new one measurement:  %+v\n", meas)
				//Create sliceMetric and check if metric exist and update existing one or create new one
				if _, err := a.metricsPolicies.AddOrUpdateMetric(meas); err != nil {
					log.Error("Metric could not be added ", err)
				}
			}
		} else {
			log.Warn(err)
		}
	}
}

func (a *App) getRRMInformation(duid string) {
	var duRRMPolicyRatio messages.ORanDuRestConf

	log.Infof("Get RRM Information from SDNR url: %v", sDNRUrl)
	if error := a.client.Get(getUrlForDistributedUnitFunctions(sDNRUrl, duid), &duRRMPolicyRatio, sDNRUsername, sDNRPassword); error == nil {
		prettyPrint(duRRMPolicyRatio.DistributedUnitFunction)
	} else {
		log.Warn("Send of Get RRM Information failed! ", error)
	}

	for _, odu := range duRRMPolicyRatio.DistributedUnitFunction {
		for _, policy := range odu.RRMPolicyRatio {
			log.Infof("Add or Update policy: %+v from DU id: %v", policy.Id, duid)
			a.metricsPolicies.AddNewPolicy(duid, policy)
		}
	}
}

func (a *App) updateDedicatedRatio() {
	for _, metric := range a.metricsPolicies.Metrics {
		policy, check := a.metricsPolicies.Policies[metric.RRMPolicyRatioId]
		//TODO What happened if dedicated ratio is already higher that default and threshold is exceed?
		if check && policy.PolicyDedicatedRatio <= DEFAULT_DEDICATED_RATIO {
			log.Infof("Send Request to update DedicatedRatio for DU id: %v Policy id: %v", metric.DUId, policy.PolicyRatioId)
			path := getUrlUpdatePolicyDedicatedRatio(sDNRUrl, metric.DUId, policy.PolicyRatioId)
			updatePolicyMessage := policy.GetUpdateDedicatedRatioMessage(metric.SliceDiff, metric.SliceServiceType, NEW_DEDICATED_RATIO)
			prettyPrint(updatePolicyMessage)
			if error := a.client.Put(path, updatePolicyMessage, nil, sDNRUsername, sDNRPassword); error == nil {
				log.Infof("Policy Dedicated Ratio for PolicyId: %v was updated to %v", policy.PolicyRatioId, NEW_DEDICATED_RATIO)
			} else {
				log.Warn("Send of Put Request to update DedicatedRatio failed! ", error)
			}
		}
	}
}

func getUrlForDistributedUnitFunctions(host string, duid string) string {
	return host + "/rests/data/network-topology:network-topology/topology=topology-netconf/node=" + NODE_ID + "/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions=" + duid
}

func getUrlUpdatePolicyDedicatedRatio(host string, duid string, policyid string) string {
	return host + "/rests/data/network-topology:network-topology/topology=topology-netconf/node=" + NODE_ID + "/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions=" + duid + "/radio-resource-management-policy-ratio=" + policyid
}

func prettyPrint(jsonStruct interface{}) {
	b, err := json.MarshalIndent(jsonStruct, "", "  ")
	if err != nil {
		fmt.Println("error:", err)
	}
	fmt.Print(string(b))
}
