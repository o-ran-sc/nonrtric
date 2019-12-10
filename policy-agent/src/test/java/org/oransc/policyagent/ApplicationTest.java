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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.net.URL;
import java.util.List;
import java.util.Vector;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.controllers.ImmutableServiceRegistrationInfo;
import org.oransc.policyagent.controllers.ImmutableServiceStatus;
import org.oransc.policyagent.controllers.PolicyTypeInfo;
import org.oransc.policyagent.controllers.ServiceRegistrationInfo;
import org.oransc.policyagent.controllers.ServiceStatus;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.ImmutablePolicy;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationTest {

    @Autowired
    private Rics rics;

    @Autowired
    private Policies policies;

    @Autowired
    private PolicyTypes policyTypes;

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    public static class MockApplicationConfig extends ApplicationConfig {
        @Override
        public void initialize() {
            URL url = MockApplicationConfig.class.getClassLoader().getResource("test_application_configuration.json");
            loadConfigurationFromFile(url.getFile());
        }
    }

    /**
     * overrides the BeanFactory
     */
    @TestConfiguration
    static class TestBeanFactory {

        @Bean
        public ApplicationConfig getApplicationConfig() {
            return new MockApplicationConfig();
        }
    }

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    public void reset() {
        rics.clear();
        policies.clear();
        policyTypes.clear();
        assertThat(policies.size()).isEqualTo(0);
    }

    @Test
    public void testGetRics() throws Exception {
        String url = baseUrl() + "/rics";
        String rsp = this.restTemplate.getForObject(url, String.class);
        assertThat(rsp).contains("kista_1");
    }

    @Test
    public void testGetRic() throws Exception {
        String url = baseUrl() + "/ric?managedElementId=kista_1";
        String rsp = this.restTemplate.getForObject(url, String.class);
        assertThat(rsp).isEqualTo("ric1");
    }

    // managedElmentId -> nodeName

    @Test
    public void testPutPolicy() throws Exception {
        putService("service1");

        String url = baseUrl() + "/policy?type=type1&instance=instance1&ric=ric1&service=service1";
        String json = "{}";
        addPolicyType("type1");

        this.restTemplate.put(url, json);

        Policy policy = policies.get("instance1");

        assertThat(policy).isNotNull();
        assertThat(policy.id()).isEqualTo("instance1");
        assertThat(policy.ownerServiceName()).isEqualTo("service1");

        url = baseUrl() + "/policies";
        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
    }

    private PolicyType addPolicyType(String name) {
        PolicyType type = ImmutablePolicyType.builder() //
            .jsonSchema("") //
            .name(name) //
            .build();

        policyTypes.put(type);
        return type;
    }

    private Ric addRic(String name) {
        Vector<String> mes = new Vector<>();
        RicConfig conf = ImmutableRicConfig.builder().name(name).baseUrl("baseUrl").managedElementIds(mes).build();
        Ric ric = new Ric(conf);
        this.rics.put(ric);
        return ric;
    }

    private Policy addPolicy(String id, String typeName, String service, String ric) throws ServiceException {
        addRic(ric);
        Policy p = ImmutablePolicy.builder().id(id) //
            .json("{}") //
            .ownerServiceName(service) //
            .ric(rics.getRic(ric)) //
            .type(addPolicyType(typeName)) //
            .lastModified("lastModified").build();
        policies.put(p);
        return p;
    }

    private Policy addPolicy(String id, String typeName, String service) throws ServiceException {
        return addPolicy(id, typeName, service, "ric");
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    public void testGetPolicy() throws Exception {
        String url = baseUrl() + "/policy?instance=id";
        Policy policy = addPolicy("id", "typeName", "service1", "ric1");
        {
            String rsp = this.restTemplate.getForObject(url, String.class);
            assertThat(rsp).isEqualTo(policy.json());
        }
        {
            policies.remove(policy);
            ResponseEntity<String> rsp = this.restTemplate.getForEntity(url, String.class);
            assertThat(rsp.getStatusCodeValue()).isEqualTo(HttpStatus.NO_CONTENT.value());
        }
    }

    @Test
    public void testDeletePolicy() throws Exception {
        reset();
        String url = baseUrl() + "/policy?instance=id";
        addPolicy("id", "typeName", "service1", "ric1");
        assertThat(policies.size()).isEqualTo(1);

        this.restTemplate.delete(url);

        assertThat(policies.size()).isEqualTo(0);
    }

    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (null == json) {
            return null;
        }
        return gson.fromJson(json, new TypeToken<T>() {}.getType());
    }

    @Test
    public void testGetPolicyTypes() throws Exception {
        String url = baseUrl() + "/policy_types";
        reset();
        addPolicy("id1", "type1", "service1");
        addPolicy("id2", "type2", "service2");

        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
        assertThat(rsp).contains("type1");
        assertThat(rsp).contains("type2");

        List<PolicyTypeInfo> info = parseList(rsp, PolicyTypeInfo.class);
        System.out.println(info.size());

    }

    @Test
    public void testGetPolicies() throws Exception {
        String url = baseUrl() + "/policies";
        addPolicy("id1", "type1", "service1");
        addPolicy("id2", "type2", "service2");

        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
        assertThat(rsp).contains("id1");
        assertThat(rsp).contains("id2");
    }

    @Test
    public void testGetPoliciesFilter() throws Exception {
        addPolicy("id1", "type1", "service1");
        addPolicy("id2", "type1", "service2");
        addPolicy("id3", "type2", "service1");

        String url = baseUrl() + "/policies?type=type1";
        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
        assertThat(rsp).contains("id1");
        assertThat(rsp).contains("id2");
        assertFalse(rsp.contains("id3"));

        url = baseUrl() + "/policies?type=type1&service=service2";
        rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
        assertFalse(rsp.contains("id1"));
        assertThat(rsp).contains("id2");
        assertFalse(rsp.contains("id3"));
    }

    private String createServiceJson(String name) {
        ServiceRegistrationInfo service = ImmutableServiceRegistrationInfo.builder() //
            .keepAliveInterval(1) //
            .name(name) //
            .build();
        String json = gson.toJson(service);
        return json;
    }

    private void putService(String name) {
        String url = baseUrl() + "/service";
        this.restTemplate.put(url, createServiceJson(name));
    }

    @Test
    public void testPutAndGetService() throws Exception {
        putService("name");

        String url = baseUrl() + "/service?name=name";
        String rsp = this.restTemplate.getForObject(url, String.class);
        ServiceStatus status = gson.fromJson(rsp, ImmutableServiceStatus.class);
        assertThat(status.keepAliveInterval() == 1);
        assertThat(status.name().equals("name"));

        url = baseUrl() + "/services";
        rsp = this.restTemplate.getForObject(url, String.class);
        assertThat(rsp.contains("name"));
        System.out.println(rsp);

        url = baseUrl() + "/service/ping";
        this.restTemplate.put(url, "name");
    }

}
