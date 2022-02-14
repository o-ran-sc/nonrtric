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
	"fmt"
	"net/http"
	"os"
	"time"

	"github.com/gorilla/mux"
	"oransc.org/usecase/oruclosedloop/internal/ves"
)

var started bool

func main() {
	r := mux.NewRouter()
	r.HandleFunc("/create/{jobId}", createJobHandler).Methods(http.MethodPut)
	r.HandleFunc("/delete/{jobId}", deleteJobHandler).Methods(http.MethodDelete)

	fmt.Println("Listening on port 8085")
	fmt.Println(http.ListenAndServe(":8085", r))
}

func createJobHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, ok := vars["jobId"]
	if !ok {
		http.Error(w, "No job ID provided", http.StatusBadRequest)
		return
	}

	started = true
	fmt.Println("Start pushing messages for job: ", id)
	go startPushingMessages()
}

func deleteJobHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, ok := vars["jobId"]
	if !ok {
		http.Error(w, "No job ID provided", http.StatusBadRequest)
		return
	}

	fmt.Println("Stop pushing messages for job: ", id)
	started = false
}

func getEnv(key string, defaultVal string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}

	return defaultVal
}

func startPushingMessages() {
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
		if !started {
			break
		}
		if critical {
			message.Event.FaultFields.EventSeverity = "CRITICAL"
			critical = false
		} else {
			critical = true
			message.Event.FaultFields.EventSeverity = "NORMAL"
		}
		m, _ := json.Marshal(message)
		msgToSend, _ := json.Marshal([]string{string(m)})

		oru_addr := getEnv("ORU_ADDR", "http://oru-app:8086")
		req, _ := http.NewRequest(http.MethodPost, oru_addr, bytes.NewBuffer(msgToSend))
		req.Header.Set("Content-Type", "application/json; charset=utf-8")

		r, err := client.Do(req)
		if err != nil {
			fmt.Println("Error sending to consumer: ", err)
		}
		fmt.Printf("Sent %v message to consumer. Got response %v\n", message.Event.FaultFields.EventSeverity, r.Status)
	}
}
