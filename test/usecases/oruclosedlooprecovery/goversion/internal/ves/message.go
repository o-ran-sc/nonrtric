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

type FaultMessage struct {
	Event Event `json:"event"`
}

type Event struct {
	CommonEventHeader CommonEventHeader `json:"commonEventHeader"`
	FaultFields       FaultFields       `json:"faultFields"`
}

type CommonEventHeader struct {
	Domain     string `json:"domain"`
	SourceName string `json:"sourceName"`
}

type FaultFields struct {
	AlarmCondition string `json:"alarmCondition"`
	EventSeverity  string `json:"eventSeverity"`
}

func (message FaultMessage) isFault() bool {
	return message.Event.CommonEventHeader.Domain == "fault"
}

func (message FaultMessage) isLinkAlarm() bool {
	return message.Event.FaultFields.AlarmCondition == "28"
}

func (message FaultMessage) isSeverityNormal() bool {
	return message.Event.FaultFields.EventSeverity == "NORMAL"
}

func (message FaultMessage) IsLinkFailure() bool {
	return message.isFault() && message.isLinkAlarm() && !message.isSeverityNormal()
}

func (message FaultMessage) IsClearLinkFailure() bool {
	return message.isFault() && message.isLinkAlarm() && message.isSeverityNormal()
}

func (message FaultMessage) GetORuId() string {
	return message.Event.CommonEventHeader.SourceName
}
