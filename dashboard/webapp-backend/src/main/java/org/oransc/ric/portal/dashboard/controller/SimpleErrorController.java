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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;

import springfox.documentation.annotations.ApiIgnore;

/**
 * Provides a controller which is invoked on any error within the Spring-managed
 * context, including page not found, and redirects the caller to a custom error
 * page. The caller is also redirected to this page if a REST controller takes
 * an uncaught exception.
 * 
 * If trace is requested via request parameter ("?trace=true") and available,
 * adds stack trace information to the standard JSON error response.
 * 
 * Excluded from Swagger API documentation.
 * 
 * https://stackoverflow.com/questions/25356781/spring-boot-remove-whitelabel-error-page
 * https://www.baeldung.com/spring-boot-custom-error-page
 */

@ApiIgnore
@Controller
@RequestMapping(value = SimpleErrorController.ERROR_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class SimpleErrorController implements ErrorController {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static final String ERROR_PATH = "/error";

	private final ErrorAttributes errorAttributes;

	@Autowired
	public SimpleErrorController(ErrorAttributes errorAttributes) {
		this.errorAttributes = errorAttributes;
	}

	@Override
	public String getErrorPath() {
		logger.warn("getErrorPath");
		return ERROR_PATH;
	}

	@GetMapping
	public String handleError(HttpServletRequest request) {
		ServletWebRequest servletWebRequest = new ServletWebRequest(request);
		Throwable t = errorAttributes.getError(servletWebRequest);
		if (t != null)
			logger.warn("handleError", t);
		Map<String, Object> attributes = errorAttributes.getErrorAttributes(servletWebRequest, true);
		attributes.forEach((attribute, value) -> {
			logger.warn("handleError: {} -> {}", attribute, value);
		});
		// Return the name of the page INCLUDING suffix, which I guess is a "view" name.
		// Just "error" is not enough, but don't seem to need a ModelAndView object.
		return "error.html";
	}

}
