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
	"crypto/tls"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"time"

	"github.com/hashicorp/go-retryablehttp"
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

func Get(url string, client HTTPClient) ([]byte, error) {
	if response, err := client.Get(url); err == nil {
		if isResponseSuccess(response.StatusCode) {
			defer response.Body.Close()
			if responseData, err := io.ReadAll(response.Body); err == nil {
				return responseData, nil
			} else {
				return nil, err
			}
		} else {
			return nil, getRequestError(response)
		}
	} else {
		return nil, err
	}
}

func Put(url string, body []byte, client HTTPClient) error {
	return do(http.MethodPut, url, body, client)
}

func Post(url string, body []byte, client HTTPClient) error {
	return do(http.MethodPost, url, body, client)
}

func do(method string, url string, body []byte, client HTTPClient) error {
	if req, reqErr := http.NewRequest(method, url, bytes.NewBuffer(body)); reqErr == nil {
		req.Header.Set("Content-Type", "application/json")
		if response, respErr := client.Do(req); respErr == nil {
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

func CreateClientCertificate(certPath string, keyPath string) (*tls.Certificate, error) {
	if cert, err := tls.LoadX509KeyPair(certPath, keyPath); err == nil {
		return &cert, nil
	} else {
		return nil, fmt.Errorf("cannot create x509 keypair from cert file %s and key file %s due to: %v", certPath, keyPath, err)
	}
}

func CreateRetryClient(cert *tls.Certificate) *http.Client {
	rawRetryClient := retryablehttp.NewClient()
	rawRetryClient.RetryWaitMax = time.Minute
	rawRetryClient.RetryMax = int(^uint(0) >> 1)
	rawRetryClient.HTTPClient.Transport = getSecureTransportWithoutVerify(cert)

	client := rawRetryClient.StandardClient()
	return client
}

func CreateClientWithoutRetry(cert *tls.Certificate, timeout time.Duration) *http.Client {
	return &http.Client{
		Timeout:   timeout,
		Transport: getSecureTransportWithoutVerify(cert),
	}
}

func getSecureTransportWithoutVerify(cert *tls.Certificate) *http.Transport {
	return &http.Transport{
		TLSClientConfig: &tls.Config{
			Certificates: []tls.Certificate{
				*cert,
			},
			InsecureSkipVerify: true,
		},
	}
}

func IsUrlSecure(configUrl string) bool {
	u, _ := url.Parse(configUrl)
	return u.Scheme == "https"
}
