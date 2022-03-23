// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2022: Nordix Foundation
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

package main

import (
	"crypto/tls"

	"net/http"
	"reflect"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestRequestError_Error(t *testing.T) {
	assertions := require.New(t)
	actualError := RequestError{
		StatusCode: http.StatusBadRequest,
		Body:       []byte("error"),
	}
	assertions.Equal("Request failed due to error response with status: 400 and body: error", actualError.Error())
}

func Test_CreateClient(t *testing.T) {
	assertions := require.New(t)

	client := CreateHttpClient(tls.Certificate{}, nil, 5*time.Second)

	transport := client.Transport
	assertions.Equal("*http.Transport", reflect.TypeOf(transport).String())
	assertions.Equal(5*time.Second, client.Timeout)
}

func TestIsUrlSecured(t *testing.T) {
	assertions := require.New(t)

	assertions.True(IsUrlSecure("https://url"))

	assertions.False(IsUrlSecure("http://url"))
}
