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

package org.oransc.ics.controllers.a1e;

public class A1eConsts {

    public static final String CONSUMER_API_NAME = "A1-EI (registration)";
    public static final String CONSUMER_API_DESCRIPTION = "Data consumer EI job registration";

    public static final String CONSUMER_API_CALLBACKS_NAME = "A1-EI (callbacks)";
    public static final String CONSUMER_API_CALLBACKS_DESCRIPTION = "Data consumer EI job status callbacks";

    public static final String API_ROOT = "/A1-EI/v1";
    public static final String OWNER_PARAM = "owner";
    public static final String OWNER_PARAM_DESCRIPTION = "selects EI jobs for one EI job owner";

    public static final String EI_TYPE_ID_PARAM = "eiTypeId";
    public static final String EI_TYPE_ID_PARAM_DESCRIPTION = "selects EI jobs of matching EI type";

    private A1eConsts() {
    }
}
