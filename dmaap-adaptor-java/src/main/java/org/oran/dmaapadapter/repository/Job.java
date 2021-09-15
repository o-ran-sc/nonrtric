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

package org.oran.dmmapadapter.repository;

import lombok.Getter;

public class Job {

    @Getter
    private final String id;

    @Getter
    private final String callbackUrl;

    @Getter
    private final InfoType type;

    public Job(String id, String callbackUrl, InfoType type) {
        this.id = id;
        this.callbackUrl = callbackUrl;
        this.type = type;
    }

}
