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

package org.oran.dmaapadapter.tasks;

import org.oran.dmaapadapter.configuration.ApplicationConfig;
import org.oran.dmaapadapter.repository.InfoType;
import org.oran.dmaapadapter.repository.InfoTypes;
import org.oran.dmaapadapter.repository.Jobs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The class fetches incoming requests from DMAAP and sends them further to the
 * consumers that has a job for this InformationType.
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
@Component
public class DmaapTopicConsumers {

    DmaapTopicConsumers(@Autowired ApplicationConfig appConfig, @Autowired InfoTypes types, @Autowired Jobs jobs) {
        // Start a consumer for each type
        for (InfoType type : types.getAll()) {
            if (type.isDmaapTopicDefined()) {
                DmaapTopicConsumer topicConsumer = new DmaapTopicConsumer(appConfig, type, jobs);
                topicConsumer.start();
            }
        }
    }

}
