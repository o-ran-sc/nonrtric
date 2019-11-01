/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
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
package org.oransc.ric.portal.dashboard.a1controller.client.test;

import java.lang.invoke.MethodHandles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.oransc.ric.a1controller.client.api.A1ControllerApi;
import org.oransc.ric.a1controller.client.invoker.ApiClient;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidPIidPISchema;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidPIidPISchemaInput;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidPIidSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;

/**
 * Demonstrates use of the generated A1 controller client.
 *
 * The tests fail because no server is available.
 */
public class A1ControllerClientTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Test
	public void demo() {
		ApiClient apiClient = new ApiClient();
		apiClient.setBasePath("http://localhost:30099/");
		A1ControllerApi a1Api = new A1ControllerApi(apiClient);
		try {
			Object o = a1Api.a1ControllerGetPolicyInstance(new InputNRRidPTidPIidSchema());
			logger.info(
					"getPolicy answered code {}, content {} ", apiClient.getStatusCode().toString(), o.toString());
			Assertions.assertTrue(apiClient.getStatusCode().is2xxSuccessful());
		} catch (RestClientException e) {
			logger.error("getPolicy failed: {}", e.toString());
		}
		try {
			String policy = "{}";
			InputNRRidPTidPIidPISchema body = new InputNRRidPTidPIidPISchema();
			InputNRRidPTidPIidPISchemaInput input = new InputNRRidPTidPIidPISchemaInput();
			input.setNearRtRicId("1");
			input.setPolicyTypeId(1);
			input.setPolicyInstanceId("1");
			input.setPolicyInstance("{}");
			body.setInput(input);
			a1Api.a1ControllerCreatePolicyInstance(body);
			logger.info("putPolicy answered: {}", apiClient.getStatusCode().toString());
			Assertions.assertTrue(apiClient.getStatusCode().is2xxSuccessful());
		} catch (RestClientException e) {
			logger.error("getPolicy failed: {}", e.toString());
		}
	}
}
