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
package generatejwt

import (
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"fmt"
	"github.com/dgrijalva/jwt-go"
	"io/ioutil"
	"log"
	"time"
)

type JWT struct {
	privateKey []byte
	publicKey  []byte
}

func NewJWT(privateKey []byte, publicKey []byte) JWT {
	return JWT{
		privateKey: privateKey,
		publicKey:  publicKey,
	}
}

func readFile(file string) []byte {
	key, err := ioutil.ReadFile(file)
	if err != nil {
		log.Fatalln(err)
	}
	return key
}

func (j JWT) createWithKey(ttl time.Duration, content interface{}, client, realm string) (string, error) {
	key, err := jwt.ParseRSAPrivateKeyFromPEM(j.privateKey)
	if err != nil {
		return "", fmt.Errorf("create: parse key: %w", err)
	}

	now := time.Now().UTC()

	claims := make(jwt.MapClaims)
	claims["dat"] = content             // Our custom data.
	claims["exp"] = now.Add(ttl).Unix() // The expiration time after which the token must be disregarded.
	claims["iat"] = now.Unix()          // The time at which the token was issued.
	claims["nbf"] = now.Unix()          // The time before which the token must be disregarded.
	claims["jti"] = "myJWTId" + fmt.Sprint(now.UnixNano())
	claims["sub"] = client
	claims["iss"] = client
	claims["aud"] = realm

	token := jwt.NewWithClaims(jwt.SigningMethodRS256, claims)
	tokenString, err := token.SignedString(key)
	if err != nil {
		return "", fmt.Errorf("create: sign token: %w", err)
	}

	return tokenString, nil
}

func createWithSecret(ttl time.Duration, content interface{}, client, realm, secret string) (string, error) {
	now := time.Now().UTC()

	claims := make(jwt.MapClaims)
	claims["dat"] = content             // Our custom data.
	claims["exp"] = now.Add(ttl).Unix() // The expiration time after which the token must be disregarded.
	claims["iat"] = now.Unix()          // The time at which the token was issued.
	claims["nbf"] = now.Unix()          // The time before which the token must be disregarded.
	claims["jti"] = "myJWTId" + fmt.Sprint(now.UnixNano())
	claims["sub"] = client
	claims["iss"] = client
	claims["aud"] = realm

	token, err := jwt.NewWithClaims(jwt.SigningMethodHS256, claims).SignedString([]byte(secret))
	if err != nil {
		return "", fmt.Errorf("create: sign token: %w", err)
	}

	return token, nil
}

func (j JWT) Validate(token string) (interface{}, error) {
	key, err := jwt.ParseRSAPublicKeyFromPEM(j.publicKey)
	if err != nil {
		return "", fmt.Errorf("validate: parse key: %w", err)
	}

	tok, err := jwt.Parse(token, func(jwtToken *jwt.Token) (interface{}, error) {
		if _, ok := jwtToken.Method.(*jwt.SigningMethodRSA); !ok {
			return nil, fmt.Errorf("unexpected method: %s", jwtToken.Header["alg"])
		}

		return key, nil
	})
	if err != nil {
		return nil, fmt.Errorf("validate: %w", err)
	}

	claims, ok := tok.Claims.(jwt.MapClaims)
	if !ok || !tok.Valid {
		return nil, fmt.Errorf("validate: invalid")
	}

	return claims["dat"], nil
}

func createPublicKeyFromPrivateKey(privkey_bytes []byte) []byte {
	block, _ := pem.Decode([]byte(privkey_bytes))
	var privateKey *rsa.PrivateKey
	pkcs1, err := x509.ParsePKCS1PrivateKey(block.Bytes)
	if err != nil {
		pkcs8, err := x509.ParsePKCS8PrivateKey(block.Bytes)
		privateKey = pkcs8.(*rsa.PrivateKey)
		if err != nil {
			log.Fatal(err)
		}
	} else {
		privateKey = pkcs1
	}

	publicKey := &privateKey.PublicKey

	pubkey_bytes, err := x509.MarshalPKIXPublicKey(publicKey)
	if err != nil {
		log.Fatal(err)
	}

	pubkey_pem := pem.EncodeToMemory(
		&pem.Block{
			Type:  "PUBLIC KEY",
			Bytes: pubkey_bytes,
		},
	)
	return pubkey_pem
}

func CreateJWT(privateKeyFile, secret, client, realm string) string {
	if secret == "" {
		prvKey := readFile(privateKeyFile)
		pubKey := createPublicKeyFromPrivateKey(prvKey)

		jwtToken := NewJWT(prvKey, pubKey)

		// 1. Create a new JWT token.
		tok, err := jwtToken.createWithKey(time.Hour, "Can be anything", client, realm)
		if err != nil {
			log.Fatalln(err)
		}

		// 2. Validate an existing JWT token.
		_, err = jwtToken.Validate(tok)
		if err != nil {
			log.Fatalln(err)
		}
		return tok
	} else {
		// 1. Create a new JWT token.
		tok, err := createWithSecret(time.Hour, "Can be anything", client, realm, secret)
		if err != nil {
			log.Fatalln(err)
		}
		return tok
	}

}
