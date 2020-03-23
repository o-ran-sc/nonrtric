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

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final String a1ControllerUsername;
    private final String a1ControllerPassword;
    private final AsyncRestClient restClient;
    private final A1UriBuilder uri;

    public SdncOscA1Client(RicConfig ricConfig, String controllerBaseUrl, String username, String password) {
        this(ricConfig, username, password, new AsyncRestClient(controllerBaseUrl + "/restconf/operations"));
        logger.debug("SdncOscA1Client for ric: {}, a1ControllerBaseUrl: {}", ricConfig.name(), controllerBaseUrl);
    }

    public SdncOscA1Client(RicConfig ricConfig, String username, String password, AsyncRestClient restClient) {
        this.a1ControllerUsername = username;
        this.a1ControllerPassword = password;
        this.restClient = restClient;
        this.uri = new StdA1UriBuilderVersion1(ricConfig);
    }

    @Override
    public Mono<List<String>> getPolicyTypeIdentities() {
        return Mono.just(Arrays.asList(""));
    }

    @Override
    public Mono<List<String>> getPolicyIdentities() {
        return getPolicyIds() //
            .collectList();
    }

    @Override
    public Mono<String> getPolicyTypeSchema(String policyTypeId) {
        return Mono.just("{}");
    }

    @Override
    public Mono<String> putPolicy(Policy policy) {
        final String ricUrl = uri.putPolicyUri(policy);
        return post("putA1", ricUrl, Optional.of(policy.json()));
    }

    @Override
    public Mono<String> deletePolicy(Policy policy) {
        return deletePolicyById(policy.id());
    }

    @Override
    public Flux<String> deleteAllPolicies() {
        return getPolicyIds() //
            .flatMap(this::deletePolicyById); //
    }

    @Override
    public Mono<A1ProtocolType> getProtocolVersion() {
        return getPolicyIdentities() //
            .flatMap(x -> Mono.just(A1ProtocolType.SDNC_OSC));
    }

    @Override
    public Mono<String> getPolicyStatus(Policy policy) {
        final String ricUrl = uri.policyStatusUri(policy.id());
        return post("getA1", ricUrl, Optional.empty());
    }

    private Flux<String> getPolicyIds() {
        final String ricUrl = uri.getPolicyIdsUri();
        return post("getA1", ricUrl, Optional.empty()) //
            .flatMapMany(JsonHelper::parseJsonArrayOfString);
    }

    private Mono<String> deletePolicyById(String policyId) {
        final String ricUrl = uri.deleteUri(policyId);
        return post("deleteA1", ricUrl, Optional.empty());
    }

    private Mono<String> post(String rpcName, String ricUrl, Optional<String> body) {
        AdapterRequest inputParams = ImmutableAdapterRequest.builder() //
            .nearRtRicUrl(ricUrl) //
            .body(body) //
            .build();
        final String inputJsonString = JsonHelper.createInputJsonString(inputParams);

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
