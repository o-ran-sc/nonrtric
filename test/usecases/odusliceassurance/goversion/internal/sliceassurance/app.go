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
	"net/http"
	"time"

	"oransc.org/usecase/oduclosedloop/internal/restclient"
	"oransc.org/usecase/oduclosedloop/internal/structures"
	"oransc.org/usecase/oduclosedloop/messages"

	log "github.com/sirupsen/logrus"
)

const (
	THRESHOLD_TPUT          = 700
	DEFAULT_DEDICATED_RATIO = 40
	NEW_DEDICATED_RATIO     = 50
	NODE_ID                 = "O-DU-1211"
)

type App struct {
	client          restclient.HTTPClient
	metricsPolicies *structures.SliceAssuranceMeas
}

var dmaapMRUrl string
var sDNRUrl string

func (a *App) Initialize(dmaapUrl string, sdnrUrl string) {
	dmaapMRUrl = dmaapUrl
	sDNRUrl = sdnrUrl

	a.client = restclient.New(&http.Client{})
	a.metricsPolicies = structures.NewSliceAssuranceMeas()
}

func (a *App) Run(topic string, pollTime int) {
	for {
		a.getMessagesFromDmaap(dmaapMRUrl + topic)

		for key := range a.metricsPolicies.Metrics {
			a.getRRMInformation(key.Duid)
		}
		a.updateDedicatedRatio()

		//Print structures
		a.metricsPolicies.PrintStructures()

		time.Sleep(time.Second * time.Duration(pollTime))
	}
}

func (a *App) getMessagesFromDmaap(url string) {
	log.Info("Polling new messages from DmaapMR")
	var stdMessage messages.StdDefinedMessage

	a.client.Get(url, &stdMessage)
	for _, meas := range stdMessage.GetMeasurements() {
		//Create sliceMetric and check if metric exist and update existing one or create new one
		if _, err := a.metricsPolicies.AddOrUpdateMetric(meas); err != nil {
			log.Info("Metric could not be added ", err)
		}
	}
}

func (a *App) getRRMInformation(duid string) {
	var duRRMPolicyRatio messages.ORanDuRestConf
	a.client.Get(getUrlForDistributedUnitFunctions(sDNRUrl, duid), &duRRMPolicyRatio)

	policies := duRRMPolicyRatio.DistributedUnitFunction.RRMPolicyRatio
	for _, policy := range policies {
		a.metricsPolicies.AddNewPolicy(duid, policy)
	}
}

func (a *App) updateDedicatedRatio() {

	for _, metric := range a.metricsPolicies.Metrics {
		policy, check := a.metricsPolicies.Policies[metric.RRMPolicyRatioId]
		//TODO What happened if dedicated ratio is already higher that default and threshold is exceed?
		if check && policy.PolicyDedicatedRatio <= DEFAULT_DEDICATED_RATIO {
			//Send PostRequest to update DedicatedRatio
			log.Info("Send Post Request to update DedicatedRatio")
			url := getUrlUpdatePolicyDedicatedRatio(sDNRUrl, metric.DUId, policy.PolicyRatioId)
			a.client.Post(url, policy.GetUpdateDedicatedRatioMessage(metric.SliceDiff, metric.SliceServiceType, NEW_DEDICATED_RATIO), nil)
		}
	}
}

func getUrlForDistributedUnitFunctions(host string, duid string) string {
	return host + "/rests/data/network-topology:network-topology/topology=topology-netconf/node=" + NODE_ID + "/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions=" + duid
}

func getUrlUpdatePolicyDedicatedRatio(host string, duid string, policyid string) string {
	return host + "/rests/data/network-topology:network-topology/topology=topology-netconf/node=" + NODE_ID + "/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions=" + duid + "/radio-resource-management-policy-ratio=" + policyid
}
