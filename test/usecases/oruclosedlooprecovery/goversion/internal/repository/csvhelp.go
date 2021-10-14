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

package repository

import (
	"encoding/csv"
	"os"
)

type CsvFileHelper interface {
	GetCsvFromFile(name string) ([][]string, error)
}

type CsvFileHelperImpl struct{}

func NewCsvFileHelperImpl() CsvFileHelperImpl {
	return CsvFileHelperImpl{}
}

func (h CsvFileHelperImpl) GetCsvFromFile(name string) ([][]string, error) {
	if csvFile, err := os.Open(name); err == nil {
		defer csvFile.Close()
		reader := csv.NewReader(csvFile)
		reader.FieldsPerRecord = -1
		if csvData, err := reader.ReadAll(); err == nil {
			return csvData, nil
		} else {
			return nil, err
		}
	} else {
		return nil, err
	}
}
