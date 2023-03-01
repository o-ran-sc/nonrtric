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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.invoke.MethodHandles;

import org.oran.pmlog.configuration.ConsumerJobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("IcsSimulatorController")
@Tag(name = "Information Coordinator Service Simulator (exists only in test)")
public class IcsSimulatorController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public static class TestResults {

        ConsumerJobInfo createdJob = null;

        public TestResults() {}

        public synchronized void reset() {
            createdJob = null;
        }

        public void setCreatedJob(ConsumerJobInfo informationJobObject) {
            this.createdJob = informationJobObject;
        }
    }

    final TestResults testResults = new TestResults();

    @PutMapping(path = "/data-consumer/v1/info-jobs/{infoJobId}", //
            produces = MediaType.APPLICATION_JSON_VALUE, //
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> putIndividualInfoJob( //
            @PathVariable("infoJobId") String jobId, //
            @RequestBody String body) {
        logger.debug("*** added consumer job {}", jobId);
        try {
            ConsumerJobInfo informationJobObject = gson.fromJson(body, ConsumerJobInfo.class);
            testResults.setCreatedJob(informationJobObject);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Received malformed data: {}, {}", body, e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
