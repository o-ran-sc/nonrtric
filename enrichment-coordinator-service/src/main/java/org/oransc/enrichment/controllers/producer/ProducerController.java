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

package org.oransc.enrichment.controllers.producer;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.oransc.enrichment.controllers.ErrorResponse;
import org.oransc.enrichment.controllers.VoidResponse;
import org.oransc.enrichment.exceptions.ServiceException;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiProducer;
import org.oransc.enrichment.repository.EiProducers;
import org.oransc.enrichment.repository.EiType;
import org.oransc.enrichment.repository.EiTypes;
import org.oransc.enrichment.repository.ImmutableEiProducerRegistrationInfo;
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
    private EiJobs eiJobs;

    @Autowired
    private EiTypes eiTypes;

    @Autowired
    private EiProducers eiProducers;

    @GetMapping(path = ProducerConsts.API_ROOT + "/eitypes", produces = MediaType.APPLICATION_JSON_VALUE) //
    @Operation(summary = "EI type identifiers", description = "") //
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "EI type identifiers", //
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))) //
        })
    public ResponseEntity<Object> getEiTypeIdentifiers( //
    ) {
        List<String> result = new ArrayList<>();
        for (EiType eiType : this.eiTypes.getAllEiTypes()) {
            result.add(eiType.getId());
        }

        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

    @GetMapping(path = ProducerConsts.API_ROOT + "/eitypes/{eiTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Individual EI type", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "EI type", //
                content = @Content(schema = @Schema(implementation = ProducerEiTypeInfo.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Enrichment Information type is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class)))})
    public ResponseEntity<Object> getEiType( //
        @PathVariable("eiTypeId") String eiTypeId) {
        try {
            EiType t = this.eiTypes.getType(eiTypeId);
            ProducerEiTypeInfo info = toEiTypeInfo(t);
            return new ResponseEntity<>(gson.toJson(info), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(path = ProducerConsts.API_ROOT + "/eitypes/{eiTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
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
    @Operation(summary = "Individual EI type", description = "")
    public ResponseEntity<Object> putEiType( //
        @PathVariable("eiTypeId") String eiTypeId, //
        @RequestBody ProducerEiTypeInfo registrationInfo) {

        EiType previousDefinition = this.eiTypes.get(eiTypeId);
        if (registrationInfo.jobDataSchema == null) {
            return ErrorResponse.create("No schema provided", HttpStatus.BAD_REQUEST);
        }
        this.eiTypes.put(new EiType(eiTypeId, registrationInfo.jobDataSchema));
        return new ResponseEntity<>(previousDefinition == null ? HttpStatus.CREATED : HttpStatus.OK);
    }

    @DeleteMapping(path = ProducerConsts.API_ROOT + "/eitypes/{eiTypeId}", produces = MediaType.APPLICATION_JSON_VALUE) //
    @Operation(summary = "Individual EI type", description = "") //
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
                description = "Enrichment Information type is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))), //
            @ApiResponse(
                responseCode = "406",
                description = "The Enrichment Information type has one or several active producers", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> deleteEiType( //
        @PathVariable("eiTypeId") String eiTypeId) {

        EiType type = this.eiTypes.get(eiTypeId);
        if (type == null) {
            return ErrorResponse.create("EI type not found", HttpStatus.NOT_FOUND);
        }
        if (!this.eiProducers.getProducersForType(type).isEmpty()) {
            String firstProducerId = this.eiProducers.getProducersForType(type).iterator().next().getId();
            return ErrorResponse.create("The type has active producers: " + firstProducerId, HttpStatus.NOT_ACCEPTABLE);
        }
        this.eiTypes.remove(type);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(path = ProducerConsts.API_ROOT + "/eiproducers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "EI producer identifiers", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "EI producer identifiers", //
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))) //
        })
    public ResponseEntity<Object> getEiProducerIdentifiers( //
        @Parameter(
            name = "ei_type_id",
            required = false,
            description = "If given, only the producers for the EI Data type is returned.") //
        @RequestParam(name = "ei_type_id", required = false) String typeId //
    ) {
        List<String> result = new ArrayList<>();
        for (EiProducer eiProducer : typeId == null ? this.eiProducers.getAllProducers()
            : this.eiProducers.getProducersForType(typeId)) {
            result.add(eiProducer.getId());
        }

        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

    @GetMapping(
        path = ProducerConsts.API_ROOT + "/eiproducers/{eiProducerId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Individual EI producer", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "EI producer", //
                content = @Content(schema = @Schema(implementation = ProducerRegistrationInfo.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Enrichment Information producer is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class)))//
        })
    public ResponseEntity<Object> getEiProducer( //
        @PathVariable("eiProducerId") String eiProducerId) {
        try {
            EiProducer p = this.eiProducers.getProducer(eiProducerId);
            ProducerRegistrationInfo info = toEiProducerRegistrationInfo(p);
            return new ResponseEntity<>(gson.toJson(info), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(
        path = ProducerConsts.API_ROOT + "/eiproducers/{eiProducerId}/eijobs",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "EI job definitions", description = "EI job definitions for one EI producer")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "404",
                description = "Enrichment Information producer is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))), //
            @ApiResponse(
                responseCode = "200",
                description = "EI producer", //
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProducerJobInfo.class)))), //
        })
    public ResponseEntity<Object> getEiProducerJobs( //
        @PathVariable("eiProducerId") String eiProducerId) {
        try {
            EiProducer producer = this.eiProducers.getProducer(eiProducerId);
            Collection<ProducerJobInfo> producerJobs = new ArrayList<>();
            for (EiType type : producer.getEiTypes()) {
                for (EiJob eiJob : this.eiJobs.getJobsForType(type)) {
                    ProducerJobInfo request = new ProducerJobInfo(eiJob);
                    producerJobs.add(request);
                }
            }

            return new ResponseEntity<>(gson.toJson(producerJobs), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(
        path = ProducerConsts.API_ROOT + "/eiproducers/{eiProducerId}/status",
        produces = MediaType.APPLICATION_JSON_VALUE) //
    @Operation(summary = "EI producer status") //
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "200",
                description = "EI producer status", //
                content = @Content(schema = @Schema(implementation = ProducerStatusInfo.class))), //
            @ApiResponse(
                responseCode = "404",
                description = "Enrichment Information producer is not found", //
                content = @Content(schema = @Schema(implementation = ErrorResponse.ErrorInfo.class))) //
        })
    public ResponseEntity<Object> getEiProducerStatus( //
        @PathVariable("eiProducerId") String eiProducerId) {
        try {
            EiProducer producer = this.eiProducers.getProducer(eiProducerId);
            return new ResponseEntity<>(gson.toJson(producerStatusInfo(producer)), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private ProducerStatusInfo producerStatusInfo(EiProducer producer) {
        ProducerStatusInfo.OperationalState opState =
            producer.isAvailable() ? ProducerStatusInfo.OperationalState.ENABLED
                : ProducerStatusInfo.OperationalState.DISABLED;
        return new ProducerStatusInfo(opState);
    }

    @PutMapping(
        path = ProducerConsts.API_ROOT + "/eiproducers/{eiProducerId}", //
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Individual EI producer", description = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                responseCode = "201",
                description = "Producer created", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))), //
            @ApiResponse(
                responseCode = "200",
                description = "Producer updated", //
                content = @Content(schema = @Schema(implementation = VoidResponse.class))) //
        })
    public ResponseEntity<Object> putEiProducer( //
        @PathVariable("eiProducerId") String eiProducerId, //
        @RequestBody ProducerRegistrationInfo registrationInfo) {
        try {
            EiProducer previousDefinition = this.eiProducers.get(eiProducerId);
            this.eiProducers.registerProducer(toEiProducerRegistrationInfo(eiProducerId, registrationInfo));
            return new ResponseEntity<>(previousDefinition == null ? HttpStatus.CREATED : HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(
        path = ProducerConsts.API_ROOT + "/eiproducers/{eiProducerId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Individual EI producer", description = "")
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
    public ResponseEntity<Object> deleteEiProducer(@PathVariable("eiProducerId") String eiProducerId) {
        try {
            final EiProducer producer = this.eiProducers.getProducer(eiProducerId);
            this.eiProducers.deregisterProducer(producer);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private ProducerRegistrationInfo toEiProducerRegistrationInfo(EiProducer p) {
        Collection<String> types = new ArrayList<>();
        for (EiType type : p.getEiTypes()) {
            types.add(type.getId());
        }
        return new ProducerRegistrationInfo(types, p.getJobCallbackUrl(), p.getProducerSupervisionCallbackUrl());
    }

    private ProducerEiTypeInfo toEiTypeInfo(EiType t) {
        return new ProducerEiTypeInfo(t.getJobDataSchema());
    }

    private EiProducers.EiProducerRegistrationInfo toEiProducerRegistrationInfo(String eiProducerId,
        ProducerRegistrationInfo info) throws ServiceException {
        Collection<EiType> supportedTypes = new ArrayList<>();
        for (String typeId : info.supportedTypeIds) {
            EiType type = this.eiTypes.getType(typeId);
            supportedTypes.add(type);
        }

        return ImmutableEiProducerRegistrationInfo.builder() //
            .id(eiProducerId) //
            .jobCallbackUrl(info.jobCallbackUrl) //
            .producerSupervisionCallbackUrl(info.producerSupervisionCallbackUrl) //
            .supportedTypes(supportedTypes) //
            .build();
    }

}
