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
	"os"
	"reflect"
	"testing"
)

func TestCsvFileHelperImpl_GetCsvFromFile(t *testing.T) {
	filePath := createTempCsvFile()
	defer os.Remove(filePath)
	type args struct {
		name string
	}
	tests := []struct {
		name       string
		fileHelper *CsvFileHelperImpl
		args       args
		want       [][]string
		wantErr    bool
	}{
		{
			name:       "Read from file should return array of content",
			fileHelper: &CsvFileHelperImpl{},
			args: args{
				name: filePath,
			},
			want:    [][]string{{"O-RU-ID", "O-DU-ID"}},
			wantErr: false,
		},
		{
			name:       "File missing should return error",
			fileHelper: &CsvFileHelperImpl{},
			args: args{
				name: "nofile.csv",
			},
			want:    nil,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &CsvFileHelperImpl{}
			got, err := h.GetCsvFromFile(tt.args.name)
			if (err != nil) != tt.wantErr {
				t.Errorf("CsvFileHelperImpl.GetCsvFromFile() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("CsvFileHelperImpl.GetCsvFromFile() = %v, want %v", got, tt.want)
			}
		})
	}
}

func createTempCsvFile() string {
	csvFile, _ := os.CreateTemp("", "test*.csv")
	filePath := csvFile.Name()
	csvFile.Write([]byte("O-RU-ID,O-DU-ID"))
	csvFile.Close()
	return filePath
}
