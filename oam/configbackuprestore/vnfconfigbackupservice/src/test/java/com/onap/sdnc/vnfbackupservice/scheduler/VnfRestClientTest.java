/*
* ============LICENSE_START=======================================================
* ONAP : SDNC-FEATURES
* ================================================================================
* Copyright 2018 TechMahindra
*=================================================================================
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* ============LICENSE_END=========================================================
*/
package com.onap.sdnc.vnfbackupservice.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import mockit.Mock;
import mockit.MockUp;


public class VnfRestClientTest {
	
	String url = "/restconf/config/VNF-API:vnfs";
	String userName = "abc";
	String password = "abc";
	
	@Test
	public void restClientTest() {

		new MockUp<RestTemplate>() {
			@SuppressWarnings("unchecked")
			@Mock
			public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
					Class<T> responseType, Object... uriVariables) throws RestClientException {
				ResponseEntity<String> str = new ResponseEntity<String>(HttpStatus.ACCEPTED);
				return (ResponseEntity<T>) str;
			}
		};
		VnfRestClient vnfRestClientmock = mock(VnfRestClient.class);
		when(vnfRestClientmock.vnfRestClient(url, userName, password)).thenReturn("successfully mocked");
	 
		VnfRestClient vnfRestClient =new VnfRestClient();
		vnfRestClient.vnfRestClient(url, userName, password);
		
	}
}
