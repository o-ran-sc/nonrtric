/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
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
package org.oransc.ric.portal.dashboard.config;

import java.lang.invoke.MethodHandles;
import org.oransc.ric.a1controller.client.api.A1ControllerApi;
import org.oransc.ric.a1controller.client.invoker.ApiClient;
import org.oransc.ric.portal.dashboard.DashboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Creates an A1 controller client as a bean to be managed by the Spring
 * container.
 */
@Configuration
@Profile("!test")
public class A1ControllerConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public static final String A1_CONTROLLER_USERNAME = DashboardConstants.A1_CONTROLLER_USERNAME;
	public static final String A1_CONTROLLER_PASSWORD = DashboardConstants.A1_CONTROLLER_PASSWORD;

	// Populated by the autowired constructor
	private final String a1ControllerUrl;

	@Autowired
	public A1ControllerConfiguration(@Value("${a1controller.url.prefix}") final String urlPrefix, //
			@Value("${a1controller.url.suffix}") final String urlSuffix) {
		logger.debug("ctor prefix '{}' suffix '{}'", urlPrefix, urlSuffix);
		a1ControllerUrl = new DefaultUriBuilderFactory(urlPrefix.trim()).builder().path(urlSuffix.trim()).build().normalize()
				.toString();
		logger.info("Configuring A1 Controller at URL {}", a1ControllerUrl);
	}

	private ApiClient apiClient() {
		ApiClient apiClient = new ApiClient(new RestTemplate());
		apiClient.setBasePath(a1ControllerUrl);
		apiClient.setUsername(A1_CONTROLLER_USERNAME);
		apiClient.setPassword(A1_CONTROLLER_PASSWORD);
		return apiClient;
	}

	@Bean
	// The bean (method) name must be globally unique
	public A1ControllerApi a1ControllerApi() {
		return new A1ControllerApi(apiClient());
	}

}
