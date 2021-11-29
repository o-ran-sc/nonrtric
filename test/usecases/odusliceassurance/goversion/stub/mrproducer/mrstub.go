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

	"github.com/gorilla/mux"
	"oransc.org/usecase/oduclosedloop/messages"
)

var rnd = rand.New(rand.NewSource(time.Now().UnixNano()))

func main() {
	port := flag.Int("port", 3905, "The port this message router will listen on")
	flag.Parse()

	r := mux.NewRouter()
	r.HandleFunc("/events/unauthenticated.PERFORMANCE_MEASUREMENTS", getStdVesMessage).Methods(http.MethodGet)

	fmt.Println("Starting mr on port: ", *port)

	http.ListenAndServe(fmt.Sprintf(":%v", *port), r)

}

// Variables ::
// DU-ID: ERICSSON-O-DU-11220
// Cell-ID: cell1
// Slice-Diff: 2
// Value: 300
func getStdVesMessage(w http.ResponseWriter, r *http.Request) {
	message := messages.StdDefinedMessage{
		Event: messages.Event{
			CommonEventHeader: messages.CommonEventHeader{
				Domain:               "stndDefined",
				StndDefinedNamespace: "o-ran-sc-du-hello-world-pm-streaming-oas3",
			},
			StndDefinedFields: messages.StndDefinedFields{
				StndDefinedFieldsVersion: "1.0",
				SchemaReference:          "https://gerrit.o-ran-sc.org/r/gitweb?p=scp/oam/modeling.git;a=blob_plain;f=data-model/oas3/experimental/o-ran-sc-du-hello-world-oas3.json;hb=refs/heads/master",
				Data: messages.Data{
					DataId: "id",
					Measurements: []messages.Measurement{
						{
							MeasurementTypeInstanceReference: "/network-function/distributed-unit-functions[id='ERICSSON-O-DU-11220']/cell[id='cell1']/supported-measurements/performance-measurement-type[.='user-equipment-average-throughput-downlink']/supported-snssai-subcounter-instances/slice-differentiator[.=2]",
							Value:                            700,
							Unit:                             "kbit/s",
						},
					},
				},
			},
		},
	}
	fmt.Println("-----------------------------------------------------------------------------")
	fmt.Println("Sending message: ", message)
	fmt.Println("-----------------------------------------------------------------------------")
	response, _ := json.Marshal(message)
	time.Sleep(time.Duration(rnd.Intn(3)) * time.Second)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write(response)
}
