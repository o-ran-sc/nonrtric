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

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.oransc.policyagent.clients.A1Client.A1ProtocolType;
import org.oransc.policyagent.clients.SdncOscA1Client.AdapterOutput;
import org.oransc.policyagent.clients.SdncOscA1Client.AdapterRequest;
import org.oransc.policyagent.configuration.ControllerConfig;
import org.oransc.policyagent.configuration.ImmutableControllerConfig;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.Ric;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class SdncOscA1ClientTest {
    private static final String CONTROLLER_USERNAME = "username";
    private static final String CONTROLLER_PASSWORD = "password";
    private static final String RIC_1_URL = "RicUrl";
    private static final String GET_A1_POLICY_URL = "/A1-ADAPTER-API:getA1Policy";
    private static final String PUT_A1_URL = "/A1-ADAPTER-API:putA1Policy";
    private static final String DELETE_A1_URL = "/A1-ADAPTER-API:deleteA1Policy";
    private static final String GET_A1_POLICY_STATUS_URL = "/A1-ADAPTER-API:getA1PolicyStatus";
    private static final String POLICY_TYPE_1_ID = "type1";
    private static final String POLICY_1_ID = "policy1";
    private static final String POLICY_2_ID = "policy2";
    private static final String POLICY_JSON_VALID = "{\"scope\":{\"ueId\":\"ue1\"}}";

    SdncOscA1Client clientUnderTest;

    AsyncRestClient asyncRestClientMock;

    private ControllerConfig controllerConfig() {
        return ImmutableControllerConfig.builder() //
            .name("name") //
            .baseUrl("baseUrl") //
            .password(CONTROLLER_PASSWORD) //
            .userName(CONTROLLER_USERNAME) //
            .build();
    }

    @BeforeEach
    public void init() {
        asyncRestClientMock = mock(AsyncRestClient.class);
        Ric ric = A1ClientHelper.createRic(RIC_1_URL);

        clientUnderTest = new SdncOscA1Client(A1ProtocolType.SDNC_OSC_STD_V1_1, ric.getConfig(), controllerConfig(),
            asyncRestClientMock);
    }

    @Test
    public void testGetPolicyTypeIdentities_STD() {
        List<String> policyTypeIds = clientUnderTest.getPolicyTypeIdentities().block();
        assertEquals(1, policyTypeIds.size(), "should hardcoded to one");
        assertEquals("", policyTypeIds.get(0), "should hardcoded to empty");
    }

    @Test
    public void testGetPolicyTypeIdentities_OSC() {
        clientUnderTest = new SdncOscA1Client(A1ProtocolType.SDNC_OSC_OSC_V1, //
            A1ClientHelper.createRic(RIC_1_URL).getConfig(), //
            controllerConfig(), asyncRestClientMock);

        String response = createResponse(Arrays.asList(POLICY_TYPE_1_ID));
        whenAsyncPostThenReturn(Mono.just(response));

        List<String> policyTypeIds = clientUnderTest.getPolicyTypeIdentities().block();
        assertEquals(1, policyTypeIds.size(), "");
        assertEquals(POLICY_TYPE_1_ID, policyTypeIds.get(0), "");

        String expUrl = RIC_1_URL + "/a1-p/policytypes";
        ImmutableAdapterRequest expectedParams = ImmutableAdapterRequest.builder() //
            .nearRtRicUrl(expUrl) //
            .build();
        String expInput = SdncJsonHelper.createInputJsonString(expectedParams);
        verify(asyncRestClientMock).postWithAuthHeader(GET_A1_POLICY_URL, expInput, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
    }

    private String loadFile(String fileName) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(fileName);
        File file = new File(url.getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }

    @Test
    public void testGetTypeSchema_OSC() throws IOException {
        clientUnderTest = new SdncOscA1Client(A1ProtocolType.SDNC_OSC_OSC_V1, //
            A1ClientHelper.createRic(RIC_1_URL).getConfig(), //
            controllerConfig(), asyncRestClientMock);

        String ricResponse = loadFile("test_osc_get_schema_response.json");
        JsonElement elem = gson().fromJson(ricResponse, JsonElement.class);
        String responseFromController = createResponse(elem);
        whenAsyncPostThenReturn(Mono.just(responseFromController));

        String response = clientUnderTest.getPolicyTypeSchema("policyTypeId").block();
        JsonElement respJson = gson().fromJson(response, JsonElement.class);
        assertEquals("policyTypeId", respJson.getAsJsonObject().get("title").getAsString(),
            "title should be updated to contain policyType ID");
    }

    @Test
    void parseJsonArrayOfString() {
        // One integer and one string
        String inputString = "[1, \"1\" ]";

        List<String> result = SdncJsonHelper.parseJsonArrayOfString(inputString).collectList().block();
        assertEquals(2, result.size(), "");
        assertEquals("1", result.get(0), "");
        assertEquals("1", result.get(1), "");
    }

    private String policiesUrl() {
        return RIC_1_URL + "/A1-P/v1/policies";
    }

    private Gson gson() {
        return SdncOscA1Client.gson;
    }

    private String createResponse(Object body) {
        AdapterOutput output = ImmutableAdapterOutput.builder() //
            .body(gson().toJson(body)) //
            .httpStatus(200) //
            .build();
        return SdncJsonHelper.createOutputJsonString(output);
    }

    @Test
    public void testGetPolicyIdentities() {

        String policyIdsResp = createResponse(Arrays.asList(POLICY_1_ID, POLICY_2_ID));
        whenAsyncPostThenReturn(Mono.just(policyIdsResp));

        List<String> returned = clientUnderTest.getPolicyIdentities().block();
        assertEquals(2, returned.size(), "");

        ImmutableAdapterRequest expectedParams = ImmutableAdapterRequest.builder() //
            .nearRtRicUrl(policiesUrl()) //
            .build();
        String expInput = SdncJsonHelper.createInputJsonString(expectedParams);
        verify(asyncRestClientMock).postWithAuthHeader(GET_A1_POLICY_URL, expInput, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);

    }

    @Test
    public void testGetValidPolicyType() {
        String policyType = clientUnderTest.getPolicyTypeSchema("").block();
        assertEquals("{}", policyType, "");
    }

    @Test
    public void testPutPolicyValidResponse() {
        whenPostReturnOkResponse();

        String returned = clientUnderTest
            .putPolicy(A1ClientHelper.createPolicy(RIC_1_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID))
            .block();
        assertEquals("OK", returned, "");
        final String expUrl = policiesUrl() + "/" + POLICY_1_ID;
        AdapterRequest expectedInputParams = ImmutableAdapterRequest.builder() //
            .nearRtRicUrl(expUrl) //
            .body(POLICY_JSON_VALID) //
            .build();
        String expInput = SdncJsonHelper.createInputJsonString(expectedInputParams);

        verify(asyncRestClientMock).postWithAuthHeader(PUT_A1_URL, expInput, CONTROLLER_USERNAME, CONTROLLER_PASSWORD);
    }

    @Test
    public void testPutPolicyRejected() {
        final String policyJson = "{}";
        AdapterOutput adapterOutput = ImmutableAdapterOutput.builder() //
            .body("NOK") //
            .httpStatus(400) // ERROR
            .build();

        String resp = SdncJsonHelper.createOutputJsonString(adapterOutput);
        whenAsyncPostThenReturn(Mono.just(resp));

        Mono<String> returnedMono = clientUnderTest
            .putPolicy(A1ClientHelper.createPolicy(RIC_1_URL, POLICY_1_ID, policyJson, POLICY_TYPE_1_ID));
        StepVerifier.create(returnedMono) //
            .expectSubscription() //
            .expectErrorMatches(t -> t instanceof WebClientResponseException) //
            .verify();

        final String expUrl = policiesUrl() + "/" + POLICY_1_ID;
        AdapterRequest expRequestParams = ImmutableAdapterRequest.builder() //
            .nearRtRicUrl(expUrl) //
            .body(policyJson) //
            .build();
        String expRequest = SdncJsonHelper.createInputJsonString(expRequestParams);
        verify(asyncRestClientMock).postWithAuthHeader(PUT_A1_URL, expRequest, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
        StepVerifier.create(returnedMono)
            .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException).verify();
    }

    @Test
    public void testDeletePolicy() {
        whenPostReturnOkResponse();

        String returned = clientUnderTest
            .deletePolicy(A1ClientHelper.createPolicy(RIC_1_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID))
            .block();
        assertEquals("OK", returned, "");
        final String expUrl = policiesUrl() + "/" + POLICY_1_ID;
        AdapterRequest expectedInputParams = ImmutableAdapterRequest.builder() //
            .nearRtRicUrl(expUrl) //
            .build();
        String expInput = SdncJsonHelper.createInputJsonString(expectedInputParams);

        verify(asyncRestClientMock).postWithAuthHeader(DELETE_A1_URL, expInput, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
    }

    @Test
    public void testGetStatus() {
        whenPostReturnOkResponse();

        Policy policy = A1ClientHelper.createPolicy(RIC_1_URL, POLICY_1_ID, POLICY_JSON_VALID, POLICY_TYPE_1_ID);

        String returnedStatus = clientUnderTest.getPolicyStatus(policy).block();

        assertEquals("OK", returnedStatus, "unexpected status");

        final String expUrl = policiesUrl() + "/" + POLICY_1_ID + "/status";
        AdapterRequest expectedInputParams = ImmutableAdapterRequest.builder() //
            .nearRtRicUrl(expUrl) //
            .build();
        String expInput = SdncJsonHelper.createInputJsonString(expectedInputParams);

        verify(asyncRestClientMock).postWithAuthHeader(GET_A1_POLICY_STATUS_URL, expInput, CONTROLLER_USERNAME,
            CONTROLLER_PASSWORD);
    }

    @Test
    public void testGetVersion() {
        whenPostReturnOkResponse();
        A1ProtocolType returnedVersion = clientUnderTest.getProtocolVersion().block();
        assertEquals(A1ProtocolType.SDNC_OSC_STD_V1_1, returnedVersion, "");

        whenPostReturnOkResponseNoBody();
        returnedVersion = clientUnderTest.getProtocolVersion().block();
        assertEquals(A1ProtocolType.SDNC_OSC_STD_V1_1, returnedVersion, "");
    }

    private void whenPostReturnOkResponse() {
        AdapterOutput adapterOutput = ImmutableAdapterOutput.builder() //
            .body("OK") //
            .httpStatus(200) //
            .build();

        String resp = SdncJsonHelper.createOutputJsonString(adapterOutput);
        whenAsyncPostThenReturn(Mono.just(resp));
    }

    private void whenPostReturnOkResponseNoBody() {
        AdapterOutput adapterOutput = ImmutableAdapterOutput.builder() //
            .httpStatus(200) //
            .body(Optional.empty()) //
            .build();

        String resp = SdncJsonHelper.createOutputJsonString(adapterOutput);
        whenAsyncPostThenReturn(Mono.just(resp));
    }

    private OngoingStubbing<Mono<String>> whenAsyncPostThenReturn(Mono<String> response) {
        return when(asyncRestClientMock.postWithAuthHeader(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(response);
    }
}
