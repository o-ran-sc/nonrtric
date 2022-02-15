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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"oransc.org/usecase/oduclosedloop/messages"
)

func TestAddMetric(t *testing.T) {
	assertions := require.New(t)
	type args struct {
		meas messages.Measurement
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "Test adding new metric",
			args: args{
				meas: messages.Measurement{
					MeasurementTypeInstanceReference: "/o-ran-sc-du-hello-world:network-function/distributed-unit-functions[id='O-DU-1211']/cell[id='cell-1']/supported-measurements[performance-measurement-type='user-equipment-average-throughput-uplink']/supported-snssai-subcounter-instances[slice-differentiator='1'][slice-service-type='1']",
					Value:                            51232,
					Unit:                             "kbit/s",
				},
			},
		},
		{
			name: "Test with invalid input",
			args: args{
				meas: messages.Measurement{
					MeasurementTypeInstanceReference: "/distributed-unit-functions[id='O-DU-1211']/cell[id='cell-1']/supported-measurements[performance-measurement-type='user-equipment-average-throughput-uplink']/supported-snssai-subcounter-instances[slice-differentiator='1'][slice-service-type='1']",
					Value:                            51232,
					Unit:                             "kbit/s",
				},
			},
		},
	}

	sliceAssuranceMeas := NewSliceAssuranceMeas()
	assertions.Equal(0, len(sliceAssuranceMeas.Metrics), "Metrics is not empty, got: %d, want: %d.", len(sliceAssuranceMeas.Metrics), 0)

	for i, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			if i == 0 {
				sliceAssuranceMeas.AddOrUpdateMetric(tt.args.meas)
				assertions.Equal(1, len(sliceAssuranceMeas.Metrics), "Metrics must have one new metric, got: %d, want: %d.", len(sliceAssuranceMeas.Metrics), 1)

				testMapKey := MapKey{"O-DU-1211", 1, 1}
				assertions.Contains(sliceAssuranceMeas.Metrics, testMapKey, "Metric added with wrong values , got: %v.", sliceAssuranceMeas.Metrics[testMapKey])
			}
			if i == 1 {
				_, got := sliceAssuranceMeas.AddOrUpdateMetric(tt.args.meas)
				assertions.EqualError(got, " wrong format for MeasurementTypeInstanceReference")
			}
		})
	}
}

func TestUpdateExistingMetric(t *testing.T) {
	assertions := require.New(t)
	meas := messages.Measurement{
		MeasurementTypeInstanceReference: "/o-ran-sc-du-hello-world:network-function/distributed-unit-functions[id='O-DU-1211']/cell[id='cell-1']/supported-measurements[performance-measurement-type='user-equipment-average-throughput-uplink']/supported-snssai-subcounter-instances[slice-differentiator='1'][slice-service-type='1']",
		Value:                            51232,
		Unit:                             "kbit/s",
	}

	updateMeas := messages.Measurement{
		MeasurementTypeInstanceReference: "/o-ran-sc-du-hello-world:network-function/distributed-unit-functions[id='O-DU-1211']/cell[id='cell-1']/supported-measurements[performance-measurement-type='user-equipment-average-throughput-uplink']/supported-snssai-subcounter-instances[slice-differentiator='1'][slice-service-type='1']",
		Value:                            897,
		Unit:                             "kbit/s",
	}

	sliceAssuranceMeas := NewSliceAssuranceMeas()
	assertions.Equal(0, len(sliceAssuranceMeas.Metrics), "Metrics is not empty, got: %d, want: %d.", len(sliceAssuranceMeas.Metrics), 0)

	sliceAssuranceMeas.AddOrUpdateMetric(meas)
	assertions.Equal(1, len(sliceAssuranceMeas.Metrics), "Metrics must have one new metric, got: %d, want: %d.", len(sliceAssuranceMeas.Metrics), 1)

	sliceAssuranceMeas.AddOrUpdateMetric(updateMeas)
	assertions.Equal(1, len(sliceAssuranceMeas.Metrics), "Metrics must have one updated metric, got: %d, want: %d.", len(sliceAssuranceMeas.Metrics), 1)

	testMapKey := MapKey{"O-DU-1211", 1, 1}
	metricName := "user-equipment-average-throughput-uplink"
	newMetricValue := 897
	if sliceAssuranceMeas.Metrics[testMapKey].PM[metricName] != newMetricValue {
		t.Errorf("Metric value was not update properly, got: %d, want: %d.", sliceAssuranceMeas.Metrics[testMapKey].PM[metricName], newMetricValue)
	}

}

func TestDeleteMetricWhenValueLessThanThreshold(t *testing.T) {

	meas := messages.Measurement{
		MeasurementTypeInstanceReference: "/o-ran-sc-du-hello-world:network-function/distributed-unit-functions[id='O-DU-1211']/cell[id='cell-1']/supported-measurements[performance-measurement-type='user-equipment-average-throughput-uplink']/supported-snssai-subcounter-instances[slice-differentiator='1'][slice-service-type='1']",
		Value:                            51232,
		Unit:                             "kbit/s",
	}

	newMeas := messages.Measurement{
		MeasurementTypeInstanceReference: "/o-ran-sc-du-hello-world:network-function/distributed-unit-functions[id='O-DU-1211']/cell[id='cell-1']/supported-measurements[performance-measurement-type='user-equipment-average-throughput-uplink']/supported-snssai-subcounter-instances[slice-differentiator='1'][slice-service-type='1']",
		Value:                            50,
		Unit:                             "kbit/s",
	}

	sliceAssuranceMeas := NewSliceAssuranceMeas()
	assert.Equal(t, 0, len(sliceAssuranceMeas.Metrics), "Metrics is not empty, got: %d, want: %d.", len(sliceAssuranceMeas.Metrics), 0)

	sliceAssuranceMeas.AddOrUpdateMetric(meas)
	assert.Equal(t, 1, len(sliceAssuranceMeas.Metrics), "Metrics must have one new metric, got: %d, want: %d.", len(sliceAssuranceMeas.Metrics), 1)

	sliceAssuranceMeas.AddOrUpdateMetric(newMeas)
	assert.Equal(t, 0, len(sliceAssuranceMeas.Metrics), "Metrics must have been deleted, got: %d, want: %d.", len(sliceAssuranceMeas.Metrics), 0)

}

func TestAddPolicy(t *testing.T) {

	meas := messages.Measurement{
		MeasurementTypeInstanceReference: "/o-ran-sc-du-hello-world:network-function/distributed-unit-functions[id='O-DU-1211']/cell[id='cell-1']/supported-measurements[performance-measurement-type='user-equipment-average-throughput-uplink']/supported-snssai-subcounter-instances[slice-differentiator='1'][slice-service-type='1']",
		Value:                            51232,
		Unit:                             "kbit/s",
	}
	sliceAssuranceMeas := NewSliceAssuranceMeas()
	sliceAssuranceMeas.AddOrUpdateMetric(meas)

	duid := "O-DU-1211"
	rrmPolicyRatio := messages.RRMPolicyRatio{
		Id:                      "id",
		AdmState:                "locked",
		UserLabel:               "user_label",
		RRMPolicyMaxRatio:       0,
		RRMPolicyMinRatio:       0,
		RRMPolicyDedicatedRatio: 0,
		ResourceType:            "prb",
		RRMPolicyMembers: []messages.RRMPolicyMember{{
			MobileCountryCode:   "046",
			MobileNetworkCode:   "651",
			SliceDifferentiator: 1,
			SliceServiceType:    1,
		}},
	}
	assert.Equal(t, 0, len(sliceAssuranceMeas.Policies), "Policies is not empty, got: %d, want: %d.", len(sliceAssuranceMeas.Policies), 0)

	sliceAssuranceMeas.AddNewPolicy(duid, rrmPolicyRatio)
	assert.Equal(t, 1, len(sliceAssuranceMeas.Policies), "Policies must have one new policy, got: %d, want: %d.", len(sliceAssuranceMeas.Policies), 1)

	sliceAssuranceMeas.PrintStructures()
}
