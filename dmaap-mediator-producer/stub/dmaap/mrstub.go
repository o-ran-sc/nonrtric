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
	"encoding/json"
	"flag"
	"fmt"
	"math/rand"
	"net/http"
	"time"
)

var r = rand.New(rand.NewSource(time.Now().UnixNano()))

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

func main() {
	port := flag.Int("port", 3905, "The port this message router will listen on")
	flag.Parse()

	http.HandleFunc("/events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/STD_Fault_Messages", handleData)

	fmt.Print("Starting mr on port: ", *port)
	http.ListenAndServeTLS(fmt.Sprintf(":%v", *port), "../../configs/producer.crt", "../../configs/producer.key", nil)

}

var critical = true

func handleData(w http.ResponseWriter, req *http.Request) {
	time.Sleep(time.Duration(r.Intn(3)) * time.Second)

	w.Header().Set("Content-Type", "application/json")

	var responseBody []byte
	if critical {
		responseBody = getFaultMessage("CRITICAL")
		critical = false
	} else {
		responseBody = getFaultMessage("NORMAL")
		critical = true
	}
	// w.Write(responseBody)
	fmt.Fprint(w, string(responseBody))
}

func getFaultMessage(eventSeverity string) []byte {
	linkFailureMessage := FaultMessage{
		Event: Event{
			CommonEventHeader: CommonEventHeader{
				Domain:     "fault",
				SourceName: "ERICSSON-O-RU-11220",
			},
			FaultFields: FaultFields{
				AlarmCondition: "28",
				EventSeverity:  eventSeverity,
			},
		},
	}
	fmt.Printf("Sending message: %v\n", linkFailureMessage)

	messageAsByteArray, _ := json.Marshal(linkFailureMessage)
	response := [1]string{string(messageAsByteArray)}
	responseAsByteArray, _ := json.Marshal(response)
	return responseAsByteArray
}
