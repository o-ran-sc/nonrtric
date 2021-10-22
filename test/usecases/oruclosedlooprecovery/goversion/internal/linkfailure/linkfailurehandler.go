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

package linkfailure

import (
	"net/http"
	"strings"

	log "github.com/sirupsen/logrus"

	"oransc.org/usecase/oruclosedloop/internal/repository"
	"oransc.org/usecase/oruclosedloop/internal/restclient"
	"oransc.org/usecase/oruclosedloop/internal/ves"
)

type Configuration struct {
	SDNRAddress  string
	SDNRUser     string
	SDNRPassword string
}

const rawSdnrPath = "/rests/data/network-topology:network-topology/topology=topology-netconf/node=[O-DU-ID]/yang-ext:mount/o-ran-sc-du-hello-world:network-function/du-to-ru-connection=[O-RU-ID]"
const unlockMessage = `{"o-ran-sc-du-hello-world:du-to-ru-connection": [{"name":"[O-RU-ID]","administrative-state":"UNLOCKED"}]}`

type LinkFailureHandler struct {
	lookupService repository.LookupService
	config        Configuration
	client        restclient.HTTPClient
}

func NewLinkFailureHandler(ls repository.LookupService, conf Configuration, client restclient.HTTPClient) *LinkFailureHandler {
	return &LinkFailureHandler{
		lookupService: ls,
		config:        conf,
		client:        client,
	}
}

func (lfh LinkFailureHandler) MessagesHandler(w http.ResponseWriter, r *http.Request) {
	log.Debug("Handling messages")
	if messages := ves.GetVesMessages(r.Body); messages != nil {
		faultMessages := ves.GetFaultMessages(messages)

		for _, message := range faultMessages {
			if message.IsLinkFailure() {
				lfh.sendUnlockMessage(message.GetORuId())
			} else if message.IsClearLinkFailure() {
				log.Debugf("Cleared Link failure for O-RU ID: %v", message.GetORuId())
			}
		}
	}
}

func (lfh LinkFailureHandler) sendUnlockMessage(oRuId string) {
	if oDuId, err := lfh.lookupService.GetODuID(oRuId); err == nil {
		sdnrPath := getSdnrPath(oRuId, oDuId)
		unlockMessage := lfh.getUnlockMessage(oRuId)
		if error := restclient.Put(lfh.config.SDNRAddress+sdnrPath, unlockMessage, lfh.client, lfh.config.SDNRUser, lfh.config.SDNRPassword); error == nil {
			log.Debugf("Sent unlock message for O-RU: %v to O-DU: %v.", oRuId, oDuId)
		} else {
			log.Warn(error)
		}
	} else {
		log.Warn(err)
	}

}

func getSdnrPath(oRuId string, oDuId string) string {
	sdnrPath := strings.Replace(rawSdnrPath, "[O-DU-ID]", oDuId, 1)
	sdnrPath = strings.Replace(sdnrPath, "[O-RU-ID]", oRuId, 1)
	return sdnrPath
}

func (lfh LinkFailureHandler) getUnlockMessage(oRuId string) string {
	return strings.Replace(unlockMessage, "[O-RU-ID]", oRuId, 1)
}
