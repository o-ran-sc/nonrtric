/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2021 Nordix Foundation
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

package org.oran.dmaapadapter.repository;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

import org.immutables.gson.Gson;
import org.oran.dmaapadapter.clients.AsyncRestClient;

public class Job {

    @Gson.TypeAdapters
    public static class Parameters {
        @Getter
        private String filter;
        @Getter
        private BufferTimeout bufferTimeout;

        private int maxConcurrency;

        public Parameters() {}

        public Parameters(String filter, BufferTimeout bufferTimeout, int maxConcurrency) {
            this.filter = filter;
            this.bufferTimeout = bufferTimeout;
            this.maxConcurrency = maxConcurrency;
        }

        public int getMaxConcurrency() {
            return maxConcurrency == 0 ? 1 : maxConcurrency;
        }
    }

    @Gson.TypeAdapters
    public static class BufferTimeout {
        public BufferTimeout(int maxSize, long maxTimeMiliseconds) {
            this.maxSize = maxSize;
            this.maxTimeMiliseconds = maxTimeMiliseconds;
        }

        public BufferTimeout() {}

        @Getter
        private int maxSize;

        private long maxTimeMiliseconds;

        public Duration getMaxTime() {
            return Duration.ofMillis(maxTimeMiliseconds);
        }
    }

    @Getter
    private final String id;

    @Getter
    private final String callbackUrl;

    @Getter
    private final InfoType type;

    @Getter
    private final String owner;

    @Getter
    private final Parameters parameters;

    @Getter
    private final String lastUpdated;

    private final Pattern jobDataFilter;

    @Getter
    private final AsyncRestClient consumerRestClient;

    public Job(String id, String callbackUrl, InfoType type, String owner, String lastUpdated, Parameters parameters,
            AsyncRestClient consumerRestClient) {
        this.id = id;
        this.callbackUrl = callbackUrl;
        this.type = type;
        this.owner = owner;
        this.lastUpdated = lastUpdated;
        this.parameters = parameters;
        if (parameters != null && parameters.filter != null) {
            jobDataFilter = Pattern.compile(parameters.filter);
        } else {
            jobDataFilter = null;
        }
        this.consumerRestClient = consumerRestClient;
    }

    public boolean isFilterMatch(String data) {
        if (jobDataFilter == null) {
            return true;
        }
        Matcher matcher = jobDataFilter.matcher(data);
        return matcher.find();
    }

    public boolean isBuffered() {
        return parameters != null && parameters.bufferTimeout != null && parameters.bufferTimeout.maxSize > 0
                && parameters.bufferTimeout.maxTimeMiliseconds > 0;
    }

}
