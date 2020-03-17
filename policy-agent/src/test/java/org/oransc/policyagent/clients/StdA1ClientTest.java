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

package org.oransc.policyagent.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.repository.Policy;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class StdA1ClientTest {
    private static final String RIC_URL = "RicUrl";
    private static final String POLICIES_IDENTITIES_URL = "/policies";
    private static final String POLICIES_URL = "/policies/";
    private static final String POLICY_TYPE_1_NAME = "type1";
    private static final String POLICY_1_ID = "policy1";
    private static final String POLICY_2_ID = "policy2";
    private static final String POLICY_JSON_VALID = "{\"policyId\":\"policy1\"}";
    private static final String POLICY_JSON_INVALID = "\"policyId\":\"policy1\"}";
    private static final String POLICY_TYPE = "typeName";

    StdA1ClientVersion1 clientUnderTest;

    @Mock
    AsyncRestClient asyncRestClientMock;

    @BeforeEach
    public void init() {
        clientUnderTest = new StdA1ClientVersion1(asyncRestClientMock);
    }

    @Test
    public void testGetPolicyTypeIdentities() {
        List<String> policyTypeIds = clientUnderTest.getPolicyTypeIdentities().block();
        assertEquals(1, policyTypeIds.size(), "should hardcoded to one");
        assertEquals("", policyTypeIds.get(0), "should hardcoded to empty");
    }

    @Test
    public void testGetPolicyIdentities() {
        Mono<String> policyIds = Mono.just(Arrays.asList(POLICY_1_ID, POLICY_2_ID).toString());
        when(asyncRestClientMock.get(anyString())).thenReturn(policyIds);

        List<String> result = clientUnderTest.getPolicyIdentities().block();
        assertEquals(2, result.size(), "");

        verify(asyncRestClientMock).get(POLICIES_IDENTITIES_URL);
    }

    @Test
    public void testGetValidPolicyType() {
        String policyType = clientUnderTest.getPolicyTypeSchema(POLICY_TYPE_1_NAME).block();
        assertEquals("{}", policyType, "");
    }

    @Test
    public void testPutPolicyValidResponse() {
        when(asyncRestClientMock.put(anyString(), anyString())).thenReturn(Mono.just(POLICY_JSON_VALID));

        Mono<String> policyMono = clientUnderTest
            .putPolicy(A1ClientHelper.createPolicy(RIC_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE));

        verify(asyncRestClientMock).put(POLICIES_URL + POLICY_1_ID, POLICY_JSON_VALID);
        StepVerifier.create(policyMono).expectNext(POLICY_JSON_VALID).expectComplete().verify();
    }

    @Test
    public void testPutPolicyInvalidResponse() {
        when(asyncRestClientMock.put(anyString(), anyString())).thenReturn(Mono.just(POLICY_JSON_INVALID));

        Mono<String> policyMono = clientUnderTest
            .putPolicy(A1ClientHelper.createPolicy(RIC_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE));

        StepVerifier.create(policyMono).expectErrorMatches(throwable -> throwable instanceof JSONException).verify();
    }

    @Test
    public void testDeletePolicy() {
        when(asyncRestClientMock.delete(POLICIES_URL + POLICY_1_ID)).thenReturn(Mono.empty());

        Policy policy = A1ClientHelper.createPolicy(RIC_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE);
        Mono<?> responseMono = clientUnderTest.deletePolicy(policy);
        verify(asyncRestClientMock).delete(POLICIES_URL + POLICY_1_ID);
        StepVerifier.create(responseMono).expectComplete().verify();
    }

    @Test
    public void testDeleteAllPolicies() {
        Mono<String> policyIds = Mono.just(Arrays.asList(POLICY_1_ID, POLICY_2_ID).toString());
        when(asyncRestClientMock.get(POLICIES_IDENTITIES_URL)).thenReturn(policyIds);
        when(asyncRestClientMock.delete(anyString())).thenReturn(Mono.empty());

        Flux<String> responseFlux = clientUnderTest.deleteAllPolicies();
        StepVerifier.create(responseFlux).expectComplete().verify();
        verify(asyncRestClientMock).get(POLICIES_IDENTITIES_URL);
        verify(asyncRestClientMock).delete(POLICIES_URL + POLICY_1_ID);
        verify(asyncRestClientMock).delete(POLICIES_URL + POLICY_2_ID);
    }
}
