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

import "testing"

func TestMessage_isFault(t *testing.T) {
	type fields struct {
		Event Event
	}
	tests := []struct {
		name   string
		fields fields
		want   bool
	}{
		{
			name: "is Fault",
			fields: fields{
				Event: Event{
					CommonEventHeader: CommonEventHeader{
						Domain: "fault",
					},
				},
			},
			want: true,
		},
		{
			name: "is not Fault",
			fields: fields{
				Event: Event{},
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			message := FaultMessage{
				Event: tt.fields.Event,
			}
			if got := message.isFault(); got != tt.want {
				t.Errorf("Message.isFault() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMessage_isLinkAlarm(t *testing.T) {
	type fields struct {
		Event Event
	}
	tests := []struct {
		name   string
		fields fields
		want   bool
	}{
		{
			name: "is Link alarm",
			fields: fields{
				Event: Event{
					FaultFields: FaultFields{
						AlarmCondition: "28",
					},
				},
			},
			want: true,
		},
		{
			name: "is not Link alarm",
			fields: fields{
				Event: Event{
					FaultFields: FaultFields{
						AlarmCondition: "2",
					},
				},
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			message := FaultMessage{
				Event: tt.fields.Event,
			}
			if got := message.isLinkAlarm(); got != tt.want {
				t.Errorf("Message.isLinkAlarm() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMessage_isSeverityNormal(t *testing.T) {
	type fields struct {
		Event Event
	}
	tests := []struct {
		name   string
		fields fields
		want   bool
	}{
		{
			name: "is severity NORMAL",
			fields: fields{
				Event: Event{
					FaultFields: FaultFields{
						EventSeverity: "NORMAL",
					},
				},
			},
			want: true,
		},
		{
			name: "is not severity NORMAL",
			fields: fields{
				Event: Event{
					FaultFields: FaultFields{
						AlarmCondition: "ERROR",
					},
				},
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			message := FaultMessage{
				Event: tt.fields.Event,
			}
			if got := message.isSeverityNormal(); got != tt.want {
				t.Errorf("Message.isSeverityNormal() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMessage_IsLinkFailure(t *testing.T) {
	type fields struct {
		Event Event
	}
	tests := []struct {
		name   string
		fields fields
		want   bool
	}{
		{
			name: "is Link Failure",
			fields: fields{
				Event: Event{
					CommonEventHeader: CommonEventHeader{
						Domain: "fault",
					},
					FaultFields: FaultFields{
						AlarmCondition: "28",
						EventSeverity:  "ERROR",
					},
				},
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			message := FaultMessage{
				Event: tt.fields.Event,
			}
			if got := message.IsLinkFailure(); got != tt.want {
				t.Errorf("Message.IsLinkFailure() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMessage_IsClearLinkFailure(t *testing.T) {
	type fields struct {
		Event Event
	}
	tests := []struct {
		name   string
		fields fields
		want   bool
	}{
		{
			name: "is not Link Failure",
			fields: fields{
				Event: Event{
					CommonEventHeader: CommonEventHeader{
						Domain: "fault",
					},
					FaultFields: FaultFields{
						AlarmCondition: "28",
						EventSeverity:  "NORMAL",
					},
				},
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			message := FaultMessage{
				Event: tt.fields.Event,
			}
			if got := message.IsClearLinkFailure(); got != tt.want {
				t.Errorf("Message.IsClearLinkFailure() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMessage_GetORuId(t *testing.T) {
	type fields struct {
		Event Event
	}
	tests := []struct {
		name   string
		fields fields
		want   string
	}{
		{
			name: "is not Link Failure",
			fields: fields{
				Event: Event{
					CommonEventHeader: CommonEventHeader{
						SourceName: "O-RU-ID",
					},
					FaultFields: FaultFields{
						AlarmCondition: "28",
						EventSeverity:  "NORMAL",
					},
				},
			},
			want: "O-RU-ID",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			message := FaultMessage{
				Event: tt.fields.Event,
			}
			if got := message.GetORuId(); got != tt.want {
				t.Errorf("Message.GetORuId() = %v, want %v", got, tt.want)
			}
		})
	}
}
