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

package org.oransc.policyagent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.oransc.policyagent.clients.A1Client;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Rics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
public class MockPolicyAgent {

    @Autowired
    private Rics rics;

    @Autowired
    private Policies policies;

    @Autowired
    private PolicyTypes policyTypes;

    static class MockApplicationConfig extends ApplicationConfig {

        @Override
        public void initialize() {
            URL url = MockApplicationConfig.class.getClassLoader().getResource("test_application_configuration.json");
            loadConfigurationFromFile(url.getFile());
        }
    }

    private static class RicPolicyDatabase {
        private Map<String, Map<String, String>> policies = new HashMap<>();

        public void putPolicy(String nearRtRicUrl, String policyId, String policyString) {
            getPolicies(nearRtRicUrl).put(policyId, policyString);
        }

        public Iterable<String> getPolicyIdentities(String nearRtRicUrl) {
            return getPolicies(nearRtRicUrl).keySet();
        }

        public void deletePolicy(String nearRtRicUrl, String policyId) {
            getPolicies(nearRtRicUrl).remove(policyId);
        }

        private Map<String, String> getPolicies(String nearRtRicUrl) {
            if (!policies.containsKey(nearRtRicUrl)) {
                policies.put(nearRtRicUrl, new HashMap<>());
            }
            return policies.get(nearRtRicUrl);
        }
    }

    static class A1ClientMock implements A1Client {

        private final RicPolicyDatabase policies = new RicPolicyDatabase();
        private final PolicyTypes policyTypes = new PolicyTypes();

        A1ClientMock() {
            loadTypes(this.policyTypes);
        }

        @Override
        public Flux<String> getPolicyTypeIdentities(String nearRtRicUrl) {
            Vector<String> result = new Vector<>();
            for (PolicyType p : this.policyTypes.getAll()) {
                result.add(p.name());
            }
            return Flux.fromIterable(result);
        }

        @Override
        public Flux<String> getPolicyIdentities(String nearRtRicUrl) {
            Iterable<String> result = policies.getPolicyIdentities(nearRtRicUrl);
            return Flux.fromIterable(result);
        }

        @Override
        public Mono<String> getPolicyType(String nearRtRicUrl, String policyTypeId) {
            try {
                return Mono.just(this.policyTypes.getType(policyTypeId).schema());
            } catch (Exception e) {
                return Mono.error(e);
            }
        }

        @Override
        public Mono<String> putPolicy(String nearRtRicUrl, String policyId, String policyString) {
            policies.putPolicy(nearRtRicUrl, policyId, policyString);
            return Mono.just("OK");
        }

        @Override
        public Mono<String> deletePolicy(String nearRtRicUrl, String policyId) {
            policies.deletePolicy(nearRtRicUrl, policyId);
            return Mono.just("OK");
        }

        private static File[] getResourceFolderFiles(String folder) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            URL url = loader.getResource(folder);
            String path = url.getPath();
            return new File(path).listFiles();
        }

        private static String readFile(File file) throws IOException {
            return new String(Files.readAllBytes(file.toPath()));
        }

        private void loadTypes(PolicyTypes policyTypes) {
            File[] files = getResourceFolderFiles("policy_types/");
            for (File file : files) {
                try {
                    String schema = readFile(file);
                    String typeName = title(schema);
                    PolicyType type = ImmutablePolicyType.builder().name(typeName).schema(schema).build();
                    policyTypes.put(type);
                } catch (Exception e) {
                    System.out.println("Could not load json schema " + e);
                }
            }
        }
    }

    /**
     * overrides the BeanFactory
     */
    @TestConfiguration
    static class TestBeanFactory {

        private final Rics rics = new Rics();
        private final Policies policies = new Policies();
        private final PolicyTypes policyTypes = new PolicyTypes();

        @Bean
        public ApplicationConfig getApplicationConfig() {
            return new MockApplicationConfig();
        }

        @Bean
        A1Client getA1Client() {
            return new A1ClientMock();
        }

        @Bean
        public Policies getPolicies() {
            return this.policies;
        }

        @Bean
        public PolicyTypes getPolicyTypes() {
            return this.policyTypes;
        }

        @Bean
        public Rics getRics() {
            return this.rics;
        }
    }

    @LocalServerPort
    private int port;

    private void keepServerAlive() {
        System.out.println("Keeping server alive!");
        try {
            synchronized (this) {
                this.wait();
            }
        } catch (Exception ex) {
            System.out.println("Unexpected: " + ex.toString());
        }
    }

    private static String title(String jsonSchema) {
        JsonObject parsedSchema = (JsonObject) new JsonParser().parse(jsonSchema);
        String title = parsedSchema.get("title").getAsString();
        return title;
    }

    @Test
    public void runMock() throws Exception {
        keepServerAlive();
    }

}
