// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2022-2023: Nordix Foundation
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
package pemtojwks 

import (
	"crypto/rsa"
	"crypto/sha1"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"fmt"
	"golang.org/x/crypto/ssh"
	"io/ioutil"
	"math/big"
)

type Jwks struct {
	Keys []Key `json:"keys"`
}
type Key struct {
	Kid string `json:"kid,omitempty"`
	Kty string `json:"kty"`
	Alg string `json:"alg"`
	Use string `json:"use"`
	N   string `json:"n"`
	E   string `json:"e"`
	X5c []string `json:"x5c"`
	X5t string `json:"x5t"`
}

func getKeyFromPrivate(key []byte) *rsa.PublicKey {
	parsed, err := ssh.ParseRawPrivateKey(key)
	if err != nil {
		fmt.Println(err)
	}

	// Convert back to an *rsa.PrivateKey
	privateKey := parsed.(*rsa.PrivateKey)

	publicKey := &privateKey.PublicKey
	return publicKey
}

func getKeyFromPublic(key []byte) *rsa.PublicKey {
	pubPem, _ := pem.Decode(key)

	parsed, err := x509.ParsePKIXPublicKey(pubPem.Bytes)
	if err != nil {
		fmt.Println("Unable to parse RSA public key", err)
	}

	// Convert back to an *rsa.PublicKey
	publicKey := parsed.(*rsa.PublicKey)

	return publicKey
}

func getCert(cert []byte) *x509.Certificate {
	certPem, _ := pem.Decode(cert)
	if certPem == nil {
		panic("Failed to parse pem file")
	}

	// pass cert bytes
	certificate, err := x509.ParseCertificate(certPem.Bytes)
	if err != nil {
		fmt.Println("Unable to parse Certificate", err)
	}

	return certificate
}

func getPublicKeyFromCert(cert_bytes []byte) *rsa.PublicKey {
        block, _ := pem.Decode([]byte(cert_bytes))
        var cert *x509.Certificate
        cert, _ = x509.ParseCertificate(block.Bytes)
        rsaPublicKey := cert.PublicKey.(*rsa.PublicKey)

        return rsaPublicKey 
}

func CreateJWKS(certFile string) (string, string, string) {
	var publicKey *rsa.PublicKey
	var kid string = "SIGNING_KEY"

	cert, err := ioutil.ReadFile(certFile)
	if err != nil {
		fmt.Println(err)
	}
	publicKey = getPublicKeyFromCert(cert)
	publicKeyBytes, err := x509.MarshalPKIXPublicKey(publicKey)
	if err != nil {
		fmt.Println(err)
	}
	publicKeyPem := pem.EncodeToMemory(&pem.Block{Type: "RSA PUBLIC KEY", Bytes: publicKeyBytes})
	block, _ := pem.Decode(publicKeyPem)
	publicKeyString := base64.StdEncoding.EncodeToString(block.Bytes)

	certificate := getCert(cert)
	// generate fingerprint with sha1
	// you can also use md5, sha256, etc.
	fingerprint := sha1.Sum(certificate.Raw)

	jwksKey := Key{
		Kid: kid,
		Kty: "RSA",
		Alg: "RS256",
		Use: "sig",
		N: base64.RawStdEncoding.EncodeToString(publicKey.N.Bytes()),
		E: base64.RawStdEncoding.EncodeToString(big.NewInt(int64(publicKey.E)).Bytes()),
		X5c: []string{base64.RawStdEncoding.EncodeToString(certificate.Raw)},
		X5t: base64.RawStdEncoding.EncodeToString(fingerprint[:]),
	}
	jwksKeys := []Key{jwksKey}
	jwks := Jwks{jwksKeys}

	jwksJson, err := json.Marshal(jwks)
	if err != nil {
		fmt.Println(err)
	}
	return string(jwksJson), publicKeyString, kid

}
