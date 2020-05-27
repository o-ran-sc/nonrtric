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
import org.json.JSONObject;
import org.oransc.policyagent.configuration.ControllerConfig;
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

    static final int CONCURRENCY_RIC = 1; // How may paralell requests that is sent to one NearRT RIC

    @Value.Immutable
    @org.immutables.gson.Gson.TypeAdapters
    public interface AdapterRequest {
        public String nearRtRicUrl();

        public Optional<String> body();
    }

    @Value.Immutable
    @org.immutables.gson.Gson.TypeAdapters
    public interface AdapterOutput {
        public Optional<String> body();

        public int httpStatus();
    }

    static com.google.gson.Gson gson = new GsonBuilder() //
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES) //
        .create(); //

    private static final String GET_POLICY_RPC = "getA1Policy";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ControllerConfig controllerConfig;
    private final AsyncRestClient restClient;
    private final RicConfig ricConfig;
    private final A1ProtocolType protocolType;

    /**
     * Constructor that creates the REST client to use.
     *
     * @param protocolType the southbound protocol of the controller. Supported protocols are SDNC_OSC_STD_V1_1 and
     *        SDNC_OSC_OSC_V1
     * @param ricConfig the configuration of the Ric to communicate with
     * @param controllerConfig the configuration of the SDNC controller to use
     *
     * @throws IllegalArgumentException when the protocolType is wrong.
     */
    public SdncOscA1Client(A1ProtocolType protocolType, RicConfig ricConfig, ControllerConfig controllerConfig) {
        this(protocolType, ricConfig, controllerConfig,
            new AsyncRestClient(controllerConfig.baseUrl() + "/restconf/operations"));
        logger.debug("SdncOscA1Client for ric: {}, a1Controller: {}", ricConfig.name(), controllerConfig);
    }

    /**
     * Constructor where the REST client to use is provided.
     *
     * @param protocolType the southbound protocol of the controller. Supported protocols are SDNC_OSC_STD_V1_1 and
     *        SDNC_OSC_OSC_V1
     * @param ricConfig the configuration of the Ric to communicate with
     * @param controllerConfig the configuration of the SDNC controller to use
     * @param restClient the REST client to use
     *
     * @throws IllegalArgumentException when the protocolType is wrong.
     */
    public SdncOscA1Client(A1ProtocolType protocolType, RicConfig ricConfig, ControllerConfig controllerConfig,
        AsyncRestClient restClient) {
        if (!(A1ProtocolType.SDNC_OSC_STD_V1_1.equals(protocolType)
            || A1ProtocolType.SDNC_OSC_OSC_V1.equals(protocolType))) {
            throw new IllegalArgumentException("Protocol type must be " + A1ProtocolType.SDNC_OSC_STD_V1_1 + " or "
                + A1ProtocolType.SDNC_OSC_OSC_V1 + ", was: " + protocolType);
        }
        this.restClient = restClient;
        this.ricConfig = ricConfig;
        this.protocolType = protocolType;
        this.controllerConfig = controllerConfig;
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        if (this.protocolType == A1ProtocolType.SDNC_OSC_STD_V1_1) {
            return Mono.just(Arrays.asList(""));
        } else {
            OscA1Client.UriBuilder uri = new OscA1Client.UriBuilder(ricConfig);
            final String ricUrl = uri.createPolicyTypesUri();
            return post(GET_POLICY_RPC, ricUrl, Optional.empty()) //
                .flatMapMany(SdncJsonHelper::parseJsonArrayOfString) //
                .collectList();
        }

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
        } else {
            OscA1Client.UriBuilder uri = new OscA1Client.UriBuilder(ricConfig);
            final String ricUrl = uri.createGetSchemaUri(policyTypeId);
            return post(GET_POLICY_RPC, ricUrl, Optional.empty()) //
                .flatMap(response -> OscA1Client.extractCreateSchema(response, policyTypeId));
        }
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        return getUriBuilder() //
            .flatMap(builder -> {
                String ricUrl = builder.createPutPolicyUri(policy.type().name(), policy.id());
                return post("putA1Policy", ricUrl, Optional.of(policy.json()));
            });
    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        return deletePolicyById(policy.type().name(), policy.id());
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        if (this.protocolType == A1ProtocolType.SDNC_OSC_STD_V1_1) {
            return getPolicyIds() //
                .flatMap(policyId -> deletePolicyById("", policyId), CONCURRENCY_RIC); //
        } else {
            OscA1Client.UriBuilder uriBuilder = new OscA1Client.UriBuilder(ricConfig);
            return getPolicyTypeIdentities() //
                .flatMapMany(Flux::fromIterable) //
                .flatMap(type -> oscDeleteInstancesForType(uriBuilder, type), CONCURRENCY_RIC);
        }
    }

    private Flux<String> oscGetInstancesForType(OscA1Client.UriBuilder uriBuilder, String type) {
        return post(GET_POLICY_RPC, uriBuilder.createGetPolicyIdsUri(type), Optional.empty()) //
            .flatMapMany(SdncJsonHelper::parseJsonArrayOfString);
    }

    private Flux<String> oscDeleteInstancesForType(OscA1Client.UriBuilder uriBuilder, String type) {
        return oscGetInstancesForType(uriBuilder, type) //
            .flatMap(instance -> deletePolicyById(type, instance), CONCURRENCY_RIC);
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return tryStdProtocolVersion() //
            .onErrorResume(t -> tryOscProtocolVersion());
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        return getUriBuilder() //
            .flatMap(builder -> {
                String ricUrl = builder.createGetPolicyStatusUri(policy.type().name(), policy.id());
                return post("getA1PolicyStatus", ricUrl, Optional.empty());
            });
    }

    private Mono<A1UriBuilder> getUriBuilder() {
        if (protocolType == A1ProtocolType.SDNC_OSC_STD_V1_1) {
            return Mono.just(new StdA1ClientVersion1.UriBuilder(ricConfig));
        } else {
            return Mono.just(new OscA1Client.UriBuilder(ricConfig));
        }
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
        } else {
            OscA1Client.UriBuilder uri = new OscA1Client.UriBuilder(ricConfig);
            return getPolicyTypeIdentities() //
                .flatMapMany(Flux::fromIterable)
                .flatMap(type -> post(GET_POLICY_RPC, uri.createGetPolicyIdsUri(type), Optional.empty())) //
                .flatMap(SdncJsonHelper::parseJsonArrayOfString);
        }
    }

    private Mono<String> deletePolicyById(String type, String policyId) {
        return getUriBuilder() //
            .flatMap(builder -> {
                String ricUrl = builder.createDeleteUri(type, policyId);
                return post("deleteA1Policy", ricUrl, Optional.empty());
            });
    }

    private Mono<String> post(String rpcName, String ricUrl, Optional<String> body) {
        AdapterRequest inputParams = ImmutableAdapterRequest.builder() //
            .nearRtRicUrl(ricUrl) //
            .body(body) //
            .build();
        final String inputJsonString = SdncJsonHelper.createInputJsonString(inputParams);
        logger.debug("POST inputJsonString = {}", inputJsonString);

        return restClient
            .postWithAuthHeader(controllerUrl(rpcName), inputJsonString, this.controllerConfig.userName(),
                this.controllerConfig.password()) //
            .flatMap(this::extractResponseBody);
    }

    private Mono<String> extractResponse(JSONObject responseOutput) {
        AdapterOutput output = gson.fromJson(responseOutput.toString(), ImmutableAdapterOutput.class);
        Optional<String> optionalBody = output.body();
        String body = optionalBody.isPresent() ? optionalBody.get() : "";
        if (HttpStatus.valueOf(output.httpStatus()).is2xxSuccessful()) {
            return Mono.just(body);
        } else {
            logger.debug("Error response: {} {}", output.httpStatus(), body);
            byte[] responseBodyBytes = body.getBytes(StandardCharsets.UTF_8);
            WebClientResponseException responseException = new WebClientResponseException(output.httpStatus(),
                "statusText", null, responseBodyBytes, StandardCharsets.UTF_8, null);

            return Mono.error(responseException);
        }
    }

    private Mono<String> extractResponseBody(String responseStr) {
        return SdncJsonHelper.getOutput(responseStr) //
            .flatMap(this::extractResponse);
    }

    private String controllerUrl(String rpcName) {
        return "/A1-ADAPTER-API:" + rpcName;
    }
}
