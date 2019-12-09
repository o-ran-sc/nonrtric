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
import static org.junit.Assert.assertFalse;
import java.net.URL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Rics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public void getRics() throws Exception {
        String cmd = "/rics";
        String rsp = this.restTemplate.getForObject("http://localhost:" + port + cmd, String.class);
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
        String url = "http://localhost:" + port + "/policy?type=type1&instance=instance1&ric=ric1&service=service1";
        String json = "{}";
        addPolicyType("type1");

        this.restTemplate.put(url, json);

        Policy policy = beans.getPolicies().get("instance1");

        assertThat(policy).isNotNull();
        assertThat(policy.id()).isEqualTo("instance1");
        assertThat(policy.ownerServiceName()).isEqualTo("service1");
    }

    private PolicyType addPolicyType(String name) {
        PolicyType type = ImmutablePolicyType.builder() //
                .jsonSchema("") //
                .name(name) //
                .build();

        beans.getPolicyTypes().put(type);
        return type;
    }

    private Policy addPolicy(String id, String typeName, String service) throws ServiceException {
        Policy p = ImmutablePolicy.builder().id(id) //
                .json("{}") //
                .ownerServiceName(service) //
                .ric(beans.getRics().getRic("ric1")) //
                .type(addPolicyType(typeName)) //
                .build();
        beans.getPolicies().put(p);
        return p;
    }

    @Test
    public void getPolicy() throws Exception {
        String url = "http://localhost:" + port + "/policy?instance=id";
        Policy policy = addPolicy("id", "typeName", "service1");
        {
            String rsp = this.restTemplate.getForObject(url, String.class);
            assertThat(rsp).isEqualTo(policy.json());
        }
        {
            beans.getPolicies().remove(policy);
            ResponseEntity<String> rsp = this.restTemplate.getForEntity(url, String.class);
            assertThat(rsp.getStatusCodeValue()).isEqualTo(HttpStatus.NO_CONTENT.value());
        }
    }

    @Test
    public void getPolicies() throws Exception {
        String url = "http://localhost:" + port + "/policies";
        addPolicy("id1", "type1", "service1");
        addPolicy("id2", "type2", "service2");

        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
        assertThat(rsp).contains("id1");
        assertThat(rsp).contains("id2");
    }

    @Test
    public void getPoliciesFilter() throws Exception {
        addPolicy("id1", "type1", "service1");
        addPolicy("id2", "type1", "service2");
        addPolicy("id3", "type2", "service1");

        String url = "http://localhost:" + port + "/policies?type=type1";
        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
        assertThat(rsp).contains("id1");
        assertThat(rsp).contains("id2");
        assertFalse(rsp.contains("id3"));

        url = "http://localhost:" + port + "/policies?type=type1&service=service2";
        rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
        assertFalse(rsp.contains("id1"));
        assertThat(rsp).contains("id2");
        assertFalse(rsp.contains("id3"));

    }

}
