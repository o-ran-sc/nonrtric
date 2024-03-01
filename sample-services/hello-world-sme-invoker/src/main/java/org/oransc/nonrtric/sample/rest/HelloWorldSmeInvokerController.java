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
import java.util.ArrayList;
import java.util.List;
import org.oransc.nonrtric.sample.exception.CapifAccessException;
import org.oransc.nonrtric.sample.rest.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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

    @RequestMapping("/helloworld/v1/sme")
    public String helloWorld(HttpServletRequest request) {
        String path = logRequestPath(request);
        return "Hello from " + path;
    }

    @RequestMapping("/helloworld/sme/invoker/v1/apiset")
    public ResponseEntity<ApiResponse> helloWorldSmeInvoker(HttpServletRequest request) {
        String path = logRequestPath(request);
        String capifUrl = createCapifUrl();
        try {
            ApiResponse apiResponse = restTemplate.getForObject(capifUrl, ApiResponse.class);
            return ResponseEntity.ok(apiResponse);
        } catch (IllegalArgumentException e) {
            throw new CapifAccessException("Error accessing the URL :- "+capifUrl);
        } catch (Exception e) {
            throw new CapifAccessException("Unexpected error");
        }
    }

    @RequestMapping("/helloworld/sme/invoker/v1")
    public ResponseEntity<String> accessHelloWorldByInvoker(HttpServletRequest request) {
        String path = logRequestPath(request);
        String capifUrl = createCapifUrl();
        String baseUrl = "";
        ApiResponse apiResponse = new ApiResponse();
        try {
            apiResponse = restTemplate.getForObject(capifUrl, ApiResponse.class);
            baseUrl = getBaseUrl(apiResponse);
        } catch (IllegalArgumentException e) {
            throw new CapifAccessException("Error accessing the URL :- "+capifUrl);
        } catch (Exception e) {
            throw new CapifAccessException("Unexpected error");
        }

        String helloWorldEndpoint = "";
        List<String> apiSetEndpoints = getApiSetEndpoints(apiResponse, baseUrl);
        if(apiSetEndpoints != null && !apiSetEndpoints.isEmpty()){
            helloWorldEndpoint = apiSetEndpoints.get(0);
        }

        try {
            String responseHelloWorld = restTemplate.getForObject(helloWorldEndpoint, String.class);
            logger.info("Response :- ", responseHelloWorld);
            return ResponseEntity.ok(responseHelloWorld);
        } catch (IllegalArgumentException e) {
            throw new CapifAccessException("Error accessing the URL :- "+helloWorldEndpoint);
        } catch (Exception e) {
            throw new CapifAccessException("Unexpected error");
        }
    }

    private String logRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        logger.info("Received request for path: {}", path);
        return path;
    }

    private String createCapifUrl() {
        String appId = System.getenv("APP_ID");
        if (appId != null) {
            logger.info("APP_ID: " + appId);
        } else {
            logger.info("APP_ID environment variable is not set. ");
            appId = "Invoker_App_1";
            logger.info("APP_ID default value :- " + appId);
        }

        String smeDiscoveryEndpoint = System.getenv("SME_DISCOVERY_ENDPOINT");
        if (smeDiscoveryEndpoint != null) {
            logger.info("SME_DISCOVERY_ENDPOINT: " + smeDiscoveryEndpoint);
        } else {
            logger.info("SME_DISCOVERY_ENDPOINT environment variable is not set.");
            smeDiscoveryEndpoint = "capifcore.nonrtric.svc.cluster.local:8090/service-apis/v1/allServiceAPIs";
            logger.info("SME_DISCOVERY_ENDPOINT default value :- " + smeDiscoveryEndpoint);
        }

        String invokerId = "api_invoker_id_Invoker_App_1";
        if (appId != null) {
            invokerId = "api_invoker_id_" + appId;
        }
        logger.info("invokerId: " + invokerId);

        String capifUrl =
            "http://capifcore.nonrtric.svc.cluster.local:8090/service-apis/v1/allServiceAPIs" + "?api-invoker-id=" +
                invokerId;
        if (smeDiscoveryEndpoint != null) {
            capifUrl = smeDiscoveryEndpoint + "?api-invoker-id=" + invokerId;
        }
        logger.info("capifUrl: " + capifUrl);

        return capifUrl;
    }

    private static String getBaseUrl(ApiResponse apiResponse) {
        if (apiResponse != null &&
            apiResponse.getServiceAPIDescriptions() != null &&
            !apiResponse.getServiceAPIDescriptions().isEmpty()) {

            ApiResponse.ServiceAPIDescription serviceAPIDescription = apiResponse.getServiceAPIDescriptions().get(0);

            if (serviceAPIDescription.getAefProfiles() != null &&
                !serviceAPIDescription.getAefProfiles().isEmpty()) {

                ApiResponse.AefProfile aefProfile = serviceAPIDescription.getAefProfiles().get(0);

                if (aefProfile.getInterfaceDescriptions() != null &&
                    !aefProfile.getInterfaceDescriptions().isEmpty()) {
                    ApiResponse.InterfaceDescription interfaceDescription = aefProfile.getInterfaceDescriptions().get(0);
                    return "http://" + interfaceDescription.getIpv4Addr() + ":" + interfaceDescription.getPort();
                }
            }
        }
        return "";
    }

    private static List<String> getApiSetEndpoints(ApiResponse apiResponse, String baseUrl){

        String helloWorldEndpoint = "";
        List<String> apiSetEndpoints = new ArrayList<>(5);

        if (apiResponse != null &&
            apiResponse.getServiceAPIDescriptions() != null &&
            !apiResponse.getServiceAPIDescriptions().isEmpty()) {

            ApiResponse.ServiceAPIDescription serviceAPIDescription = apiResponse.getServiceAPIDescriptions().get(0);

            if (serviceAPIDescription.getAefProfiles() != null &&
                !serviceAPIDescription.getAefProfiles().isEmpty()) {

                ApiResponse.AefProfile aefProfile = serviceAPIDescription.getAefProfiles().get(0);

                if (aefProfile.getInterfaceDescriptions() != null &&
                    !aefProfile.getInterfaceDescriptions().isEmpty()) {

                    ApiResponse.InterfaceDescription interfaceDescription = aefProfile.getInterfaceDescriptions().get(0);

                    if (aefProfile.getVersions() != null &&
                        !aefProfile.getVersions().isEmpty()) {

                        ApiResponse.ApiVersion apiVersion = aefProfile.getVersions().get(0);

                        if (apiVersion.getResources() != null &&
                            !apiVersion.getResources().isEmpty()) {

                            for(ApiResponse.Resource resource : apiVersion.getResources()) {
                                helloWorldEndpoint = baseUrl + resource.getUri();
                                logger.info("Complete URL for resource " + helloWorldEndpoint);
                                apiSetEndpoints.add(helloWorldEndpoint);
                            }
                        }
                    }
                }
            }
        }
        return apiSetEndpoints;
    }

}
