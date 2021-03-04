/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2021 Nordix Foundation. All rights reserved.
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

package org.oransc.rappmanager.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("rAppController")
@RequestMapping("rms")
@Api(tags = {"rapp"})
public class AppController {
    
    private static final Logger logger = LoggerFactory.getLogger(AppController.class);

    @GetMapping(path = "/apps", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Return all Running rApps")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "rApp List")})
    public ResponseEntity<Object> getAllApps() {
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @GetMapping(path = "/apps/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Return a Running rApp")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "rApp")})
    public ResponseEntity<Object> getApp(@PathVariable("id") String appId) {
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @PostMapping(path = "/apps", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Install the rApp")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "rApp Installed")})
    public ResponseEntity<Object> installApp(@RequestBody RApp rApp) {
        return new ResponseEntity<>(null, HttpStatus.CREATED);
    }

    @DeleteMapping(path = "/apps/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Uninstall the rApp")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "rApp Uninstalled")})
    public ResponseEntity<Object> deleteApp(@PathVariable("id") String appId) {
        return new ResponseEntity<>(null, HttpStatus.CREATED);
    }
}
