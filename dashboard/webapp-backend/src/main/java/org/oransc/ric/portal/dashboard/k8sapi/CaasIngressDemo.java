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
package org.oransc.ric.portal.dashboard.k8sapi;

import java.lang.invoke.MethodHandles;

import org.oransc.ric.portal.dashboard.util.HttpsURLConnectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class CaasIngressDemo {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static void main(String[] args) throws Exception {
		HttpsURLConnectionUtils.turnOffSslChecking();
		// Get IP address from REC deployment team for testing
		final String podsUrl = "https://10.0.0.1:16443/api/v1/namespaces/ricaux/pods";
		RestTemplate rt = new RestTemplate();
		ResponseEntity<String> podsResponse = rt.getForEntity(podsUrl, String.class);
		logger.info(podsResponse.getBody());
		HttpsURLConnectionUtils.turnOnSslChecking();
	}

}
