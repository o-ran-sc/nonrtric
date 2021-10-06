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

package ves

import (
	"encoding/json"
	"io"
	"io/ioutil"

	log "github.com/sirupsen/logrus"
)

func GetFaultMessages(messageStrings *[]string) []FaultMessage {
	faultMessages := make([]FaultMessage, 0, len(*messageStrings))
	for _, msgString := range *messageStrings {
		var message FaultMessage
		if err := json.Unmarshal([]byte(msgString), &message); err == nil {
			if message.isFault() {
				faultMessages = append(faultMessages, message)
			}
		} else {
			log.Warn(err)
		}
	}
	return faultMessages
}

func GetVesMessages(r io.ReadCloser) *[]string {
	var messages []string
	body, err := ioutil.ReadAll(r)
	if err != nil {
		log.Warn(err)
		return nil
	}
	err = json.Unmarshal(body, &messages)
	if err != nil {
		log.Warn(err)
		return nil
	}
	return &messages
}
