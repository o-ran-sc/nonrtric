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

package org.oransc.ics.controllers.r1consumer;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONObject;
import org.oransc.ics.controllers.ErrorResponse;
import org.oransc.ics.controllers.VoidResponse;
import org.oransc.ics.controllers.r1producer.ProducerCallbacks;
import org.oransc.ics.exceptions.ServiceException;
import org.oransc.ics.repository.InfoJob;
import org.oransc.ics.repository.InfoJobs;
import org.oransc.ics.repository.InfoProducer;
import org.oransc.ics.repository.InfoProducers;
import org.oransc.ics.repository.InfoType;
import org.oransc.ics.repository.InfoTypeSubscriptions;
import org.oransc.ics.repository.InfoTypes;
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
@RestController("Consumer API")
@Tag(name = ConsumerConsts.CONSUMER_API_NAME, description = ConsumerConsts.CONSUMER_API_DESCRIPTION)
@RequestMapping(path = ConsumerConsts.API_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConsumerController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final InfoJobs infoJobs;
    private final InfoTypes infoTypes;
    private final InfoProducers infoProducers;
    private final ProducerCallbacks producerCallbacks;
    private final InfoTypeSubscriptions infoTypeSubscriptions;
    private static Gson gson = new GsonBuilder().create();

    public ConsumerController(@Autowired InfoJobs jobs, @Autowired InfoTypes infoTypes,
        @Autowired InfoProducers infoProducers, @Autowired ProducerCallbacks producerCallbacks,
        @Autowired InfoTypeSubscriptions infoTypeSubscriptions) {
        this.infoProducers = infoProducers;
        this.infoJobs = jobs;
        this.infoTypeSubscriptions = infoTypeSubscriptions;
        this.infoTypes = infoTypes;
        this.producerCallbacks = producerCallbacks;
    }

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
        for (InfoType infoType : this.infoTypes.getAllInfoTypes()) {
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
        @PathVariable(ConsumerConsts.INFO_TYPE_ID_PATH) String infoTypeId) {
        try {
            InfoType type = this.infoTypes.getType(infoTypeId);
            ConsumerInfoTypeInfo info = toInfoTypeInfo(type);
            return new ResponseEntity<>(gson.toJson(info), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(path = "/info-jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Information Job identifiers", description = "query for information job identifiers")
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
                for (InfoJob job : this.infoJobs.getJobsForOwner(owner)) {
                    if (infoTypeId == null || job.getTypeId().equals(infoTypeId)) {
                        result.add(job.getId());
                    }
                }
            } else if (infoTypeId != null) {
                this.infoJobs.getJobsForType(infoTypeId).forEach(job -> result.add(job.getId()));
            } else {
                this.infoJobs.getJobs().forEach(job -> result.add(job.getId()));
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
        @PathVariable(ConsumerConsts.INFO_JOB_ID_PATH) String infoJobId) {
        try {
            InfoJob job = this.infoJobs.getJob(infoJobId);
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
        @PathVariable(ConsumerConsts.INFO_JOB_ID_PATH) String jobId) {
        try {
            InfoJob job = this.infoJobs.getJob(jobId);
            return new ResponseEntity<>(gson.toJson(toInfoJobStatus(job)), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private ConsumerJobStatus toInfoJobStatus(InfoJob job) {
        Collection<String> producerIds = new ArrayList<>();
        this.infoProducers.getProducersForType(job.getTypeId()).forEach(producer -> producerIds.add(producer.getId()));
        return this.infoProducers.isJobEnabled(job)
            ? new ConsumerJobStatus(ConsumerJobStatus.InfoJobStatusValues.ENABLED, producerIds)
            : new ConsumerJobStatus(ConsumerJobStatus.InfoJobStatusValues.DISABLED, producerIds);

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
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), // "Individual
                                                                                            // Information Job"
            @ApiResponse(
                responseCode = "404",
                description = "Information subscription job is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> deleteIndividualEiJob( //
        @PathVariable(ConsumerConsts.INFO_JOB_ID_PATH) String jobId) {
        try {
            InfoJob job = this.infoJobs.getJob(jobId);
            this.infoJobs.remove(job, this.infoProducers);
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
                description = "Information type is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))), //
            @ApiResponse(
                responseCode = "400",
                description = "Input validation failed", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))), //
            @ApiResponse(
                responseCode = "409",
                description = "Cannot modify job type", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class)))})
    public Mono<ResponseEntity<Object>> putIndividualInfoJob( //
        @PathVariable(ConsumerConsts.INFO_JOB_ID_PATH) String jobId, //
        @Parameter(
            name = ConsumerConsts.PERFORM_TYPE_CHECK_PARAM,
            required = false, //
            description = ConsumerConsts.PERFORM_TYPE_CHECK_PARAM_DESCRIPTION) //
        @RequestParam(
            name = ConsumerConsts.PERFORM_TYPE_CHECK_PARAM,
            required = false,
            defaultValue = "false") boolean performTypeCheck,
        @RequestBody ConsumerJobInfo informationJobObject) {

        final boolean isNewJob = this.infoJobs.get(jobId) == null;

        return validatePutInfoJob(jobId, informationJobObject, performTypeCheck) //
            .flatMap(this::startInfoSubscriptionJob) //
            .doOnNext(this.infoJobs::put) //
            .map(newEiJob -> new ResponseEntity<>(isNewJob ? HttpStatus.CREATED : HttpStatus.OK)) //
            .onErrorResume(throwable -> Mono.just(ErrorResponse.create(throwable, HttpStatus.NOT_FOUND)));
    }

    @GetMapping(path = "/info-type-subscription", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Information type subscription identifiers",
        description = "query for information type subscription identifiers")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Information type subscription identifiers", //
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),})
    public ResponseEntity<Object> getInfoTypeSubscriptions( //

        @Parameter(
            name = ConsumerConsts.OWNER_PARAM,
            required = false, //
            description = ConsumerConsts.OWNER_PARAM_DESCRIPTION) //
        @RequestParam(name = ConsumerConsts.OWNER_PARAM, required = false) String owner) {
        try {
            List<String> result = new ArrayList<>();
            if (owner != null) {
                this.infoTypeSubscriptions.getSubscriptionsForOwner(owner)
                    .forEach(subscription -> result.add(subscription.getId()));
            } else {
                this.infoTypeSubscriptions.getAllSubscriptions()
                    .forEach(subscription -> result.add(subscription.getId()));
            }
            return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(path = "/info-type-subscription/{subscriptionId}", produces = MediaType.APPLICATION_JSON_VALUE) //
    @Operation(summary = ConsumerConsts.INDIVIDUAL_TYPE_SUBSCRIPTION, description = "") //
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Type subscription", //
                content = @Content(schema = @Schema(implementation = ConsumerTypeSubscriptionInfo.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Subscription is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> getIndividualTypeSubscription( //
        @PathVariable(ConsumerConsts.SUBSCRIPTION_ID_PATH) String subscriptionId) {
        try {
            InfoTypeSubscriptions.SubscriptionInfo subscription =
                this.infoTypeSubscriptions.getSubscription(subscriptionId);
            return new ResponseEntity<>(gson.toJson(toTypeSuscriptionInfo(subscription)), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(
        path = "/info-type-subscription/{subscriptionId}", //
        produces = MediaType.APPLICATION_JSON_VALUE, //
        consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = ConsumerConsts.INDIVIDUAL_TYPE_SUBSCRIPTION,
        description = ConsumerConsts.TYPE_SUBSCRIPTION_DESCRIPTION)
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "201",
                description = "Subscription created", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), //
            @ApiResponse(
                responseCode = "200",
                description = "Subscription updated", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
        })
    public Mono<ResponseEntity<Object>> putIndividualTypeSubscription( //
        @PathVariable(ConsumerConsts.SUBSCRIPTION_ID_PATH) String subscriptionId, //
        @RequestBody ConsumerTypeSubscriptionInfo subscription) {

        final boolean isNewSubscription = this.infoTypeSubscriptions.get(subscriptionId) == null;
        this.infoTypeSubscriptions.put(toTypeSuscriptionInfo(subscription, subscriptionId));
        return Mono.just(new ResponseEntity<>(isNewSubscription ? HttpStatus.CREATED : HttpStatus.OK));
    }

    @DeleteMapping(path = "/info-type-subscription/{subscriptionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = ConsumerConsts.INDIVIDUAL_TYPE_SUBSCRIPTION, description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Not used", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))),
            @ApiResponse(
                responseCode = "204",
                description = "Subscription deleted", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Subscription is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> deleteIndividualTypeSubscription( //
        @PathVariable(ConsumerConsts.SUBSCRIPTION_ID_PATH) String subscriptionId) {
        try {
            InfoTypeSubscriptions.SubscriptionInfo subscription =
                this.infoTypeSubscriptions.getSubscription(subscriptionId);
            this.infoTypeSubscriptions.remove(subscription);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private ConsumerTypeSubscriptionInfo toTypeSuscriptionInfo(InfoTypeSubscriptions.SubscriptionInfo s) {
        return new ConsumerTypeSubscriptionInfo(s.getCallbackUrl(), s.getOwner());
    }

    private InfoTypeSubscriptions.SubscriptionInfo toTypeSuscriptionInfo(ConsumerTypeSubscriptionInfo s,
        String subscriptionId) {
        return InfoTypeSubscriptions.SubscriptionInfo.builder() //
            .apiVersion(ConsumerCallbacks.API_VERSION) //
            .owner(s.owner) //
            .id(subscriptionId) //
            .callbackUrl(s.statusResultUri).build();
    }

    private Mono<InfoJob> startInfoSubscriptionJob(InfoJob newInfoJob) {
        return this.producerCallbacks.startInfoSubscriptionJob(newInfoJob, infoProducers) //
            .doOnNext(noOfAcceptingProducers -> this.logger.debug("Started job {}, number of activated producers: {}",
                newInfoJob.getId(), noOfAcceptingProducers)) //
            .map(noOfAcceptingProducers -> newInfoJob);
    }

    private Mono<InfoJob> validatePutInfoJob(String jobId, ConsumerJobInfo jobInfo, boolean performTypeCheck) {
        try {
            if (performTypeCheck) {
                InfoType infoType = this.infoTypes.getType(jobInfo.infoTypeId);
                validateJsonObjectAgainstSchema(infoType.getJobDataSchema(), jobInfo.jobDefinition);
            }
            InfoJob existingEiJob = this.infoJobs.get(jobId);
            validateUri(jobInfo.statusNotificationUri);
            validateUri(jobInfo.jobResultUri);

            if (existingEiJob != null && !existingEiJob.getTypeId().equals(jobInfo.infoTypeId)) {
                throw new ServiceException("Not allowed to change type for existing job", HttpStatus.CONFLICT);
            }
            return Mono.just(toEiJob(jobInfo, jobId, jobInfo.infoTypeId));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private void validateUri(String url) throws URISyntaxException, ServiceException {
        if (url != null && !url.isEmpty()) {
            URI uri = new URI(url);
            if (!uri.isAbsolute()) {
                throw new ServiceException("URI: " + url + " is not absolute", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void validateJsonObjectAgainstSchema(Object schemaObj, Object object) throws ServiceException {
        if (schemaObj != null) { // schema is optional for now
            try {
                ObjectMapper mapper = new ObjectMapper();

                String schemaAsString = mapper.writeValueAsString(schemaObj);
                JSONObject schemaJSON = new JSONObject(schemaAsString);
                var schema = org.everit.json.schema.loader.SchemaLoader.load(schemaJSON);

                String objectAsString = mapper.writeValueAsString(object);
                JSONObject json = new JSONObject(objectAsString);
                schema.validate(json);
            } catch (Exception e) {
                throw new ServiceException("Json validation failure " + e.toString(), HttpStatus.BAD_REQUEST);
            }
        }
    }

    private InfoJob toEiJob(ConsumerJobInfo info, String id, String typeId) {
        return InfoJob.builder() //
            .id(id) //
            .typeId(typeId) //
            .owner(info.owner) //
            .jobData(info.jobDefinition) //
            .targetUrl(info.jobResultUri) //
            .jobStatusUrl(info.statusNotificationUri == null ? "" : info.statusNotificationUri) //
            .build();
    }

    private ConsumerInfoTypeInfo toInfoTypeInfo(InfoType type) {
        return new ConsumerInfoTypeInfo(type.getJobDataSchema(), typeStatus(type),
            this.infoProducers.getProducerIdsForType(type.getId()).size());
    }

    private ConsumerInfoTypeInfo.ConsumerTypeStatusValues typeStatus(InfoType type) {
        for (InfoProducer producer : this.infoProducers.getProducersForType(type)) {
            if (producer.isAvailable()) {
                return ConsumerInfoTypeInfo.ConsumerTypeStatusValues.ENABLED;
            }
        }
        return ConsumerInfoTypeInfo.ConsumerTypeStatusValues.DISABLED;
    }

    private ConsumerJobInfo toInfoJobInfo(InfoJob s) {
        return new ConsumerJobInfo(s.getTypeId(), s.getJobData(), s.getOwner(), s.getTargetUrl(), s.getJobStatusUrl());
    }
}
