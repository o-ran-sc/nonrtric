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
package org.oransc.ric.portal.dashboard.portalapi;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.onap.portalsdk.core.onboarding.crossapi.IPortalRestCentralService;
import org.onap.portalsdk.core.onboarding.exception.PortalAPIException;
import org.onap.portalsdk.core.restful.domain.EcompUser;
import org.oransc.ric.portal.dashboard.DashboardUserManager;
import org.oransc.ric.portal.dashboard.config.SpringContextCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Implements the contract used by the Portal to transmit user details to this
 * on-boarded application. The requests are intercepted first by a servlet in
 * the EPSDK-FW library, which proxies the calls to these methods.
 * 
 * An instance of this class is created upon first request to the API. But this
 * class is found and instantiated via Class.forName(), so cannot use Spring
 * annotations.
 */
public class PortalRestCentralServiceImpl implements IPortalRestCentralService {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final PortalAuthManager authManager;
	private final DashboardUserManager userManager;

	public PortalRestCentralServiceImpl() throws IOException, PortalAPIException {
		final ApplicationContext context = SpringContextCache.getApplicationContext();
		authManager = (PortalAuthManager) context.getBean(PortalAuthManager.class);
		userManager = (DashboardUserManager) context.getBean(DashboardUserManager.class);
	}

	/*
	 * Answers the Portal API credentials.
	 */
	@Override
	public Map<String, String> getAppCredentials() throws PortalAPIException {
		logger.debug("getAppCredentials");
		return authManager.getAppCredentials();
	}

	/*
	 * Extracts the user ID from a cookie in the header
	 */
	@Override
	public String getUserId(HttpServletRequest request) throws PortalAPIException {
		logger.debug("getuserId");
		return authManager.validateEcompSso(request);
	}

	@Override
	public void pushUser(EcompUser user) throws PortalAPIException {
		logger.debug("pushUser: {}", user);
		userManager.createUser(user);
	}

	@Override
	public void editUser(String loginId, EcompUser user) throws PortalAPIException {
		logger.debug("editUser: {}", user);
		userManager.updateUser(loginId, user);
	}

}
