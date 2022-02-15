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
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/httputil"

	log "github.com/sirupsen/logrus"
)

type RequestError struct {
	StatusCode int
	Body       []byte
}

func (e RequestError) Error() string {
	return fmt.Sprintf("error response with status: %v and body: %v", e.StatusCode, string(e.Body))
}

type Client struct {
	httpClient *http.Client
	verbose    bool
}

func New(httpClient *http.Client, verbose bool) *Client {
	return &Client{
		httpClient: httpClient,
		verbose:    verbose,
	}
}

func (c *Client) Get(path string, v interface{}, userInfo ...string) error {
	var req *http.Request
	var err error

	if len(userInfo) > 1 {
		req, err = c.newRequest(http.MethodGet, path, nil, userInfo[0], userInfo[1])
	} else {
		req, err = c.newRequest(http.MethodGet, path, nil)
	}

	if err != nil {
		return fmt.Errorf("failed to create GET request: %w", err)
	}

	if err := c.doRequest(req, v); err != nil {
		return err
	}

	return nil
}

func (c *Client) Post(path string, payload interface{}, v interface{}, userInfo ...string) error {
	var req *http.Request
	var err error

	if len(userInfo) > 1 {
		req, err = c.newRequest(http.MethodPost, path, payload, userInfo[0], userInfo[1])
	} else {
		req, err = c.newRequest(http.MethodPost, path, payload)
	}

	if err != nil {
		return fmt.Errorf("failed to create POST request: %w", err)
	}

	if err := c.doRequest(req, v); err != nil {
		return err
	}

	return nil
}

func (c *Client) Put(path string, payload interface{}, v interface{}, userName string, password string) error {
	req, err := c.newRequest(http.MethodPut, path, payload, userName, password)
	if err != nil {
		return fmt.Errorf("failed to create PUT request: %w", err)
	}

	if err := c.doRequest(req, v); err != nil {
		return err
	}

	return nil
}

func (c *Client) newRequest(method, path string, payload interface{}, userInfo ...string) (*http.Request, error) {
	var reqBody io.Reader

	if payload != nil {
		bodyBytes, err := json.Marshal(payload)
		if err != nil {
			return nil, fmt.Errorf("failed to marshal request body: %w", err)
		}
		reqBody = bytes.NewReader(bodyBytes)
	}

	req, err := http.NewRequest(method, path, reqBody)

	if err != nil {
		return nil, fmt.Errorf("failed to create HTTP request: %w", err)
	}

	if len(userInfo) > 0 {
		req.SetBasicAuth(userInfo[0], userInfo[1])
	}

	if reqBody != nil {
		req.Header.Set("Content-Type", "application/json")
	}

	if c.verbose {
		if reqDump, error := httputil.DumpRequest(req, true); error != nil {
			fmt.Println(err)
		} else {
			fmt.Println(string(reqDump))
		}
	}

	return req, nil
}

func (c *Client) doRequest(r *http.Request, v interface{}) error {
	resp, err := c.do(r)
	if err != nil {
		return err
	}

	if resp == nil {
		return nil
	}
	defer resp.Body.Close()

	if v == nil {
		return nil
	}

	dec := json.NewDecoder(resp.Body)
	if err := dec.Decode(&v); err != nil {
		return fmt.Errorf("could not parse response body: %w [%s:%s]", err, r.Method, r.URL.String())
	}
	log.Debugf("Http Client Response: %v\n", v)
	return nil
}

func (c *Client) do(r *http.Request) (*http.Response, error) {
	resp, err := c.httpClient.Do(r)
	if err != nil {
		return nil, fmt.Errorf("failed to make request [%s:%s]: %w", r.Method, r.URL.String(), err)
	}

	if c.verbose {
		if responseDump, error := httputil.DumpResponse(resp, true); error != nil {
			fmt.Println(err)
		} else {
			fmt.Println(string(responseDump))
		}
	}

	if resp.StatusCode >= http.StatusOK && resp.StatusCode <= 299 {
		return resp, nil
	}

	defer resp.Body.Close()
	responseData, _ := io.ReadAll(resp.Body)

	putError := RequestError{
		StatusCode: resp.StatusCode,
		Body:       responseData,
	}

	return resp, putError
}
