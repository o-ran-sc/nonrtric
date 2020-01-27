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
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.controllers.PolicyController;
import org.oransc.policyagent.model.DmaapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class DmaapMessageHandler {

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private PolicyController policyController;
    private AsyncRestClient restClient;
    private String policyAgentResponseTopic = "POLICY-AGENT-A1-READ";

    @Async("threadPoolTaskExecutor")
    public void handleDmaapMsg(String msg) {
        DmaapMessage dmaapMessage =null;
        ResponseEntity<String> response =null;
        // Process the message
        /** Sample Request Message from DMAAP
                     * {
              "type": "request",
              "correlationId": "c09ac7d1-de62-0016-2000-e63701125557-201",
              "target": "policy-agent",
              "timestamp": "2019-05-14T11:44:51.36Z",
              "apiVersion": "1.0",
              "originatorId": "849e6c6b420",
              "requestId": "23343221",
              "operation": "getPolicySchemas",
              "payload": "{\"ric\":\"ric1\"}"
            }

          Sample Response Message to DMAAP
           {
            "type": "response",
            "correlation-id": "c09ac7d1-de62-0016-2000-e63701125557-201",
            "timestamp": "2019-05-14T11:44:51.36Z",
            "originator-id": "849e6c6b420",
            "request-id": "23343221",
            "status" : "ACCEPTED"
            "error" : ""
          }
         */
        System.out.println("Message Received" + msg);
        try {
            dmaapMessage = mapper.readValue(msg, DmaapMessage.class);
            // Post the accepted message to the DMAAP bus
            System.out.println("Dmaap Message" + dmaapMessage);
            restClient.post("/events/" + policyAgentResponseTopic,
                    buildDmaapResponse(dmaapMessage.getCorrelationId(), dmaapMessage.getOriginatorId(),
                            dmaapMessage.getRequestId(), "ACCEPTED", StringUtils.EMPTY, StringUtils.EMPTY)).block(); //
            // Call the Controller
            response = invokeController(dmaapMessage);
            // Post the Response message to the DMAAP bus
            System.out.println("response Dmaap Message" + response);
            restClient.post("/events/" + policyAgentResponseTopic,
                    buildDmaapResponse(dmaapMessage.getCorrelationId(), dmaapMessage.getOriginatorId(),
                            dmaapMessage.getRequestId(), "SUCCESS", response.getBody(), StringUtils.EMPTY)).block(); //
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private  ResponseEntity<String> invokeController(DmaapMessage dmaapMessage) {
        String formattedString = "";
        System.out.println("dmaapMessage.getPayload()" + dmaapMessage.getPayload());
        try {
            formattedString = new JSONTokener(dmaapMessage.getPayload()).nextValue().toString();
            System.out.println("formattedString" + formattedString);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = new JSONObject(formattedString);
        switch (dmaapMessage.getOperation()) {
            case "getPolicySchemas":
                String ricName = (String) jsonObject.get("ric");
                System.out.println("ricName" + ricName);
                return policyController.getPolicySchemas(ricName);
            case "getPolicySchema":
                String policyTypeId = (String) jsonObject.get("id");
                System.out.println("policyTypeId" + policyTypeId);
                return policyController.getPolicySchema(policyTypeId);
            case "getPolicyTypes":
                String ric = (String) jsonObject.get("ric");
                System.out.println("ric" + ric);
                return policyController.getPolicyTypes(ric);
            case "getPolicy":
                String instance = (String) jsonObject.get("instance");
                System.out.println("instance" + instance);
                return policyController.getPolicy(instance);
            case "deletePolicy":
                String deleteInstance = (String) jsonObject.get("instance");
                System.out.println("deleteInstance" + deleteInstance);
                return null;//policyController.deletePolicy(deleteInstance);
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
            String message, String error) {
        System.out.println("buildResponse ");
        return new JSONObject().put("type", "response").put(correlationId, correlationId).put("timestamp", "")
                .put("originatorId", originatorId).put("requestId", requestId).put("status", status)
                .put("message", message).put("error", error).toString();
    }

    @PostConstruct
    public void init() {
        this.restClient = new AsyncRestClient("http://localhost:6845"); // get this value from application config
    }
}
