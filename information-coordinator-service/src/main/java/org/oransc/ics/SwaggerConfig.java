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

package org.oransc.ics;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

/**
 * Swagger configuration class that uses swagger documentation type and scans
 * all the controllers. To access the swagger gui go to
 * http://ip:port/swagger-ui.html
 */

@OpenAPIDefinition( //
    info = @Info(
        title = SwaggerConfig.API_TITLE, //
        version = "1.0", //
        description = SwaggerConfig.DESCRIPTION, //
        license = @License(
            name = "Copyright (C) 2020-2022 Nordix Foundation. Licensed under the Apache License.",
            url = "http://www.apache.org/licenses/LICENSE-2.0")))
public class SwaggerConfig {
    private SwaggerConfig() {
    }

    static final String API_TITLE = "Data management and exposure";

    static final String DESCRIPTION = "<h1>API documentation</h1>" //
        + "<h2>General</h2>" //
        + "<p>" //
        + "  The service is mainly a broker between data producers and data consumers. A data producer has the ability to produce one or several types of data (Information Type). One type of data can be produced by zero to many producers. <br /><br />A data consumer can have several active data subscriptions (Information Job). One Information Job consists of the type of data to produce and additional parameters for filtering of the data. These parameters are different for different data types." //
        + "</p>" //
        + "<h2>APIs provided by the service</h2>" //
        + "<h4>A1-EI</h4>" //
        + "<p>" //
        + "  This API is between Near-RT RIC and the Non-RT RIC." //
        + "  The Near-RT RIC is a data consumer, which creates Information Jobs to subscribe for data." //
        + "  In this context, the information is referred to as 'Enrichment Information', EI." //
        + "</p>" //
        + "<h4>Data producer API</h4>" //
        + "<p>" //
        + "  This API is provided by the Non-RT RIC platform and is intended to be part of the O-RAN R1 interface." //
        + "  The API is for use by different kinds of data producers and provides support for:" //
        + "<ul>" //
        + "<li>Registry of supported information types and which parameters needed to setup a subscription.</li>" //
        + "<li>Registry of existing data producers.</li>" //
        + "<li>Callback API provided by producers to setup subscriptions.</li>" //
        + "</ul>" //
        + "</p>" //
        + "<h4>Data consumer API</h4>" //
        + "<p>" //
        + "  This API is provided by the Non-RT RIC platform and is intended to be part of the O-RAN R1 interface." //
        + "  The API is for use by different kinds of data consumers and provides support for:" //
        + "<ul>" //
        + "<li>Querying of available types of data to consume.</li>" //
        + "<li>Management of data subscription jobs</li>" //
        + "<li>Optional callback API provided by consumers to get notification on added and removed information types.</li>" //
        + "</ul>" //
        + "</p>" //
        + "<h4>Service status</h4>" //
        + "<p>" //
        + "  This API provides a means to monitor the health of this service." //
        + "</p>";

}
