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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

import org.immutables.gson.Gson;

public class Job {

    @Gson.TypeAdapters
    public static class Parameters {
        public String filter;
        public BufferTimeout bufferTimeout;

        public Parameters() {
        }

        public Parameters(String filter, BufferTimeout bufferTimeout) {
            this.filter = filter;
            this.bufferTimeout = bufferTimeout;
        }

        public static class BufferTimeout {
            public BufferTimeout(int maxSize, int maxTimeMiliseconds) {
                this.maxSize = maxSize;
                this.maxTimeMiliseconds = maxTimeMiliseconds;
            }

            public BufferTimeout() {
            }

            public int maxSize;
            public int maxTimeMiliseconds;
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

    public Job(String id, String callbackUrl, InfoType type, String owner, String lastUpdated, Parameters parameters) {
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
