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

package jobs

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

type Type struct {
	TypeId string
	Schema string
}

type JobInfo struct {
	Owner            string `json:"owner"`
	LastUpdated      string `json:"last_updated"`
	InfoJobIdentity  string `json:"info_job_identity"`
	TargetUri        string `json:"target_uri"`
	InfoJobData      string `json:"info_job_data"`
	InfoTypeIdentity string `json:"info_type_identity"`
}

type JobHandler interface {
	AddJob(JobInfo) error
}

var (
	typeDir = "configs"
	Handler JobHandler
	allJobs = make(map[string]map[string]JobInfo)
)

func init() {
	Handler = newJobHandlerImpl()
}

type jobHandlerImpl struct{}

func newJobHandlerImpl() *jobHandlerImpl {
	return &jobHandlerImpl{}
}

func (jh *jobHandlerImpl) AddJob(ji JobInfo) error {
	if err := validateJobInfo(ji); err == nil {
		jobs := allJobs[ji.InfoTypeIdentity]
		jobs[ji.InfoJobIdentity] = ji
		return nil
	} else {
		return err
	}
}

func validateJobInfo(ji JobInfo) error {
	if _, ok := allJobs[ji.InfoTypeIdentity]; !ok {
		return fmt.Errorf("type not supported: %v", ji.InfoTypeIdentity)
	}
	if ji.InfoJobIdentity == "" {
		return fmt.Errorf("missing required job identity: %v", ji)
	}
	// Temporary for when there are only REST callbacks needed
	if ji.TargetUri == "" {
		return fmt.Errorf("missing required target URI: %v", ji)
	}
	return nil
}

func GetTypes() ([]*Type, error) {
	types := make([]*Type, 0, 1)
	err := filepath.Walk(typeDir,
		func(path string, info os.FileInfo, err error) error {
			if err != nil {
				return err
			}
			if strings.Contains(path, ".json") {
				if jobType, err := getType(path); err == nil {
					types = append(types, jobType)
				}
			}
			return nil
		})
	if err != nil {
		return nil, err
	}
	return types, nil
}

func GetSupportedTypes() []string {
	supportedTypes := []string{}
	for k := range allJobs {
		supportedTypes = append(supportedTypes, k)
	}
	return supportedTypes
}

func AddJob(job JobInfo) error {
	return Handler.AddJob(job)
}

func getType(path string) (*Type, error) {
	fileName := filepath.Base(path)
	typeName := strings.TrimSuffix(fileName, filepath.Ext(fileName))

	if typeSchema, err := os.ReadFile(path); err == nil {
		typeInfo := Type{
			TypeId: typeName,
			Schema: string(typeSchema),
		}
		if _, ok := allJobs[typeName]; !ok {
			allJobs[typeName] = make(map[string]JobInfo)
		}
		return &typeInfo, nil
	} else {
		return nil, err
	}
}

func clearAll() {
	allJobs = make(map[string]map[string]JobInfo)
}
