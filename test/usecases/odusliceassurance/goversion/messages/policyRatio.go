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

package messages

type ORanDuRestConf struct {
	DistributedUnitFunction DistributedUnitFunction `json:"distributed-unit-functions"`
}

type DistributedUnitFunction struct {
	Id             string           `json:"id"`
	RRMPolicyRatio []RRMPolicyRatio `json:"radio-resource-management-policy-ratio"`
}

type RRMPolicyRatio struct {
	Id                      string            `json:"id"`
	AdmState                string            `json:"administrative-state"`
	UserLabel               string            `json:"user-label"`
	RRMPolicyMaxRatio       int               `json:"radio-resource-management-policy-max-ratio"`
	RRMPolicyMinRatio       int               `json:"radio-resource-management-policy-min-ratio"`
	RRMPolicyDedicatedRatio int               `json:"radio-resource-management-policy-dedicated-ratio"`
	ResourceType            string            `json:"resource-type"`
	RRMPolicyMembers        []RRMPolicyMember `json:"radio-resource-management-policy-members"`
}

type RRMPolicyMember struct {
	MobileCountryCode   string `json:"mobile-country-code"`
	MobileNetworkCode   string `json:"mobile-network-code"`
	SliceDifferentiator int    `json:"slice-differentiator"`
	SliceServiceType    int    `json:"slice-service-type"`
}
