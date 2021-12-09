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

import (
	"fmt"
	"regexp"
	"strconv"

	"oransc.org/usecase/oduclosedloop/messages"
)

type MapKey struct {
	Duid string
	sd   int
	sst  int
}

type SliceAssuranceMeas struct {
	Metrics  map[MapKey]*SliceMetric
	Policies map[string]*PolicyRatio
}

func NewSliceAssuranceMeas() *SliceAssuranceMeas {
	s := SliceAssuranceMeas{}
	s.Metrics = make(map[MapKey]*SliceMetric)
	s.Policies = make(map[string]*PolicyRatio)
	return &s
}

func (sa *SliceAssuranceMeas) AddNewPolicy(duid string, rrmPolicyRatio messages.RRMPolicyRatio) {
	for _, policyMember := range rrmPolicyRatio.RRMPolicyMembers {
		metric := sa.GetSliceMetric(duid, policyMember.SliceDifferentiator, policyMember.SliceServiceType)
		if metric != nil {
			pr := NewPolicyRatio(rrmPolicyRatio.Id, rrmPolicyRatio.RRMPolicyMaxRatio, rrmPolicyRatio.RRMPolicyMinRatio, rrmPolicyRatio.RRMPolicyDedicatedRatio)
			sa.Policies[pr.PolicyRatioId] = pr
			metric.RRMPolicyRatioId = rrmPolicyRatio.Id
		}
	}
}

func (sa *SliceAssuranceMeas) GetSliceMetric(duid string, sd int, sst int) *SliceMetric {
	key := MapKey{duid, sd, sst}
	value, check := sa.Metrics[key]

	if check {
		return value
	}

	return nil
}

func (sa *SliceAssuranceMeas) AddOrUpdateMetric(meas messages.Measurement) (string, error) {

	var duid string
	var sd, sst int

	regex := *regexp.MustCompile(`\/network-function\/distributed-unit-functions\[id=\'(.*)\'\]/cell\[id=\'(.*)\'\]/supported-measurements\/performance-measurement-type\[\.=\'(.*)\'\]\/supported-snssai-subcounter-instances\/slice-differentiator\[\.=(\d)\]\[slice-service-type=(\d+)\]`)
	res := regex.FindAllStringSubmatch(meas.MeasurementTypeInstanceReference, -1)

	if res != nil && len(res[0]) == 6 {
		duid = res[0][1]
		sd = toInt(res[0][4])
		sst = toInt(res[0][5])

		key := MapKey{duid, sd, sst}
		value, check := sa.Metrics[key]

		if check {
			sa.updateMetric(key, value, res[0][3], meas.Value)
		} else {
			// Only add new one if value exceeds threshold
			sa.addMetric(res, meas.Value)
		}
	} else {
		return duid, fmt.Errorf(" wrong format for MeasurementTypeInstanceReference")
	}
	return duid, nil
}

func (sa *SliceAssuranceMeas) addMetric(res [][]string, metricValue int) {
	if metricValue > 700 {
		metric := NewSliceMetric(res[0][1], res[0][2], toInt(res[0][4]), toInt(res[0][5]))
		metric.PM[res[0][3]] = metricValue
		key := MapKey{res[0][1], toInt(res[0][4]), toInt(res[0][5])}
		sa.Metrics[key] = metric
	}
}

func (sa *SliceAssuranceMeas) updateMetric(key MapKey, value *SliceMetric, metricName string, metricValue int) {
	if metricValue < 700 {
		delete(sa.Metrics, key)
	} else {
		value.PM[metricName] = metricValue
	}
}

func toInt(num string) int {
	res, err := strconv.Atoi(num)
	if err != nil {
		return -1
	}
	return res
}

func (sa *SliceAssuranceMeas) PrintStructures() {
	fmt.Printf("SliceAssurance Metrics: \n")
	for key, metric := range sa.Metrics {
		fmt.Printf("Key: %+v\n", key)
		fmt.Printf("Metric: %+v\n", metric)
	}
	fmt.Printf("SliceAssurance Policies: \n")
	for key, metric := range sa.Policies {
		fmt.Printf("Key: %+v\n", key)
		fmt.Printf("Metric: %+v\n", metric)
	}
}
