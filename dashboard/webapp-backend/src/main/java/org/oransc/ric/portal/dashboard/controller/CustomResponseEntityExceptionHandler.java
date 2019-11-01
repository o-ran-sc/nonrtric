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

import org.oransc.ric.portal.dashboard.model.ErrorTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Catches certain exceptions. This controller advice factors out try-catch
 * blocks in many controller methods.
 * 
 * Also see:<br>
 * https://www.baeldung.com/exception-handling-for-rest-with-spring
 * https://www.springboottutorial.com/spring-boot-exception-handling-for-rest-services
 */
@ControllerAdvice
public class CustomResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	// Superclass has "logger" that is exposed here, so use a different name
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Logs the error and generates a JSON response when a REST controller method
	 * takes a RestClientResponseException. This is thrown by the Http client when a
	 * remote method returns a non-2xx code. All the controller methods are proxies
	 * in that they just forward the request along to a remote system, so if that
	 * remote system fails, return 502 plus some details about the failure, rather
	 * than the generic 500 that Spring-Boot will return on an uncaught exception.
	 * 
	 * Why 502? I quote: <blockquote>HTTP server received an invalid response from a
	 * server it consulted when acting as a proxy or gateway.</blockquote>
	 * 
	 * @param ex
	 *                    The exception
	 * 
	 * @param request
	 *                    The original request
	 * 
	 * @return A response entity with status code 502 plus some details in the body.
	 */
	@ExceptionHandler({ RestClientResponseException.class })
	public final ResponseEntity<ErrorTransport> handleProxyMethodException(Exception ex, WebRequest request) {
		// Capture the full stack trace in the log.
		log.error("handleProxyMethodException: request {}, exception {}", request.getDescription(false), ex);
		if (ex instanceof HttpStatusCodeException) {
			HttpStatusCodeException hsce = (HttpStatusCodeException) ex;
			return new ResponseEntity<>(new ErrorTransport(hsce.getRawStatusCode(), hsce.getResponseBodyAsString(),
					ex.toString(), request.getDescription(false)), HttpStatus.BAD_GATEWAY);
		} else {
			return new ResponseEntity<>(new ErrorTransport(500, ex), HttpStatus.BAD_GATEWAY);
		}
	}

}
