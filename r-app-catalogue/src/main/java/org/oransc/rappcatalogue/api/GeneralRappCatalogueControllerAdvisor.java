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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.oransc.rappcatalogue.exception.HeaderException;
import org.oransc.rappcatalogue.exception.InvalidServiceException;
import org.oransc.rappcatalogue.exception.ServiceNotFoundException;
import org.oransc.rappcatalogue.model.ErrorInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GeneralRappCatalogueControllerAdvisor extends ResponseEntityExceptionHandler {
    @ExceptionHandler(InvalidServiceException.class)
    public ResponseEntity<Object> handleInvalidServiceException(InvalidServiceException ex) {

        return new ResponseEntity<>(getErrorInformation(ex, BAD_REQUEST), BAD_REQUEST);
    }

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<Object> handleServiceNotFoundException(ServiceNotFoundException ex) {

        return new ResponseEntity<>(getErrorInformation(ex, NOT_FOUND), NOT_FOUND);
    }

    @ExceptionHandler(HeaderException.class)
    public ResponseEntity<Object> handleHeaderException(HeaderException ex) {

        return new ResponseEntity<>(getErrorInformation(ex, INTERNAL_SERVER_ERROR), INTERNAL_SERVER_ERROR);
    }

    private ErrorInformation getErrorInformation(Exception cause, HttpStatus status) {
        ErrorInformation errorInfo = new ErrorInformation();
        errorInfo.setDetail(cause.getMessage());
        errorInfo.setStatus(status.value());
        return errorInfo;
    }
}
