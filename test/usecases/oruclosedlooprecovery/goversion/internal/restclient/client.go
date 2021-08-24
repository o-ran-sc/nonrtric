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

package restclient

import (
	"bytes"
	"fmt"
	"io"
	"net/http"
	"time"
)

type RequestError struct {
	StatusCode int
	Body       []byte
}

func (pe RequestError) Error() string {
	return fmt.Sprintf("Request failed due to error response with status: %v and body: %v", pe.StatusCode, string(pe.Body))
}

// HTTPClient interface
type HTTPClient interface {
	Get(url string) (*http.Response, error)

	Do(*http.Request) (*http.Response, error)
}

var (
	Client HTTPClient
)

func init() {
	Client = &http.Client{
		Timeout: time.Second * 5,
	}
}

func Get(url string) ([]byte, error) {
	if response, err := Client.Get(url); err == nil {
		defer response.Body.Close()
		if responseData, err := io.ReadAll(response.Body); err == nil {
			return responseData, nil
		} else {
			return nil, err
		}
	} else {
		return nil, err
	}
}

func PutWithoutAuth(url string, body []byte) error {
	return do(http.MethodPut, url, body)
}

func Put(url string, body string, userName string, password string) error {
	return do(http.MethodPut, url, []byte(body), userName, password)
}

func Delete(url string) error {
	return do(http.MethodDelete, url, nil)
}

func do(method string, url string, body []byte, userInfo ...string) error {
	if req, reqErr := http.NewRequest(method, url, bytes.NewBuffer(body)); reqErr == nil {
		if body != nil {
			req.Header.Set("Content-Type", "application/json; charset=utf-8")
		}
		if len(userInfo) > 0 {
			req.SetBasicAuth(userInfo[0], userInfo[1])
		}
		if response, respErr := Client.Do(req); respErr == nil {
			if isResponseSuccess(response.StatusCode) {
				return nil
			} else {
				return getResponseError(response)
			}
		} else {
			return respErr
		}
	} else {
		return reqErr
	}
}

func isResponseSuccess(statusCode int) bool {
	return statusCode >= http.StatusOK && statusCode <= 299
}

func getResponseError(response *http.Response) RequestError {
	defer response.Body.Close()
	responseData, _ := io.ReadAll(response.Body)
	putError := RequestError{
		StatusCode: response.StatusCode,
		Body:       responseData,
	}
	return putError
}
