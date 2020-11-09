/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2020 Nordix Foundation. All rights reserved.
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

package org.oransc.rappcatalogue.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.junit.jupiter.api.Test;
import org.oransc.rappcatalogue.exception.HeaderException;
import org.oransc.rappcatalogue.exception.InvalidServiceException;
import org.oransc.rappcatalogue.exception.ServiceNotFoundException;
import org.oransc.rappcatalogue.model.ErrorInformation;
import org.springframework.http.ResponseEntity;

class GeneralRappCatalogueControllerAdvisorTest {

    @Test
    void handleInvalidServiceException_shouldReturnBadRequestWithMessage() {
        GeneralRappCatalogueControllerAdvisor advisorUnderTest = new GeneralRappCatalogueControllerAdvisor();

        InvalidServiceException exception = new InvalidServiceException();

        ResponseEntity<Object> response = advisorUnderTest.handleInvalidServiceException(exception);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        ErrorInformation body = (ErrorInformation) response.getBody();
        assertThat(body.getStatus()).isEqualTo(BAD_REQUEST.value());
        assertThat(body.getDetail()).isEqualTo("Service is missing required property: version");
    }

    @Test
    void handleServiceNotFoundException_shouldReturnNotFoundWithMessage() {
        GeneralRappCatalogueControllerAdvisor advisorUnderTest = new GeneralRappCatalogueControllerAdvisor();

        ServiceNotFoundException exception = new ServiceNotFoundException("Name");

        ResponseEntity<Object> response = advisorUnderTest.handleServiceNotFoundException(exception);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        ErrorInformation body = (ErrorInformation) response.getBody();
        assertThat(body.getStatus()).isEqualTo(NOT_FOUND.value());
        assertThat(body.getDetail()).isEqualTo("Service Name not found");
    }

    @Test
    void handleHeaderException_shouldReturnInternalServerErrorWithMessage() {
        GeneralRappCatalogueControllerAdvisor advisorUnderTest = new GeneralRappCatalogueControllerAdvisor();

        String serviceName = "Service";
        HeaderException exception = new HeaderException("Header", serviceName, new Exception("Cause"));

        ResponseEntity<Object> response = advisorUnderTest.handleHeaderException(exception);

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        ErrorInformation body = (ErrorInformation) response.getBody();
        assertThat(body.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
        assertThat(body.getDetail())
            .isEqualTo("Unable to set header Header in put response for service " + serviceName + ". Cause: Cause");
    }
}
