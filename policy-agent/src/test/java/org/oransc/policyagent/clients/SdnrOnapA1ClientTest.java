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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class SdnrOnapA1ClientTest {
    private static final String CONTROLLER_USERNAME = "username";
    private static final String CONTROLLER_PASSWORD = "password";
    private static final String RIC_1_URL = "RicUrl";
    private static final String POLICYTYPES_IDENTITIES_URL = "/A1-ADAPTER-API:getPolicyTypes";
    private static final String POLICIES_IDENTITIES_URL = "/A1-ADAPTER-API:getPolicyInstances";
    private static final String POLICYTYPES_URL = "/A1-ADAPTER-API:getPolicyType";
    private static final String PUT_POLICY_URL = "/A1-ADAPTER-API:createPolicyInstance";
    private static final String DELETE_POLICY_URL = "/A1-ADAPTER-API:deletePolicyInstance";

    private static final String POLICY_TYPE_1_ID = "type1";
    private static final String POLICY_TYPE_2_ID = "type2";
    private static final String POLICY_TYPE_SCHEMA_VALID = "{\"type\":\"type1\"}";
    private static final String POLICY_TYPE_SCHEMA_INVALID = "\"type\":\"type1\"}";
    private static final String POLICY_1_ID = "policy1";
    private static final String POLICY_2_ID = "policy2";
    private static final String POLICY_JSON_VALID = "{\"scope\":{\"ueId\":\"ue1\"}}";

    SdnrOnapA1Client clientUnderTest;

    AsyncRestClient asyncRestClientMock;

    private static Gson gson = new GsonBuilder() //
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES) //
        .create(); //

    @BeforeEach
    public void init() {
        asyncRestClientMock = mock(AsyncRestClient.class);
        clientUnderTest = new SdnrOnapA1Client(A1ClientHelper.createRic(RIC_1_URL).getConfig(), CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD, asyncRestClientMock);
    }

    @Test
    public void testGetPolicyTypeIdentities() {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .build();
        String inputJsonString = createInputJsonString(inputParams);

        List<String> policyTypeIds = Arrays.asList(POLICY_TYPE_1_ID, POLICY_TYPE_2_ID);
        Mono<String> policyTypeIdsResp =
            A1ClientHelper.createOutputJsonResponse("policy-type-id-list", policyTypeIds.toString());
        whenAsyncPostThenReturn(policyTypeIdsResp);

        Mono<List<String>> returnedMono = clientUnderTest.getPolicyTypeIdentities();
        verify(asyncRestClientMock).postWithAuthHeader(POLICYTYPES_IDENTITIES_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectNext(policyTypeIds).expectComplete().verify();
    }

    @Test
    public void testGetPolicyIdentities() {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .build();
        String inputJsonStringGetTypeIds = createInputJsonString(inputParams);
        inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .build();
        String inputJsonStringGetPolicyIdsType1 = createInputJsonString(inputParams);
        inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_2_ID) //
            .build();
        String inputJsonStringGetPolicyIdsType2 = createInputJsonString(inputParams);

        List<String> policyTypeIds = Arrays.asList(POLICY_TYPE_1_ID, POLICY_TYPE_2_ID);
        Mono<String> policyTypeIdsResp =
            A1ClientHelper.createOutputJsonResponse("policy-type-id-list", policyTypeIds.toString());
        List<String> policyIdsType1 = Arrays.asList(POLICY_1_ID);
        Mono<String> policyIdsType1Resp =
            A1ClientHelper.createOutputJsonResponse("policy-instance-id-list", policyIdsType1.toString());
        List<String> policyIdsType2 = Arrays.asList(POLICY_2_ID);
        Mono<String> policyIdsType2Resp =
            A1ClientHelper.createOutputJsonResponse("policy-instance-id-list", policyIdsType2.toString());
        whenAsyncPostThenReturn(policyTypeIdsResp).thenReturn(policyIdsType1Resp).thenReturn(policyIdsType2Resp);

        Mono<List<String>> returnedMono = clientUnderTest.getPolicyIdentities();
        StepVerifier.create(returnedMono).expectNext(Arrays.asList(POLICY_1_ID, POLICY_2_ID)).expectComplete().verify();
        verify(asyncRestClientMock).postWithAuthHeader(POLICYTYPES_IDENTITIES_URL, inputJsonStringGetTypeIds,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
        verify(asyncRestClientMock).postWithAuthHeader(POLICIES_IDENTITIES_URL, inputJsonStringGetPolicyIdsType1,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
        verify(asyncRestClientMock).postWithAuthHeader(POLICIES_IDENTITIES_URL, inputJsonStringGetPolicyIdsType2,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
    }

    @Test
    public void testGetValidPolicyType() {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .build();
        String inputJsonString = createInputJsonString(inputParams);

        String policyType = "{\"policySchema\": " + POLICY_TYPE_SCHEMA_VALID + ", \"statusSchema\": {} }";
        Mono<String> policyTypeResp = A1ClientHelper.createOutputJsonResponse("policy-type", policyType);
        whenAsyncPostThenReturn(policyTypeResp);

        Mono<String> returnedMono = clientUnderTest.getPolicyTypeSchema(POLICY_TYPE_1_ID);
        verify(asyncRestClientMock).postWithAuthHeader(POLICYTYPES_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectNext(POLICY_TYPE_SCHEMA_VALID).expectComplete().verify();
    }

    @Test
    public void testGetInvalidPolicyType() {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .build();
        String inputJsonString = createInputJsonString(inputParams);

        String policyType = "{\"policySchema\": " + POLICY_TYPE_SCHEMA_INVALID + ", \"statusSchema\": {} }";
        Mono<String> policyTypeResp = A1ClientHelper.createOutputJsonResponse("policy-type", policyType);
        whenAsyncPostThenReturn(policyTypeResp);

        Mono<String> returnedMono = clientUnderTest.getPolicyTypeSchema(POLICY_TYPE_1_ID);
        verify(asyncRestClientMock).postWithAuthHeader(POLICYTYPES_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectErrorMatches(throwable -> throwable instanceof JSONException).verify();
    }

    @Test
    public void testPutPolicy() {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .policyInstanceId(POLICY_1_ID) //
            .policyInstance(POLICY_JSON_VALID) //
            .properties(new ArrayList<String>()) //
            .build();
        String inputJsonString = createInputJsonString(inputParams);

        whenAsyncPostThenReturn(Mono.empty());

        Mono<String> returnedMono = clientUnderTest
            .putPolicy(A1ClientHelper.createPolicy(RIC_1_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID));
        verify(asyncRestClientMock).postWithAuthHeader(PUT_POLICY_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectComplete().verify();
    }

    @Test
    public void testDeletePolicy() {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .policyInstanceId(POLICY_1_ID) //
            .build();
        String inputJsonString = createInputJsonString(inputParams);

        whenAsyncPostThenReturn(Mono.empty());

        Mono<String> returnedMono = clientUnderTest
            .deletePolicy(A1ClientHelper.createPolicy(RIC_1_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID));
        verify(asyncRestClientMock).postWithAuthHeader(DELETE_POLICY_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectComplete().verify();
    }

    @Test
    public void testDeleteAllPolicies() {
        SdnrOnapAdapterInput inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .build();
        String inputJsonStringGetTypeIds = createInputJsonString(inputParams);
        inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .build();
        String inputJsonStringGetPolicyIdsType1 = createInputJsonString(inputParams);
        inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_2_ID) //
            .build();
        String inputJsonStringGetPolicyIdsType2 = createInputJsonString(inputParams);
        inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .policyInstanceId(POLICY_1_ID) //
            .build();
        String inputJsonStringDeletePolicy1 = createInputJsonString(inputParams);
        inputParams = ImmutableSdnrOnapAdapterInput.builder() //
            .nearRtRicId(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_2_ID) //
            .policyInstanceId(POLICY_2_ID) //
            .build();
        String inputJsonStringDeletePolicy2 = createInputJsonString(inputParams);

        List<String> policyTypeIds = Arrays.asList(POLICY_TYPE_1_ID, POLICY_TYPE_2_ID);
        Mono<String> policyTypeIdsResp =
            A1ClientHelper.createOutputJsonResponse("policy-type-id-list", policyTypeIds.toString());
        List<String> policyIdsType1 = Arrays.asList(POLICY_1_ID);
        Mono<String> policyIdsType1Resp =
            A1ClientHelper.createOutputJsonResponse("policy-instance-id-list", policyIdsType1.toString());
        List<String> policyIdsType2 = Arrays.asList(POLICY_2_ID);
        Mono<String> policyIdsType2Resp =
            A1ClientHelper.createOutputJsonResponse("policy-instance-id-list", policyIdsType2.toString());
        whenAsyncPostThenReturn(policyTypeIdsResp).thenReturn(policyIdsType1Resp).thenReturn(Mono.empty())
            .thenReturn(policyIdsType2Resp).thenReturn(Mono.empty());

        Flux<String> returnedFlux = clientUnderTest.deleteAllPolicies();
        StepVerifier.create(returnedFlux).expectComplete().verify();
        verify(asyncRestClientMock).postWithAuthHeader(POLICYTYPES_IDENTITIES_URL, inputJsonStringGetTypeIds,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
        verify(asyncRestClientMock).postWithAuthHeader(POLICIES_IDENTITIES_URL, inputJsonStringGetPolicyIdsType1,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
        verify(asyncRestClientMock).postWithAuthHeader(DELETE_POLICY_URL, inputJsonStringDeletePolicy1,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
        verify(asyncRestClientMock).postWithAuthHeader(POLICIES_IDENTITIES_URL, inputJsonStringGetPolicyIdsType2,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
        verify(asyncRestClientMock).postWithAuthHeader(DELETE_POLICY_URL, inputJsonStringDeletePolicy2,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
    }

    private OngoingStubbing<Mono<String>> whenAsyncPostThenReturn(Mono<String> response) {
        return when(asyncRestClientMock.postWithAuthHeader(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(response);
    }

    private String createInputJsonString(SdnrOnapAdapterInput inputParams) {
        JSONObject inputJson = new JSONObject();
        inputJson.put("input", new JSONObject(gson.toJson(inputParams)));
        return inputJson.toString();
    }
}
