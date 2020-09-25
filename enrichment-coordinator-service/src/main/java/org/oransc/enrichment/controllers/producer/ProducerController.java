/*-
 * ========================LICENSE_START=================================
 * ONAP : ccsdk oran
 * ======================================================================
 * Copyright (C) 2019-2020 Nordix Foundation. All rights reserved.
 * ======================================================================
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.oransc.enrichment.clients.ProducerCallbacks;
import org.oransc.enrichment.clients.ProducerJobInfo;
import org.oransc.enrichment.controllers.ErrorResponse;
import org.oransc.enrichment.controllers.producer.ProducerRegistrationInfo.ProducerEiTypeRegistrationInfo;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiProducer;
import org.oransc.enrichment.repository.EiProducers;
import org.oransc.enrichment.repository.EiType;
import org.oransc.enrichment.repository.EiTypes;
import org.oransc.enrichment.repository.ImmutableEiProducer;
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
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
@RestController("ProducerController")
@Api(tags = {ProducerConsts.PRODUCER_API_NAME})
public class ProducerController {

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @Autowired
    private EiJobs eiJobs;

    @Autowired
    private EiTypes eiTypes;

    @Autowired
    private EiProducers eiProducers;

    @Autowired
    ProducerCallbacks producerCallbacks;

    @GetMapping(path = ProducerConsts.API_ROOT + "/eitypes", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @GetMapping(path = ProducerConsts.API_ROOT + "/eitypes/{eiTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Individual EI Type", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "EI type", response = ProducerEiTypeInfo.class), //
            @ApiResponse(
                code = 404,
                message = "Enrichment Information type is not found",
                response = ErrorResponse.ErrorInfo.class)})
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

    @GetMapping(path = ProducerConsts.API_ROOT + "/eiproducers", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "EI producer identifiers", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(
                code = 200,
                message = "EI producer identifiers",
                response = String.class,
                responseContainer = "List"), //
        })
    public ResponseEntity<Object> getEiProducerIdentifiers( //
    ) {
        List<String> result = new ArrayList<>();
        for (EiProducer eiProducer : this.eiProducers.getAllProducers()) {
            result.add(eiProducer.id());
        }

        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

    @GetMapping(
        path = ProducerConsts.API_ROOT + "/eiproducers/{eiProducerId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Individual EI producer", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "EI Jobs", response = ProducerRegistrationInfo.class), //
            @ApiResponse(
                code = 404,
                message = "Enrichment Information producer is not found",
                response = ErrorResponse.ErrorInfo.class)})
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
    @ApiOperation(value = "EI job definitions", notes = "EI job definitions for one EI producer")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "EI jobs", response = ProducerJobInfo.class, responseContainer = "List"), //
            @ApiResponse(
                code = 404,
                message = "Enrichment Information producer is not found",
                response = ErrorResponse.ErrorInfo.class)})
    public ResponseEntity<Object> getEiProducerJobs( //
        @PathVariable("eiProducerId") String eiProducerId) {
        try {
            EiProducer producer = this.eiProducers.getProducer(eiProducerId);
            Collection<ProducerJobInfo> producerJobs = new ArrayList<>();
            for (EiType type : producer.eiTypes()) {
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

    @PutMapping(
        path = ProducerConsts.API_ROOT + "/eiproducers/{eiProducerId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Individual EI producer", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 201, message = "Producer created", response = void.class), //
            @ApiResponse(code = 200, message = "Producer updated", response = void.class)}//
    )
    public ResponseEntity<Object> putEiProducer( //
        @PathVariable("eiProducerId") String eiProducerId, //
        @RequestBody ProducerRegistrationInfo registrationInfo) {
        try {
            EiProducer previousDefinition = this.eiProducers.get(eiProducerId);
            if (previousDefinition != null) {
                for (EiType type : previousDefinition.eiTypes()) {
                    type.removeProducer(previousDefinition);
                }
            }

            registerProducer(eiProducerId, registrationInfo);
            if (previousDefinition != null) {
                purgeTypes(previousDefinition.eiTypes());
            }

            return new ResponseEntity<>(previousDefinition == null ? HttpStatus.CREATED : HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private void purgeTypes(Collection<EiType> types) {
        for (EiType type : types) {
            if (type.getProducerIds().isEmpty()) {
                this.deregisterType(type);
            }
        }
    }

    @DeleteMapping(
        path = ProducerConsts.API_ROOT + "/eiproducers/{eiProducerId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Individual EI producer", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "Not used", response = void.class),
            @ApiResponse(code = 204, message = "Producer deleted", response = void.class),
            @ApiResponse(code = 404, message = "Producer is not found", response = ErrorResponse.ErrorInfo.class)})
    public ResponseEntity<Object> deleteEiProducer(@PathVariable("eiProducerId") String eiProducerId) {
        try {
            final EiProducer producer = this.eiProducers.getProducer(eiProducerId);
            deregisterProducer(producer);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private EiType registerType(ProducerEiTypeRegistrationInfo typeInfo) {
        EiType type = this.eiTypes.get(typeInfo.eiTypeId);
        if (type == null) {
            type = new EiType(typeInfo.eiTypeId, typeInfo.jobDataSchema);
            this.eiTypes.put(type);
        }
        return type;

    }

    EiProducer createProducer(Collection<EiType> types, String producerId, ProducerRegistrationInfo registrationInfo) {
        return ImmutableEiProducer.builder() //
            .id(producerId) //
            .eiTypes(types) //
            .jobCreationCallbackUrl(registrationInfo.jobCreationCallbackUrl) //
            .jobDeletionCallbackUrl(registrationInfo.jobDeletionCallbackUrl) //
            .build();
    }

    private EiProducer registerProducer(String producerId, ProducerRegistrationInfo registrationInfo) {
        ArrayList<EiType> types = new ArrayList<>();
        for (ProducerEiTypeRegistrationInfo typeInfo : registrationInfo.types) {
            types.add(registerType(typeInfo));
        }
        EiProducer producer = createProducer(types, producerId, registrationInfo);
        this.eiProducers.put(producer);

        for (EiType type : types) {
            for (EiJob job : this.eiJobs.getJobsForType(type)) {
                this.producerCallbacks.notifyProducerJobStarted(producer, job);
            }
            type.addProducer(producer);
        }
        return producer;
    }

    private void deregisterType(EiType type) {
        this.eiTypes.remove(type);
        for (EiJob job : this.eiJobs.getJobsForType(type.getId())) {
            this.eiJobs.remove(job);
            this.logger.warn("Deleted job {} because no producers left", job.id());
        }
    }

    private void deregisterProducer(EiProducer producer) {
        this.eiProducers.remove(producer);
        for (EiType type : producer.eiTypes()) {
            boolean removed = type.removeProducer(producer) != null;
            if (!removed) {
                this.logger.error("Bug, no producer found");
            }
            if (type.getProducerIds().isEmpty()) {
                deregisterType(type);
            }
        }
    }

    ProducerRegistrationInfo toEiProducerRegistrationInfo(EiProducer p) {
        Collection<ProducerEiTypeRegistrationInfo> types = new ArrayList<>();
        for (EiType type : p.eiTypes()) {
            types.add(toEiTypeRegistrationInfo(type));
        }
        return new ProducerRegistrationInfo(types, p.jobCreationCallbackUrl(), p.jobDeletionCallbackUrl());
    }

    private ProducerEiTypeRegistrationInfo toEiTypeRegistrationInfo(EiType type) {
        return new ProducerEiTypeRegistrationInfo(type.getJobDataSchema(), type.getId());
    }

    private ProducerEiTypeInfo toEiTypeInfo(EiType t) {
        return new ProducerEiTypeInfo(t.getJobDataSchema(), t.getProducerIds());
    }
}
