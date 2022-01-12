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
	"net/http"

	"github.com/gorilla/mux"
	"oransc.org/usecase/oduclosedloop/messages"
)

func main() {
	port := flag.Int("port", 3904, "The port this SDNR stub will listen on")
	flag.Parse()

	r := mux.NewRouter()
	r.HandleFunc("/rests/data/network-topology:network-topology/topology=topology-netconf/node={NODE-ID}/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions={O-DU-ID}", getDistributedUnitFunctions).Methods(http.MethodGet)
	r.HandleFunc("/rests/data/network-topology:network-topology/topology=topology-netconf/node={NODE-ID}/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions={O-DU-ID}/radio-resource-management-policy-ratio={POLICY-ID}", updateRRMPolicyDedicatedRatio).Methods(http.MethodPost)

	fmt.Println("Starting SDNR on port: ", *port)
	http.ListenAndServe(fmt.Sprintf(":%v", *port), r)

}

func getDistributedUnitFunctions(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)

	message := messages.ORanDuRestConf{
		DistributedUnitFunction: messages.DistributedUnitFunction{
			Id: vars["O-DU-ID"],
			RRMPolicyRatio: []messages.RRMPolicyRatio{
				{
					Id:                      "rrm-pol-1",
					AdmState:                "locked",
					UserLabel:               "rrm-pol-1",
					RRMPolicyMaxRatio:       100,
					RRMPolicyMinRatio:       0,
					RRMPolicyDedicatedRatio: 0,
					ResourceType:            "prb",
					RRMPolicyMembers: []messages.RRMPolicyMember{
						{
							MobileCountryCode:   "046",
							MobileNetworkCode:   "651",
							SliceDifferentiator: 1,
							SliceServiceType:    0,
						},
					},
				},
				{
					Id:                      "rrm-pol-2",
					AdmState:                "unlocked",
					UserLabel:               "rrm-pol-2",
					RRMPolicyMaxRatio:       20,
					RRMPolicyMinRatio:       10,
					RRMPolicyDedicatedRatio: 15,
					ResourceType:            "prb",
					RRMPolicyMembers: []messages.RRMPolicyMember{
						{
							MobileCountryCode:   "046",
							MobileNetworkCode:   "651",
							SliceDifferentiator: 2,
							SliceServiceType:    1,
						},
					},
				},
				{
					Id:                      "rrm-pol-3",
					AdmState:                "unlocked",
					UserLabel:               "rrm-pol-3",
					RRMPolicyMaxRatio:       30,
					RRMPolicyMinRatio:       10,
					RRMPolicyDedicatedRatio: 5,
					ResourceType:            "prb",
					RRMPolicyMembers: []messages.RRMPolicyMember{
						{
							MobileCountryCode:   "310",
							MobileNetworkCode:   "150",
							SliceDifferentiator: 2,
							SliceServiceType:    2,
						},
					},
				},
			},
		},
	}

	respondWithJSON(w, http.StatusOK, message)
}

func updateRRMPolicyDedicatedRatio(w http.ResponseWriter, r *http.Request) {
	var prMessage messages.DistributedUnitFunction
	decoder := json.NewDecoder(r.Body)

	if err := decoder.Decode(&prMessage); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request payload")
		return
	}
	defer r.Body.Close()

	fmt.Println("prMessage: ", prMessage)

	respondWithJSON(w, http.StatusOK, map[string]string{"status": "200"})
}

func respondWithError(w http.ResponseWriter, code int, message string) {
	fmt.Println("-----------------------------------------------------------------------------")
	fmt.Println("Sending error message: ", message)
	fmt.Println("-----------------------------------------------------------------------------")
	respondWithJSON(w, code, map[string]string{"error": message})
}

func respondWithJSON(w http.ResponseWriter, code int, payload interface{}) {
	fmt.Println("-----------------------------------------------------------------------------")
	fmt.Println("Sending message: ", payload)
	fmt.Println("-----------------------------------------------------------------------------")
	response, _ := json.Marshal(payload)

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	w.Write(response)
}
