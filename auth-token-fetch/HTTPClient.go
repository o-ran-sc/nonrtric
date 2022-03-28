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
	"crypto/x509"

	"net/http"
	"time"
)

func CreateHttpClient(cert tls.Certificate, caCerts *x509.CertPool, timeout time.Duration) *http.Client {
	return &http.Client{
		Timeout:   timeout,
		Transport: createTransport(cert, caCerts),
	}
}

func createTransport(cert tls.Certificate, caCerts *x509.CertPool) *http.Transport {
	return &http.Transport{
		TLSClientConfig: &tls.Config{
			ClientCAs: caCerts,
			RootCAs:   caCerts,
			Certificates: []tls.Certificate{
				cert,
			},
			InsecureSkipVerify: true,
		},
	}
}
