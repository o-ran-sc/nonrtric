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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URLEncoder;
import java.util.HashSet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.onap.portalsdk.core.onboarding.util.KeyProperties;
import org.onap.portalsdk.core.onboarding.util.PortalApiConstants;
import org.onap.portalsdk.core.onboarding.util.PortalApiProperties;
import org.onap.portalsdk.core.restful.domain.EcompRole;
import org.onap.portalsdk.core.restful.domain.EcompUser;
import org.oransc.ric.portal.dashboard.DashboardConstants;
import org.oransc.ric.portal.dashboard.DashboardUserManager;
import org.oransc.ric.portal.dashboard.model.EcompUserDetails;
import org.owasp.esapi.reference.DefaultSecurityConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * This filter checks every request for the cookie set by the ONAP Portal single
 * sign on process. The possible paths and actions:
 * <OL>
 * <LI>User starts at an app page via a bookmark. No Portal cookie is set.
 * Redirect there to get one; then continue as below.
 * <LI>User starts at Portal and goes to app. Alternately, the user's session
 * times out and the user hits refresh. The Portal cookie is set, but there is
 * no valid session. Create one and publish info.
 * <LI>User has valid Portal cookie and session. Reset the max idle in that
 * session.
 * </OL>
 * <P>
 * Notes:
 * <UL>
 * <LI>While redirecting, the cookie "redirectUrl" should also be set so that
 * Portal knows where to forward the request to once the Portal Session is
 * created and EPService cookie is set.
 * </UL>
 * 
 * TODO: What about sessions? Will this be stateless?
 * 
 * This filter uses no annotations to avoid Spring's automatic registration,
 * which add this filter in the chain in the wrong order.
 */
public class PortalAuthenticationFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	// Unfortunately not all file names are defined as constants
	private static final String[] securityPropertyFiles = { KeyProperties.PROPERTY_FILE_NAME,
			PortalApiProperties.PROPERTY_FILE_NAME, DefaultSecurityConfiguration.DEFAULT_RESOURCE_FILE,
			"validation.properties" };

	public static final String REDIRECT_URL_KEY = "redirectUrl";

	private final boolean enforcePortalSecurity;
	private final PortalAuthManager authManager;

	private final DashboardUserManager userManager;

	public PortalAuthenticationFilter(boolean portalSecurity, PortalAuthManager authManager,
			DashboardUserManager userManager) {
		this.enforcePortalSecurity = portalSecurity;
		this.authManager = authManager;
		this.userManager = userManager;
		if (portalSecurity) {
			// Throw if security is requested and prerequisites are not met
			for (String pf : securityPropertyFiles) {
				InputStream in = MethodHandles.lookup().lookupClass().getClassLoader().getResourceAsStream(pf);
				if (in == null) {
					String msg = "Failed to find property file on classpath: " + pf;
					logger.error(msg);
					throw new RuntimeException(msg);
				} else {
					try {
						in.close();
					} catch (IOException ex) {
						logger.warn("Failed to close stream", ex);
					}
				}
			}
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// complain loudly if this key property is missing
		String url = PortalApiProperties.getProperty(PortalApiConstants.ECOMP_REDIRECT_URL);
		logger.debug("init: Portal redirect URL {}", url);
		if (url == null)
			logger.error(
					"init: Failed to find property in portal.properties: " + PortalApiConstants.ECOMP_REDIRECT_URL);
	}

	@Override
	public void destroy() {
		// No resources to release
	}

	/**
	 * Requests for pages ignored in the web security config do not hit this filter.
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		if (enforcePortalSecurity)
			doFilterEPSDKFW(req, res, chain);
		else
			doFilterMockUserAdminRole(req, res, chain);
	}

	/*
	 * Populates security context with a mock user in the admin role.
	 * 
	 */
	private void doFilterMockUserAdminRole(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getAuthorities().isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("doFilter adding auth to request URI {}",
						(req instanceof HttpServletRequest) ? ((HttpServletRequest) req).getRequestURL() : req);
			}
			EcompRole admin = new EcompRole();
			admin.setId(1L);
			admin.setName(DashboardConstants.ROLE_ADMIN);
			HashSet<EcompRole> roles = new HashSet<>();
			roles.add(admin);
			EcompUser user = new EcompUser();
			user.setLoginId("fakeLoginId");
			user.setRoles(roles);
			user.setActive(true);
			EcompUserDetails userDetails = new EcompUserDetails(user);
			PreAuthenticatedAuthenticationToken authToken = new PreAuthenticatedAuthenticationToken(userDetails,
					"fakeCredentials", userDetails.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authToken);
		} else {
			logger.debug("doFilter: authorities {}", auth.getAuthorities());
		}
		chain.doFilter(req, res);
	}

	/*
	 * Checks for valid cookies and allows request to be served if found; redirects
	 * to Portal otherwise.
	 */
	private void doFilterEPSDKFW(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		if (logger.isTraceEnabled())
			logger.trace("doFilter: req {}", request.getRequestURI());
		// Need to authenticate the request
		final String userId = authManager.validateEcompSso(request);
		final EcompUser ecompUser = (userId == null ? null : userManager.getUser(userId));
		if (userId == null || ecompUser == null) {
			logger.debug("doFilter: unauthorized user requests URI {}, serving login page", request.getRequestURI());
			StringBuffer sb = request.getRequestURL();
			sb.append(request.getQueryString() == null ? "" : "?" + request.getQueryString());
			String body = generateLoginRedirectPage(sb.toString());
			response.setContentType(MediaType.TEXT_HTML_VALUE);
			response.getWriter().print(body);
			response.getWriter().flush();
		} else {
			EcompUserDetails userDetails = new EcompUserDetails(ecompUser);
			// Using portal session as credentials is a hack
			PreAuthenticatedAuthenticationToken authToken = new PreAuthenticatedAuthenticationToken(userDetails,
					getPortalSessionId(request), userDetails.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authToken);
			// Pass request back down the filter chain
			chain.doFilter(request, response);
		}
	}

	/**
	 * Generates a page with text only, absolutely no references to any webapp
	 * resources, so this can be served to an unauthenticated user without
	 * triggering a new authentication attempt. The page has a link to the Portal
	 * URL from configuration, with a return URL that is the original request.
	 * 
	 * @param appUrl
	 *                   Original requested URL
	 * @return HTML
	 * @throws UnsupportedEncodingException
	 *                                          On error
	 */
	private static String generateLoginRedirectPage(String appUrl) throws UnsupportedEncodingException {
		String encodedAppUrl = URLEncoder.encode(appUrl, "UTF-8");
		String portalBaseUrl = PortalApiProperties.getProperty(PortalApiConstants.ECOMP_REDIRECT_URL);
		String redirectUrl = portalBaseUrl + "?" + PortalAuthenticationFilter.REDIRECT_URL_KEY + "=" + encodedAppUrl;
		String aHref = "<a href=\"" + redirectUrl + "\">";
		// If only Java had "here" documents.
		String body = String.join(//
				System.getProperty("line.separator"), //
				"<html>", //
				"<head>", //
				"<title>RIC Dashboard</title>", //
				"<style>", //
				"html, body { ", //
				"  font-family: Helvetica, Arial, sans-serif;", //
				"}", //
				"</style>", //
				"</head>", //
				"<body>", //
				"<h2>RIC Dashboard</h2>", //
				"<h4>Please log in.</h4>", //
				"<p>", //
				aHref, "Click here to authenticate at the ONAP Portal</a>", //
				"</p>", //
				"</body>", //
				"</html>");
		return body;
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
	 * Gets the ECOMP Portal service cookie value.
	 * 
	 * @param request
	 * @return Cookie value, or null if not found.
	 */
	private String getPortalSessionId(HttpServletRequest request) {
		Cookie ep = getCookie(request, PortalApiConstants.EP_SERVICE);
		if (ep == null)
			return null;
		return ep.getValue();
	}

}
