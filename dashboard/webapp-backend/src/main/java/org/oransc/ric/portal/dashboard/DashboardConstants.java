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
package org.oransc.ric.portal.dashboard;

public abstract class DashboardConstants {

	private DashboardConstants() {
		// Sonar insists on hiding the constructor
	}

	public static final String ENDPOINT_PREFIX = "/api";
	// Factor out method names used in multiple controllers
	public static final String VERSION_METHOD = "version";
	public static final String APP_NAME_AC = "AC";
	public static final String APP_NAME_MC = "MC";
	// The role names are defined by ONAP Portal.
	// The prefix "ROLE_" is required by Spring.
	// These are used in Java code annotations that require constants.
	public static final String ROLE_NAME_STANDARD = "Standard_User";
	public static final String ROLE_NAME_ADMIN = "System_Administrator";
	private static final String ROLE_PREFIX = "ROLE_";
	public static final String ROLE_ADMIN = ROLE_PREFIX + ROLE_NAME_ADMIN;
	public static final String ROLE_STANDARD = ROLE_PREFIX + ROLE_NAME_STANDARD;
	public static final String A1_CONTROLLER_USERNAME = "admin";
	public static final String A1_CONTROLLER_PASSWORD = "Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U";

}
