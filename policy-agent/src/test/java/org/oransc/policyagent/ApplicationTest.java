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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationTest {

    @Autowired
    private Beans beans;

    static class MockApplicationConfig extends ApplicationConfig {
        @Override
        public void initialize() {
            URL url = MockApplicationConfig.class.getClassLoader().getResource("test_application_configuration.json");
            loadConfigurationFromFile(url.getFile());
        }
    }

    @TestConfiguration
    static class Beans {
        @Bean
        public Rics getRics() {
            return new Rics();
        }

        @Bean
        public Policies getPolicies() {
            return new Policies();
        }

        @Bean
        public PolicyTypes getPolicyTypes() {
            return new PolicyTypes();
        }

        @Bean
        public ApplicationConfig getApplicationConfig() {
            return new MockApplicationConfig();
        }
    }

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate = new RestTemplate();

    @Test
    public void getPolicy() throws Exception {
        String cmd = "/policy?type=type3&instance=xxx";
        String rsp = this.restTemplate.getForObject("http://localhost:" + port + cmd, String.class);
        System.out.println("*** rsp " + rsp);
        assertThat(rsp).contains("type3");
    }

    @Test
    public void getRics() throws Exception {
        String cmd = "/rics";
        String rsp = this.restTemplate.getForObject("http://localhost:" + port + cmd, String.class);
        System.out.println("*** rsp " + rsp);
        assertThat(rsp).contains("kista_1");
    }

    @Test
    public void getRic() throws Exception {
        String cmd = "/ric?managedElementId=kista_1";
        String rsp = this.restTemplate.getForObject("http://localhost:" + port + cmd, String.class);
        assertThat(rsp).isEqualTo("ric1");
    }

    // managedElmentId -> nodeName

    @Test
    public void putPolicy() throws Exception {
        // types.putType("type1", ImmutablePolicyType.builder().name("").jsonSchema("").build());

        String url = "http://localhost:" + port + "/policy?type={type}&instance={instance}&ric={ric}&service={service}";

        Map<String, String> uriVariables = new HashMap<String, String>();
        uriVariables.put("type", "type1");
        uriVariables.put("instance", "instance1");
        uriVariables.put("ric", "ric1");
        uriVariables.put("service", "service");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = "{}";
        HttpEntity<String> entity = new HttpEntity<String>(json);

        addRic(beans.getRics(), "ric1", url);
        addPolicyType(beans.getPolicyTypes(), "type1");

        this.restTemplate.put(url, entity, uriVariables);
        Policy policy = beans.getPolicies().get("instance1");
        assertThat(policy).isNotNull();
        assertThat(policy.id()).isEqualTo("instance1");
        assertThat(policy.ownerServiceName()).isEqualTo("service");
    }

    private void addRic(Rics rics, String ric, String url) {
        Vector<String> nodeNames = new Vector<>(1);
        nodeNames.add("node1");
        RicConfig ricConfig = ImmutableRicConfig.builder() //
            .name(ric) //
            .baseUrl(url) //
            .managedElementIds(nodeNames) //
            .build();
        Ric ricObj = new Ric(ricConfig);

        rics.put(ricObj);
    }

    private void addPolicyType(PolicyTypes policyTypes, String name) {
        PolicyType type = ImmutablePolicyType.builder() //
            .jsonSchema("") //
            .name(name) //
            .build();

        policyTypes.putType(name, type);
    }

}
