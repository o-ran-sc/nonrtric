/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2023-2024 OpenInfra Foundation Europe.
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

package org.oransc.nonrtric.sample.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldController.class);

    @RequestMapping("/helloworld/v1")
    public String helloWorld(HttpServletRequest request) {
        String path = logRequestPath(request);
        return "Hello from "+path;
    }

    @RequestMapping("/helloworld/v1/subpath1")
    public String helloWorldSubpath1(HttpServletRequest request) {
        String path = logRequestPath(request);
        return "Hello from "+path;
    }

    @RequestMapping("/helloworld2/v1")
    public String helloWorld2(HttpServletRequest request) {
        String path = logRequestPath(request);
        return "Hello from "+path;
    }

    @RequestMapping("/helloworld/v2")
    public String helloWorldV2(HttpServletRequest request) {
        String path = logRequestPath(request);
        return "Hello from "+path;
    }

    @RequestMapping("/helloworld/v2/subpath1")
    public String helloWorldSubpath1V2(HttpServletRequest request) {
        String path = logRequestPath(request);
        return "Hello from "+path;
    }

    private String logRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        logger.info("Received request for path: {}", path);
        return path;
    }
}
