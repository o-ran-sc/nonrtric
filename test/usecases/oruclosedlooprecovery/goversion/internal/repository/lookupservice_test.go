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
	"reflect"
	"testing"

	"oransc.org/usecase/oruclosedloop/mocks"
)

func TestNewLookupServiceImpl(t *testing.T) {
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
			if got := NewLookupServiceImpl(tt.args.fileHelper, tt.args.fileName); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("NewLookupServiceImpl() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestLookupServiceImpl_Init(t *testing.T) {
	mockCsvFileHelper := &mocks.CsvFileHelper{}
	mockCsvFileHelper.On("GetCsvFromFile", "./map.csv").Return([][]string{{"O-RU-ID", "O-DU-ID"}}, nil).Once()
	mockCsvFileHelper.On("GetCsvFromFile", "foo.csv").Return(nil, errors.New("Error")).Once()
	type fields struct {
		csvFileHelper   CsvFileHelper
		csvFileName     string
		oRuIdToODuIdMap map[string]string
	}
	tests := []struct {
		name    string
		fields  fields
		wantErr bool
	}{
		{
			name: "Init with proper csv file should not return error",
			fields: fields{
				csvFileHelper:   mockCsvFileHelper,
				csvFileName:     "./map.csv",
				oRuIdToODuIdMap: map[string]string{}},
			wantErr: false,
		},
		{
			name: "Init with missing file should return error",
			fields: fields{
				csvFileHelper:   mockCsvFileHelper,
				csvFileName:     "foo.csv",
				oRuIdToODuIdMap: map[string]string{},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := LookupServiceImpl{
				csvFileHelper:   tt.fields.csvFileHelper,
				csvFileName:     tt.fields.csvFileName,
				oRuIdToODuIdMap: tt.fields.oRuIdToODuIdMap,
			}
			if err := s.Init(); (err != nil) != tt.wantErr {
				t.Errorf("LookupServiceImpl.Init() error = %v, wantErr %v", err, tt.wantErr)
			} else if !tt.wantErr {
				wantedMap := map[string]string{"O-RU-ID": "O-DU-ID"}
				if !reflect.DeepEqual(wantedMap, s.oRuIdToODuIdMap) {
					t.Errorf("LookupServiceImpl.Init() map not initialized, wanted map: %v, got map: %v", wantedMap, s.oRuIdToODuIdMap)
				}
			}
		})
	}
	mockCsvFileHelper.AssertNumberOfCalls(t, "GetCsvFromFile", 2)
}

func TestLookupServiceImpl_GetODuID(t *testing.T) {
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
			want:    "O-DU-ID",
			wantErr: nil,
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
			want:    "",
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
			if err != tt.wantErr {
				t.Errorf("LookupServiceImpl.GetODuID() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("LookupServiceImpl.GetODuID() = %v, want %v", got, tt.want)
			}
		})
	}
}
