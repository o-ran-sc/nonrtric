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
package org.oransc.ric.portal.dashboard.policyagentapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oransc.ric.portal.dashboard.model.ImmutablePolicyInfo;
import org.oransc.ric.portal.dashboard.model.PolicyInfo;
import org.oransc.ric.portal.dashboard.model.PolicyInstances;
import org.oransc.ric.portal.dashboard.model.PolicyType;
import org.oransc.ric.portal.dashboard.model.PolicyTypes;
import org.oransc.ric.portal.dashboard.policyagentapi.PolicyAgentApiImpl.RicInfo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class PolicyAgentApiImplTest {
    private static final String URL_PREFIX = "UrlPrefix";
    private static final String URL_POLICY_SCHEMAS = "/policy_schemas";
    private static final String URL_POLICY_INSTANCES = "/policies?type={type}";
    private static final String URL_POLICY_INSTANCE = "/policy?instance={id}";
    private static final String URL_PUT_POLICY = "/policy?type={type}&instance={instance}&ric={ric}&service={service}";
    private static final String URL_DELETE_POLICY = "/policy?instance={instance}";
    private static final String URL_RIC_INFO = "/rics?policyType={typeName}";
    private static final String POLICY_TYPE_1_ID = "type1";
    private static final String POLICY_TYPE_1_VALID = "{\"title\":\"type1\"}";
    private static final String POLICY_TYPE_1_INVALID = "\"title\":\"type1\"}";
    private static final String POLICY_TYPE_2_VALID = "{\"title\":\"type2\"}";
    private static final String POLICY_1_ID = "policy1";
    private static final String POLICY_1_VALID = "{\"policyId\":\"policy1\"}";
    private static final String POLICY_1_INVALID = "\"policyId\":\"policy1\"}";
    private static final String RIC_1_ID = "ric1";
    private static final String RIC_1_INFO_VALID = "{\"name\":\"ric1\",\"policyTypes\":[\"type1\"]}";
    private static final String RIC_1_INFO_INVALID = "{\"name\":\"ric1\",\"policyTypes\":\"type1\"]}";
    private static final String CLIENT_ERROR_MESSAGE = "Exception: Client returned failure";

    private static com.google.gson.Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    PolicyAgentApiImpl apiUnderTest;

    RestTemplate restTemplateMock;

    @BeforeEach
    public void init() {
        restTemplateMock = mock(RestTemplate.class);
        apiUnderTest = new PolicyAgentApiImpl(URL_PREFIX, restTemplateMock);
    }

    @Test
    public void testGetAllPolicyTypesFailure() {
        ResponseEntity<String> getAllPolicyTypesResp = new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        when(restTemplateMock.getForEntity(eq(URL_PREFIX + URL_POLICY_SCHEMAS), eq(String.class)))
            .thenReturn(getAllPolicyTypesResp);

        ResponseEntity<String> returnedResp = apiUnderTest.getAllPolicyTypes();

        verify(restTemplateMock).getForEntity(URL_PREFIX + URL_POLICY_SCHEMAS, String.class);
        assertNull(returnedResp.getBody());
        assertEquals(HttpStatus.NOT_FOUND, returnedResp.getStatusCode());
    }

    @Test
    public void testGetAllPolicyTypesSuccessValidJson() {
        String policyTypes = Arrays.asList(POLICY_TYPE_1_VALID, POLICY_TYPE_2_VALID).toString();
        String policyTypesJson = parsePolicyTypesJson(policyTypes);
        ResponseEntity<String> getAllPolicyTypesResp = new ResponseEntity<>(policyTypes, HttpStatus.OK);
        when(restTemplateMock.getForEntity(eq(URL_PREFIX + URL_POLICY_SCHEMAS), eq(String.class)))
            .thenReturn(getAllPolicyTypesResp);

        ResponseEntity<String> returnedResp = apiUnderTest.getAllPolicyTypes();

        verify(restTemplateMock).getForEntity(URL_PREFIX + URL_POLICY_SCHEMAS, String.class);
        assertEquals(returnedResp.getBody(), policyTypesJson);
        assertEquals(HttpStatus.OK, returnedResp.getStatusCode());
    }

    @Test
    public void testGetAllPolicyTypesSuccessInvalidJson() {
        String policyTypes = Arrays.asList(POLICY_TYPE_1_INVALID, POLICY_TYPE_2_VALID).toString();
        ResponseEntity<String> getAllPolicyTypesResp = new ResponseEntity<>(policyTypes, HttpStatus.OK);
        when(restTemplateMock.getForEntity(eq(URL_PREFIX + URL_POLICY_SCHEMAS), eq(String.class)))
            .thenReturn(getAllPolicyTypesResp);

        ResponseEntity<String> returnedResp = apiUnderTest.getAllPolicyTypes();

        verify(restTemplateMock).getForEntity(URL_PREFIX + URL_POLICY_SCHEMAS, String.class);
        assertTrue(returnedResp.getBody().contains("Exception"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, returnedResp.getStatusCode());
    }

    @Test
    public void testGetPolicyInstancesForTypeFailure() {
        ResponseEntity<String> getPolicyInstancesForTypeResp = new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        Map<String, ?> uriVariables = Map.of("type", POLICY_TYPE_1_ID);
        when(restTemplateMock.getForEntity(eq(URL_PREFIX + URL_POLICY_INSTANCES), eq(String.class), eq(uriVariables)))
            .thenReturn(getPolicyInstancesForTypeResp);

        ResponseEntity<String> returnedResp = apiUnderTest.getPolicyInstancesForType(POLICY_TYPE_1_ID);

        verify(restTemplateMock).getForEntity(URL_PREFIX + URL_POLICY_INSTANCES, String.class, uriVariables);
        assertNull(returnedResp.getBody());
        assertEquals(HttpStatus.NOT_FOUND, returnedResp.getStatusCode());
    }

    @Test
    public void testGetPolicyInstancesForTypeSuccessValidJson() {
        String policyInstances = Arrays.asList(POLICY_1_VALID).toString();
        String policyInstancesJson = parsePolicyInstancesJson(policyInstances);
        ResponseEntity<String> getPolicyInstancesForTypeResp = new ResponseEntity<>(policyInstances, HttpStatus.OK);
        Map<String, ?> uriVariables = Map.of("type", POLICY_TYPE_1_ID);
        when(restTemplateMock.getForEntity(eq(URL_PREFIX + URL_POLICY_INSTANCES), eq(String.class), eq(uriVariables)))
            .thenReturn(getPolicyInstancesForTypeResp);

        ResponseEntity<String> returnedResp = apiUnderTest.getPolicyInstancesForType(POLICY_TYPE_1_ID);

        verify(restTemplateMock).getForEntity(URL_PREFIX + URL_POLICY_INSTANCES, String.class, uriVariables);
        assertEquals(returnedResp.getBody(), policyInstancesJson);
        assertEquals(HttpStatus.OK, returnedResp.getStatusCode());
    }

    @Test
    public void testGetPolicyInstancesForTypeSuccessInvalidJson() {
        String policyInstances = Arrays.asList(POLICY_1_INVALID).toString();
        ResponseEntity<String> getPolicyInstancesForTypeResp = new ResponseEntity<>(policyInstances, HttpStatus.OK);
        Map<String, ?> uriVariables = Map.of("type", POLICY_TYPE_1_ID);
        when(restTemplateMock.getForEntity(eq(URL_PREFIX + URL_POLICY_INSTANCES), eq(String.class), eq(uriVariables)))
            .thenReturn(getPolicyInstancesForTypeResp);

        ResponseEntity<String> returnedResp = apiUnderTest.getPolicyInstancesForType(POLICY_TYPE_1_ID);

        verify(restTemplateMock).getForEntity(URL_PREFIX + URL_POLICY_INSTANCES, String.class, uriVariables);
        assertTrue(returnedResp.getBody().contains("Exception"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, returnedResp.getStatusCode());
    }

    @Test
    public void testGetPolicyInstance() {
        ResponseEntity<Object> getPolicyInstanceResp = new ResponseEntity<>(POLICY_1_VALID, HttpStatus.OK);
        Map<String, ?> uriVariables = Map.of("id", POLICY_1_ID);
        when(restTemplateMock.getForEntity(eq(URL_PREFIX + URL_POLICY_INSTANCE), eq(Object.class), eq(uriVariables)))
            .thenReturn(getPolicyInstanceResp);

        ResponseEntity<Object> returnedResp = apiUnderTest.getPolicyInstance(POLICY_1_ID);

        verify(restTemplateMock).getForEntity(URL_PREFIX + URL_POLICY_INSTANCE, Object.class, uriVariables);
        assertEquals(POLICY_1_VALID, returnedResp.getBody());
        assertEquals(HttpStatus.OK, returnedResp.getStatusCode());
    }

    @Test
    public void testPutPolicyFailure() {
        HttpEntity<Object> jsonHttpEntity = createJsonHttpEntity(POLICY_1_VALID);
        Map<String, ?> uriVariables = Map.of( //
            "type", POLICY_TYPE_1_ID, //
            "instance", POLICY_1_ID, //
            "ric", RIC_1_ID, //
            "service", "dashboard");
        doThrow(new RestClientException(CLIENT_ERROR_MESSAGE)).when(restTemplateMock)
            .put(eq(URL_PREFIX + URL_PUT_POLICY), eq(jsonHttpEntity), eq(uriVariables));

        ResponseEntity<String> returnedResp =
            apiUnderTest.putPolicy(POLICY_TYPE_1_ID, POLICY_1_ID, POLICY_1_VALID, RIC_1_ID);

        verify(restTemplateMock).put(URL_PREFIX + URL_PUT_POLICY, jsonHttpEntity, uriVariables);
        assertTrue(returnedResp.getBody().contains("Exception"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, returnedResp.getStatusCode());
    }

    @Test
    public void testPutPolicySuccess() {
        HttpEntity<Object> jsonHttpEntity = createJsonHttpEntity(POLICY_1_VALID);
        Map<String, ?> uriVariables = Map.of( //
            "type", POLICY_TYPE_1_ID, //
            "instance", POLICY_1_ID, //
            "ric", RIC_1_ID, //
            "service", "dashboard");

        ResponseEntity<String> returnedResp =
            apiUnderTest.putPolicy(POLICY_TYPE_1_ID, POLICY_1_ID, POLICY_1_VALID, RIC_1_ID);

        verify(restTemplateMock).put(URL_PREFIX + URL_PUT_POLICY, jsonHttpEntity, uriVariables);
        assertNull(returnedResp.getBody());
        assertEquals(HttpStatus.OK, returnedResp.getStatusCode());
    }

    @Test
    public void testDeletePolicyFailure() {
        Map<String, ?> uriVariables = Map.of("instance", POLICY_1_ID);
        doThrow(new RestClientException(CLIENT_ERROR_MESSAGE)).when(restTemplateMock)
            .delete(eq(URL_PREFIX + URL_DELETE_POLICY), eq(uriVariables));

        ResponseEntity<String> returnedResp = apiUnderTest.deletePolicy(POLICY_1_ID);

        verify(restTemplateMock).delete(URL_PREFIX + URL_DELETE_POLICY, uriVariables);
        assertTrue(returnedResp.getBody().contains("Exception"));
        assertEquals(HttpStatus.NOT_FOUND, returnedResp.getStatusCode());
    }

    @Test
    public void testDeletePolicySuccess() {
        Map<String, ?> uriVariables = Map.of("instance", POLICY_1_ID);

        ResponseEntity<String> returnedResp = apiUnderTest.deletePolicy(POLICY_1_ID);

        verify(restTemplateMock).delete(URL_PREFIX + URL_DELETE_POLICY, uriVariables);
        assertNull(returnedResp.getBody());
        assertEquals(HttpStatus.OK, returnedResp.getStatusCode());
    }

    @Test
    public void testGetRicsSupportingTypeValidJson() {
        String rics = Arrays.asList(RIC_1_INFO_VALID).toString();
        String ricsJson = parseRicsJson(rics);
        Map<String, ?> uriVariables = Map.of("typeName", POLICY_TYPE_1_ID);
        when(restTemplateMock.getForObject(eq(URL_PREFIX + URL_RIC_INFO), eq(String.class), eq(uriVariables)))
            .thenReturn(rics);

        ResponseEntity<String> returnedResp = apiUnderTest.getRicsSupportingType(POLICY_TYPE_1_ID);

        verify(restTemplateMock).getForObject(URL_PREFIX + URL_RIC_INFO, String.class, uriVariables);
        assertEquals(returnedResp.getBody(), ricsJson);
        assertEquals(HttpStatus.OK, returnedResp.getStatusCode());
    }

    @Test
    public void testGetRicsSupportingTypeInvalidJson() {
        String rics = Arrays.asList(RIC_1_INFO_INVALID).toString();
        Map<String, ?> uriVariables = Map.of("typeName", POLICY_TYPE_1_ID);
        when(restTemplateMock.getForObject(eq(URL_PREFIX + URL_RIC_INFO), eq(String.class), eq(uriVariables)))
            .thenReturn(rics);

        ResponseEntity<String> returnedResp = apiUnderTest.getRicsSupportingType(POLICY_TYPE_1_ID);

        verify(restTemplateMock).getForObject(URL_PREFIX + URL_RIC_INFO, String.class, uriVariables);
        assertTrue(returnedResp.getBody().contains("Exception"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, returnedResp.getStatusCode());
    }

    private String parsePolicyTypesJson(String inputString) {
        PolicyTypes policyTypes = new PolicyTypes();
        JsonArray schemas = new JsonParser().parse(inputString).getAsJsonArray();
        for (JsonElement schema : schemas) {
            JsonObject schemaObj = schema.getAsJsonObject();
            policyTypes.add(new PolicyType(schemaObj.get("title").getAsString(), schemaObj.toString()));
        }
        return gson.toJson(policyTypes);
    }

    private String parsePolicyInstancesJson(String inputString) {
        Type listType = new TypeToken<List<ImmutablePolicyInfo>>() {}.getType();
        List<PolicyInfo> rspParsed = gson.fromJson(inputString, listType);
        PolicyInstances policyInstances = new PolicyInstances();
        for (PolicyInfo policy : rspParsed) {
            policyInstances.add(policy);
        }
        return gson.toJson(policyInstances);
    }

    private String parseRicsJson(String inputString) {
        Type listType = new TypeToken<List<ImmutableRicInfo>>() {}.getType();
        List<RicInfo> rspParsed = gson.fromJson(inputString, listType);
        Collection<String> rics = new ArrayList<>(rspParsed.size());
        for (RicInfo ric : rspParsed) {
            rics.add(ric.ricName());
        }
        return gson.toJson(rics);
    }

    private HttpEntity<Object> createJsonHttpEntity(Object content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(content, headers);
    }
}
