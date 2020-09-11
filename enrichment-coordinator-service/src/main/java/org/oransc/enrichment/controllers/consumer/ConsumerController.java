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

package org.oransc.enrichment.controllers.consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.util.ArrayList;
import java.util.List;

import org.oransc.enrichment.controllers.ErrorResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController("ConsumerController")
@Api(tags = {ConsumerConsts.CONSUMER_API_NAME})
public class ConsumerController {

    @Autowired
    private EiJobs eiJobs;

    @Autowired
    private EiTypes eiTypes;

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    @GetMapping(path = ConsumerConsts.A1E_API_ROOT + "/eitypes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Query EI type identifiers", notes = "DETAILS TBD")
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
            result.add(eiType.id());
        }

        return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
    }

    @GetMapping(path = ConsumerConsts.A1E_API_ROOT + "/eitypes/{eiTypeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Definitions for an individual EI Type", notes = "Query EI type")
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
            EiType t = this.eiTypes.getType(eiTypeId);
            ConsumerEiTypeInfo info = toEiTypeInfo(t);
            return new ResponseEntity<>(gson.toJson(info), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(
        path = ConsumerConsts.A1E_API_ROOT + "/eitypes/{eiTypeId}/eijobs",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Query EI job identifiers", notes = "Returns the EI Job identifiers for an EI Type")
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
        @PathVariable("eiTypeId") String eiTypeId, //
        @ApiParam(
            name = ConsumerConsts.OWNER_PARAM,
            required = false, //
            value = ConsumerConsts.OWNER_PARAM_DESCRIPTION) //
        String owner) {
        try {
            this.eiTypes.getType(eiTypeId); // Just to check that the type exists
            List<String> result = new ArrayList<>();
            for (EiJob job : this.eiJobs.getJobsForType(eiTypeId)) {
                result.add(job.id());
            }
            return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(
        path = ConsumerConsts.A1E_API_ROOT + "/eitypes/{eiTypeId}/eijobs/{eiJobId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Individual EI Job", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "EI Job", response = ConsumerEiJobInfo.class), //
            @ApiResponse(
                code = 404,
                message = "Enrichment Information type or job is not found",
                response = ErrorResponse.ErrorInfo.class)})
    public ResponseEntity<Object> getIndividualEiJob( //
        @PathVariable("eiTypeId") String eiTypeId, //
        @PathVariable("eiJobId") String eiJobId) {
        try {
            this.eiTypes.getType(eiTypeId); // Just to check that the type exists
            EiJob job = this.eiJobs.getJob(eiJobId);
            return new ResponseEntity<>(gson.toJson(toEiJobInfo(job)), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(
        path = ConsumerConsts.A1E_API_ROOT + "/eitypes/{eiTypeId}/eijobs/{eiJobId}/status",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "EI Job status", notes = "")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "EI Job status", response = ConsumerEiJobStatus.class), //
            @ApiResponse(
                code = 404,
                message = "Enrichment Information type or job is not found",
                response = ErrorResponse.ErrorInfo.class)})
    public ResponseEntity<Object> getEiJobStatus( //
        @PathVariable("eiTypeId") String eiTypeId, //
        @PathVariable("eiJobId") String eiJobId) {
        try {
            this.eiTypes.getType(eiTypeId); // Just to check that the type exists
            EiJob job = this.eiJobs.getJob(eiJobId);
            return new ResponseEntity<>(gson.toJson(toEiJobStatus(job)), HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    private ConsumerEiJobStatus toEiJobStatus(EiJob job) {
        return new ConsumerEiJobStatus(ConsumerEiJobStatus.OperationalState.ENABLED);
    }

    @DeleteMapping(
        path = ConsumerConsts.A1E_API_ROOT + "/eitypes/{eiTypeId}/eijobs/{eiJobId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Individual EI Job", notes = "Delete EI job")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 200, message = "Not used", response = void.class),
            @ApiResponse(code = 204, message = "Job deleted", response = void.class),
            @ApiResponse(
                code = 404,
                message = "Enrichment Information type or job is not found",
                response = ErrorResponse.ErrorInfo.class)})
    public ResponseEntity<Object> deleteIndividualEiJob( //
        @PathVariable("eiTypeId") String eiTypeId, //
        @PathVariable("eiJobId") String eiJobId) {
        try {
            this.eiJobs.remove(eiJobId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(
        path = ConsumerConsts.A1E_API_ROOT + "/eitypes/{eiTypeId}/eijobs/{eiJobId}", //
        produces = MediaType.APPLICATION_JSON_VALUE, //
        consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Individual EI Job", notes = "Create or update an EI Job")
    @ApiResponses(
        value = { //
            @ApiResponse(code = 201, message = "Job created", response = void.class), //
            @ApiResponse(code = 200, message = "Job updated", response = void.class), // ,
            @ApiResponse(
                code = 404,
                message = "Enrichment Information type is not found",
                response = ErrorResponse.ErrorInfo.class)})
    public ResponseEntity<Object> putIndividualEiJob( //
        @PathVariable("eiTypeId") String eiTypeId, //
        @PathVariable("eiJobId") String eiJobId, //
        @RequestBody ConsumerEiJobInfo eiJobInfo) {
        try {
            this.eiTypes.getType(eiTypeId); // Just to check that the type exists
            final boolean newJob = this.eiJobs.get(eiJobId) == null;
            this.eiJobs.put(toEiJob(eiJobInfo, eiJobId, eiTypeId));
            return new ResponseEntity<>(newJob ? HttpStatus.CREATED : HttpStatus.OK);
        } catch (Exception e) {
            return ErrorResponse.create(e, HttpStatus.NOT_FOUND);
        }
    }

    // Status TBD

    private EiJob toEiJob(ConsumerEiJobInfo info, String id, String typeId) {
        return ImmutableEiJob.builder() //
            .id(id) //
            .typeId(typeId) //
            .owner(info.owner) //
            .jobData(info.jobData) //
            .build();
    }

    private ConsumerEiTypeInfo toEiTypeInfo(EiType t) {
        return new ConsumerEiTypeInfo(t.jobDataSchema());
    }

    private ConsumerEiJobInfo toEiJobInfo(EiJob s) {
        return new ConsumerEiJobInfo(s.jobData(), s.owner());
    }
}
