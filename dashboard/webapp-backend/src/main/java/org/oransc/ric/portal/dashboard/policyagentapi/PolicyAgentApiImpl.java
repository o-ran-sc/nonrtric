/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 AT&T Intellectual Property
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
package org.oransc.ric.portal.dashboard.policyagentapi;

import org.oransc.ric.portal.dashboard.DashboardConstants;
import org.oransc.ric.portal.dashboard.model.ImmutablePolicyInfo;
import org.oransc.ric.portal.dashboard.model.PolicyInfo;
import org.oransc.ric.portal.dashboard.model.PolicyInstances;
import org.oransc.ric.portal.dashboard.model.PolicyType;
import org.oransc.ric.portal.dashboard.model.PolicyTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Component("PolicyAgentApi")
public class PolicyAgentApiImpl implements PolicyAgentApi {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    RestTemplate restTemplate = new RestTemplate();

    private static com.google.gson.Gson gson = new GsonBuilder() //
            .serializeNulls() //
            .create(); //

    private final String urlPrefix;

    @Autowired
    public PolicyAgentApiImpl(
            @org.springframework.beans.factory.annotation.Value("${policycontroller.url.prefix}") final String urlPrefix) {
        logger.debug("ctor prefix '{}'", urlPrefix);
        this.urlPrefix = urlPrefix;
    }

    private String baseUrl() {
        return urlPrefix;
    }

    @Value.Immutable
    @Gson.TypeAdapters
    interface PolicyTypeInfo {

        public String name();

        public String schema();
    }

    @Override
    public ResponseEntity<PolicyTypes> getAllPolicyTypes() throws RestClientException {
        String url = baseUrl() + "/policy_schemas";
        ResponseEntity<String> rsp = this.restTemplate.getForEntity(url, String.class);
        if (!rsp.getStatusCode().is2xxSuccessful()) {
            return new ResponseEntity<>(rsp.getStatusCode());
        }

        PolicyTypes result = new PolicyTypes();
        JsonParser jsonParser = new JsonParser();
        JsonArray schemas = jsonParser.parse(rsp.getBody()).getAsJsonArray();
        for (JsonElement schema : schemas) {
            JsonObject schemaObj = schema.getAsJsonObject();
            String title = schemaObj.get("title").getAsString();
            String schemaAsStr = schemaObj.toString();
            PolicyType pt = new PolicyType(title, schemaAsStr);
            result.add(pt);
        }
        return new ResponseEntity<>(result, rsp.getStatusCode());
    }

    @Override
    public ResponseEntity<PolicyInstances> getPolicyInstancesForType(String type) {
        String url = baseUrl() + "/policies?type={type}";
        Map<String, ?> uriVariables = Map.of("type", type);
        ResponseEntity<String> rsp = this.restTemplate.getForEntity(url, String.class, uriVariables);
        if (!rsp.getStatusCode().is2xxSuccessful()) {
            return new ResponseEntity<>(rsp.getStatusCode());
        }

        Type listType = new TypeToken<List<ImmutablePolicyInfo>>() {
        }.getType();
        List<PolicyInfo> rspParsed = gson.fromJson(rsp.getBody(), listType);

        PolicyInstances result = new PolicyInstances();
        for (PolicyInfo p : rspParsed) {
            result.add(p);
        }
        return new ResponseEntity<>(result, rsp.getStatusCode());
    }

    @Override
    public ResponseEntity<String> getPolicyInstance(String id) throws RestClientException {
        String url = baseUrl() + "/policy?instance={id}";
        Map<String, ?> uriVariables = Map.of("id", id);

        return this.restTemplate.getForEntity(url, String.class, uriVariables);
    }

    @Override
    public ResponseEntity<String> putPolicy(String policyTypeIdString, String policyInstanceId, String json, String ric)
            throws RestClientException {
        String url = baseUrl() + "/policy?type={type}&instance={instance}&ric={ric}&service={service}";
        Map<String, ?> uriVariables = Map.of( //
                "type", policyTypeIdString, //
                "instance", policyInstanceId, //
                "ric", ric, //
                "service", "dashboard");

        try {
            this.restTemplate.put(url, json, uriVariables);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public void deletePolicy(String policyInstanceId) throws RestClientException {
        String url = baseUrl() + "/policy?instance={instance}";
        Map<String, ?> uriVariables = Map.of("instance", policyInstanceId);
        this.restTemplate.delete(url, uriVariables);
    }

    @Value.Immutable
    @Gson.TypeAdapters
    interface RicInfo {
        public String name();

        public Collection<String> nodeNames();

        public Collection<String> policyTypes();
    }

    @Override
    public ResponseEntity<Collection<String>> getRicsSupportingType(String typeName) {
        String url = baseUrl() + "/rics?policyType={typeName}";
        Map<String, ?> uriVariables = Map.of("typeName", typeName);
        String rsp = this.restTemplate.getForObject(url, String.class, uriVariables);

        Type listType = new TypeToken<List<ImmutableRicInfo>>() {
        }.getType();
        List<RicInfo> rspParsed = gson.fromJson(rsp, listType);

        Collection<String> result = new Vector<>(rspParsed.size());
        for (RicInfo ric : rspParsed) {
            result.add(ric.name());
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
