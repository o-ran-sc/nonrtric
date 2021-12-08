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

type SliceAssuranceMeas struct {
	Metrics  []*SliceMetric
	Policies map[string]*PolicyRatio
}

func NewSliceAssuranceMeas() *SliceAssuranceMeas {
	s := SliceAssuranceMeas{}
	s.Metrics = make([]*SliceMetric, 0)
	s.Policies = make(map[string]*PolicyRatio)
	return &s
}

func (sa *SliceAssuranceMeas) AddNewPolicy(pr *PolicyRatio) {
	sa.Policies[pr.PolicyRatioId] = pr
}

func (sa *SliceAssuranceMeas) GetSliceMetric(duid string, sd int, sst int) *SliceMetric {
	for _, metric := range sa.Metrics {
		if metric.DUId == duid && metric.SliceDiff == sd && metric.SliceServiceType == sst {
			return metric
		}
	}
	return nil
}

func (sa *SliceAssuranceMeas) AddOrUpdateMetric(sm *SliceMetric) {
	metric := sa.GetSliceMetric(sm.DUId, sm.SliceDiff, sm.SliceServiceType)
	if metric != nil {
		for key, value := range sm.PM {
			metric.PM[key] = value
		}
	} else {
		sa.Metrics = append(sa.Metrics, sm)
	}
}
