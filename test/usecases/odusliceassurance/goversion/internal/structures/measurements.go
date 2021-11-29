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

package structures

import "oransc.org/usecase/oduclosedloop/messages"

type SliceMetric struct {
	DUId             string
	CellId           string
	SliceDiff        int
	SliceServiceType int
	RRMPolicyRatioId string
	PM               map[string]int
}

func NewSliceMetric(duid string, cellid string, sd int, sst int) *SliceMetric {
	sm := SliceMetric{
		DUId:             duid,
		CellId:           cellid,
		SliceDiff:        sd,
		SliceServiceType: sst,
	}
	sm.PM = make(map[string]int)
	return &sm
}

type PolicyRatio struct {
	PolicyRatioId        string
	PolicyMaxRatio       int
	PolicyMinRatio       int
	PolicyDedicatedRatio int
}

func NewPolicyRatio(id string, max_ratio int, min_ratio int, ded_ratio int) *PolicyRatio {
	pr := PolicyRatio{
		PolicyRatioId:        id,
		PolicyMaxRatio:       max_ratio,
		PolicyMinRatio:       min_ratio,
		PolicyDedicatedRatio: ded_ratio,
	}
	return &pr
}

func (pr *PolicyRatio) GetUpdateDedicatedRatioMessage(sd int, sst int, dedicatedRatio int) []messages.RRMPolicyRatio {
	message := messages.RRMPolicyRatio{
		Id:                      pr.PolicyRatioId,
		AdmState:                "Locked",
		UserLabel:               "Some user label",
		RRMPolicyMaxRatio:       pr.PolicyMaxRatio,
		RRMPolicyMinRatio:       pr.PolicyMinRatio,
		RRMPolicyDedicatedRatio: dedicatedRatio,
		ResourceType:            "prb",
		RRMPolicyMembers: []messages.RRMPolicyMember{
			{
				MobileCountryCode:   "046",
				MobileNetworkCode:   "651",
				SliceDifferentiator: sd,
				SliceServiceType:    sst,
			},
		},
	}
	return []messages.RRMPolicyRatio{message}
}
