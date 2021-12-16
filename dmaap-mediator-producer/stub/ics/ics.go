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
	"flag"
	"fmt"
	"net/http"

	"github.com/gorilla/mux"
)

func main() {
	port := flag.Int("port", 8434, "The port this stub will listen on")
	flag.Parse()
	fmt.Println("Starting ICS stub on port ", *port)

	r := mux.NewRouter()
	r.HandleFunc("/data-producer/v1/info-types/{typeId}", handleTypeRegistration).Methods(http.MethodPut, http.MethodPut)
	r.HandleFunc("/data-producer/v1/info-producers/{producerId}", handleProducerRegistration).Methods(http.MethodPut, http.MethodPut)
	fmt.Println(http.ListenAndServe(fmt.Sprintf(":%v", *port), r))
}

func handleTypeRegistration(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, ok := vars["typeId"]
	if ok {
		fmt.Println("Registered type ", id)
	}
}

func handleProducerRegistration(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, ok := vars["producerId"]
	if ok {
		fmt.Println("Registered producer ", id)
	}
}
