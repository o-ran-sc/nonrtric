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

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.oransc.policyagent.model.DmaapRequestMessage;
import org.oransc.policyagent.model.DmaapResponseMessage;
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
        DmaapRequestMessage dmaapRequestMessage = null;
        String dmaapResponse = null;
        // Process the message
        /**
         * Sample Request Message from DMAAP { "type": "request", "correlationId":
         * "c09ac7d1-de62-0016-2000-e63701125557-201", "target": "policy-agent", "timestamp": "2019-05-14T11:44:51.36Z",
         * "apiVersion": "1.0", "originatorId": "849e6c6b420", "requestId": "23343221", "operation": "getPolicySchemas",
         * "payload": "{\"ricName\":\"ric1\"}" }
         *
         * --------------------------------------------------------------------------------------------------------------
         * Sample Response Message to DMAAP {type=response, correlationId=c09ac7d1-de62-0016-2000-e63701125557-201,
         * timestamp=null, originatorId=849e6c6b420, requestId=23343221, status=200 OK, message=[]}
         * -------------------------------------------------------------------------------------------------------------
         */
        try {
            dmaapRequestMessage = mapper.readValue(msg, DmaapRequestMessage.class);
            // Call the Controller
            logger.debug("Invoke the Policy Agent Controller");
            dmaapResponse = invokeController(dmaapRequestMessage);
            // Post the Response message to the DMAAP bus
            logger.debug("DMAAP Response Message to Client- {}", dmaapResponse);
            restClient.post("A1-POLICY-AGENT-WRITE", dmaapResponse).block(); //
        } catch (IOException e) {
            logger.error("Exception occured during message processing", e);
        }
    }

    private String invokeController(DmaapRequestMessage dmaapRequestMessage) {
        String formattedString = "";
        String ricName;
        String instance;
        String jsonBody;
        logger.debug("Payload from the Message - {}", dmaapRequestMessage.getPayload());
        try {
            formattedString = new JSONTokener(dmaapRequestMessage.getPayload()).nextValue().toString();
            logger.debug("Removed the Escape charater in payload- {}", formattedString);
        } catch (JSONException e) {
            logger.error("Exception occurred during formating Payload- {}", dmaapRequestMessage.getPayload());
        }
        JSONObject jsonObject = new JSONObject(formattedString);
        switch (dmaapRequestMessage.getOperation()) {
            case "getPolicySchemas":
                ricName = (String) jsonObject.get("ricName");
                logger.debug("Received the request for getPolicySchemas with Ric Name- {}", ricName);
                return getDmaapResponseMessage(dmaapRequestMessage, policyController.getPolicySchemas(ricName));
            case "getPolicySchema":
                String policyTypeId = (String) jsonObject.get("id");
                logger.debug("Received the request for getPolicySchema with Policy Type Id- {}", policyTypeId);
                return getDmaapResponseMessage(dmaapRequestMessage, policyController.getPolicySchema(policyTypeId));
            case "getPolicyTypes":
                ricName = (String) jsonObject.get("ricName");
                logger.debug("Received the request for getPolicyTypes with Ric Name- {}", ricName);
                return getDmaapResponseMessage(dmaapRequestMessage, policyController.getPolicyTypes(ricName));
            case "getPolicy":
                instance = (String) jsonObject.get("instance");
                logger.debug("Received the request for getPolicy with Instance- {}", instance);
                return getDmaapResponseMessage(dmaapRequestMessage, policyController.getPolicy(instance));
            case "deletePolicy":
                instance = (String) jsonObject.get("instance");
                logger.debug("Received the request for deletePolicy with Instance- {}", instance);
                return getDmaapResponseMessage(dmaapRequestMessage, policyController.deletePolicy(instance).block());
            case "putPolicy":
                String type = (String) jsonObject.get("type");
                String putPolicyInstance = (String) jsonObject.get("instance");
                String putPolicyRic = (String) jsonObject.get("ric");
                String service = (String) jsonObject.get("service");
                jsonBody = (String) jsonObject.get("jsonBody");
                return getDmaapResponseMessage(dmaapRequestMessage,
                        policyController.putPolicy(type, putPolicyInstance, putPolicyRic, service, jsonBody).block());
            case "getPolicies":
                String getPolicyType = (String) jsonObject.get("type");
                instance = (String) jsonObject.get("instance");
                String getPolicyRic = (String) jsonObject.get("ric");
                String getPolicyService = (String) jsonObject.get("service");
                jsonBody = (String) jsonObject.get("jsonBody");
                return getDmaapResponseMessage(dmaapRequestMessage, policyController
                        .putPolicy(getPolicyType, instance, getPolicyRic, getPolicyService, jsonBody).block());
            default:
                break;
        }
        return null;
    }

    private String getDmaapResponseMessage(DmaapRequestMessage dmaapRequestMessage, ResponseEntity<?> policySchemas) {
        DmaapResponseMessage dmaapResponseMessage = DmaapResponseMessage.builder()
                .status(policySchemas.getStatusCode().toString()).message(policySchemas.getBody().toString())
                .type("response").correlationId(dmaapRequestMessage.getCorrelationId())
                .originatorId(dmaapRequestMessage.getOriginatorId()).requestId(dmaapRequestMessage.getRequestId())
                .build();
        try {
            return mapper.writeValueAsString(dmaapResponseMessage);
        } catch (JsonProcessingException e) {
            logger.error("Exception occured during getDmaapResponseMessage", e);
        }
        return StringUtils.EMPTY;
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
