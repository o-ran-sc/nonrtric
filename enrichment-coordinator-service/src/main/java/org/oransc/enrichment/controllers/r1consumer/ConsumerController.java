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

package org.oransc.enrichment.controllers.r1consumer;

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
import org.oransc.enrichment.controllers.r1producer.ProducerCallbacks;
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
@RestController("Consumer registry")
@Tag(name = ConsumerConsts.CONSUMER_API_NAME)
@RequestMapping(path = ConsumerConsts.API_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConsumerController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    ApplicationConfig applicationConfig;

    @Autowired
    private EiJobs jobs;

    @Autowired
    private EiTypes infoTypes;

    @Autowired
    private EiProducers infoProducers;

    @Autowired
    ProducerCallbacks producerCallbacks;

    private static Gson gson = new GsonBuilder().create();

    @GetMapping(path = "/info-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Information type identifiers", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Information type identifiers", //
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))), //
        })
    public ResponseEntity<Object> getinfoTypeIdentifiers( //
    ) {
        List<String> result = new ArrayList<>();
        for (EiType infoType : this.infoTypes.getAllInfoTypes()) {
            result.add(infoType.getId());
        }

        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

    @GetMapping(path = "/info-types/{infoTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Individual information type", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Information type", //
                content = @Content(schema = @Schema(implementation = ConsumerInfoTypeInfo.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Information type is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> getInfoType( //
        @PathVariable("infoTypeId") String infoTypeId) {
        try {
            EiType type = this.infoTypes.getType(infoTypeId);
            ConsumerInfoTypeInfo info = toInfoTypeInfo(type);
            return new ResponseEntity<>(gson.toJson(info), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(path = "/info-jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "EI job identifiers", description = "query for information job identifiers")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Information information job identifiers", //
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(
                responseCode = "404",
                description = "Information type is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> getJobIds( //
        @Parameter(
            name = ConsumerConsts.INFO_TYPE_ID_PARAM,
            required = false, //
            description = ConsumerConsts.INFO_TYPE_ID_PARAM_DESCRIPTION) //
        @RequestParam(name = ConsumerConsts.INFO_TYPE_ID_PARAM, required = false) String infoTypeId,
        @Parameter(
            name = ConsumerConsts.OWNER_PARAM,
            required = false, //
            description = ConsumerConsts.OWNER_PARAM_DESCRIPTION) //
        @RequestParam(name = ConsumerConsts.OWNER_PARAM, required = false) String owner) {
        try {
            List<String> result = new ArrayList<>();
            if (owner != null) {
                for (EiJob job : this.jobs.getJobsForOwner(owner)) {
                    if (infoTypeId == null || job.getTypeId().equals(infoTypeId)) {
                        result.add(job.getId());
                    }
                }
            } else if (infoTypeId != null) {
                this.jobs.getJobsForType(infoTypeId).forEach(job -> result.add(job.getId()));
            } else {
                this.jobs.getJobs().forEach(job -> result.add(job.getId()));
            }
            return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
        } catch (

        Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(path = "/info-jobs/{infoJobId}", produces = MediaType.APPLICATION_JSON_VALUE) //
    @Operation(summary = ConsumerConsts.INDIVIDUAL_JOB, description = "") //
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Information subscription job", //
                content = @Content(schema = @Schema(implementation = ConsumerJobInfo.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Information subscription job is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> getIndividualEiJob( //
        @PathVariable("infoJobId") String infoJobId) {
        try {
            EiJob job = this.jobs.getJob(infoJobId);
            return new ResponseEntity<>(gson.toJson(toInfoJobInfo(job)), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(path = "/info-jobs/{infoJobId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Job status", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Information subscription job status", //
                content = @Content(schema = @Schema(implementation = ConsumerJobStatus.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Information subscription job is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> getEiJobStatus( //
        @PathVariable("infoJobId") String jobId) {
        try {
            EiJob job = this.jobs.getJob(jobId);
            return new ResponseEntity<>(gson.toJson(toInfoJobStatus(job)), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private ConsumerJobStatus toInfoJobStatus(EiJob job) {
        return this.infoProducers.isJobEnabled(job)
            ? new ConsumerJobStatus(ConsumerJobStatus.InfoJobStatusValues.ENABLED)
            : new ConsumerJobStatus(ConsumerJobStatus.InfoJobStatusValues.DISABLED);

    }

    @DeleteMapping(path = "/info-jobs/{infoJobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = ConsumerConsts.INDIVIDUAL_JOB, description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Not used", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))),
            @ApiResponse(
                responseCode = "204",
                description = "Job deleted", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), // "Individual EI job"
            @ApiResponse(
                responseCode = "404",
                description = "Information subscription job is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> deleteIndividualEiJob( //
        @PathVariable("infoJobId") String jobId) {
        try {
            EiJob job = this.jobs.getJob(jobId);
            this.jobs.remove(job, this.infoProducers);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(
        path = "/info-jobs/{infoJobId}", //
        produces = MediaType.APPLICATION_JSON_VALUE, //
        consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = ConsumerConsts.INDIVIDUAL_JOB, description = ConsumerConsts.PUT_INDIVIDUAL_JOB_DESCRIPTION)
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
    public Mono<ResponseEntity<Object>> putIndividualInfoJob( //
        @PathVariable("infoJobId") String jobId, //
        @Parameter(
            name = ConsumerConsts.PERFORM_TYPE_CHECK_PARAM,
            required = false, //
            description = ConsumerConsts.PERFORM_TYPE_CHECK_PARAM_DESCRIPTION) //
        @RequestParam(
            name = ConsumerConsts.PERFORM_TYPE_CHECK_PARAM,
            required = false,
            defaultValue = "false") boolean performTypeCheck,
        @RequestBody ConsumerJobInfo informationJobObject) {

        final boolean isNewJob = this.jobs.get(jobId) == null;

        return validatePutInfoJob(jobId, informationJobObject, performTypeCheck) //
            .flatMap(this::startInfoSubscriptionJob) //
            .doOnNext(newEiJob -> this.jobs.put(newEiJob)) //
            .flatMap(newEiJob -> Mono.just(new ResponseEntity<>(isNewJob ? HttpStatus.CREATED : HttpStatus.OK)))
            .onErrorResume(throwable -> Mono.just(ErrorResponse.create(throwable, HttpStatus.NOT_FOUND)));
    }

    private Mono<EiJob> startInfoSubscriptionJob(EiJob newInfoJob) {
        return this.producerCallbacks.startInfoSubscriptionJob(newInfoJob, infoProducers) //
            .doOnNext(noOfAcceptingProducers -> this.logger.debug("Started job {}, number of activated producers: {}",
                newInfoJob.getId(), noOfAcceptingProducers)) //
            .flatMap(noOfAcceptingProducers -> Mono.just(newInfoJob));
    }

    private Mono<EiJob> validatePutInfoJob(String jobId, ConsumerJobInfo jobInfo, boolean performTypeCheck) {
        try {
            if (performTypeCheck) {
                EiType infoType = this.infoTypes.getType(jobInfo.infoTypeId);
                validateJsonObjectAgainstSchema(infoType.getJobDataSchema(), jobInfo.jobDefinition);
            }
            EiJob existingEiJob = this.jobs.get(jobId);

            if (existingEiJob != null && !existingEiJob.getTypeId().equals(jobInfo.infoTypeId)) {
                throw new ServiceException("Not allowed to change type for existing job", HttpStatus.CONFLICT);
            }
            return Mono.just(toEiJob(jobInfo, jobId, jobInfo.infoTypeId));
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

    private EiJob toEiJob(ConsumerJobInfo info, String id, String typeId) {
        return EiJob.builder() //
            .id(id) //
            .typeId(typeId) //
            .owner(info.owner) //
            .jobData(info.jobDefinition) //
            .targetUrl(info.jobResultUri) //
            .jobStatusUrl(info.statusNotificationUri == null ? "" : info.statusNotificationUri) //
            .build();
    }

    private EiJob toEiJob(ConsumerJobInfo info, String id, EiType type) {
        return toEiJob(info, id, type.getId());
    }

    private ConsumerInfoTypeInfo toInfoTypeInfo(EiType type) {
        return new ConsumerInfoTypeInfo(type.getJobDataSchema());
    }

    private ConsumerJobInfo toInfoJobInfo(EiJob s) {
        return new ConsumerJobInfo(s.getTypeId(), s.getJobData(), s.getOwner(), s.getTargetUrl(), s.getJobStatusUrl());
    }
}
