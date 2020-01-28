/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
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

package org.oransc.policyagent.dmaap;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.controllers.PolicyController;
import org.oransc.policyagent.model.DmaapMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class DmaapMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DmaapMessageHandler.class);

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private PolicyController policyController;
    private AsyncRestClient restClient;
    private ApplicationConfig applicationConfig;
    private String topic = "";

    @Autowired
    public DmaapMessageHandler(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    // The publish properties is corrupted. It contains the subscribe property values.
    @Async("threadPoolTaskExecutor")
    public void handleDmaapMsg(String msg) {
        init();
        DmaapMessage dmaapMessage = null;
        ResponseEntity<String> response = null;
        // Process the message
        /**
         * Sample Request Message from DMAAP { "type": "request", "correlationId":
         * "c09ac7d1-de62-0016-2000-e63701125557-201", "target": "policy-agent", "timestamp": "2019-05-14T11:44:51.36Z",
         * "apiVersion": "1.0", "originatorId": "849e6c6b420", "requestId": "23343221", "operation": "getPolicySchemas",
         * "payload": "{\"ric\":\"ric1\"}" }
         * --------------------------------------------------------------------------------------------------------------
         * Sample Response Message to DMAAP { "type": "response", "correlation-id":
         * "c09ac7d1-de62-0016-2000-e63701125557-201", "timestamp": "2019-05-14T11:44:51.36Z", "originator-id":
         * "849e6c6b420", "request-id": "23343221", "status" : "ACCEPTED", "message" : "" }
         * -------------------------------------------------------------------------------------------------------------
         * Sample Response Message to DMAAP { "type": "response", "correlation-id":
         * "c09ac7d1-de62-0016-2000-e63701125557-201", "timestamp": "2019-05-14T11:44:51.36Z", "originator-id":
         * "849e6c6b420", "request-id": "23343221", "status" : "SUCCESS" "message" : "" }
         */
        try {
            dmaapMessage = mapper.readValue(msg, DmaapMessage.class);
            // Post the accepted message to the DMAAP bus
            logger.debug("DMAAP Message- {}", dmaapMessage);
            logger.debug("Post Accepted Message to Client");
            restClient
                .post("A1-POLICY-AGENT-WRITE", buildDmaapResponse(dmaapMessage.getCorrelationId(),
                    dmaapMessage.getOriginatorId(), dmaapMessage.getRequestId(), "ACCEPTED", StringUtils.EMPTY))
                .block(); //
            // Call the Controller
            logger.debug("Invoke the Policy Agent Controller");
            response = invokeController(dmaapMessage);
            // Post the Response message to the DMAAP bus
            logger.debug("DMAAP Response Message to Client- {}", response);
            restClient
                .post("A1-POLICY-AGENT-WRITE", buildDmaapResponse(dmaapMessage.getCorrelationId(),
                    dmaapMessage.getOriginatorId(), dmaapMessage.getRequestId(), "SUCCESS", response.getBody()))
                .block(); //
        } catch (IOException e) {
            logger.error("Exception occured during message processing", e);
        }
    }

    private ResponseEntity<String> invokeController(DmaapMessage dmaapMessage) {
        String formattedString = "";
        String ricName;
        String instance;
        logger.debug("Payload from the Message - {}", dmaapMessage.getPayload());
        try {
            formattedString = new JSONTokener(dmaapMessage.getPayload()).nextValue().toString();
            logger.debug("Removed the Escape charater in payload- {}", formattedString);
        } catch (JSONException e) {
            logger.error("Exception occurred during formating Payload- {}", dmaapMessage.getPayload());
        }
        JSONObject jsonObject = new JSONObject(formattedString);
        switch (dmaapMessage.getOperation()) {
            case "getPolicySchemas":
                ricName = (String) jsonObject.get("ricName");
                logger.debug("Received the request for getPolicySchemas with Ric Name- {}", ricName);
                return policyController.getPolicySchemas(ricName);
            case "getPolicySchema":
                String policyTypeId = (String) jsonObject.get("id");
                logger.debug("Received the request for getPolicySchema with Policy Type Id- {}", policyTypeId);
                System.out.println("policyTypeId" + policyTypeId);
                return policyController.getPolicySchema(policyTypeId);
            case "getPolicyTypes":
                ricName = (String) jsonObject.get("ricName");
                logger.debug("Received the request for getPolicyTypes with Ric Name- {}", ricName);
                return policyController.getPolicyTypes(ricName);
            case "getPolicy":
                instance = (String) jsonObject.get("instance");
                logger.debug("Received the request for getPolicy with Instance- {}", instance);
                return policyController.getPolicy(instance);
            case "deletePolicy":
                instance = (String) jsonObject.get("instance");
                logger.debug("Received the request for deletePolicy with Instance- {}", instance);
                return null;// policyController.deletePolicy(deleteInstance);
            case "putPolicy":
                String type = (String) jsonObject.get("type");
                String putPolicyInstance = (String) jsonObject.get("instance");
                String putPolicyRic = (String) jsonObject.get("ric");
                String service = (String) jsonObject.get("service");
                String jsonBody = (String) jsonObject.get("jsonBody");
                return null;// policyController.putPolicy(type, putPolicyInstance, putPolicyRic, service, jsonBody);
            case "getPolicies":
                String getPolicyType = (String) jsonObject.get("type");
                String getPolicyRic = (String) jsonObject.get("ric");
                String getPolicyService = (String) jsonObject.get("service");
                return policyController.getPolicies(getPolicyType, getPolicyRic, getPolicyService);
            default:
                break;
        }
        return null;
    }

    private String buildDmaapResponse(String correlationId, String originatorId, String requestId, String status,
        String message) {
        System.out.println("buildResponse ");
        return new JSONObject().put("type", "response").put(correlationId, correlationId).put("timestamp", "")
            .put("originatorId", originatorId).put("requestId", requestId).put("status", status).put("message", message)
            .toString();
    }

    // @PostConstruct
    // The application properties value is always NULL for the first time
    // Need to fix this
    public void init() {
        logger.debug("Reading DMAAP Publisher bus details from Application Config");
        Properties dmaapPublisherConfig = applicationConfig.getDmaapPublisherConfig();
        String host = (String) dmaapPublisherConfig.get("ServiceName");
        topic = dmaapPublisherConfig.getProperty("topic");
        logger.debug("Read the topic & Service Name - {} , {}", host, topic);
        this.restClient = new AsyncRestClient("http://" + host + "/"); // get this value from application config

    }
}
