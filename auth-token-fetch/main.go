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
	"encoding/json"
	"io/ioutil"
	"net/http"
	"net/url"
	"time"

	"os"

	log "github.com/sirupsen/logrus"
)

type JwtToken struct {
	Access_token string
	Expires_in   int
	Token_type   string
}

type Context struct {
	Running bool
	Config  *Config
}

func NewContext(config *Config) *Context {
	return &Context{
		Running: true,
		Config:  config,
	}
}

// @title Auth token fetcher
// @version 1.1.0

// @license.name  Apache 2.0
// @license.url   http://www.apache.org/licenses/LICENSE-2.0.html

func main() {
	configuration := NewConfig()
	log.SetLevel(configuration.LogLevel)

	log.Debug("Using configuration: ", configuration)
	start(NewContext(configuration))

	keepAlive()
}

func start(context *Context) {
	log.Debug("Initializing")
	if err := validateConfiguration(context.Config); err != nil {
		log.Fatalf("Stopping due to error: %v", err)
	}

	cert := loadCertificate(context.Config.CertPath, context.Config.KeyPath)
	caCerts := loadCaCerts(context.Config.CACertsPath)

	webClient := CreateHttpClient(cert, caCerts, 10*time.Second)

	go periodicRefreshIwtToken(webClient, context)
}

func periodicRefreshIwtToken(webClient *http.Client, context *Context) {
	for context.Running {
		jwtToken, err := fetchJwtToken(webClient, context.Config)
		if check(err) {
			saveAccessToken(jwtToken, context.Config)
		}
		delayTime := calcDelayTime(jwtToken, err, context.Config)
		log.WithFields(log.Fields{"seconds": delayTime.Seconds()}).Debug("Sleeping")
		time.Sleep(delayTime)
	}
}

func calcDelayTime(token JwtToken, e error, confing *Config) time.Duration {
	if e != nil {
		return time.Second * 1
	}
	remains := token.Expires_in - confing.RefreshMarginSeconds
	if remains < 1 {
		remains = 1
	}
	return time.Second * time.Duration(remains)
}

func check(e error) bool {
	if e != nil {
		log.Errorf("Failure reason: %v", e)
		return false
	}
	return true
}

func saveAccessToken(token JwtToken, configuration *Config) {
	log.WithFields(log.Fields{"file": configuration.AuthTokenOutputFileName}).Debug("Saving access token")
	data := []byte(token.Access_token)
	err := os.WriteFile(configuration.AuthTokenOutputFileName, data, 0644)
	check(err)
}

func fetchJwtToken(webClient *http.Client, configuration *Config) (JwtToken, error) {
	log.WithFields(log.Fields{"url": configuration.AuthServiceUrl}).Debug("Fetching token")
	var jwt JwtToken
	var err error
	resp, err := webClient.PostForm(configuration.AuthServiceUrl,
		url.Values{"client_secret": {configuration.ClientSecret}, "grant_type": {configuration.GrantType}, "client_id": {configuration.ClientId}})

	if check(err) {
		var body []byte
		defer resp.Body.Close()
		body, err = ioutil.ReadAll(resp.Body)
		if check(err) {
			err = json.Unmarshal([]byte(body), &jwt)
		}
	}
	return jwt, err
}

func loadCertificate(certPath string, keyPath string) tls.Certificate {
	log.WithFields(log.Fields{"certPath": certPath, "keyPath": keyPath}).Debug("Loading cert")
	cert, err := tls.LoadX509KeyPair(certPath, keyPath)
	if check(err) {
		return cert
	} else {
		log.Fatalf("cannot create x509 keypair from cert file %s and key file %s due to: %v", certPath, keyPath, err)
		return tls.Certificate{}
	}
}

func loadCaCerts(caCertsPath string) *x509.CertPool {
	var err error
	if caCertsPath == "" {
		return nil
	}
	caCert, err := ioutil.ReadFile(caCertsPath)
	check(err)
	caCertPool := x509.NewCertPool()
	caCertPool.AppendCertsFromPEM(caCert)
	return caCertPool
}

func keepAlive() {
	channel := make(chan int)
	<-channel
}
