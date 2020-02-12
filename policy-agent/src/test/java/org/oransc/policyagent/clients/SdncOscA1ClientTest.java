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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.Ric;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class SdncOscA1ClientTest {
    private static final String CONTROLLER_USERNAME = "username";
    private static final String CONTROLLER_PASSWORD = "password";
    private static final String RIC_1_URL = "RicUrl";
    private static final String POLICYTYPES_IDENTITIES_URL = "/A1-ADAPTER-API:getPolicyTypeIdentities";
    private static final String POLICIES_IDENTITIES_URL = "/A1-ADAPTER-API:getPolicyIdentities";
    private static final String POLICYTYPES_URL = "/A1-ADAPTER-API:getPolicyType";
    private static final String PUT_POLICY_URL = "/A1-ADAPTER-API:putPolicy";
    private static final String DELETE_POLICY_URL = "/A1-ADAPTER-API:deletePolicy";

    private static final String POLICY_TYPE_1_ID = "type1";
    private static final String POLICY_TYPE_2_ID = "type2";
    private static final String POLICY_TYPE_SCHEMA_VALID = "{\"type\":\"type1\"}";
    private static final String POLICY_TYPE_SCHEMA_INVALID = "\"type\":\"type1\"}";
    private static final String POLICY_1_ID = "policy1";
    private static final String POLICY_2_ID = "policy2";
    private static final String POLICY_JSON_VALID = "{\"scope\":{\"ueId\":\"ue1\"}}";
    private static final String POLICY_JSON_INVALID = "\"scope\":{\"ueId\":\"ue1\"}}";

    SdncOscA1Client a1Client;

    AsyncRestClient asyncRestClientMock;

    private static Gson gson = new GsonBuilder() //
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES) //
        .create(); //

    @BeforeEach
    public void init() {
        asyncRestClientMock = mock(AsyncRestClient.class);
        a1Client = spy(new SdncOscA1Client(createRic(RIC_1_URL).getConfig(), CONTROLLER_USERNAME, CONTROLLER_PASSWORD,
            asyncRestClientMock));
    }

    @Test
    public void testGetPolicyTypeIdentities() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .build();
        String inputJsonString = createInputJsonString(gson.toJson(inputParams));

        List<String> policyTypeIds = Arrays.asList(new String[] {POLICY_TYPE_1_ID, POLICY_TYPE_2_ID});
        Mono<String> policyTypeIdsResp =
            Mono.just(createOutputJsonString("policy-type-id-list", policyTypeIds.toString()));
        when(asyncRestClientMock.postWithAuthHeader(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(policyTypeIdsResp);

        Mono<List<String>> returnedMono = a1Client.getPolicyTypeIdentities();
        verify(asyncRestClientMock).postWithAuthHeader(POLICYTYPES_IDENTITIES_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectNext(policyTypeIds).expectComplete().verify();
    }

    @Test
    public void testGetPolicyIdentities() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .build();
        String inputJsonString = createInputJsonString(gson.toJson(inputParams));

        List<String> policyIds = Arrays.asList(new String[] {POLICY_1_ID, POLICY_2_ID});
        Mono<String> policyIdsResp = Mono.just(createOutputJsonString("policy-id-list", policyIds.toString()));
        when(asyncRestClientMock.postWithAuthHeader(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(policyIdsResp);

        Mono<List<String>> returnedMono = a1Client.getPolicyIdentities();
        verify(asyncRestClientMock).postWithAuthHeader(POLICIES_IDENTITIES_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectNext(policyIds).expectComplete().verify();
    }

    @Test
    public void testGetValidPolicyType() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .build();
        String inputJsonString = createInputJsonString(gson.toJson(inputParams));

        String policyType = "{\"policySchema\": " + POLICY_TYPE_SCHEMA_VALID + ", \"statusSchema\": {} }";
        Mono<String> policyTypeResp = Mono.just(createOutputJsonString("policy-type", policyType));
        when(asyncRestClientMock.postWithAuthHeader(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(policyTypeResp);

        Mono<String> returnedMono = a1Client.getPolicyTypeSchema(POLICY_TYPE_1_ID);
        verify(asyncRestClientMock).postWithAuthHeader(POLICYTYPES_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectNext(POLICY_TYPE_SCHEMA_VALID).expectComplete().verify();
    }

    @Test
    public void testGetInvalidPolicyType() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .build();
        String inputJsonString = createInputJsonString(gson.toJson(inputParams));

        String policyType = "{\"policySchema\": " + POLICY_TYPE_SCHEMA_INVALID + ", \"statusSchema\": {} }";
        Mono<String> policyTypeResp = Mono.just(createOutputJsonString("policy-type", policyType));
        when(asyncRestClientMock.postWithAuthHeader(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(policyTypeResp);

        Mono<String> returnedMono = a1Client.getPolicyTypeSchema(POLICY_TYPE_1_ID);
        verify(asyncRestClientMock).postWithAuthHeader(POLICYTYPES_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectErrorMatches(throwable -> throwable instanceof JSONException).verify();
    }

    @Test
    public void testPutPolicyValidResponse() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .policyTypeId(POLICY_TYPE_1_ID) //
            .policyId(POLICY_1_ID) //
            .policy(POLICY_JSON_VALID) //
            .build();
        String inputJsonString = createInputJsonString(gson.toJson(inputParams));

        Mono<String> policyResp = Mono.just(createOutputJsonString("returned-policy", POLICY_JSON_VALID));
        when(asyncRestClientMock.postWithAuthHeader(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(policyResp);

        Mono<String> returnedMono =
            a1Client.putPolicy(createPolicy(RIC_1_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID));
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
        String inputJsonString = createInputJsonString(gson.toJson(inputParams));

        Mono<String> policyResp = Mono.just(createOutputJsonString("returned-policy", POLICY_JSON_INVALID));
        when(asyncRestClientMock.postWithAuthHeader(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(policyResp);

        Mono<String> returnedMono =
            a1Client.putPolicy(createPolicy(RIC_1_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID));
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
        String inputJsonString = createInputJsonString(gson.toJson(inputParams));

        when(asyncRestClientMock.postWithAuthHeader(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Mono.empty());

        Mono<String> returnedMono =
            a1Client.deletePolicy(createPolicy(RIC_1_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID));
        verify(asyncRestClientMock).postWithAuthHeader(DELETE_POLICY_URL, inputJsonString, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono).expectComplete().verify();
    }

    @Test
    public void testDeleteAllPolicies() {
        SdncOscAdapterInput inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .build();
        String inputJsonStringGetIds = createInputJsonString(gson.toJson(inputParams));
        inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .policyId(POLICY_1_ID) //
            .build();
        String inputJsonStringDeletePolicy1 = createInputJsonString(gson.toJson(inputParams));
        inputParams = ImmutableSdncOscAdapterInput.builder() //
            .nearRtRicUrl(RIC_1_URL) //
            .policyId(POLICY_2_ID) //
            .build();
        String inputJsonStringDeletePolicy2 = createInputJsonString(gson.toJson(inputParams));

        List<String> policyIds = Arrays.asList(new String[] {POLICY_1_ID, POLICY_2_ID});
        Mono<String> policyIdsResp = Mono.just(createOutputJsonString("policy-id-list", policyIds.toString()));
        when(asyncRestClientMock.postWithAuthHeader(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(policyIdsResp).thenReturn(Mono.empty());

        Flux<String> returnedFlux = a1Client.deleteAllPolicies();
        StepVerifier.create(returnedFlux).expectComplete().verify();
        verify(asyncRestClientMock).postWithAuthHeader(POLICIES_IDENTITIES_URL, inputJsonStringGetIds,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
        verify(asyncRestClientMock).postWithAuthHeader(DELETE_POLICY_URL, inputJsonStringDeletePolicy1,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
        verify(asyncRestClientMock).postWithAuthHeader(DELETE_POLICY_URL, inputJsonStringDeletePolicy2,
            CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
    }

    private String createInputJsonString(String paramsJson) {
        JSONObject inputJson = new JSONObject();
        inputJson.put("input", new JSONObject(paramsJson));
        return inputJson.toString();
    }

    private String createOutputJsonString(String key, String value) {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put(key, value);
        JSONObject responseJson = new JSONObject();
        responseJson.put("output", paramsJson);
        return responseJson.toString();
    }

    private Ric createRic(String url) {
        RicConfig cfg = ImmutableRicConfig.builder().name("ric") //
            .baseUrl(url) //
            .managedElementIds(new Vector<String>(Arrays.asList("kista_1", "kista_2"))) //
            .build();
        return new Ric(cfg);
    }

    private Policy createPolicy(String nearRtRicUrl, String policyId, String json, String type) {
        return ImmutablePolicy.builder() //
            .id(policyId) //
            .json(json) //
            .ownerServiceName("service") //
            .ric(createRic(nearRtRicUrl)) //
            .type(createPolicyType(type)) //
            .lastModified("now") //
            .build();
    }

    private PolicyType createPolicyType(String name) {
        return ImmutablePolicyType.builder().name(name).schema("schema").build();
    }

}
