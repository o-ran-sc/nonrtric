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
	"fmt"
	"regexp"
	"strconv"
	"strings"

	"oransc.org/usecase/oduclosedloop/internal/structures"
)

type StdDefinedMessage struct {
	Event Event `json:"event"`
}

type Event struct {
	CommonEventHeader CommonEventHeader `json:"commonEventHeader"`
	StndDefinedFields StndDefinedFields `json:"stndDefinedFields"`
}

type CommonEventHeader struct {
	Domain               string `json:"domain"`
	StndDefinedNamespace string `json:"stndDefinedNamespace"`
}

type StndDefinedFields struct {
	StndDefinedFieldsVersion string `json:"stndDefinedFieldsVersion"`
	SchemaReference          string `json:"schemaReference"`
	Data                     Data   `json:"data"`
}

type Data struct {
	DataId       string        `json:"id"`
	Measurements []Measurement `json:"measurements"`
}

type Measurement struct {
	MeasurementTypeInstanceReference string `json:"measurement-type-instance-reference"`
	Value                            int    `json:"value"`
	Unit                             string `json:"unit"`
}

func (message StdDefinedMessage) GetMeasurements() []Measurement {
	return message.Event.StndDefinedFields.Data.Measurements
}

func (meas Measurement) CreateSliceMetric() *structures.SliceMetric {
	var pmName string
	var duid, cellid string
	var sd, sst int

	typeParts := strings.Split(meas.MeasurementTypeInstanceReference, "/")
	for _, part := range typeParts {
		if strings.Contains(part, "distributed-unit-functions") {
			duid = getValueInsideQuotes(part)

		} else if strings.Contains(part, "cell[") {
			cellid = getValueInsideQuotes(part)

		} else if strings.Contains(part, "performance-measurement-type") {
			pmName = getValueInsideQuotes(part)

		} else if strings.Contains(part, "slice-differentiator") {
			sd = getPropertyNumber(part)

		} else if strings.Contains(part, "slice-differentiator") {
			res, err := strconv.Atoi(getValueInsideQuotes(part))
			if err != nil {
				sst = -1
			}
			sst = res
		}
	}

	sm := structures.NewSliceMetric(duid, cellid, sd, sst)
	sm.PM[pmName] = meas.Value
	return sm
}

func getValueInsideQuotes(text string) string {
	re := regexp.MustCompile(`\'(.*?)\'`)

	match := re.FindAllString(text, -1)
	var res string
	if len(match) == 1 {
		res = strings.Trim(match[0], "'")
	}
	return res
}

func getPropertyNumber(text string) int {
	re := regexp.MustCompile("[0-9]+")
	match := re.FindAllString(text, -1)
	var res int
	var err error
	if len(match) == 1 {
		res, err = strconv.Atoi(match[0])
		if err != nil {
			fmt.Println(err)
			return -1
		}
	}
	return res
}
