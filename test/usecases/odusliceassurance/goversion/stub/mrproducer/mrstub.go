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

func main() {
	rand.Seed(time.Now().UnixNano())
	port := flag.Int("port", 3905, "The port this message router will listen on")
	flag.Parse()

	r := mux.NewRouter()
	r.HandleFunc("/events/unauthenticated.PERFORMANCE_MEASUREMENTS", sendStdMessage).Methods(http.MethodGet)

	fmt.Println("Starting mr on port: ", *port)

	http.ListenAndServe(fmt.Sprintf(":%v", *port), r)

}

// Variables ::
// DU-ID: ERICSSON-O-DU-11220
// Cell-ID: cell1
// Slice-Diff: 2
// Value: 300
func sendStdMessage(w http.ResponseWriter, r *http.Request) {
	message := fetchMessage()
	fmt.Println("-----------------------------------------------------------------------------")
	fmt.Println("Sending message: ", message)
	fmt.Println("-----------------------------------------------------------------------------")
	response, _ := json.Marshal(message)
	time.Sleep(time.Duration(rand.Intn(3)) * time.Second)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write(response)
}

func fetchMessage() messages.StdDefinedMessage {

	index := rand.Intn(5)
	fmt.Println(index)

	measurements := [5][]messages.Measurement{meas1, meas2, meas3, meas4, meas5}

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
					DataId:       "id",
					Measurements: measurements[index],
				},
			},
		},
	}
	return message
}

var meas1 = []messages.Measurement{
	{
		MeasurementTypeInstanceReference: "/network-function/distributed-unit-functions[id='ERICSSON-O-DU-11220']/cell[id='cell1']/supported-measurements/performance-measurement-type[.='user-equipment-average-throughput-downlink']/supported-snssai-subcounter-instances/slice-differentiator[.=2][slice-service-type=1]",
		Value:                            300,
		Unit:                             "kbit/s",
	},
}

var meas2 = []messages.Measurement{
	{
		MeasurementTypeInstanceReference: "/network-function/distributed-unit-functions[id='ERICSSON-O-DU-11220']/cell[id='cell1']/supported-measurements/performance-measurement-type[.='user-equipment-average-throughput-downlink']/supported-snssai-subcounter-instances/slice-differentiator[.=1]",
		Value:                            400,
		Unit:                             "kbit/s",
	},
}

var meas3 = []messages.Measurement{
	{
		MeasurementTypeInstanceReference: "/network-function/distributed-unit-functions[id='ERICSSON-O-DU-11220']/cell[id='cell1']/supported-measurements/performance-measurement-type[.='user-equipment-average-throughput-uplink']/supported-snssai-subcounter-instances/slice-differentiator[.=2][slice-service-type=2]",
		Value:                            800,
		Unit:                             "kbit/s",
	},
}

var meas4 = []messages.Measurement{
	{
		MeasurementTypeInstanceReference: "/network-function/distributed-unit-functions[id='ERICSSON-O-DU-11220']/cell[id='cell1']/supported-measurements/performance-measurement-type[.='user-equipment-average-throughput-downlink']/supported-snssai-subcounter-instances/slice-differentiator[.=1]",
		Value:                            750,
		Unit:                             "kbit/s",
	},
}

var meas5 = []messages.Measurement{
	{
		MeasurementTypeInstanceReference: "/network-function/distributed-unit-functions[id='ERICSSON-O-DU-11220']/cell[id='cell1']/supported-measurements/performance-measurement-type[.='user-equipment-average-throughput-downlink']/supported-snssai-subcounter-instances/[slice-differentiator[.=2]][slice-service-type=1]",
		Value:                            900,
		Unit:                             "kbit/s",
	},
}
