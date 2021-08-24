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
	"fmt"
)

type IdNotMappedError struct {
	Id string
}

func (inme IdNotMappedError) Error() string {
	return fmt.Sprintf("O-RU-ID: %v not mapped.", inme.Id)
}

type LookupService interface {
	Init() error
	GetODuID(oRuId string) (string, error)
}

type LookupServiceImpl struct {
	csvFileHelper CsvFileHelper
	csvFileName   string

	oRuIdToODuIdMap map[string]string
}

func NewLookupServiceImpl(fileHelper CsvFileHelper, fileName string) *LookupServiceImpl {
	s := LookupServiceImpl{
		csvFileHelper: fileHelper,
		csvFileName:   fileName,
	}
	s.oRuIdToODuIdMap = make(map[string]string)
	return &s
}

func (s LookupServiceImpl) Init() error {
	if csvData, err := s.csvFileHelper.GetCsvFromFile(s.csvFileName); err == nil {
		for _, each := range csvData {
			s.oRuIdToODuIdMap[each[0]] = each[1]
		}
		return nil
	} else {
		return err
	}
}

func (s LookupServiceImpl) GetODuID(oRuId string) (string, error) {
	if oDuId, ok := s.oRuIdToODuIdMap[oRuId]; ok {
		return oDuId, nil
	} else {
		return "", IdNotMappedError{Id: oRuId}
	}
}
