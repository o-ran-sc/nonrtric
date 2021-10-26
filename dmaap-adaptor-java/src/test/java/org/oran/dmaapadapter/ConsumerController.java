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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.oran.dmaapadapter.controllers.VoidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("ConsumerSimulatorController")
@Tag(name = "Test Consumer Simulator (exists only in test)")
public class ConsumerController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String CONSUMER_TARGET_URL = "/consumer";

    public static class TestResults {

        public List<String> receivedBodies = Collections.synchronizedList(new ArrayList<String>());

        public TestResults() {}

        public void reset() {
            receivedBodies.clear();
        }
    }

    final TestResults testResults = new TestResults();

    @PostMapping(path = CONSUMER_TARGET_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consume data", description = "The call is invoked to push data to consumer")
    @ApiResponses(value = { //
            @ApiResponse(responseCode = "200", description = "OK", //
                    content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
    })
    public ResponseEntity<Object> postData(@RequestBody String body) {
        logger.info("Received by consumer: {}", body);
        testResults.receivedBodies.add(body);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
