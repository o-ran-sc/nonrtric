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

package org.oransc.ric.portal.dashboard.model;

import java.time.Instant;

/**
 * This mimics the model Spring-Boot uses for a message returned on failure, to
 * be serialized as JSON.
 */
public class ErrorTransport implements IDashboardResponse {

	private Instant timestamp;
	private Integer status;
	private String error;
	private String message;
	private String path;

	/**
	 * Builds an empty object.
	 */
	public ErrorTransport() {
		// no-arg constructor
	}

	/**
	 * Convenience constructor for minimal value set.
	 * 
	 * @param status
	 *                   Integer value like 400
	 * @param error
	 *                   Error message
	 */
	public ErrorTransport(int status, String error) {
		this(status, error, null, null);
	}

	/**
	 * Convenience constructor for populating an error from an exception
	 * 
	 * @param status
	 *                      Integer value like 400
	 * @param throwable
	 *                      The caught exception/throwable to convert to String with
	 *                      an upper bound on characters
	 */
	public ErrorTransport(int status, Throwable throwable) {
		this.timestamp = Instant.now();
		this.status = status;
		final int enough = 256;
		String exString = throwable.toString();
		this.error = exString.length() > enough ? exString.substring(0, enough) : exString;
	}

	/**
	 * Builds an object with all fields
	 * 
	 * @param status
	 *                    Integer value like 500
	 * @param error
	 *                    Explanation
	 * @param message
	 *                    Additional explanation
	 * @param path
	 *                    Requested path
	 */
	public ErrorTransport(int status, String error, String message, String path) {
		this.timestamp = Instant.now();
		this.status = status;
		this.error = error;
		this.message = message;
		this.path = path;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String error) {
		this.message = error;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
