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

import static org.awaitility.Awaitility.await;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;
import org.oransc.policyagent.utils.MockA1ClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StringUtils;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class MockPolicyAgent {
    private static final Logger logger = LoggerFactory.getLogger(MockPolicyAgent.class);

    @Autowired
    Rics rics;

    @Autowired
    Policies policies;

    @Autowired
    PolicyTypes policyTypes;

    @Autowired
    ApplicationConfig applicationConfig;

    static class MockApplicationConfig extends ApplicationConfig {
        @Override
        public String getLocalConfigurationFilePath() {
            URL url = MockApplicationConfig.class.getClassLoader().getResource("test_application_configuration.json");
            return url.getFile();
        }
    }

    /**
     * Overrides the BeanFactory.
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
        public MockA1ClientFactory getA1ClientFactory() {
            PolicyTypes ricTypes = new PolicyTypes();
            loadTypes(ricTypes);
            return new MockA1ClientFactory(ricTypes);
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

        private static File[] getResourceFolderFiles(String folder) {
            return getFile(folder).listFiles();
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
                    logger.error("Could not load json schema ", e);
                }
            }
            policyTypes.put(ImmutablePolicyType.builder().name("").schema("{}").build());
        }
    }

    private static File getFile(String path) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(path);
        return new File(url.getPath());
    }

    @LocalServerPort
    private int port;

    private void keepServerAlive() throws InterruptedException, IOException {
        waitForConfigurationToBeLoaded();
        loadInstances();
        logger.info("Keeping server alive!");
        synchronized (this) {
            this.wait();
        }
    }

    private void waitForConfigurationToBeLoaded() throws IOException {
        String json = getConfigJsonFromFile();
        try {
            int noOfRicsInConfigFile = StringUtils.countOccurrencesOf(json, "baseUrl");
            await().until(() -> rics.size() == noOfRicsInConfigFile);
        } catch (Exception e) {
            logger.info("Loaded rics: {}, and no of rics in config file: {} never matched!", rics.size(),
                StringUtils.countOccurrencesOf(json, "baseUrl"));
        }
    }

    private static String title(String jsonSchema) {
        JsonObject parsedSchema = (JsonObject) JsonParser.parseString(jsonSchema);
        String title = parsedSchema.get("title").getAsString();
        return title;
    }

    private void loadInstances() throws IOException {
        PolicyType unnamedPolicyType = policyTypes.get("");
        Ric ric = rics.get("ric1");
        String json = getConfigJsonFromFile();

        Policy policy = ImmutablePolicy.builder() //
            .id("typelessPolicy") //
            .json(json) //
            .ownerServiceName("MockPolicyAgent") //
            .ric(ric) //
            .type(unnamedPolicyType) //
            .lastModified("now") //
            .isTransient(false) //
            .build();
        this.policies.put(policy);
    }

    private String getConfigJsonFromFile() throws IOException {
        File jsonFile = getFile("test_application_configuration.json");
        String json = new String(Files.readAllBytes(jsonFile.toPath()));
        return json;
    }

    @Test
    @SuppressWarnings("squid:S2699") // Tests should include assertions. This test is only for keeping the server
                                     // alive, so it will only be confusing to add an assertion.
    void runMock() throws Exception {
        keepServerAlive();
    }

}
