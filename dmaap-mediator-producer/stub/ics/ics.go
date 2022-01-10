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
	"io/ioutil"
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
		fmt.Printf("Registered type %v with schema: %v\n", id, readBody(r))
	}
}

func handleProducerRegistration(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, ok := vars["producerId"]
	if ok {
		fmt.Printf("Registered producer %v with data: %v\n", id, readBody(r))
	}
}

func readBody(r *http.Request) string {
	b, readErr := ioutil.ReadAll(r.Body)
	if readErr != nil {
		return fmt.Sprintf("Unable to read body due to: %v", readErr)
	}
	return string(b)
}
