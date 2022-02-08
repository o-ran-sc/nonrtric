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

type StdDefinedMessage struct {
	Event Event `json:"event"`
}

type Event struct {
	CommonEventHeader CommonEventHeader `json:"commonEventHeader"`
	StndDefinedFields StndDefinedFields `json:"stndDefinedFields"`
}

type CommonEventHeader struct {
	Domain                  string `json:"domain"`
	EventId                 string `json:"eventId"`
	EventName               string `json:"eventName"`
	EventType               string `json:"eventType"`
	Sequence                int    `json:"sequence"`
	Priority                string `json:"priority"`
	ReportingEntityId       string `json:"reportingEntityId"`
	ReportingEntityName     string `json:"reportingEntityName"`
	SourceId                string `json:"sourceId"`
	SourceName              string `json:"sourceName"`
	StartEpochMicrosec      int64  `json:"startEpochMicrosec"`
	LastEpochMicrosec       int64  `json:"lastEpochMicrosec"`
	NfNamingCode            string `json:"nfNamingCode"`
	NfVendorName            string `json:"nfVendorName"`
	StndDefinedNamespace    string `json:"stndDefinedNamespace"`
	TimeZoneOffset          string `json:"timeZoneOffset"`
	Version                 string `json:"version"`
	VesEventListenerVersion string `json:"vesEventListenerVersion"`
}

type StndDefinedFields struct {
	StndDefinedFieldsVersion string `json:"stndDefinedFieldsVersion"`
	SchemaReference          string `json:"schemaReference"`
	Data                     Data   `json:"data"`
}

type Data struct {
	DataId              string        `json:"id"`
	StartTime           string        `json:"start-time"`
	AdministrativeState string        `json:"administrative-state"`
	OperationalState    string        `json:"operational-state"`
	UserLabel           string        `json:"user-label"`
	JobTag              string        `json:"job-tag"`
	GranularityPeriod   int           `json:"granularity-period"`
	Measurements        []Measurement `json:"measurements"`
}

type Measurement struct {
	MeasurementTypeInstanceReference string `json:"measurement-type-instance-reference"`
	Value                            int    `json:"value"`
	Unit                             string `json:"unit"`
}

func (message StdDefinedMessage) GetMeasurements() []Measurement {
	return message.Event.StndDefinedFields.Data.Measurements
}
