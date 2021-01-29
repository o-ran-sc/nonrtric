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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.controllers.ErrorResponse;
import org.oransc.enrichment.controllers.VoidResponse;
import org.oransc.enrichment.controllers.producer.ProducerCallbacks;
import org.oransc.enrichment.exceptions.ServiceException;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiProducers;
import org.oransc.enrichment.repository.EiType;
import org.oransc.enrichment.repository.EiTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RestController("A1-EI")
@Tag(name = ConsumerConsts.CONSUMER_API_NAME)
@RequestMapping(path = ConsumerConsts.API_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConsumerController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    ApplicationConfig applicationConfig;

    @Autowired
    private EiJobs eiJobs;

    @Autowired
    private EiTypes eiTypes;

    @Autowired
    private EiProducers eiProducers;

    @Autowired
    ProducerCallbacks producerCallbacks;

    private static Gson gson = new GsonBuilder().create();

    @GetMapping(path = "/eitypes", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "EI type identifiers", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "EI type identifiers", //
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))), //
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
    @Operation(summary = "Individual EI type", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "EI type", //
                content = @Content(schema = @Schema(implementation = ConsumerEiTypeInfo.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Enrichment Information type is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
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
    @Operation(summary = "EI job identifiers", description = "query for EI job identifiers")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "EI job identifiers", //
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(
                responseCode = "404",
                description = "Enrichment Information type is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> getEiJobIds( //
        @Parameter(
            name = ConsumerConsts.EI_TYPE_ID_PARAM,
            required = false, //
            description = ConsumerConsts.EI_TYPE_ID_PARAM_DESCRIPTION) //
        @RequestParam(name = ConsumerConsts.EI_TYPE_ID_PARAM, required = false) String eiTypeId,
        @Parameter(
            name = ConsumerConsts.OWNER_PARAM,
            required = false, //
            description = ConsumerConsts.OWNER_PARAM_DESCRIPTION) //
        @RequestParam(name = ConsumerConsts.OWNER_PARAM, required = false) String owner) {
        try {
            List<String> result = new ArrayList<>();
            if (owner != null) {
                for (EiJob job : this.eiJobs.getJobsForOwner(owner)) {
                    if (eiTypeId == null || job.getTypeId().equals(eiTypeId)) {
                        result.add(job.getId());
                    }
                }
            } else if (eiTypeId != null) {
                this.eiJobs.getJobsForType(eiTypeId).forEach(job -> result.add(job.getId()));
            } else {
                this.eiJobs.getJobs().forEach(job -> result.add(job.getId()));
            }
            return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
        } catch (

        Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(path = "/eijobs/{eiJobId}", produces = MediaType.APPLICATION_JSON_VALUE) //
    @Operation(summary = "Individual EI job", description = "") //
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "EI job", //
                content = @Content(schema = @Schema(implementation = ConsumerEiJobInfo.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Enrichment Information job is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
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
    @Operation(summary = "EI job status", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "EI job status", //
                content = @Content(schema = @Schema(implementation = ConsumerEiJobStatus.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Enrichment Information job is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
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
        return this.eiProducers.isJobEnabled(job)
            ? new ConsumerEiJobStatus(ConsumerEiJobStatus.EiJobStatusValues.ENABLED)
            : new ConsumerEiJobStatus(ConsumerEiJobStatus.EiJobStatusValues.DISABLED);

    }

    @DeleteMapping(path = "/eijobs/{eiJobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Individual EI job", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Not used", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))),
            @ApiResponse(
                responseCode = "204",
                description = "Job deleted", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Enrichment Information job is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> deleteIndividualEiJob( //
        @PathVariable("eiJobId") String eiJobId) {
        try {
            EiJob job = this.eiJobs.getJob(eiJobId);
            this.eiJobs.remove(job, this.eiProducers);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(
        path = "/eijobs/{eiJobId}", //
        produces = MediaType.APPLICATION_JSON_VALUE, //
        consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Individual EI job", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "201",
                description = "Job created", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), //
            @ApiResponse(
                responseCode = "200",
                description = "Job updated", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Enrichment Information type is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public Mono<ResponseEntity<Object>> putIndividualEiJob( //
        @PathVariable("eiJobId") String eiJobId, //
        @RequestBody ConsumerEiJobInfo eiJobObject) {

        final boolean isNewJob = this.eiJobs.get(eiJobId) == null;

        return validatePutEiJob(eiJobId, eiJobObject) //
            .flatMap(this::startEiJob) //
            .doOnNext(newEiJob -> this.eiJobs.put(newEiJob)) //
            .flatMap(newEiJob -> Mono.just(new ResponseEntity<>(isNewJob ? HttpStatus.CREATED : HttpStatus.OK)))
            .onErrorResume(throwable -> Mono.just(ErrorResponse.create(throwable, HttpStatus.NOT_FOUND)));
    }

    private Mono<EiJob> startEiJob(EiJob newEiJob) {
        return this.producerCallbacks.startEiJob(newEiJob, eiProducers) //
            .doOnNext(noOfAcceptingProducers -> this.logger.debug(
                "Started EI job {}, number of activated producers: {}", newEiJob.getId(), noOfAcceptingProducers)) //
            .flatMap(noOfAcceptingProducers -> Mono.just(newEiJob));
    }

    private Mono<EiJob> validatePutEiJob(String eiJobId, ConsumerEiJobInfo eiJobInfo) {
        try {
            EiType eiType = this.eiTypes.getType(eiJobInfo.eiTypeId);
            validateJsonObjectAgainstSchema(eiType.getJobDataSchema(), eiJobInfo.jobDefinition);
            EiJob existingEiJob = this.eiJobs.get(eiJobId);

            if (existingEiJob != null && !existingEiJob.getTypeId().equals(eiJobInfo.eiTypeId)) {
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
                org.everit.json.schema.Schema schema = org.everit.json.schema.loader.SchemaLoader.load(schemaJSON);

                String objectAsString = mapper.writeValueAsString(object);
                JSONObject json = new JSONObject(objectAsString);
                schema.validate(json);
            } catch (Exception e) {
                throw new ServiceException("Json validation failure " + e.toString(), HttpStatus.CONFLICT);
            }
        }
    }

    private EiJob toEiJob(ConsumerEiJobInfo info, String id, EiType type) {
        return EiJob.builder() //
            .id(id) //
            .typeId(type.getId()) //
            .owner(info.owner) //
            .jobData(info.jobDefinition) //
            .targetUrl(info.jobResultUri) //
            .jobStatusUrl(info.statusNotificationUri == null ? "" : info.statusNotificationUri) //
            .build();
    }

    private ConsumerEiTypeInfo toEiTypeInfo() {
        return new ConsumerEiTypeInfo();
    }

    private ConsumerEiJobInfo toEiJobInfo(EiJob s) {
        return new ConsumerEiJobInfo(s.getTypeId(), s.getJobData(), s.getOwner(), s.getTargetUrl(),
            s.getJobStatusUrl());
    }
}
