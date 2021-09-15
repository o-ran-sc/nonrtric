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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.oran.dmaapadapter.clients.AsyncRestClient;
import org.oran.dmaapadapter.r1.ConsumerJobInfo;
import org.oran.dmaapadapter.r1.ProducerInfoTypeInfo;
import org.oran.dmaapadapter.r1.ProducerJobInfo;
import org.oran.dmaapadapter.r1.ProducerRegistrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("EcsSimulatorController")
@Tag(name = "EcsSimulator")
public class EcsSimulatorController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final static Gson gson = new GsonBuilder().create();

    public static class TestResults {

        ProducerRegistrationInfo registrationInfo;
        Map<String, ProducerInfoTypeInfo> types = new HashMap<>();

        public TestResults() {}

        public void reset() {
            registrationInfo = null;
            types.clear();
        }
    }

    final TestResults testResults = new TestResults();
    public static final String API_ROOT = "/data-producer/v1";

    @GetMapping(path = API_ROOT + "/info-producers/{infoProducerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getInfoProducer( //
            @PathVariable("infoProducerId") String infoProducerId) {

        if (testResults.registrationInfo != null) {
            return new ResponseEntity<>(gson.toJson(testResults.registrationInfo), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

    }

    @PutMapping(path = API_ROOT + "/info-producers/{infoProducerId}", //
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> putInfoProducer( //
            @PathVariable("infoProducerId") String infoProducerId, //
            @RequestBody ProducerRegistrationInfo registrationInfo) {
        testResults.registrationInfo = registrationInfo;
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping(path = API_ROOT + "/info-types/{infoTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> putInfoType( //
            @PathVariable("infoTypeId") String infoTypeId, //
            @RequestBody ProducerInfoTypeInfo registrationInfo) {
        testResults.types.put(infoTypeId, registrationInfo);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public void addJob(ConsumerJobInfo job, AsyncRestClient restClient) {
        String url = this.testResults.registrationInfo.jobCallbackUrl;
        ProducerJobInfo request =
                new ProducerJobInfo(job.jobDefinition, "ID", job.infoTypeId, job.jobResultUri, job.owner, "TIMESTAMP");
        String body = gson.toJson(request);
        restClient.post(url, body).block();

    }
}
