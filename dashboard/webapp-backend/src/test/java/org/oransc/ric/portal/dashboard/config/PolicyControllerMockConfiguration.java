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
package org.oransc.ric.portal.dashboard.config;

import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

import org.oransc.ric.portal.dashboard.model.ImmutablePolicyInfo;
import org.oransc.ric.portal.dashboard.model.PolicyInfo;
import org.oransc.ric.portal.dashboard.model.PolicyInstances;
import org.oransc.ric.portal.dashboard.model.PolicyType;
import org.oransc.ric.portal.dashboard.model.PolicyTypes;
import org.oransc.ric.portal.dashboard.policyagentapi.PolicyAgentApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

/**
 * Creates a mock implementation of the policy controller client API.
 */
@TestConfiguration
public class PolicyControllerMockConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static com.google.gson.Gson gson = new GsonBuilder() //
            .serializeNulls() //
            .create(); //

    @Bean
    public PolicyAgentApi policyAgentApi() {
        MockPolicyAgentApi apiClient = new MockPolicyAgentApi();
        return apiClient;
    }

    class MockPolicyAgentApi implements PolicyAgentApi {
        private final Database database = new Database();

        @Override
        public ResponseEntity<Object> getPolicyInstance(String id) {
            return new ResponseEntity<>(database.getInstance(id), HttpStatus.OK);
        }

        @Override
        public ResponseEntity<String> putPolicy(String policyTypeIdString, String policyInstanceId, Object json,
                String ric) {
            database.putInstance(policyTypeIdString, policyInstanceId, json, ric);
            return new ResponseEntity<>("Policy was put successfully", HttpStatus.OK);
        }

        @Override
        public ResponseEntity<String> deletePolicy(String policyInstanceId) {
            database.deleteInstance(policyInstanceId);
            return new ResponseEntity<>("Policy was deleted successfully", HttpStatus.NO_CONTENT);
        }

        @Override
        public ResponseEntity<String> getAllPolicyTypes() {
            PolicyTypes result = new PolicyTypes();
            result.addAll(database.getTypes());
            return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
        }

        @Override
        public ResponseEntity<String> getPolicyInstancesForType(String type) {
            PolicyInstances result = new PolicyInstances();
            List<PolicyInfo> inst = database.getInstances(Optional.of(type));
            result.addAll(inst);
            return new ResponseEntity<>(gson.toJson(result), HttpStatus.OK);
        }

        @Override
        public ResponseEntity<String> getRicsSupportingType(String typeName) {
            Vector<String> res = new Vector<>();
            res.add("ric_1");
            res.add("ric_2");
            res.add("ric_3");
            return new ResponseEntity<>(gson.toJson(res), HttpStatus.OK);
        }
    }

    class Database {

        Database() {
            String schema = getStringFromFile("demo-policy-schema-1.json");
            PolicyType policyType = new PolicyType("type2", schema);
            types.put("type2", policyType);

            schema = getStringFromFile("demo-policy-schema-2.json");
            policyType = new PolicyType("type3", schema);
            types.put("type3", policyType);

            schema = getStringFromFile("demo-policy-schema-3.json");
            policyType = new PolicyType("type4", schema);
            types.put("type4", policyType);
        }

        private String getStringFromFile(String path) {
            try {
                InputStream inputStream = MethodHandles.lookup().lookupClass().getClassLoader()
                        .getResourceAsStream(path);
                return new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            } catch (Exception e) {
                logger.error("Cannot read file :" + path, e);
                return "";
            }
        }

        String normalize(String str) {
            return str.replace('\n', ' ');
        }

        private String getTimeStampUTC() {
            return java.time.Instant.now().toString();
        }

        void putInstance(String typeId, String instanceId, Object instanceData, String ric) {
            PolicyInfo i = ImmutablePolicyInfo.builder().json(instanceData).lastModified(getTimeStampUTC())
                    .id(instanceId).ric(ric).service("service").type(typeId).build();
            instances.put(instanceId, i);
        }

        public void deleteInstance(String instanceId) {
            instances.remove(instanceId);
        }

        Object getInstance(String id) throws RestClientException {
            PolicyInfo i = instances.get(id);
            if (i == null) {
                throw new RestClientException("Type not found: " + id);
            }
            return i.json();
        }

        public Collection<PolicyType> getTypes() {
            return types.values();
        }

        public List<PolicyInfo> getInstances(Optional<String> typeId) {
            ArrayList<PolicyInfo> result = new ArrayList<>();
            for (PolicyInfo i : instances.values()) {
                if (typeId.isPresent()) {
                    if (i.type().equals(typeId.get())) {
                        result.add(i);
                    }

                } else {
                    result.add(i);
                }
            }
            return result;
        }

        private Map<String, PolicyType> types = new HashMap<>();
        private Map<String, PolicyInfo> instances = new HashMap<>();

    }

}
