/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 AT&T Intellectual Property
 * %%
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
 * ========================LICENSE_END===================================
 */
package org.oransc.ric.portal.dashboard.k8sapi;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Provides a method to get a list of pods using RestTemplate. As currently
 * configured this uses the default JVM HTTPS support which can be configured to
 * ignore errors as a workaround for self-signed SSL certificates.
 */
public class SimpleKubernetesClient {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final String k8sUrl;

	public SimpleKubernetesClient(String baseUrl) {
		logger.debug("ctor: baseUrl {}", baseUrl);
		k8sUrl = baseUrl;
	}

	public String listPods(String namespace) {
		logger.debug("listPods for namespace {}", namespace);
		String podsUrl = new DefaultUriBuilderFactory(k8sUrl.trim()).builder().pathSegment("v1")
				.pathSegment("namespaces").pathSegment(namespace.trim()).pathSegment("pods").build().normalize()
				.toString();
		RestTemplate rt = new RestTemplate();
		ResponseEntity<String> podsResponse = rt.getForEntity(podsUrl, String.class);
		return podsResponse.getBody();
	}

}
