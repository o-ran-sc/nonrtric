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

package org.oransc.enrichment.repository;

import java.lang.invoke.MethodHandles;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the dynamic information about a EI job
 */

public class EiJob {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter
    private final String id;

    @Getter
    private final String typeId;

    @Getter
    private final String owner;

    @Getter
    private final Object jobData;

    @Getter
    private final String targetUrl;

    @Getter
    private final String jobStatusUrl;

    @Getter
    private boolean isLastStatusReportedEnabled = true;

    public EiJob(String id, String typeId, String owner, Object jobData, String targetUrl, String jobStatusUrl) {
        this.id = id;
        this.typeId = typeId;
        this.owner = owner;
        this.jobData = jobData;
        this.targetUrl = targetUrl;
        this.jobStatusUrl = jobStatusUrl;
    }

    public void setLastReportedStatus(boolean isEnabled) {
        this.isLastStatusReportedEnabled = isEnabled;
        logger.debug("Job status id: {}, enabled: {}", this.isLastStatusReportedEnabled, isEnabled);
    }

}
