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

package org.oransc.enrichment;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.oransc.enrichment.controllers.StatusController;
import org.oransc.enrichment.controllers.consumer.ConsumerConsts;
import org.oransc.enrichment.controllers.producer.ProducerConsts;

/**
 * Swagger configuration class that uses swagger documentation type and scans
 * all the controllers. To access the swagger gui go to
 * http://ip:port/swagger-ui.html
 */

@OpenAPIDefinition( //
    tags = {@Tag(name = ConsumerConsts.CONSUMER_API_NAME, description = ConsumerConsts.CONSUMER_API_DESCRIPTION),
        @Tag(
            name = ConsumerConsts.CONSUMER_API_CALLBACKS_NAME,
            description = ConsumerConsts.CONSUMER_API_CALLBACKS_DESCRIPTION),
        @Tag(
            name = ProducerConsts.PRODUCER_API_CALLBACKS_NAME,
            description = ProducerConsts.PRODUCER_API_CALLBACKS_DESCRIPTION),
        @Tag(name = ProducerConsts.PRODUCER_API_NAME, description = ProducerConsts.PRODUCER_API_DESCRIPTION), //
        @Tag(name = StatusController.API_NAME, description = StatusController.API_DESCRIPTION)}, //
    info = @Info(
        title = "Enrichment Information Service", //
        version = "1.0", //
        description = SwaggerConfig.DESCRIPTION, //
        license = @License(
            name = "Copyright (C) 2020 Nordix Foundation. Licensed under the Apache License.",
            url = "http://www.apache.org/licenses/LICENSE-2.0")))
public class SwaggerConfig {
    private SwaggerConfig() {
    }

    static final String API_TITLE = "Enrichment Data service";

    static final String DESCRIPTION = "<h1>API documentation</h1>" //
        + "<h2>General</h2>" //
        + "<p>" //
        + "  The service is mainly a broker between data producers and data consumers. A data producer has the ability to producer one or several type of data (EI type). One type of data can be produced by zero to many producers. <br /><br />A data consumer can have several active data subscriptions (EI job). One EI job consists of the type of data to produce and additional parameters for filtering of the data. These parameters are different for different data types." //
        + "</p>" //
        + "<h2>APIs provided by the service</h2>" //
        + "<h4>A1-EI</h4>" //
        + "<p>" //
        + "  This API is between Near-RT RIC, which is a data consumer, and the Non-RT RIC. " //
        + "</p>" + "<h4>Data producer API</h4>" //
        + "<p>"
        + "  This API is between data producers and this service. It is divivided into two parts, where one is provided by this service (registration) and one part is provided by the data producer."
        + "</p>" //
        + "<h4>EI Service status</h4>" //
        + "<p>" //
        + "  This API provides a means to monitor the service." //
        + "</p>";

}
