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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.onap.portalsdk.core.onboarding.crossapi.IPortalRestCentralService;
import org.onap.portalsdk.core.onboarding.exception.CipherUtilException;
import org.onap.portalsdk.core.onboarding.util.PortalApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides services to authenticate requests from/to ONAP Portal.
 */
public class PortalAuthManager {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	final Map<String, String> credentialsMap;
	private final IPortalSdkDecryptor portalSdkDecryptor;
	private final String userIdCookieName;

	public PortalAuthManager(final String appName, final String username, final String password,
			final String decryptorClassName, final String userCookie)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		credentialsMap = new HashMap<>();
		credentialsMap.put(IPortalRestCentralService.CREDENTIALS_APP, appName);
		credentialsMap.put(IPortalRestCentralService.CREDENTIALS_USER, username);
		credentialsMap.put(IPortalRestCentralService.CREDENTIALS_PASS, password);
		this.userIdCookieName = userCookie;
		// Instantiate here so configuration errors are detected at app-start time
		logger.debug("ctor: using decryptor class {}", decryptorClassName);
		Class<?> decryptorClass = Class.forName(decryptorClassName);
		portalSdkDecryptor = (IPortalSdkDecryptor) decryptorClass.getDeclaredConstructor().newInstance();
	}

	/**
	 * @return A map of key-value pairs with application name, user name and
	 *         password.
	 */
	public Map<String, String> getAppCredentials() {
		return credentialsMap;
	}

	/**
	 * Searches the request for a cookie with the specified name.
	 *
	 * @param request
	 *                       HttpServletRequest
	 * @param cookieName
	 *                       Cookie name
	 * @return Cookie, or null if not found.
	 */
	private Cookie getCookie(HttpServletRequest request, String cookieName) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null)
			for (Cookie cookie : cookies)
				if (cookie.getName().equals(cookieName))
					return cookie;
		return null;
	}

	/**
	 * Validates whether the ECOMP Portal sign-on process has completed. Checks for
	 * the ECOMP cookie first, then the user cookie.
	 * 
	 * @param request
	 *                    HttpServletRequest
	 * @return User ID if the ECOMP cookie is present and the sign-on process
	 *         established a user ID; else null.
	 */
	public String validateEcompSso(HttpServletRequest request) {
		// Check ECOMP Portal cookie
		Cookie ep = getCookie(request, PortalApiConstants.EP_SERVICE);
		if (ep == null) {
			logger.debug("validateEcompSso: cookie not found: {}", PortalApiConstants.EP_SERVICE);
			return null;
		}
		logger.trace("validateEcompSso: found cookie {}", PortalApiConstants.EP_SERVICE);
		Cookie user = getCookie(request, userIdCookieName);
		if (user == null) {
			logger.debug("validateEcompSso: cookie not found: {}", userIdCookieName);
			return null;
		}
		logger.trace("validateEcompSso: user cookie {}", userIdCookieName);
		String userid = null;
		try {
			userid = portalSdkDecryptor.decrypt(user.getValue());
		} catch (CipherUtilException e) {
			throw new IllegalArgumentException("validateEcompSso failed", e);
		}
		return userid;
	}

}
