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

package org.oransc.enrichment.controllers.r1consumer;

public class ConsumerConsts {

    public static final String API_ROOT = "/data-consumer/v1";

    public static final String CONSUMER_API_NAME = "Data consumer";
    public static final String CONSUMER_API_DESCRIPTION = "API for data consumers";

    public static final String OWNER_PARAM = "owner";
    public static final String OWNER_PARAM_DESCRIPTION = "selects subscription jobs for one job owner";

    public static final String INDIVIDUAL_JOB = "Individual data subscription job";

    public static final String INFO_TYPE_ID_PARAM = "infoTypeId";
    public static final String INFO_TYPE_ID_PARAM_DESCRIPTION =
        "selects subscription jobs of matching information type";

    private ConsumerConsts() {
    }
}
