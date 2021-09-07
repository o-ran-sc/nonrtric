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

// HTTPClient interface
type HTTPClient interface {
	Get(url string) (*http.Response, error)

	Do(*http.Request) (*http.Response, error)
}

type RequestError struct {
	StatusCode int
	Body       []byte
}

func (pe RequestError) Error() string {
	return fmt.Sprintf("Request failed due to error response with status: %v and body: %v", pe.StatusCode, string(pe.Body))
}

var (
	Client HTTPClient
)

func init() {
	Client = &http.Client{}
}

func Get(url string) ([]byte, error) {
	if response, err := Client.Get(url); err == nil {
		defer response.Body.Close()
		if responseData, err := io.ReadAll(response.Body); err == nil {
			if isResponseSuccess(response.StatusCode) {
				return responseData, nil
			} else {
				requestError := RequestError{
					StatusCode: response.StatusCode,
					Body:       responseData,
				}
				return nil, requestError
			}
		} else {
			return nil, err
		}
	} else {
		return nil, err
	}
}

func Put(url string, body []byte) error {
	if req, reqErr := http.NewRequest(http.MethodPut, url, bytes.NewBuffer(body)); reqErr == nil {
		req.Header.Set("Content-Type", "application/json; charset=utf-8")
		if response, respErr := Client.Do(req); respErr == nil {
			if isResponseSuccess(response.StatusCode) {
				return nil
			} else {
				return getRequestError(response)
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

func getRequestError(response *http.Response) RequestError {
	defer response.Body.Close()
	responseData, _ := io.ReadAll(response.Body)
	putError := RequestError{
		StatusCode: response.StatusCode,
		Body:       responseData,
	}
	return putError
}
