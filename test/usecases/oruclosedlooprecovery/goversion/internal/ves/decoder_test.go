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

package ves

import (
	"reflect"
	"testing"
)

func TestGetFaultMessages(t *testing.T) {
	type args struct {
		messageStrings *[]string
	}
	tests := []struct {
		name string
		args args
		want []FaultMessage
	}{
		{
			name: "",
			args: args{
				messageStrings: &[]string{"{\"event\":{\"commonEventHeader\":{\"domain\":\"heartbeat\"}}}",
					`{"event":{"commonEventHeader":{"domain":"fault","sourceName":"ERICSSON-O-RU-11220"},"faultFields":{"eventSeverity":"CRITICAL","alarmCondition":"28"}}}`},
			},
			want: []FaultMessage{{
				Event: Event{
					CommonEventHeader: CommonEventHeader{
						Domain:     "fault",
						SourceName: "ERICSSON-O-RU-11220",
					},
					FaultFields: FaultFields{
						AlarmCondition: "28",
						EventSeverity:  "CRITICAL",
					},
				},
			}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetFaultMessages(tt.args.messageStrings); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetFaultMessages() = %v, want %v", got, tt.want)
			}
		})
	}
}
