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
	"testing"

	"github.com/stretchr/testify/require"
)

func TestCsvFileHelperImpl_GetCsvFromFile(t *testing.T) {
	assertions := require.New(t)
	filePath := createTempCsvFile()
	defer os.Remove(filePath)
	type args struct {
		name string
	}
	tests := []struct {
		name          string
		args          args
		want          [][]string
		wantErrString string
	}{
		{
			name: "Read from file should return array of content",
			args: args{
				name: filePath,
			},
			want: [][]string{{"O-RU-ID", "O-DU-ID"}},
		},
		{
			name: "File missing should return error",
			args: args{
				name: "nofile.csv",
			},
			wantErrString: "open nofile.csv: no such file or directory",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := NewCsvFileHelperImpl()
			got, err := h.GetCsvFromFile(tt.args.name)
			assertions.Equal(tt.want, got)
			if tt.wantErrString != "" {
				assertions.Contains(err.Error(), tt.wantErrString)
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
