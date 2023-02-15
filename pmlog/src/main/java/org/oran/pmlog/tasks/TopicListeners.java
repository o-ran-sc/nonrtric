/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2023 Nordix Foundation
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

package org.oran.pmlog.tasks;

import org.oran.pmlog.configuration.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
@Component
public class TopicListeners {
    private static final Logger logger = LoggerFactory.getLogger(TopicListeners.class);

    public TopicListeners(@Autowired ApplicationConfig appConfig) {
        logger.info("Starting");
        KafkaTopicListener listener = new KafkaTopicListener(appConfig);

        InfluxStore distr = new InfluxStore(appConfig);
        distr.start(listener.getFlux());
    }

}
