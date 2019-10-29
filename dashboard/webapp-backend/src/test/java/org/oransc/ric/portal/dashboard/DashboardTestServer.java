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

import java.lang.invoke.MethodHandles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * This class supports front-end web development. Placing this class in the test
 * area allows excluding the mock configuration classes and the Mockito
 * dependencies from the packaged version of the app.
 *
 * To launch a development server set the environment variable as listed below.
 * This runs a Spring-Boot server with mock back-end beans, and keeps the server
 * alive for manual testing. Supply this JVM argument:
 *
 * <pre>
 * -Dorg.oransc.ric.portal.dashboard=mock
 * </pre>
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class DashboardTestServer {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/*
	 * Keeps the test server alive forever. Use a guard so this test is never run by
	 * Jenkins.
	 */
	@EnabledIfSystemProperty(named = "org.oransc.ric.portal.dashboard", matches = "mock")
	@Test
	public void keepServerAlive() {
		logger.warn("Keeping server alive!");
		try {
			synchronized (this) {
				this.wait();
			}
		} catch (Exception ex) {
			logger.warn(ex.toString());
		}
	}
}
