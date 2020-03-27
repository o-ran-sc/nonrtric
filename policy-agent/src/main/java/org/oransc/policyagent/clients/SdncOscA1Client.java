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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Client for accessing the A1 adapter in the SDNC controller in OSC.
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class SdncOscA1Client implements A1Client {

    @Value.Immutable
    @org.immutables.gson.Gson.TypeAdapters
    public interface AdapterRequest {
        public String nearRtRicUrl();

        public Optional<String> body();
    }

    @Value.Immutable
    @org.immutables.gson.Gson.TypeAdapters
    public interface AdapterResponse {
        public String body();

        public int httpStatus();
    }

    static com.google.gson.Gson gson = new GsonBuilder() //
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES) //
        .create(); //

    private static final String GET_POLICY_RPC = "getA1Policy";
    private static final String UNHANDELED_PROTOCOL = "Bug, unhandeled protocoltype: ";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final String a1ControllerUsername;
    private final String a1ControllerPassword;
    private final AsyncRestClient restClient;
    private final RicConfig ricConfig;
    private final A1ProtocolType protocolType;

    /**
     * Constructor
     * 
     * @param protocolType the southbound protocol of the controller. Supported
     *        protocols are SDNC_OSC_STD_V1_1 and SDNC_OSC_OSC_V1
     * @param ricConfig
     * @param controllerBaseUrl the base URL of the SDNC controller
     * @param username username to accesss the SDNC controller
     * @param password password to accesss the SDNC controller
     */
    public SdncOscA1Client(A1ProtocolType protocolType, RicConfig ricConfig, String controllerBaseUrl, String username,
        String password) {
        this(protocolType, ricConfig, username, password,
            new AsyncRestClient(controllerBaseUrl + "/restconf/operations"));
        logger.debug("SdncOscA1Client for ric: {}, a1ControllerBaseUrl: {}", ricConfig.name(), controllerBaseUrl);
    }

    public SdncOscA1Client(A1ProtocolType protocolType, RicConfig ricConfig, String username, String password,
        AsyncRestClient restClient) {
        this.a1ControllerUsername = username;
        this.a1ControllerPassword = password;
        this.restClient = restClient;
        this.ricConfig = ricConfig;
        this.protocolType = protocolType;

    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        if (this.protocolType == A1ProtocolType.SDNC_OSC_STD_V1_1) {
            return Mono.just(Arrays.asList(""));
        } else if (this.protocolType == A1ProtocolType.SDNC_OSC_OSC_V1) {
            OscA1Client.UriBuilder uri = new OscA1Client.UriBuilder(ricConfig);
            final String ricUrl = uri.createPolicyTypesUri();
            return post(GET_POLICY_RPC, ricUrl, Optional.empty()) //
                .flatMapMany(SdncJsonHelper::parseJsonArrayOfString) //
                .collectList();
        }
        throw new NullPointerException(UNHANDELED_PROTOCOL + this.protocolType);
    }

    @Override
    public Mono<List<String>> getPolicyIdentities() {
        return getPolicyIds() //
            .collectList();
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        if (this.protocolType == A1ProtocolType.SDNC_OSC_STD_V1_1) {
            return Mono.just("{}");
        } else if (this.protocolType == A1ProtocolType.SDNC_OSC_OSC_V1) {
            OscA1Client.UriBuilder uri = new OscA1Client.UriBuilder(ricConfig);
            final String ricUrl = uri.createGetSchemaUri(policyTypeId);
            return post(GET_POLICY_RPC, ricUrl, Optional.empty());
        }
        throw new NullPointerException(UNHANDELED_PROTOCOL + this.protocolType);
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        final String ricUrl = getUriBuilder().createPutPolicyUri(policy.type().name(), policy.id());
        return post("putA1Policy", ricUrl, Optional.of(policy.json()));
    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        return deletePolicyById(policy.type().name(), policy.id());
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        if (this.protocolType == A1ProtocolType.SDNC_OSC_STD_V1_1) {
            return getPolicyIds() //
                .flatMap(policyId -> deletePolicyById("", policyId)); //
        } else if (this.protocolType == A1ProtocolType.SDNC_OSC_OSC_V1) {
            OscA1Client.UriBuilder uriBuilder = new OscA1Client.UriBuilder(ricConfig);
            return getPolicyTypeIdentities() //
                .flatMapMany(Flux::fromIterable)
                .flatMap(type -> post(GET_POLICY_RPC, uriBuilder.createGetPolicyIdsUri(type), Optional.empty())) //
                .flatMap(SdncJsonHelper::parseJsonArrayOfString);
        }
        throw new NullPointerException(UNHANDELED_PROTOCOL + this.protocolType);
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return tryStdProtocolVersion() //
            .onErrorResume(t -> tryOscProtocolVersion());
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        final String ricUrl = getUriBuilder().createGetPolicyStatusUri(policy.type().name(), policy.id());
        return post("getA1PolicyStatus", ricUrl, Optional.empty());
    }

    private A1UriBuilder getUriBuilder() {
        if (protocolType == A1ProtocolType.SDNC_OSC_STD_V1_1) {
            return new StdA1ClientVersion1.UriBuilder(ricConfig);
        } else if (this.protocolType == A1ProtocolType.SDNC_OSC_OSC_V1) {
            return new OscA1Client.UriBuilder(ricConfig);
        }
        throw new NullPointerException(UNHANDELED_PROTOCOL + this.protocolType);
    }

    private Mono<A1ProtocolType> tryOscProtocolVersion() {
        OscA1Client.UriBuilder oscApiuriBuilder = new OscA1Client.UriBuilder(ricConfig);
        return post(GET_POLICY_RPC, oscApiuriBuilder.createHealtcheckUri(), Optional.empty()) //
            .flatMap(x -> Mono.just(A1ProtocolType.SDNC_OSC_OSC_V1));
    }

    private Mono<A1ProtocolType> tryStdProtocolVersion() {
        StdA1ClientVersion1.UriBuilder uriBuilder = new StdA1ClientVersion1.UriBuilder(ricConfig);
        return post(GET_POLICY_RPC, uriBuilder.createGetPolicyIdsUri(), Optional.empty()) //
            .flatMap(x -> Mono.just(A1ProtocolType.SDNC_OSC_STD_V1_1));
    }

    private Flux<String> getPolicyIds() {
        if (this.protocolType == A1ProtocolType.SDNC_OSC_STD_V1_1) {
            StdA1ClientVersion1.UriBuilder uri = new StdA1ClientVersion1.UriBuilder(ricConfig);
            final String ricUrl = uri.createGetPolicyIdsUri();
            return post(GET_POLICY_RPC, ricUrl, Optional.empty()) //
                .flatMapMany(SdncJsonHelper::parseJsonArrayOfString);
        } else if (this.protocolType == A1ProtocolType.SDNC_OSC_OSC_V1) {
            OscA1Client.UriBuilder uri = new OscA1Client.UriBuilder(ricConfig);
            return getPolicyTypeIdentities() //
                .flatMapMany(Flux::fromIterable)
                .flatMap(type -> post(GET_POLICY_RPC, uri.createGetPolicyIdsUri(type), Optional.empty())) //
                .flatMap(SdncJsonHelper::parseJsonArrayOfString);
        }
        throw new NullPointerException(UNHANDELED_PROTOCOL + this.protocolType);
    }

    private Mono<String> deletePolicyById(String type, String policyId) {
        final String ricUrl = getUriBuilder().createDeleteUri(type, policyId);
        return post("deleteA1Policy", ricUrl, Optional.empty());
    }

    private Mono<String> post(String rpcName, String ricUrl, Optional<String> body) {
        AdapterRequest inputParams = ImmutableAdapterRequest.builder() //
            .nearRtRicUrl(ricUrl) //
            .body(body) //
            .build();
        final String inputJsonString = SdncJsonHelper.createInputJsonString(inputParams);

        return restClient
            .postWithAuthHeader(controllerUrl(rpcName), inputJsonString, a1ControllerUsername, a1ControllerPassword)
            .flatMap(this::extractResponseBody);
    }

    private Mono<String> extractResponseBody(String response) {
        AdapterResponse output = gson.fromJson(response, ImmutableAdapterResponse.class);
        String body = output.body();
        if (HttpStatus.valueOf(output.httpStatus()).is2xxSuccessful()) {
            return Mono.just(body);
        }
        byte[] responseBodyBytes = body.getBytes(StandardCharsets.UTF_8);
        WebClientResponseException e = new WebClientResponseException(output.httpStatus(), "statusText", null,
            responseBodyBytes, StandardCharsets.UTF_8, null);

        return Mono.error(e);
    }

    private String controllerUrl(String rpcName) {
        return "/A1-ADAPTER-API:" + rpcName;
    }
}
