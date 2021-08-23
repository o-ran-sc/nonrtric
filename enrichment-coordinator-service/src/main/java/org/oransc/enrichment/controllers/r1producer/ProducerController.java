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

package org.oransc.enrichment.controllers.r1producer;

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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.oransc.enrichment.controllers.ErrorResponse;
import org.oransc.enrichment.controllers.VoidResponse;
import org.oransc.enrichment.exceptions.ServiceException;
import org.oransc.enrichment.repository.InfoJob;
import org.oransc.enrichment.repository.InfoJobs;
import org.oransc.enrichment.repository.InfoProducer;
import org.oransc.enrichment.repository.InfoProducers;
import org.oransc.enrichment.repository.InfoType;
import org.oransc.enrichment.repository.InfoTypeSubscriptions;
import org.oransc.enrichment.repository.InfoTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
@RestController("Producer registry")
@Tag(name = ProducerConsts.PRODUCER_API_NAME)
public class ProducerController {

    private static Gson gson = new GsonBuilder().create();

    @Autowired
    private InfoJobs infoJobs;

    @Autowired
    private InfoTypes infoTypes;

    @Autowired
    private InfoProducers infoProducers;

    @Autowired
    private InfoTypeSubscriptions typeSubscriptions;

    @GetMapping(path = ProducerConsts.API_ROOT + "/info-types", produces = MediaType.APPLICATION_JSON_VALUE) //
    @Operation(summary = "Info Type identifiers", description = "") //
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Info Type identifiers", //
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))) //
        })
    public ResponseEntity<Object> getInfoTypdentifiers( //
    ) {
        List<String> result = new ArrayList<>();
        for (InfoType infoType : this.infoTypes.getAllInfoTypes()) {
            result.add(infoType.getId());
        }

        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

    @GetMapping(
        path = ProducerConsts.API_ROOT + "/info-types/{infoTypeId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Individual Information Type", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Info Type", //
                content = @Content(schema = @Schema(implementation = ProducerInfoTypeInfo.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Information type is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class)))})
    public ResponseEntity<Object> getInfoType( //
        @PathVariable("infoTypeId") String infoTypeId) {
        try {
            InfoType t = this.infoTypes.getType(infoTypeId);
            ProducerInfoTypeInfo info = toInfoTypeInfo(t);
            return new ResponseEntity<>(gson.toJson(info), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(
        path = ProducerConsts.API_ROOT + "/info-types/{infoTypeId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Type updated", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), //
            @ApiResponse(
                responseCode = "201",
                description = "Type created", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), //
            @ApiResponse(
                responseCode = "400",
                description = "Bad request", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class)))})
    @Operation(summary = "Individual Information Type", description = "")
    public ResponseEntity<Object> putInfoType( //
        @PathVariable("infoTypeId") String infoTypeId, //
        @RequestBody ProducerInfoTypeInfo registrationInfo) {

        InfoType previousDefinition = this.infoTypes.get(infoTypeId);
        if (registrationInfo.jobDataSchema == null) {
            return ErrorResponse.create("No schema provided", HttpStatus.BAD_REQUEST);
        }
        InfoType newDefinition = new InfoType(infoTypeId, registrationInfo.jobDataSchema);
        this.infoTypes.put(newDefinition);
        this.typeSubscriptions.notifyTypeRegistered(newDefinition);
        return new ResponseEntity<>(previousDefinition == null ? HttpStatus.CREATED : HttpStatus.OK);
    }

    @DeleteMapping(
        path = ProducerConsts.API_ROOT + "/info-types/{infoTypeId}",
        produces = MediaType.APPLICATION_JSON_VALUE) //
    @Operation(summary = "Individual Information Type", description = "") //
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Not used", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), //
            @ApiResponse(
                responseCode = "204",
                description = "Producer deleted", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Information type is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))), //
            @ApiResponse(
                responseCode = "406",
                description = "The Information type has one or several active producers", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> deleteInfoType( //
        @PathVariable("infoTypeId") String infoTypeId) {

        InfoType type = this.infoTypes.get(infoTypeId);
        if (type == null) {
            return ErrorResponse.create("Information type not found", HttpStatus.NOT_FOUND);
        }
        if (!this.infoProducers.getProducersForType(type).isEmpty()) {
            String firstProducerId = this.infoProducers.getProducersForType(type).iterator().next().getId();
            return ErrorResponse.create("The type has active producers: " + firstProducerId, HttpStatus.NOT_ACCEPTABLE);
        }
        this.infoTypes.remove(type);
        this.typeSubscriptions.notifyTypeRemoved(type);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path = ProducerConsts.API_ROOT + "/info-producers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Information producer identifiers", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Information producer identifiers", //
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))) //
        })
    public ResponseEntity<Object> getInfoProducerIdentifiers( //
        @Parameter(
            name = "info_type_id",
            required = false,
            description = "If given, only the producers for the EI Data type is returned.") //
        @RequestParam(name = "info_type_id", required = false) String typeId //
    ) {
        List<String> result = new ArrayList<>();
        for (InfoProducer infoProducer : typeId == null ? this.infoProducers.getAllProducers()
            : this.infoProducers.getProducersForType(typeId)) {
            result.add(infoProducer.getId());
        }

        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

    @GetMapping(
        path = ProducerConsts.API_ROOT + "/info-producers/{infoProducerId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Individual Information Producer", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Information producer", //
                content = @Content(schema = @Schema(implementation = ProducerRegistrationInfo.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Information producer is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class)))//
        })
    public ResponseEntity<Object> getInfoProducer( //
        @PathVariable("infoProducerId") String infoProducerId) {
        try {
            InfoProducer producer = this.infoProducers.getProducer(infoProducerId);
            ProducerRegistrationInfo info = toProducerRegistrationInfo(producer);
            return new ResponseEntity<>(gson.toJson(info), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(
        path = ProducerConsts.API_ROOT + "/info-producers/{infoProducerId}/info-jobs",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Information Job definitions",
        description = "Information Job definitions for one Information Producer")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "404",
                description = "Information producer is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))), //
            @ApiResponse(
                responseCode = "200",
                description = "Information producer", //
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProducerJobInfo.class)))), //
        })
    public ResponseEntity<Object> getInfoProducerJobs( //
        @PathVariable("infoProducerId") String infoProducerId) {
        try {
            InfoProducer producer = this.infoProducers.getProducer(infoProducerId);
            Collection<ProducerJobInfo> producerJobs = new ArrayList<>();
            for (InfoType type : producer.getInfoTypes()) {
                for (InfoJob infoJob : this.infoJobs.getJobsForType(type)) {
                    ProducerJobInfo request = new ProducerJobInfo(infoJob);
                    producerJobs.add(request);
                }
            }

            return new ResponseEntity<>(gson.toJson(producerJobs), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(
        path = ProducerConsts.API_ROOT + "/info-producers/{infoProducerId}/status",
        produces = MediaType.APPLICATION_JSON_VALUE) //
    @Operation(summary = "Information producer status") //
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Information producer status", //
                content = @Content(schema = @Schema(implementation = ProducerStatusInfo.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Information producer is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> getInfoProducerStatus( //
        @PathVariable("infoProducerId") String infoProducerId) {
        try {
            InfoProducer producer = this.infoProducers.getProducer(infoProducerId);
            return new ResponseEntity<>(gson.toJson(producerStatusInfo(producer)), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private ProducerStatusInfo producerStatusInfo(InfoProducer producer) {
        var opState = producer.isAvailable() ? ProducerStatusInfo.OperationalState.ENABLED
            : ProducerStatusInfo.OperationalState.DISABLED;
        return new ProducerStatusInfo(opState);
    }

    @PutMapping(
        path = ProducerConsts.API_ROOT + "/info-producers/{infoProducerId}", //
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Individual Information Producer", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "201",
                description = "Producer created", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), //
            @ApiResponse(
                responseCode = "200",
                description = "Producer updated", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Producer not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> putInfoProducer( //
        @PathVariable("infoProducerId") String infoProducerId, //
        @RequestBody ProducerRegistrationInfo registrationInfo) {
        try {
            validateUri(registrationInfo.jobCallbackUrl);
            validateUri(registrationInfo.producerSupervisionCallbackUrl);
            InfoProducer previousDefinition = this.infoProducers.get(infoProducerId);
            this.infoProducers.registerProducer(toProducerRegistrationInfo(infoProducerId, registrationInfo));
            return new ResponseEntity<>(previousDefinition == null ? HttpStatus.CREATED : HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private void validateUri(String url) throws URISyntaxException, ServiceException {
        if (url != null && !url.isEmpty()) {
            URI uri = new URI(url);
            if (!uri.isAbsolute()) {
                throw new ServiceException("URI: " + url + " is not absolute", HttpStatus.CONFLICT);
            }
        } else {
            throw new ServiceException("Missing required URL", HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping(
        path = ProducerConsts.API_ROOT + "/info-producers/{infoProducerId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Individual Information Producer", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "Not used", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))),
            @ApiResponse(
                responseCode = "204",
                description = "Producer deleted", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Producer is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> deleteInfoProducer(@PathVariable("infoProducerId") String infoProducerId) {
        try {
            final InfoProducer producer = this.infoProducers.getProducer(infoProducerId);
            this.infoProducers.deregisterProducer(producer);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private ProducerRegistrationInfo toProducerRegistrationInfo(InfoProducer p) {
        Collection<String> types = new ArrayList<>();
        for (InfoType type : p.getInfoTypes()) {
            types.add(type.getId());
        }
        return new ProducerRegistrationInfo(types, p.getJobCallbackUrl(), p.getProducerSupervisionCallbackUrl());
    }

    private ProducerInfoTypeInfo toInfoTypeInfo(InfoType t) {
        return new ProducerInfoTypeInfo(t.getJobDataSchema());
    }

    private InfoProducers.InfoProducerRegistrationInfo toProducerRegistrationInfo(String infoProducerId,
        ProducerRegistrationInfo info) throws ServiceException {
        Collection<InfoType> supportedTypes = new ArrayList<>();
        for (String typeId : info.supportedTypeIds) {
            InfoType type = this.infoTypes.getType(typeId);
            supportedTypes.add(type);
        }

        return InfoProducers.InfoProducerRegistrationInfo.builder() //
            .id(infoProducerId) //
            .jobCallbackUrl(info.jobCallbackUrl) //
            .producerSupervisionCallbackUrl(info.producerSupervisionCallbackUrl) //
            .supportedTypes(supportedTypes) //
            .build();
    }
}
