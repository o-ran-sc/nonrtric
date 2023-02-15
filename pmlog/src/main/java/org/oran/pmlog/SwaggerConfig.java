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

package org.oran.pmlog;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

/**
 * Swagger configuration class that uses swagger documentation type and scans
 * all the controllers. To access the swagger gui go to
 * http://ip:port/swagger-ui.html
 */
@OpenAPIDefinition( //
        info = @Info(title = SwaggerConfig.API_TITLE, //
                version = "1.0", //
                description = SwaggerConfig.DESCRIPTION, //
                license = @License(name = "Copyright (C) 2023 Nordix Foundation. Licensed under the Apache License.",
                        url = "http://www.apache.org/licenses/LICENSE-2.0")))
public class SwaggerConfig {
    private SwaggerConfig() {}

    static final String API_TITLE = "PM Logger";
    static final String DESCRIPTION =
            "Receives PM reports from a Kafka topic and writes the PM counters into an Influx database.";
}
