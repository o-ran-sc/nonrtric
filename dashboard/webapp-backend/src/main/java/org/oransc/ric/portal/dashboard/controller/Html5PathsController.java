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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Listens for requests to known Angular routes.
 */
@Controller
public class Html5PathsController {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Forwards the browser to the index (main) page upon request of a known route.
	 * This unfortunately requires duplication of the Angular route strings in the
	 * path mappings on this method. Could switch to a regex pattern instead someday
	 * if the routes change too often.
	 * 
	 * https://stackoverflow.com/questions/44692781/configure-spring-boot-to-redirect-404-to-a-single-page-app
	 * 
	 * @param request
	 *                     HttpServletRequest
	 * @param response
	 *                     HttpServletResponse
	 * @throws IOException
	 *                         On error
	 */
	@RequestMapping(method = { RequestMethod.OPTIONS, RequestMethod.GET }, //
			path = { "/catalog", "/control", "/stats", "/user" })
	public void forwardAngularRoutes(HttpServletRequest request, HttpServletResponse response) throws IOException {
		URL url = new URL(request.getScheme(), request.getServerName(), request.getServerPort(), "/index.html");
		if (logger.isDebugEnabled())
			logger.debug("forwardAngularRoutes: {} redirected to {}", request.getRequestURI(), url);
		response.sendRedirect(url.toString());
	}

}
