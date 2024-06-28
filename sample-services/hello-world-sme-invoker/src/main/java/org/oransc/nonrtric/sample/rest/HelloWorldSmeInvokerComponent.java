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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.oransc.nonrtric.sample.exception.CapifAccessException;
import org.oransc.nonrtric.sample.rest.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HelloWorldSmeInvokerComponent {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(HelloWorldSmeInvokerComponent.class);

    public HelloWorldSmeInvokerComponent(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 5000)
    public void accessHelloWorldByInvoker() {
        String capifUrl = createCapifUrl();
        if (capifUrl != null && !capifUrl.isEmpty()) {
            String baseUrl = "";
            ApiResponse apiResponse;
            try {
                apiResponse = restTemplate.getForObject(capifUrl, ApiResponse.class);
                logger.info("Discovery endpoint response is {}",
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiResponse));
                baseUrl = getBaseUrl(apiResponse);
            } catch (IllegalArgumentException e) {
                throw new CapifAccessException("Error accessing the URL :- " + capifUrl);
            } catch (Exception e) {
                throw new CapifAccessException("Unexpected error");
            }

            String helloWorldEndpoint = "";
            List<String> apiSetEndpoints = getApiSetEndpoints(apiResponse, baseUrl);
            if (apiSetEndpoints != null && !apiSetEndpoints.isEmpty()) {
                helloWorldEndpoint = apiSetEndpoints.get(0);
            }

            if (helloWorldEndpoint != null && !helloWorldEndpoint.isEmpty()) {
                try {
                    String responseHelloWorld = restTemplate.getForObject(helloWorldEndpoint, String.class);
                    logger.info("rApp SME Provider Response : {}", responseHelloWorld);
                } catch (IllegalArgumentException e) {
                    throw new CapifAccessException("Error accessing the URL :- " + helloWorldEndpoint);
                } catch (Exception e) {
                    throw new CapifAccessException("Unexpected error");
                }
            }
        }
    }

    private String createCapifUrl() {
        String appId = System.getenv("APP_ID");
        String invokerId = "";
        String capifUrl = "";
        if (appId != null) {
            logger.info("APP_ID: " + appId);
            invokerId = "api_invoker_id_" + appId;
            logger.info("invokerId: " + invokerId);
        } else {
            logger.info("APP_ID environment variable is not set. ");
        }

        String smeDiscoveryEndpoint = System.getenv("SME_DISCOVERY_ENDPOINT");
        if (smeDiscoveryEndpoint != null) {
            logger.info("SME_DISCOVERY_ENDPOINT: " + smeDiscoveryEndpoint);
            capifUrl = smeDiscoveryEndpoint + "?api-invoker-id=" + invokerId;
            logger.info("capifUrl: " + capifUrl);
        } else {
            logger.info("SME_DISCOVERY_ENDPOINT environment variable is not set.");
        }
        return capifUrl;
    }

    private static String getBaseUrl(ApiResponse apiResponse) {
        if (apiResponse != null && apiResponse.getServiceAPIDescriptions() != null && !apiResponse.getServiceAPIDescriptions()
                .isEmpty()) {

            ApiResponse.ServiceAPIDescription serviceAPIDescription = apiResponse.getServiceAPIDescriptions().get(0);

            if (serviceAPIDescription.getAefProfiles() != null && !serviceAPIDescription.getAefProfiles().isEmpty()) {

                ApiResponse.AefProfile aefProfile = serviceAPIDescription.getAefProfiles().get(0);

                if (aefProfile.getInterfaceDescriptions() != null && !aefProfile.getInterfaceDescriptions().isEmpty()) {
                    ApiResponse.InterfaceDescription interfaceDescription = aefProfile.getInterfaceDescriptions().get(0);
                    return "http://" + interfaceDescription.getIpv4Addr() + ":" + interfaceDescription.getPort();
                }
            }
        }
        return "";
    }

    private static List<String> getApiSetEndpoints(ApiResponse apiResponse, String baseUrl) {

        String helloWorldEndpoint = "";
        List<String> apiSetEndpoints = new ArrayList<>(5);

        if (apiResponse != null && apiResponse.getServiceAPIDescriptions() != null && !apiResponse.getServiceAPIDescriptions()
                .isEmpty()) {

            ApiResponse.ServiceAPIDescription serviceAPIDescription = apiResponse.getServiceAPIDescriptions().get(0);

            if (serviceAPIDescription.getAefProfiles() != null && !serviceAPIDescription.getAefProfiles().isEmpty()) {

                ApiResponse.AefProfile aefProfile = serviceAPIDescription.getAefProfiles().get(0);

                if (aefProfile.getInterfaceDescriptions() != null && !aefProfile.getInterfaceDescriptions().isEmpty()) {

                    if (aefProfile.getVersions() != null && !aefProfile.getVersions().isEmpty()) {

                        ApiResponse.ApiVersion apiVersion = aefProfile.getVersions().get(0);

                        if (apiVersion.getResources() != null && !apiVersion.getResources().isEmpty()) {

                            for (ApiResponse.Resource resource : apiVersion.getResources()) {
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
