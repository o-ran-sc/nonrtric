/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2024 OpenInfra Foundation Europe.
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
import java.util.List;
import org.oransc.nonrtric.sample.rest.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class HelloWorldSmeInvokerController {

    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldSmeInvokerController.class);

    public HelloWorldSmeInvokerController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @RequestMapping("/helloworld/sme/invoker/v1")
    public List<ApiResponse> helloWorldSmeInvoker(HttpServletRequest request) {
        String path = logRequestPath(request);

        String rappInstanceId = System.getenv("RAPP_INSTANCE_ID");
        if (rappInstanceId != null) {
            logger.info("RAPP_INSTANCE_ID: " + rappInstanceId);
        } else {
            logger.info("RAPP_INSTANCE_ID is not set.");
        }

        String smeDiscoveryEndpoint = System.getenv("SME_DISCOVERY_ENDPOINT");
        if (smeDiscoveryEndpoint != null) {
            logger.info("SME_DISCOVERY_ENDPOINT: " + smeDiscoveryEndpoint);
        } else {
            logger.info("SME_DISCOVERY_ENDPOINT is not set.");
        }

        String invokerId = "api_invoker_id_Invoker_Rapp_Id_Invoker_App_1";
        if (rappInstanceId != null) {
            invokerId = "api_invoker_id_"+rappInstanceId + "_Invoker_App_1";
        }
        logger.info("invokerId: " + invokerId);

        String apiUrl = "http://capifcore.nonrtric.svc.cluster.local:8090/service-apis/v1/allServiceAPIs" + "?api-invoker-id=" + invokerId;
        if(smeDiscoveryEndpoint != null){
            apiUrl = smeDiscoveryEndpoint + "?api-invoker-id=" + invokerId;
        }
        logger.info("apiUrl: " + apiUrl);


        List<ApiResponse> apiResponses = restTemplate.getForObject(apiUrl, List.class);
        logger.info("apiResponses : " + apiResponses.size());
        return apiResponses;
    }

    private String logRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        logger.info("Received request for path: {}", path);
        return path;
    }
}
