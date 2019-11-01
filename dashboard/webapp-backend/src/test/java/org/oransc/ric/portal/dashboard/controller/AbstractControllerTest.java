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
package org.oransc.ric.portal.dashboard.controller;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oransc.ric.portal.dashboard.config.WebSecurityMockConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
// Need the fake answers from the backend
@ActiveProfiles("test")
public class AbstractControllerTest {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	// Created by Spring black magic
	// https://spring.io/guides/gs/testing-web/
	@LocalServerPort
	private int localServerPort;

	@Autowired
	protected TestRestTemplate restTemplate;

	/**
	 * Flexible URI builder.
	 * 
	 * @param queryParams
	 *                        Map of string-string query parameters
	 * @param path
	 *                        Array of path components. If a component has an
	 *                        embedded slash, the string is split and each
	 *                        subcomponent is added individually.
	 * @return URI
	 */
	protected URI buildUri(final Map<String, String> queryParams, final String... path) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://localhost:" + localServerPort + "/");
		for (int p = 0; p < path.length; ++p) {
			if (path[p] == null || path[p].isEmpty()) {
				throw new IllegalArgumentException("Unexpected null or empty at path index " + Integer.toString(p));
			} else if (path[p].contains("/")) {
				String[] subpaths = path[p].split("/");
				for (String s : subpaths)
					if (!s.isEmpty())
						builder.pathSegment(s);
			} else {
				builder.pathSegment(path[p]);
			}
		}
		if (queryParams != null && queryParams.size() > 0) {
			for (Map.Entry<String, String> entry : queryParams.entrySet()) {
				if (entry.getKey() == null || entry.getValue() == null)
					throw new IllegalArgumentException("Unexpected null key or value");
				else
					builder.queryParam(entry.getKey(), entry.getValue());
			}
		}
		return builder.build().encode().toUri();
	}

	// Because I put the annotations on this parent class,
	// must define at least one test here.
	@Test
	public void contextLoads() {
		// Silence Sonar warning about missing assertion.
		Assertions.assertTrue(logger.isWarnEnabled());
		logger.info("Context loads on mock profile");
	}

	public TestRestTemplate testRestTemplateAdminRole() {
		return restTemplate.withBasicAuth(WebSecurityMockConfiguration.TEST_CRED_ADMIN,
				WebSecurityMockConfiguration.TEST_CRED_ADMIN);
	}

	public TestRestTemplate testRestTemplateStandardRole() {
		return restTemplate.withBasicAuth(WebSecurityMockConfiguration.TEST_CRED_STANDARD,
				WebSecurityMockConfiguration.TEST_CRED_STANDARD);
	}

}
