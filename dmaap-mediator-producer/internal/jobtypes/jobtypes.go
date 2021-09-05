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

package jobtypes

import (
	"os"
	"path/filepath"
	"strings"
)

type Type struct {
	Name   string
	Schema string
}

var typeDir = "configs"

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

func getType(path string) (*Type, error) {
	fileName := filepath.Base(path)
	typeName := strings.TrimSuffix(fileName, filepath.Ext(fileName))

	if typeSchema, err := os.ReadFile(path); err == nil {
		return &Type{
			Name:   typeName,
			Schema: string(typeSchema),
		}, nil
	} else {
		return nil, err
	}
}
