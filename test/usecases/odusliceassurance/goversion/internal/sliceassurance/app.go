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
	"fmt"
	"net/http"
	"strconv"
	"time"

	"oransc.org/usecase/oduclosedloop/internal/restclient"
	"oransc.org/usecase/oduclosedloop/internal/structures"
	"oransc.org/usecase/oduclosedloop/messages"
)

const THRESHOLD_TPUT int = 700
const DEFAULT_DEDICATED_RATIO int = 40
const NEW_DEDICATED_RATIO int = 50
const NODE_ID string = "O-DU-1211"

type App struct {
	Client          restclient.HTTPClient
	MetricsPolicies *structures.SliceAssuranceMeas
}

var DmaapMRUrl string
var SDNRUrl string

func (a *App) Initialize(dmaapUrl string, sdnrUrl string) {
	DmaapMRUrl = dmaapUrl
	SDNRUrl = sdnrUrl

	a.Client = restclient.New(&http.Client{})
	a.MetricsPolicies = structures.NewSliceAssuranceMeas()
}

func (a *App) Run(topic string, pollTime int) {
	for {
		fmt.Printf("Polling new messages from DmaapMR\n")
		var stdMessage messages.StdDefinedMessage

		a.Client.Get(DmaapMRUrl+topic, &stdMessage)

		a.processMessages(stdMessage)
		a.checkThresholdInMetrics()
		time.Sleep(time.Second * time.Duration(pollTime))
	}
}

func (a *App) processMessages(stdMessage messages.StdDefinedMessage) {

	for _, meas := range stdMessage.GetMeasurements() {

		fmt.Printf("New measurement: %+v\n", meas)
		//Create sliceMetric and check if metric exist and update existing one or create new one
		tmpSm := meas.CreateSliceMetric()
		a.MetricsPolicies.AddOrUpdateMetric(tmpSm)

		//Fetch policy ratio metrics from SDNR
		var duRRMPolicyRatio messages.ORanDuRestConf
		a.Client.Get(getUrlForDistributedUnitFunctions(SDNRUrl, tmpSm.DUId), &duRRMPolicyRatio)

		//Get DuId and check if we have metrics for it
		policyRatioDuId := duRRMPolicyRatio.DistributedUnitFunction.Id
		policies := duRRMPolicyRatio.DistributedUnitFunction.RRMPolicyRatio
		for _, policy := range policies {
		members:
			for _, member := range policy.RRMPolicyMembers {
				metric := a.MetricsPolicies.GetSliceMetric(policyRatioDuId, member.SliceDifferentiator, member.SliceServiceType)
				if metric != nil {
					a.MetricsPolicies.AddNewPolicy(addOrUpdatePolicyRatio(metric, policy))
					break members
				}
			}
		}
	}
}

func (a *App) checkThresholdInMetrics() {
	for _, metric := range a.MetricsPolicies.Metrics {
		for key, value := range metric.PM {

			if (value) > THRESHOLD_TPUT {
				fmt.Printf("PM: [%v, %v] exceeds threshold value!\n", key, value)

				//Check if RRMPolicyDedicatedRatio is higher than default value
				policy := a.MetricsPolicies.Policies[metric.RRMPolicyRatioId]

				if policy != nil && policy.PolicyDedicatedRatio <= DEFAULT_DEDICATED_RATIO {
					//Send PostRequest to update DedicatedRatio
					url := getUrlUpdatePolicyDedicatedRatio(SDNRUrl, metric.DUId, policy.PolicyRatioId)
					a.Client.Post(url, messages.GetDedicatedRatioUpdateMessage(*metric, *policy, NEW_DEDICATED_RATIO), nil)
				}
			}
		}
	}
}

func addOrUpdatePolicyRatio(metric *structures.SliceMetric, policy messages.RRMPolicyRatio) *structures.PolicyRatio {
	if metric.RRMPolicyRatioId == "" {
		metric.RRMPolicyRatioId = policy.Id
	}
	return &structures.PolicyRatio{
		PolicyRatioId:        policy.Id,
		PolicyMaxRatio:       policy.RRMPolicyMaxRatio,
		PolicyMinRatio:       policy.RRMPolicyMinRatio,
		PolicyDedicatedRatio: toInt(policy.RRMPolicyDedicatedRatio),
	}
}

func toInt(num string) int {
	res, err := strconv.Atoi(num)
	if err != nil {
		return -1
	}
	return res
}

func getUrlForDistributedUnitFunctions(host string, duid string) string {
	return host + "/rests/data/network-topology:network-topology/topology=topology-netconf/node=" + NODE_ID + "/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions=" + duid
}

func getUrlUpdatePolicyDedicatedRatio(host string, duid string, policyid string) string {
	return host + "/rests/data/network-topology:network-topology/topology=topology-netconf/node=" + NODE_ID + "/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions=" + duid + "/radio-resource-management-policy-ratio=" + policyid
}
