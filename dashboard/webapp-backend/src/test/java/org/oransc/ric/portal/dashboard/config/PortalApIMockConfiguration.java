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
package org.oransc.ric.portal.dashboard.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.onap.portalsdk.core.onboarding.crossapi.PortalRestAPIProxy;
import org.onap.portalsdk.core.onboarding.util.PortalApiConstants;
import org.oransc.ric.portal.dashboard.portalapi.PortalAuthManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class PortalApIMockConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	// Unfortunately EPSDK-FW does not define these as constants
	public static final String PORTAL_USERNAME_HEADER_KEY = "username";
	public static final String PORTAL_PASSWORD_HEADER_KEY = "password";

	@Bean
	public ServletRegistrationBean<PortalRestAPIProxy> portalApiProxyServlet() {
		PortalRestAPIProxy servlet = new PortalRestAPIProxy();
		final ServletRegistrationBean<PortalRestAPIProxy> servletBean = new ServletRegistrationBean<>(servlet,
				PortalApiConstants.API_PREFIX + "/*");
		servletBean.setName("PortalRestApiProxyServlet");
		return servletBean;
	}

	@Bean
	public PortalAuthManager portalAuthManager() throws Exception {
		PortalAuthManager mockManager = mock(PortalAuthManager.class);
		final Map<String, String> credentialsMap = new HashMap<>();
		credentialsMap.put("appName", "appName");
		credentialsMap.put(PORTAL_USERNAME_HEADER_KEY, PORTAL_USERNAME_HEADER_KEY);
		credentialsMap.put(PORTAL_PASSWORD_HEADER_KEY, PORTAL_PASSWORD_HEADER_KEY);
		doAnswer(inv -> {
			logger.debug("getAppCredentials");
			return credentialsMap;
		}).when(mockManager).getAppCredentials();
		doAnswer(inv -> {
			logger.debug("getUserId");
			return "userId";
		}).when(mockManager).validateEcompSso(any(HttpServletRequest.class));
		doAnswer(inv -> {
			logger.debug("getAppCredentials");
			return credentialsMap;
		}).when(mockManager).getAppCredentials();
		return mockManager;
	}

}
