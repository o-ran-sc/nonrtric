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

package org.oransc.ics.controllers.r1consumer;

public class ConsumerConsts {

    public static final String API_ROOT = "/data-consumer/v1";

    public static final String CONSUMER_API_NAME = "Data consumer";
    public static final String CONSUMER_API_CALLBACKS_NAME = "Data consumer (callbacks)";
    public static final String CONSUMER_API_DESCRIPTION = "API for data consumers";

    public static final String OWNER_PARAM = "owner";
    public static final String OWNER_PARAM_DESCRIPTION = "selects result for one owner";

    public static final String INDIVIDUAL_JOB = "Individual data subscription job";

    public static final String PUT_INDIVIDUAL_JOB_DESCRIPTION = "The job will be enabled when a producer is available";

    public static final String INFO_TYPE_ID_PARAM = "infoTypeId";
    public static final String INFO_TYPE_ID_PARAM_DESCRIPTION =
        "selects subscription jobs of matching information type";
    public static final String INFO_TYPE_ID_PATH = "infoTypeId";

    public static final String INFO_JOB_ID_PATH = "infoJobId";

    public static final String SUBSCRIPTION_ID_PATH = "subscriptionId";

    public static final String PERFORM_TYPE_CHECK_PARAM = "typeCheck";
    public static final String PERFORM_TYPE_CHECK_PARAM_DESCRIPTION =
        "when true, a validation of that the type exists and that the job matches the type schema.";

    public static final String INDIVIDUAL_TYPE_SUBSCRIPTION =
        "Individual subscription for information types (registration/deregistration)";

    public static final String TYPE_SUBSCRIPTION_DESCRIPTION =
        "This service operation is used to subscribe to notifications for changes in the availability of data types.";

    private ConsumerConsts() {
    }
}
