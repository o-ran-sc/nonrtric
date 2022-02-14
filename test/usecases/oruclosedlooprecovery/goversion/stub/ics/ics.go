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
	"os"
	"time"

	"github.com/gorilla/mux"
)

var client = &http.Client{
	Timeout: 5 * time.Second,
}

func main() {
	port := flag.Int("port", 8083, "The port this consumer will listen on")
	flag.Parse()
	fmt.Println("Starting ICS stub on port ", *port)

	r := mux.NewRouter()
	r.HandleFunc("/data-consumer/v1/info-jobs/{jobId}", handleCalls).Methods(http.MethodPut, http.MethodDelete)
	fmt.Println(http.ListenAndServe(fmt.Sprintf(":%v", *port), r))
}

func getEnv(key string, defaultVal string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}

	return defaultVal
}

func handleCalls(w http.ResponseWriter, r *http.Request) {
	producer_addr := getEnv("PRODUCER_ADDR", "http://producer-sim:8085/")
	vars := mux.Vars(r)
	id, ok := vars["jobId"]
	if ok {
		fmt.Println(r.Method, " of job ", id)
		if r.Method == http.MethodPut {
			req, _ := http.NewRequest(http.MethodPut, producer_addr+"create/"+id, nil)
			r, err := client.Do(req)
			if err != nil {
				fmt.Println("Failed to create job in producer ", err)
				return
			}
			fmt.Println("Created job in producer ", r.Status)
		} else {
			req, _ := http.NewRequest(http.MethodDelete, producer_addr+"delete/"+id, nil)
			r, err := client.Do(req)
			if err != nil {
				fmt.Println("Failed to delete job in producer ", err)
				return
			}
			fmt.Println("Deleted job in producer ", r.Status)
		}
	}

}
