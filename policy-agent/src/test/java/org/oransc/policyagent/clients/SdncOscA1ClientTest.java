/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2020 Nordix Foundation
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class SdncOscA1ClientTest {
    private static final String CONTROLLER_USERNAME = "username";
    private static final String CONTROLLER_PASSWORD = "password";
    private static final String RIC_1_URL = "RicUrl";
    private static final String POLICY_IDENTITIES_URL = "/A1-ADAPTER-API:getPolicyIdentities";
    private static final String PUT_POLICY_URL = "/A1-ADAPTER-API:putPolicy";
    private static final String DELETE_POLICY_URL = "/A1-ADAPTER-API:deletePolicy";
    private static final String POLICY_TYPE_1_ID = "type1";
    private static final String POLICY_1_ID = "policy1";
    private static final String POLICY_2_ID = "policy2";
    private static final String POLICY_JSON_VALID = "{\"scope\":{\"ueId\":\"ue1\"}}";
    private static final String POLICY_JSON_INVALID = "\"scope\":{\"ueId\":\"ue1\"}}";

    SdncOscA1Client clientUnderTest;

    AsyncRestClient asyncRestClientMock;

    @BeforeEach
    public void init() {
        asyncRestClientMock = mock(AsyncRestClient.class);
        clientUnderTest = new SdncOscA1Client(A1ClientHelper.createRic(RIC_1_URL).getConfig(), CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD, asyncRestClientMock);
    }

    @Test
    public void testGetPolicyTypeIdentities() {
        List<String> policyTypeIds = clientUnderTest.getPolicyTypeIdentities().block();
        assertEquals(1, policyTypeIds.size(), "should hardcoded to one");
        assertEquals("", policyTypeIds.get(0), "should hardcoded to empty");
    }

    @Test
    public void testGetPolicyIdentities() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .build();
        String inputJsonString = A1ClientHelper.createInputJsonString(inputParams);

        List<String> policyIds = Arrays.asList(POLICY_1_ID, POLICY_2_ID);
        Mono<String> policyIdsResp = A1ClientHelper.createOutputJsonResponse("policy-id-list", policyIds.toString());
        whenAsyncPostThenReturn(policyIdsResp);

        Mono<List<String>> returnedMono = clientUnderTest.getPolicyIdentities();

        verify(asyncRestClientMock).postWithAuthHeader(POLICY_IDENTITIES_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectNext(policyIds).expectComplete().verify();
    }

    @Test
    public void testGetValidPolicyType() {
        String policyType = clientUnderTest.getPolicyTypeSchema("").block();
        assertEquals("{}", policyType, "");
    }

    @Test
    public void testPutPolicyValidResponse() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .policyId(POLICY_1_ID) //
            .policy(POLICY_JSON_VALID) //
            .build();
        String inputJsonString = A1ClientHelper.createInputJsonString(inputParams);

        Mono<String> policyResp = A1ClientHelper.createOutputJsonResponse("returned-policy", POLICY_JSON_VALID);
        whenAsyncPostThenReturn(policyResp);

        Mono<String> returnedMono = clientUnderTest
            .putPolicy(A1ClientHelper.createPolicy(RIC_1_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID));

        verify(asyncRestClientMock).postWithAuthHeader(PUT_POLICY_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectNext(POLICY_JSON_VALID).expectComplete().verify();
    }

    @Test
    public void testPutPolicyInvalidResponse() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .policyId(POLICY_1_ID) //
            .policy(POLICY_JSON_VALID) //
            .build();
        String inputJsonString = A1ClientHelper.createInputJsonString(inputParams);

        Mono<String> policyResp = A1ClientHelper.createOutputJsonResponse("returned-policy", POLICY_JSON_INVALID);
        whenAsyncPostThenReturn(policyResp);

        Mono<String> returnedMono = clientUnderTest
            .putPolicy(A1ClientHelper.createPolicy(RIC_1_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID));

        verify(asyncRestClientMock).postWithAuthHeader(PUT_POLICY_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectErrorMatches(throwable -> throwable instanceof JSONException).verify();
    }

    @Test
    public void testDeletePolicy() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .policyId(POLICY_1_ID) //
            .build();
        String inputJsonString = A1ClientHelper.createInputJsonString(inputParams);

        whenAsyncPostThenReturn(Mono.empty());

        Mono<String> returnedMono = clientUnderTest
            .deletePolicy(A1ClientHelper.createPolicy(RIC_1_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID));

        verify(asyncRestClientMock).postWithAuthHeader(DELETE_POLICY_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectComplete().verify();
    }

    @Test
    public void testDeleteAllPolicies() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .build();
        String inputJsonStringGetIds = A1ClientHelper.createInputJsonString(inputParams);
        inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .policyId(POLICY_1_ID) //
            .build();
        String inputJsonStringDeletePolicy1 = A1ClientHelper.createInputJsonString(inputParams);
        inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .policyId(POLICY_2_ID) //
            .build();
        String inputJsonStringDeletePolicy2 = A1ClientHelper.createInputJsonString(inputParams);

        List<String> policyIds = Arrays.asList(POLICY_1_ID, POLICY_2_ID);
        Mono<String> policyIdsResp = A1ClientHelper.createOutputJsonResponse("policy-id-list", policyIds.toString());
        whenAsyncPostThenReturn(policyIdsResp).thenReturn(Mono.empty());

        Flux<String> returnedFlux = clientUnderTest.deleteAllPolicies();

        StepVerifier.create(returnedFlux).expectComplete().verify();
        verify(asyncRestClientMock).postWithAuthHeader(POLICY_IDENTITIES_URL, inputJsonStringGetIds,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
        verify(asyncRestClientMock).postWithAuthHeader(DELETE_POLICY_URL, inputJsonStringDeletePolicy1,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
        verify(asyncRestClientMock).postWithAuthHeader(DELETE_POLICY_URL, inputJsonStringDeletePolicy2,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
    }

    private OngoingStubbing<Mono<String>> whenAsyncPostThenReturn(Mono<String> response) {
        return when(asyncRestClientMock.postWithAuthHeader(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(response);
    }
}
