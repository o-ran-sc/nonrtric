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
public class OscA1ClientTest {
    private static final String RIC_URL = "RicUrl";
    private static final String POLICYTYPES_IDENTITIES_URL = "/policytypes";
    private static final String POLICIES_IDENTITIES_URL = "/policies";
    private static final String POLICYTYPES_URL = "/policytypes/";
    private static final String POLICIES_URL = "/policies/";

    private static final String POLICY_TYPE_1_ID = "type1";
    private static final String POLICY_TYPE_2_ID = "type2";
    private static final String POLICY_TYPE_SCHEMA_VALID = "{\"type\":\"type1\"}";
    private static final String POLICY_TYPE_SCHEMA_INVALID = "\"type\":\"type1\"}";
    private static final String POLICY_1_ID = "policy1";
    private static final String POLICY_2_ID = "policy2";
    private static final String POLICY_JSON_VALID = "{\"policyId\":\"policy1\"}";

    OscA1Client a1Client;

    AsyncRestClient asyncRestClientMock;

    @BeforeEach
    public void init() {
        asyncRestClientMock = mock(AsyncRestClient.class);
        a1Client = spy(new OscA1Client(asyncRestClientMock));
    }

    @Test
    public void testGetPolicyTypeIdentities() {
        List<String> policyTypeIds = Arrays.asList(new String[] {POLICY_TYPE_1_ID, POLICY_TYPE_2_ID});
        Mono<String> policyTypeIdsResp = Mono.just(policyTypeIds.toString());
        when(asyncRestClientMock.get(anyString())).thenReturn(policyTypeIdsResp);

        Mono<List<String>> returnedMono = a1Client.getPolicyTypeIdentities();
        verify(asyncRestClientMock).get(POLICYTYPES_IDENTITIES_URL);
        StepVerifier.create(returnedMono).expectNext(policyTypeIds).expectComplete().verify();
    }

    @Test
    public void testGetPolicyIdentities() {
        Mono<String> policyTypeIdsResp = Mono.just(Arrays.toString(new String[] {POLICY_TYPE_1_ID, POLICY_TYPE_2_ID}));
        Mono<String> policyIdsType1Resp = Mono.just(Arrays.toString(new String[] {POLICY_1_ID}));
        Mono<String> policyIdsType2Resp = Mono.just(Arrays.toString(new String[] {POLICY_2_ID}));
        when(asyncRestClientMock.get(anyString())).thenReturn(policyTypeIdsResp).thenReturn(policyIdsType1Resp)
            .thenReturn(policyIdsType2Resp);

        Mono<List<String>> returnedMono = a1Client.getPolicyIdentities();
        StepVerifier.create(returnedMono).expectNext(Arrays.asList(new String[] {POLICY_1_ID, POLICY_2_ID}))
            .expectComplete().verify();
        verify(asyncRestClientMock).get(POLICYTYPES_IDENTITIES_URL);
        verify(asyncRestClientMock).get(POLICYTYPES_URL + POLICY_TYPE_1_ID + POLICIES_IDENTITIES_URL);
        verify(asyncRestClientMock).get(POLICYTYPES_URL + POLICY_TYPE_2_ID + POLICIES_IDENTITIES_URL);
    }

    @Test
    public void testGetValidPolicyType() {
        String policyType = "{\"create_schema\": " + POLICY_TYPE_SCHEMA_VALID + "}";
        Mono<String> policyTypeResp = Mono.just(policyType);

        when(asyncRestClientMock.get(anyString())).thenReturn(policyTypeResp);

        Mono<String> returnedMono = a1Client.getPolicyTypeSchema(POLICY_TYPE_1_ID);
        verify(asyncRestClientMock).get(POLICYTYPES_URL + POLICY_TYPE_1_ID);
        StepVerifier.create(returnedMono).expectNext(getCreateSchema(policyType, POLICY_TYPE_1_ID)).expectComplete()
            .verify();
    }

    @Test
    public void testGetInValidPolicyTypeJson() {
        String policyType = "{\"create_schema\": " + POLICY_TYPE_SCHEMA_INVALID + "}";
        Mono<String> policyTypeResp = Mono.just(policyType);

        when(asyncRestClientMock.get(anyString())).thenReturn(policyTypeResp);

        Mono<String> returnedMono = a1Client.getPolicyTypeSchema(POLICY_TYPE_1_ID);
        verify(asyncRestClientMock).get(POLICYTYPES_URL + POLICY_TYPE_1_ID);
        StepVerifier.create(returnedMono).expectErrorMatches(throwable -> throwable instanceof JSONException).verify();
    }

    @Test
    public void testGetPolicyTypeWithoutCreateSchema() {
        Mono<String> policyTypeResp = Mono.just(POLICY_TYPE_SCHEMA_VALID);

        when(asyncRestClientMock.get(anyString())).thenReturn(policyTypeResp);

        Mono<String> returnedMono = a1Client.getPolicyTypeSchema(POLICY_TYPE_1_ID);
        verify(asyncRestClientMock).get(POLICYTYPES_URL + POLICY_TYPE_1_ID);
        StepVerifier.create(returnedMono).expectErrorMatches(throwable -> throwable instanceof Exception).verify();
    }

    @Test
    public void testPutPolicy() {
        when(asyncRestClientMock.put(anyString(), anyString())).thenReturn(Mono.empty());

        Mono<String> returnedMono =
            a1Client.putPolicy(createPolicy(RIC_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID));
        verify(asyncRestClientMock).put(POLICYTYPES_URL + POLICY_TYPE_1_ID + POLICIES_URL + POLICY_1_ID,
            POLICY_JSON_VALID);
        StepVerifier.create(returnedMono).expectComplete().verify();
    }

    @Test
    public void testDeletePolicy() {
        when(asyncRestClientMock.delete(anyString())).thenReturn(Mono.empty());

        Mono<String> returnedMono =
            a1Client.deletePolicy(createPolicy(RIC_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID));
        verify(asyncRestClientMock).delete(POLICYTYPES_URL + POLICY_TYPE_1_ID + POLICIES_URL + POLICY_1_ID);
        StepVerifier.create(returnedMono).expectComplete().verify();
    }

    @Test
    public void testDeleteAllPolicies() {
        Mono<String> policyTypeIdsResp = Mono.just(Arrays.toString(new String[] {POLICY_TYPE_1_ID, POLICY_TYPE_2_ID}));
        Mono<String> policyIdsType1Resp = Mono.just(Arrays.toString(new String[] {POLICY_1_ID}));
        Mono<String> policyIdsType2Resp = Mono.just(Arrays.toString(new String[] {POLICY_2_ID}));
        when(asyncRestClientMock.get(anyString())).thenReturn(policyTypeIdsResp).thenReturn(policyIdsType1Resp)
            .thenReturn(policyIdsType2Resp);
        when(asyncRestClientMock.delete(anyString())).thenReturn(Mono.empty());

        Flux<String> returnedFlux = a1Client.deleteAllPolicies();
        StepVerifier.create(returnedFlux).expectComplete().verify();
        verify(asyncRestClientMock).get(POLICYTYPES_IDENTITIES_URL);
        verify(asyncRestClientMock).get(POLICYTYPES_URL + POLICY_TYPE_1_ID + POLICIES_IDENTITIES_URL);
        verify(asyncRestClientMock).delete(POLICYTYPES_URL + POLICY_TYPE_1_ID + POLICIES_URL + POLICY_1_ID);
        verify(asyncRestClientMock).get(POLICYTYPES_URL + POLICY_TYPE_2_ID + POLICIES_IDENTITIES_URL);
        verify(asyncRestClientMock).delete(POLICYTYPES_URL + POLICY_TYPE_2_ID + POLICIES_URL + POLICY_2_ID);
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

    private Ric createRic(String url) {
        RicConfig cfg = ImmutableRicConfig.builder().name("ric") //
            .baseUrl(url) //
            .managedElementIds(new Vector<String>(Arrays.asList("kista_1", "kista_2"))) //
            .build();
        return new Ric(cfg);
    }

    private String getCreateSchema(String policyType, String policyTypeId) {
        JSONObject obj = new JSONObject(policyType);
        JSONObject schemaObj = obj.getJSONObject("create_schema");
        schemaObj.put("title", policyTypeId);
        return schemaObj.toString();
    }
}
