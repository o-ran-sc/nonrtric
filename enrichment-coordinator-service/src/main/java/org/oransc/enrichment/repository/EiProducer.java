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

import java.util.Collection;

import lombok.Getter;

public class EiProducer {
    @Getter
    private final String id;

    @Getter
    private final Collection<EiType> eiTypes;

    @Getter
    private final String jobCreationCallbackUrl;

    @Getter
    private final String jobDeletionCallbackUrl;

    @Getter
    private final String producerSupervisionCallbackUrl;

    private int unresponsiveCounter = 0;

    public EiProducer(String id, Collection<EiType> eiTypes, String jobCreationCallbackUrl,
        String jobDeletionCallbackUrl, String producerSupervisionCallbackUrl) {
        this.id = id;
        this.eiTypes = eiTypes;
        this.jobCreationCallbackUrl = jobCreationCallbackUrl;
        this.jobDeletionCallbackUrl = jobDeletionCallbackUrl;
        this.producerSupervisionCallbackUrl = producerSupervisionCallbackUrl;
    }

    public synchronized void setAliveStatus(boolean isAlive) {
        if (isAlive) {
            unresponsiveCounter = 0;
        } else {
            unresponsiveCounter++;
        }
    }

    public synchronized boolean isDead() {
        return this.unresponsiveCounter >= 3;
    }

    public synchronized boolean isAvailable() {
        return this.unresponsiveCounter == 0;
    }

}
