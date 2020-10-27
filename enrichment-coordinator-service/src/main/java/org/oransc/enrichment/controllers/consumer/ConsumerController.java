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

package org.oransc.enrichment.controllers.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.ArrayList;
import java.util.List;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.oransc.enrichment.clients.ProducerCallbacks;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.controllers.ErrorResponse;
import org.oransc.enrichment.controllers.VoidResponse;
import org.oransc.enrichment.exceptions.ServiceException;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiType;
import org.oransc.enrichment.repository.EiTypes;
import org.oransc.enrichment.repository.ImmutableEiJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SuppressWarnings("java:S3457") // No need to call "toString()" method as formatting and string ..
@RestController("ConsumerController")
@Api(tags = {ConsumerConsts.CONSUMER_API_NAME})
@RequestMapping(path = ConsumerConsts.API_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConsumerController {

    @Autowired
    ApplicationConfig applicationConfig;

    @Autowired
    private EiJobs eiJobs;

    @Autowired
    private EiTypes eiTypes;

    @Autowired
    ProducerCallbacks producerCallbacks;

    private static Gson gson = new GsonBuilder() //
        .create(); //

    @GetMapping(path = "/eitypes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "EI type identifiers", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                code = 200,
                message = "EI type identifiers",
                response = String.class,
                responseContainer = "List"), //
        })
    public ResponseEntity<Object> getEiTypeIdentifiers( //
    ) {
        List<String> result = new ArrayList<>();
        for (EiType eiType : this.eiTypes.getAllEiTypes()) {
            result.add(eiType.getId());
        }

        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

    @GetMapping(path = "/eitypes/{eiTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Individual EI type", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "EI type", response = ConsumerEiTypeInfo.class), //
            @ApiResponse(
                code = 404,
                message = "Enrichment Information type is not found",
                response = ErrorResponse.ErrorInfo.class)})
    public ResponseEntity<Object> getEiType( //
        @PathVariable("eiTypeId") String eiTypeId) {
        try {
            this.eiTypes.getType(eiTypeId); // Make sure that the type exists
            ConsumerEiTypeInfo info = toEiTypeInfo();
            return new ResponseEntity<>(gson.toJson(info), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(path = "/eijobs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "EI job identifiers", notes = "query for EI job identifiers")
    @ApiResponses(
        value = { //
            @ApiResponse(
                code = 200,
                message = "EI job identifiers",
                response = String.class,
                responseContainer = "List"), //
            @ApiResponse(
                code = 404,
                message = "Enrichment Information type is not found",
                response = ErrorResponse.ErrorInfo.class)})
    public ResponseEntity<Object> getEiJobIds( //
        @ApiParam(
            name = ConsumerConsts.EI_TYPE_ID_PARAM,
            required = false, //
            value = ConsumerConsts.EI_TYPE_ID_PARAM_DESCRIPTION) //
        @RequestParam(name = ConsumerConsts.EI_TYPE_ID_PARAM, required = false) String eiTypeId,
        @ApiParam(
            name = ConsumerConsts.OWNER_PARAM,
            required = false, //
            value = ConsumerConsts.OWNER_PARAM_DESCRIPTION) //
        @RequestParam(name = ConsumerConsts.OWNER_PARAM, required = false) String owner) {
        try {
            List<String> result = new ArrayList<>();
            if (owner != null) {
                for (EiJob job : this.eiJobs.getJobsForOwner(owner)) {
                    if (eiTypeId == null || job.type().getId().equals(eiTypeId)) {
                        result.add(job.id());
                    }
                }
            } else if (eiTypeId != null) {
                this.eiJobs.getJobsForType(eiTypeId).forEach(job -> result.add(job.id()));
            } else {
                this.eiJobs.getJobs().forEach(job -> result.add(job.id()));
            }
            return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
        } catch (

        Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(path = "/eijobs/{eiJobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Individual EI job", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "EI job", response = ConsumerEiJobInfo.class), //
            @ApiResponse(
                code = 404,
                message = "Enrichment Information job is not found",
                response = ErrorResponse.ErrorInfo.class)})
    public ResponseEntity<Object> getIndividualEiJob( //
        @PathVariable("eiJobId") String eiJobId) {
        try {
            EiJob job = this.eiJobs.getJob(eiJobId);
            return new ResponseEntity<>(gson.toJson(toEiJobInfo(job)), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(path = "/eijobs/{eiJobId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "EI job status", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "EI job status", response = ConsumerEiJobStatus.class), //
            @ApiResponse(
                code = 404,
                message = "Enrichment Information job is not found",
                response = ErrorResponse.ErrorInfo.class)})
    public ResponseEntity<Object> getEiJobStatus( //
        @PathVariable("eiJobId") String eiJobId) {
        try {
            EiJob job = this.eiJobs.getJob(eiJobId);
            return new ResponseEntity<>(gson.toJson(toEiJobStatus(job)), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private ConsumerEiJobStatus toEiJobStatus(EiJob job) {
        // TODO
        return new ConsumerEiJobStatus(ConsumerEiJobStatus.EiJobStatusValues.ENABLED);
    }

    @DeleteMapping(path = "/eijobs/{eiJobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Individual EI job", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "Not used", response = VoidResponse.class),
            @ApiResponse(code = 204, message = "Job deleted", response = VoidResponse.class),
            @ApiResponse(
                code = 404,
                message = "Enrichment Information job is not found",
                response = ErrorResponse.ErrorInfo.class)})
    public ResponseEntity<Object> deleteIndividualEiJob( //
        @PathVariable("eiJobId") String eiJobId) {
        try {
            EiJob job = this.eiJobs.getJob(eiJobId);
            this.eiJobs.remove(job);
            this.producerCallbacks.notifyProducersJobDeleted(job);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(
        path = "/eijobs/{eiJobId}", //
        produces = MediaType.APPLICATION_JSON_VALUE, //
        consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Individual EI job", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 201, message = "Job created", response = VoidResponse.class), //
            @ApiResponse(code = 200, message = "Job updated", response = VoidResponse.class), // ,
            @ApiResponse(
                code = 404,
                message = "Enrichment Information type is not found",
                response = ErrorResponse.ErrorInfo.class)})
    public Mono<ResponseEntity<Object>> putIndividualEiJob( //
        @PathVariable("eiJobId") String eiJobId, //
        @RequestBody ConsumerEiJobInfo eiJobObject) {

        final boolean isNewJob = this.eiJobs.get(eiJobId) == null;

        return validatePutEiJob(eiJobId, eiJobObject) //
            .flatMap(this::notifyProducersNewJob) //
            .doOnNext(newEiJob -> this.eiJobs.put(newEiJob)) //
            .flatMap(newEiJob -> Mono.just(new ResponseEntity<>(isNewJob ? HttpStatus.CREATED : HttpStatus.OK)))
            .onErrorResume(throwable -> Mono.just(ErrorResponse.create(throwable, HttpStatus.NOT_FOUND)));
    }

    private Mono<EiJob> notifyProducersNewJob(EiJob newEiJob) {
        return this.producerCallbacks.notifyProducersJobStarted(newEiJob) //
            .flatMap(noOfAcceptingProducers -> {
                if (noOfAcceptingProducers.intValue() > 0) {
                    return Mono.just(newEiJob);
                } else {
                    return Mono.error(new ServiceException("Job not accepted by any producers", HttpStatus.CONFLICT));
                }
            });
    }

    private Mono<EiJob> validatePutEiJob(String eiJobId, ConsumerEiJobInfo eiJobInfo) {
        try {
            EiType eiType = this.eiTypes.getType(eiJobInfo.eiTypeId);
            validateJsonObjectAgainstSchema(eiType.getJobDataSchema(), eiJobInfo.jobData);
            EiJob existingEiJob = this.eiJobs.get(eiJobId);

            if (existingEiJob != null && !existingEiJob.type().getId().equals(eiJobInfo.eiTypeId)) {
                throw new ServiceException("Not allowed to change type for existing EI job", HttpStatus.CONFLICT);
            }
            return Mono.just(toEiJob(eiJobInfo, eiJobId, eiType));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private void validateJsonObjectAgainstSchema(Object schemaObj, Object object) throws ServiceException {
        if (schemaObj != null) { // schema is optional for now
            try {
                ObjectMapper mapper = new ObjectMapper();

                String schemaAsString = mapper.writeValueAsString(schemaObj);
                JSONObject schemaJSON = new JSONObject(schemaAsString);
                Schema schema = SchemaLoader.load(schemaJSON);

                String objectAsString = mapper.writeValueAsString(object);
                JSONObject json = new JSONObject(objectAsString);
                schema.validate(json);
            } catch (Exception e) {
                throw new ServiceException("Json validation failure " + e.toString(), HttpStatus.CONFLICT);
            }
        }
    }

    // Status TBD

    private EiJob toEiJob(ConsumerEiJobInfo info, String id, EiType type) {
        return ImmutableEiJob.builder() //
            .id(id) //
            .type(type) //
            .owner(info.owner) //
            .jobData(info.jobData) //
            .targetUri(info.targetUri) //
            .build();
    }

    private ConsumerEiTypeInfo toEiTypeInfo() {
        return new ConsumerEiTypeInfo();
    }

    private ConsumerEiJobInfo toEiJobInfo(EiJob s) {
        return new ConsumerEiJobInfo(s.type().getId(), s.jobData(), s.owner(), s.targetUri());
    }
}
