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

import org.oransc.policyagent.exceptions.AsyncRestClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class AsyncRestClient {
    private final WebClient client;

    public AsyncRestClient(String baseUrl) {
        this.client = WebClient.create(baseUrl);
    }

    public Mono<String> put(String uri, String body) {
        return client.put() //
            .uri(uri) //
            .contentType(MediaType.APPLICATION_JSON) //
            .syncBody(body) //
            .retrieve() //
            .onStatus(HttpStatus::isError,
                response -> Mono.error(new AsyncRestClientException(response.statusCode().toString()))) //
            .bodyToMono(String.class);
    }

    public Mono<String> get(String uri) {
        return client.get() //
            .uri(uri) //
            .retrieve() //
            .onStatus(HttpStatus::isError,
                response -> Mono.error(new AsyncRestClientException(response.statusCode().toString()))) //
            .bodyToMono(String.class);
    }

    public Mono<Void> delete(String uri) {
        return client.delete() //
            .uri(uri) //
            .retrieve() //
            .onStatus(HttpStatus::isError,
                response -> Mono.error(new AsyncRestClientException(response.statusCode().toString()))) //
            .bodyToMono(Void.class);
    }
}
