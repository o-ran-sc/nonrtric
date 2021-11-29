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
)

type Client struct {
	httpClient *http.Client
}

func New(httpClient *http.Client) *Client {
	return &Client{
		httpClient: httpClient,
	}
}

type HTTPClient interface {
	Get(path string, v interface{}) error
	Post(path string, payload interface{}, v interface{}) error
}

func (c *Client) Get(path string, v interface{}) error {
	req, err := c.newRequest(http.MethodGet, path, nil)
	if err != nil {
		return fmt.Errorf("failed to create GET request: %w", err)
	}

	if err := c.doRequest(req, v); err != nil {
		return err
	}

	return nil
}

func (c *Client) Post(path string, payload interface{}, v interface{}) error {
	req, err := c.newRequest(http.MethodPost, path, payload)
	if err != nil {
		return fmt.Errorf("failed to create POST request: %w", err)
	}

	if err := c.doRequest(req, v); err != nil {
		return err
	}

	return nil
}

func (c *Client) newRequest(method, path string, payload interface{}) (*http.Request, error) {
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

	if reqBody != nil {
		req.Header.Set("Content-Type", "application/json; charset=utf-8")
	}
	fmt.Printf("Http Client Request: [%s:%s]\n", req.Method, req.URL)
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
	if err := dec.Decode(v); err != nil {
		return fmt.Errorf("could not parse response body: %w [%s:%s]", err, r.Method, r.URL.String())
	}
	fmt.Printf("Http Client Response: %+v\n", v)
	return nil
}

func (c *Client) do(r *http.Request) (*http.Response, error) {
	resp, err := c.httpClient.Do(r)
	if err != nil {
		return nil, fmt.Errorf("failed to make request [%s:%s]: %w", r.Method, r.URL.String(), err)
	}

	if resp.StatusCode >= http.StatusOK && resp.StatusCode <= 299 {
		return resp, nil
	}

	defer resp.Body.Close()

	return resp, fmt.Errorf("failed to do request, %d status code received", resp.StatusCode)
}
