/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.o_ran_sc.nonrtric.sdnc_a1.northbound.restadapter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

/**
 * This class provides the Generic Rest Adapter interface to the RestTemplate
 *
 * @author lathishbabu.ganesan@est.tech
 *
 */

public class RestAdapterImpl implements RestAdapter {

  private static final String PROPERTIES_FILE = "nonrt-ric-api-provider.properties";
  private final Logger log = LoggerFactory.getLogger(RestAdapterImpl.class);

  private RestTemplate restTemplateHttp;
  private RestTemplate restTemplateHttps;

  public RestAdapterImpl() {
      restTemplateHttp = new RestTemplate();
      try {
          restTemplateHttps = createRestTemplateForHttps();
      } catch (IOException | UnrecoverableKeyException | KeyManagementException | CertificateException
              | NoSuchAlgorithmException | KeyStoreException ex) {
        log.error("Caught exception when trying to create restTemplateHttps: {}", ex.getMessage());
      }
  }

  private RestTemplate createRestTemplateForHttps() throws IOException, UnrecoverableKeyException, CertificateException,
              NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
      InputStream inputStream = RestAdapterImpl.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
      if (inputStream == null) {
          throw new FileNotFoundException("properties file not found in classpath");
      } else {
          Properties properties = new Properties();
          properties.load(inputStream);
          final String keystorePassword = properties.getProperty("key-store-password");
          SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                  SSLContexts.custom()
                             .loadKeyMaterial(ResourceUtils.getFile(properties.getProperty("key-store")),
                                     keystorePassword.toCharArray(), keystorePassword.toCharArray())
                             .loadTrustMaterial(null, new TrustAllStrategy())
                             .build(),
                  NoopHostnameVerifier.INSTANCE);
          HttpClient client = HttpClients.custom().setSSLSocketFactory(scsf).build();
          HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
          requestFactory.setHttpClient(client);
          inputStream.close();
          return new RestTemplate(requestFactory);
      }
  }

  private HttpEntity<?> getHttpEntity(final Object object) {
    return new HttpEntity<>(object);
  }

  @Override
  public <T> ResponseEntity<T> get(String uri, Class<?> clazz) {
    HttpEntity<?> entity = getHttpEntity(null);
    return invokeHttpRequest(uri, HttpMethod.GET, clazz, entity);
  }

  @Override
  public <T> ResponseEntity<T> put(String uri, String body, Class<T> clazz) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(body, headers);
    return invokeHttpRequest(uri, HttpMethod.PUT, clazz, entity);
  }

  @Override
  public <T> ResponseEntity<T> delete(String uri) {
    HttpEntity<?> entity = getHttpEntity(null);
    return invokeHttpRequest(uri, HttpMethod.DELETE, null, entity);
  }

  @SuppressWarnings("unchecked")
  private <T> ResponseEntity<T> invokeHttpRequest(String uri, HttpMethod httpMethod, Class<?> clazz,
      HttpEntity<?> entity) {
    try {
        URL url = new URL(uri);
        if (url.getProtocol().equals("https")) {
            return (ResponseEntity<T>) restTemplateHttps.exchange(uri, httpMethod, entity, clazz);
        } else if (url.getProtocol().equals("http")) {
            return (ResponseEntity<T>) restTemplateHttp.exchange(uri, httpMethod, entity, clazz);
        } else {
            log.error("Invalid protocol in URL");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    } catch (MalformedURLException ex) {
        log.error("URL is not valid, exception: {}", ex.getMessage());
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }
}