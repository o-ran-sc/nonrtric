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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Vector;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.Ric;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class StdA1ClientTest {
    private static final String RIC_URL = "RicUrl";
    private static final String POLICYTYPES_IDENTITIES_URL = "/policytypes/identities";
    private static final String POLICIES_IDENTITIES_URL = "/policies/identities";
    private static final String POLICYTYPES_URL = "/policytypes/";
    private static final String POLICIES_URL = "/policies/";

    private static final String POLICY_TYPE_1_NAME = "type1";
    private static final String POLICY_TYPE_2_NAME = "type2";
    private static final String POLICY_TYPE_SCHEMA_VALID = "{\"type\":\"type1\"}";
    private static final String POLICY_TYPE_SCHEMA_INVALID = "\"type\":\"type1\"}";
    private static final String POLICY_1_ID = "policy1";
    private static final String POLICY_2_ID = "policy2";
    private static final String POLICY_JSON_VALID = "{\"policyId\":\"policy1\"}";
    private static final String POLICY_JSON_INVALID = "\"policyId\":\"policy1\"}";
    private static final String POLICY_TYPE = "typeName";

    StdA1Client a1Client;

    AsyncRestClient asyncRestClientMock;

    @BeforeEach
    public void init() {
        asyncRestClientMock = mock(AsyncRestClient.class);
        a1Client = spy(new StdA1Client(createRic(RIC_URL).getConfig(), asyncRestClientMock));
    }

    @Test
    public void testGetPolicyTypeIdentities() {
        Mono<String> policyTypeIds = Mono.just(Arrays.toString(new String[] {POLICY_TYPE_1_NAME, POLICY_TYPE_2_NAME}));
        when(asyncRestClientMock.get(POLICYTYPES_IDENTITIES_URL)).thenReturn(policyTypeIds);

        Mono<?> policyTypeIdsFlux = a1Client.getPolicyTypeIdentities();
        verify(asyncRestClientMock).get(POLICYTYPES_IDENTITIES_URL);
        StepVerifier.create(policyTypeIdsFlux).expectNextCount(1).expectComplete().verify();
    }

    @Test
    public void testGetPolicyIdentities() {
        Mono<String> policyIds = Mono.just(Arrays.toString(new String[] {POLICY_1_ID, POLICY_2_ID}));
        when(asyncRestClientMock.get(POLICIES_IDENTITIES_URL)).thenReturn(policyIds);

        Mono<?> policyIdsFlux = a1Client.getPolicyIdentities();
        verify(asyncRestClientMock).get(POLICIES_IDENTITIES_URL);
        StepVerifier.create(policyIdsFlux).expectNextCount(1).expectComplete().verify();
    }

    @Test
    public void testGetValidPolicyType() {
        when(asyncRestClientMock.get(POLICYTYPES_URL + POLICY_TYPE_1_NAME))
            .thenReturn(Mono.just(POLICY_TYPE_SCHEMA_VALID));

        Mono<String> policyTypeMono = a1Client.getPolicyTypeSchema(POLICY_TYPE_1_NAME);
        verify(asyncRestClientMock).get(POLICYTYPES_URL + POLICY_TYPE_1_NAME);
        StepVerifier.create(policyTypeMono).expectNext(POLICY_TYPE_SCHEMA_VALID).expectComplete().verify();
    }

    @Test
    public void testGetInvalidPolicyType() {
        when(asyncRestClientMock.get(POLICYTYPES_URL + POLICY_TYPE_1_NAME))
            .thenReturn(Mono.just(POLICY_TYPE_SCHEMA_INVALID));

        Mono<String> policyTypeMono = a1Client.getPolicyTypeSchema(POLICY_TYPE_1_NAME);
        verify(asyncRestClientMock).get(POLICYTYPES_URL + POLICY_TYPE_1_NAME);
        StepVerifier.create(policyTypeMono).expectErrorMatches(throwable -> throwable instanceof JSONException)
            .verify();
    }

    @Test
    public void testPutPolicyValidResponse() {
        when(asyncRestClientMock.put(anyString(), anyString())).thenReturn(Mono.just(POLICY_JSON_VALID));

        Mono<String> policyMono =
            a1Client.putPolicy(createPolicy(RIC_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE));
        verify(asyncRestClientMock).put(POLICIES_URL + POLICY_1_ID, POLICY_JSON_VALID);
        StepVerifier.create(policyMono).expectNext(POLICY_JSON_VALID).expectComplete().verify();
    }

    @Test
    public void testPutPolicyInvalidResponse() {
        when(asyncRestClientMock.put(anyString(), anyString())).thenReturn(Mono.just(POLICY_JSON_INVALID));

        Mono<String> policyMono =
            a1Client.putPolicy(createPolicy(RIC_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE));
        StepVerifier.create(policyMono).expectErrorMatches(throwable -> throwable instanceof JSONException).verify();
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

    @Test
    public void testDeletePolicy() {
        when(asyncRestClientMock.delete(POLICIES_URL + POLICY_1_ID)).thenReturn(Mono.empty());

        Policy policy = createPolicy(RIC_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE);
        Mono<?> responseMono = a1Client.deletePolicy(policy);
        verify(asyncRestClientMock).delete(POLICIES_URL + POLICY_1_ID);
        StepVerifier.create(responseMono).expectComplete().verify();
    }
}
