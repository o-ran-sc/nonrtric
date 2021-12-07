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
	"io"
	"net/http"

	"github.com/gorilla/mux"
)

func main() {
	port := flag.Int("port", 3904, "The port this SDNR stub will listen on")
	flag.Parse()

	r := mux.NewRouter()
	r.HandleFunc("/rests/data/network-topology:network-topology/topology=topology-netconf/node={O-DU-ID}/yang-ext:mount/o-ran-sc-du-hello-world:network-function/du-to-ru-connection={O-RU-ID}", handleData)

	fmt.Println("Starting SDNR on port: ", *port)
	fmt.Println(http.ListenAndServe(fmt.Sprintf(":%v", *port), r))

}

func handleData(w http.ResponseWriter, req *http.Request) {
	defer req.Body.Close()
	if reqData, err := io.ReadAll(req.Body); err == nil {
		fmt.Println("SDNR received body: ", string(reqData))
	}
}
