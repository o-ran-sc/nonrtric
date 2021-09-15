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

package org.oran.dmaapadapter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.oran.dmaapadapter.controllers.ErrorResponse;
import org.oran.dmaapadapter.controllers.VoidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("ProducerSimulatorController")
@Tag(name = "ProducerConsts.PRODUCER_API_CALLBACKS_NAME")
public class DmaapSimulatorController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String DMAAP_TOPIC_URL = "/dmaap-topic-1";

    public static List<String> dmaapResponses = Collections.synchronizedList(new LinkedList<String>());

    @GetMapping(path = DMAAP_TOPIC_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "GET from topic",
            description = "The call is invoked to activate or to modify a data subscription. The endpoint is provided by the Information Producer.")
    @ApiResponses(value = { //
            @ApiResponse(responseCode = "200", description = "OK", //
                    content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
    })
    public ResponseEntity<Object> getFromTopic() {
        if (dmaapResponses.isEmpty()) {
            return ErrorResponse.create("", HttpStatus.NOT_FOUND);
        } else {
            String resp = dmaapResponses.remove(0);
            return new ResponseEntity<>(resp, HttpStatus.OK);
        }

    }

}
