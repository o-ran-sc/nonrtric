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
)

type RequestError struct {
	StatusCode int
	Body       []byte
}

// HTTPClient interface
type HTTPClient interface {
	Get(url string) (*http.Response, error)

	Do(*http.Request) (*http.Response, error)
}

func (pe RequestError) Error() string {
	return fmt.Sprintf("Request failed due to error response with status: %v and body: %v", pe.StatusCode, string(pe.Body))
}

func PutWithoutAuth(url string, body []byte, client HTTPClient) error {
	return do(http.MethodPut, url, body, client)
}

func Put(url string, body string, client HTTPClient, userName string, password string) error {
	return do(http.MethodPut, url, []byte(body), client, userName, password)
}

func Delete(url string, client HTTPClient) error {
	return do(http.MethodDelete, url, nil, client)
}

func do(method string, url string, body []byte, client HTTPClient, userInfo ...string) error {
	if req, reqErr := http.NewRequest(method, url, bytes.NewBuffer(body)); reqErr == nil {
		if body != nil {
			req.Header.Set("Content-Type", "application/json; charset=utf-8")
		}
		if len(userInfo) > 0 {
			req.SetBasicAuth(userInfo[0], userInfo[1])
		}
		if response, respErr := client.Do(req); respErr == nil {
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
