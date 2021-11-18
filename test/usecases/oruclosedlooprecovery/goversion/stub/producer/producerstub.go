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

package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"time"

	"oransc.org/usecase/oruclosedloop/internal/ves"
)

func main() {
	message := ves.FaultMessage{
		Event: ves.Event{
			CommonEventHeader: ves.CommonEventHeader{
				Domain:     "fault",
				SourceName: "ERICSSON-O-RU-11220",
			},
			FaultFields: ves.FaultFields{
				AlarmCondition: "28",
			},
		},
	}
	client := &http.Client{
		Timeout: 5 * time.Second,
	}

	critical := true
	for range time.Tick(2 * time.Second) {
		if critical {
			message.Event.FaultFields.EventSeverity = "CRITICAL"
			critical = false
		} else {
			critical = true
			message.Event.FaultFields.EventSeverity = "NORMAL"
		}
		m, _ := json.Marshal(message)
		msgToSend, _ := json.Marshal([]string{string(m)})

		req, _ := http.NewRequest(http.MethodPost, "http://localhost:40935", bytes.NewBuffer(msgToSend))
		req.Header.Set("Content-Type", "application/json; charset=utf-8")

		client.Do(req)
	}

}
