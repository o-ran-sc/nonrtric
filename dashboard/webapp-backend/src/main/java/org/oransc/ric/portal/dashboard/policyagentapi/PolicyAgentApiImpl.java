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
import org.oransc.ric.portal.dashboard.model.PolicyInstance;
import org.oransc.ric.portal.dashboard.model.PolicyInstances;
import org.oransc.ric.portal.dashboard.model.PolicyType;
import org.oransc.ric.portal.dashboard.model.PolicyTypes;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;

import com.google.gson.GsonBuilder;
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

        @SerializedName("id")
        public String name();

        @SerializedName("schema")
        public String schema();
    }

    private PolicyType toPolicyType(PolicyTypeInfo i) {
        return new PolicyType(i.name(), i.name(), i.schema());
    }

    @Override
    public PolicyTypes getAllPolicyTypes() throws RestClientException {
        String url = baseUrl() + "/policy_types";
        String rsp = this.restTemplate.getForObject(url, String.class);

        Type listType = new TypeToken<List<ImmutablePolicyTypeInfo>>() {
        }.getType();
        List<PolicyTypeInfo> rspParsed = gson.fromJson(rsp, listType);

        PolicyTypes result = new PolicyTypes();
        for (PolicyTypeInfo i : rspParsed) {
            result.add(toPolicyType(i));
        }
        return result;
    }

    @Value.Immutable
    @Gson.TypeAdapters
    interface PolicyInfo {

        @SerializedName("id")
        public String name();

        @SerializedName("type")
        public String type();

        @SerializedName("ric")
        public String ric();

        @SerializedName("json")
        public String json();
    }

    private PolicyInstance toPolicyInstance(PolicyInfo p) {
        return new PolicyInstance(p.name(), p.json());
    }

    @Override
    public PolicyInstances getPolicyInstancesForType(String type) {
        String url = baseUrl() + "/policies?type={type}";
        Map<String, ?> uriVariables = Map.of("type", type);
        String rsp = this.restTemplate.getForObject(url, String.class, uriVariables);

        Type listType = new TypeToken<List<ImmutablePolicyInfo>>() {
        }.getType();
        List<PolicyInfo> rspParsed = gson.fromJson(rsp, listType);

        PolicyInstances result = new PolicyInstances();
        for (PolicyInfo p : rspParsed) {
            result.add(toPolicyInstance(p));
        }
        return result;

    }

    @Override
    public String getPolicyInstance(String id) throws RestClientException {
        String url = baseUrl() + "/policy?instance={id}";
        Map<String, ?> uriVariables = Map.of("id", id);

        return this.restTemplate.getForObject(url, String.class, uriVariables);
    }

    @Override
    public void putPolicy(String policyTypeIdString, String policyInstanceId, String json) throws RestClientException {
        String url = baseUrl() + "/policy?type={type}&instance={instance}&ric={ric}&service={service}";
        Map<String, ?> uriVariables = Map.of( //
                "type", policyTypeIdString, //
                "instance", policyInstanceId, //
                "ric", "ric1", // TODO
                "service", "dashboard");

        this.restTemplate.put(url, json, uriVariables);
    }

    @Override
    public void deletePolicy(String policyInstanceId) throws RestClientException {
        String url = baseUrl() + "/policy?instance={instance}";
        Map<String, ?> uriVariables = Map.of("instance", policyInstanceId);
        this.restTemplate.delete(url, uriVariables);
    }

}
