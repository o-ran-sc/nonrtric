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
	"errors"
	"testing"

	"github.com/stretchr/testify/require"
	"oransc.org/usecase/oruclosedloop/mocks"
)

func TestIdNotMappedError(t *testing.T) {
	assertions := require.New(t)

	actualError := IdNotMappedError{
		Id: "1",
	}
	assertions.Equal("O-RU-ID: 1 not mapped.", actualError.Error())
}

func TestNewLookupServiceImpl(t *testing.T) {
	assertions := require.New(t)
	mockCsvFileHelper := &mocks.CsvFileHelper{}
	type args struct {
		fileHelper CsvFileHelper
		fileName   string
	}
	tests := []struct {
		name string
		args args
		want *LookupServiceImpl
	}{
		{
			name: "Should return populated service",
			args: args{
				fileHelper: mockCsvFileHelper,
				fileName:   "test.csv",
			},
			want: &LookupServiceImpl{
				csvFileHelper:   mockCsvFileHelper,
				csvFileName:     "test.csv",
				oRuIdToODuIdMap: map[string]string{},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := NewLookupServiceImpl(tt.args.fileHelper, tt.args.fileName)
			assertions.Equal(tt.want, got)
		})
	}
}

func TestLookupServiceImpl_Init(t *testing.T) {
	assertions := require.New(t)
	type args struct {
		csvFileName     string
		mockReturn      [][]string
		mockReturnError error
	}
	tests := []struct {
		name                  string
		args                  args
		wantedORuIdToODuIdMap map[string]string
		wantErr               error
	}{
		{
			name: "Init with proper csv file should not return error and map should be initialized",
			args: args{
				csvFileName: "./map.csv",
				mockReturn:  [][]string{{"O-RU-ID", "O-DU-ID"}},
			},
			wantedORuIdToODuIdMap: map[string]string{"O-RU-ID": "O-DU-ID"},
		},
		{
			name: "Init with missing file should return error and map should not be initialized",
			args: args{
				csvFileName:     "foo.csv",
				mockReturnError: errors.New("Error"),
			},
			wantedORuIdToODuIdMap: map[string]string{},
			wantErr:               errors.New("Error"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mockCsvFileHelper := &mocks.CsvFileHelper{}
			mockCsvFileHelper.On("GetCsvFromFile", tt.args.csvFileName).Return(tt.args.mockReturn, tt.args.mockReturnError)

			s := NewLookupServiceImpl(mockCsvFileHelper, tt.args.csvFileName)

			err := s.Init()

			assertions.Equal(tt.wantErr, err, tt.name)
			assertions.Equal(tt.wantedORuIdToODuIdMap, s.oRuIdToODuIdMap)
			mockCsvFileHelper.AssertNumberOfCalls(t, "GetCsvFromFile", 1)
		})
	}
}

func TestLookupServiceImpl_GetODuID(t *testing.T) {
	assertions := require.New(t)
	type fields struct {
		csvFileHelper   CsvFileHelper
		csvFileName     string
		oRuIdToODuIdMap map[string]string
	}
	type args struct {
		oRuId string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    string
		wantErr error
	}{
		{
			name: "Id mapped should return mapped id",
			fields: fields{
				csvFileHelper:   nil,
				csvFileName:     "",
				oRuIdToODuIdMap: map[string]string{"O-RU-ID": "O-DU-ID"},
			},
			args: args{
				oRuId: "O-RU-ID",
			},
			want: "O-DU-ID",
		},
		{
			name: "Id not mapped should return IdNotMappedError",
			fields: fields{
				csvFileHelper:   nil,
				csvFileName:     "",
				oRuIdToODuIdMap: map[string]string{},
			},
			args: args{
				oRuId: "O-RU-ID",
			},
			wantErr: IdNotMappedError{Id: "O-RU-ID"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := LookupServiceImpl{
				csvFileHelper:   tt.fields.csvFileHelper,
				csvFileName:     tt.fields.csvFileName,
				oRuIdToODuIdMap: tt.fields.oRuIdToODuIdMap,
			}

			got, err := s.GetODuID(tt.args.oRuId)

			assertions.Equal(tt.wantErr, err, tt.name)
			assertions.Equal(tt.want, got, tt.name)
		})
	}
}
