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

import (
	"testing"
)

func TestGetMeasurements(t *testing.T) {
	type fields struct {
		Event Event
	}
	tests := []struct {
		name   string
		fields fields
		want   []Measurement
	}{
		{
			name: "get measurements message",
			fields: fields{
				Event: Event{
					CommonEventHeader: CommonEventHeader{
						Domain:               "stndDefined",
						StndDefinedNamespace: "o-ran-sc-du-hello-world-pm-streaming-oas3",
					},
					StndDefinedFields: StndDefinedFields{
						StndDefinedFieldsVersion: "1.0",
						SchemaReference:          "https://gerrit.o-ran-sc.org/r/gitweb?p=scp/oam/modeling.git;a=blob_plain;f=data-model/oas3/experimental/o-ran-sc-du-hello-world-oas3.json;hb=refs/heads/master",
						Data: Data{
							DataId: "id",
							Measurements: []Measurement{{
								MeasurementTypeInstanceReference: "/o-ran-sc-du-hello-world:network-function/distributed-unit-functions[id='O-DU-1211']/cell[id='cell-1']/supported-measurements[performance-measurement-type='user-equipment-average-throughput-uplink']/supported-snssai-subcounter-instances[slice-differentiator='1'][slice-service-type='1']",
								Value:                            51232,
								Unit:                             "kbit/s",
							}},
						},
					},
				},
			},
			want: []Measurement{{
				MeasurementTypeInstanceReference: "/o-ran-sc-du-hello-world:network-function/distributed-unit-functions[id='O-DU-1211']/cell[id='cell-1']/supported-measurements[performance-measurement-type='user-equipment-average-throughput-uplink']/supported-snssai-subcounter-instances[slice-differentiator='1'][slice-service-type='1']",
				Value:                            51232,
				Unit:                             "kbit/s",
			}},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			message := StdDefinedMessage{
				Event: tt.fields.Event,
			}
			if got := message.GetMeasurements(); len(got) != len(tt.want) {
				t.Errorf("Message.GetMeasurements() = %v, want %v", got, tt.want)
			}
		})
	}
}
