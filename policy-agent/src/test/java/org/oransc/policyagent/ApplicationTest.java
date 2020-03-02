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
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.controllers.PolicyInfo;
import org.oransc.policyagent.controllers.ServiceRegistrationInfo;
import org.oransc.policyagent.controllers.ServiceStatus;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.ImmutablePolicyType;
import org.oransc.policyagent.repository.Lock.LockType;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Ric.RicState;
import org.oransc.policyagent.repository.Rics;
import org.oransc.policyagent.repository.Services;
import org.oransc.policyagent.tasks.RepositorySupervision;
import org.oransc.policyagent.utils.MockA1Client;
import org.oransc.policyagent.utils.MockA1ClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationTest {
    @Autowired
    ApplicationContext context;

    @Autowired
    private Rics rics;

    @Autowired
    private Policies policies;

    @Autowired
    private PolicyTypes policyTypes;

    @Autowired
    MockA1ClientFactory a1ClientFactory;

    @Autowired
    RepositorySupervision supervision;

    @Autowired
    Services services;

    private static Gson gson = new GsonBuilder() //
        .serializeNulls() //
        .create(); //

    public static class MockApplicationConfig extends ApplicationConfig {
        @Override
        public String getLocalConfigurationFilePath() {
            return ""; // No config file loaded for the test
        }
    }

    /**
     * Overrides the BeanFactory.
     */
    @TestConfiguration
    static class TestBeanFactory {
        private final PolicyTypes policyTypes = new PolicyTypes();

        @Bean
        public ApplicationConfig getApplicationConfig() {
            return new MockApplicationConfig();
        }

        @Bean
        MockA1ClientFactory getA1ClientFactory() {
            return new MockA1ClientFactory(this.policyTypes);
        }

        @Bean
        public PolicyTypes getPolicyTypes() {
            return this.policyTypes;
        }
    }

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return (httpResponse.getStatusCode().series() == Series.CLIENT_ERROR
                || httpResponse.getStatusCode().series() == Series.SERVER_ERROR);
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            System.out.println("Error " + httpResponse.toString());
        }
    }

    private void setRestErrorhandler() {
        restTemplate.setErrorHandler(new RestTemplateResponseErrorHandler());
    }

    @BeforeEach
    public void reset() {
        rics.clear();
        policies.clear();
        policyTypes.clear();
        assertThat(policies.size()).isEqualTo(0);
    }

    @AfterEach
    public void verifyNoRicLocks() {
        for (Ric ric : this.rics.getRics()) {
            ric.getLock().lockBlocking(LockType.EXCLUSIVE);
            assertThat(ric.getLock().getLockCounter()).isEqualTo(1);
            assertThat(ric.getState()).isEqualTo(Ric.RicState.IDLE);
            ric.getLock().unlock();
        }
    }

    @Test
    public void testGetRics() throws Exception {
        addRic("kista_1");
        String url = baseUrl() + "/rics";
        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
        assertThat(rsp).contains("kista_1");

        url = baseUrl() + "/rics?policyType=STD_PolicyModelUnconstrained_0.2.0";
        rsp = this.restTemplate.getForObject(url, String.class);
        assertThat(rsp).isEqualTo("[]");
    }

    @Test
    public void testRecovery() throws Exception {
        addRic("ric").setState(Ric.RicState.UNDEFINED);
        Policy policy2 = addPolicy("policyId2", "typeName", "service", "ric");

        getA1Client("ric").putPolicy(policy2); // put it in the RIC
        policies.remove(policy2); // Remove it from the repo -> should be deleted in the RIC

        Policy policy = addPolicy("policyId", "typeName", "service", "ric"); // This should be created in the RIC
        supervision.checkAllRics(); // The created policy should be put in the RIC
        await().untilAsserted(() -> RicState.RECOVERING.equals(rics.getRic("ric").getState()));
        await().untilAsserted(() -> RicState.IDLE.equals(rics.getRic("ric").getState()));

        Policies ricPolicies = getA1Client("ric").getPolicies();
        assertThat(ricPolicies.size()).isEqualTo(1);
        Policy ricPolicy = ricPolicies.get("policyId");
        assertThat(ricPolicy.json()).isEqualTo(policy.json());
    }

    @Test
    public void testGetRic() throws Exception {
        Ric ric = addRic("ric1");
        ric.addManagedElement("kista_1");
        String url = baseUrl() + "/ric?managedElementId=kista_1";

        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
        assertThat(rsp).isEqualTo("ric1");
    }

    @Test
    public void testPutPolicy() throws Exception {
        putService("service1");
        this.addRic("ric1");
        addPolicyType("type1", "ric1");

        final String json = jsonString();
        String url = baseUrl() + "/policy?type=type1&instance=instance1&ric=ric1&service=service1";
        this.restTemplate.put(url, createJsonHttpEntity(json));
        Policy policy = policies.getPolicy("instance1");

        assertThat(policy).isNotNull();
        assertThat(policy.id()).isEqualTo("instance1");
        assertThat(policy.ownerServiceName()).isEqualTo("service1");
        assertThat(policy.ric().name()).isEqualTo("ric1");

        url = baseUrl() + "/policies";
        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);

    }

    @Test
    public void testRefuseToUpdatePolicy() throws Exception {
        // Test that only the json can be changed for a already created policy
        // In this case service is attempted to be changed
        this.addRic("ric1");
        this.addRic("ricXXX");

        this.addPolicy("instance1", "type1", "service1", "ric1");
        this.setRestErrorhandler();
        String urlWrongRic = baseUrl() + "/policy?type=type1&instance=instance1&ric=ricXXX&service=service1";
        ResponseEntity<String> entity = this.putForEntity(urlWrongRic, jsonString());
        assertThat(entity.getStatusCode().equals(HttpStatus.METHOD_NOT_ALLOWED));

        Policy policy = policies.getPolicy("instance1");
        assertThat(policy.ric().name()).isEqualTo("ric1"); // Not changed
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
        String url = baseUrl() + "/policy?instance=id";
        addPolicy("id", "typeName", "service1", "ric1");
        assertThat(policies.size()).isEqualTo(1);

        this.restTemplate.delete(url);

        assertThat(policies.size()).isEqualTo(0);
    }

    @Test
    public void testGetPolicySchemas() throws Exception {
        addPolicyType("type1", "ric1");
        addPolicyType("type2", "ric2");

        String url = baseUrl() + "/policy_schemas";
        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println("*** " + rsp);
        assertThat(rsp).contains("type1");
        assertThat(rsp).contains("[{\"title\":\"type2\"}");

        List<String> info = parseSchemas(rsp);
        assertEquals(2, info.size());

        url = baseUrl() + "/policy_schemas?ric=ric1";
        rsp = this.restTemplate.getForObject(url, String.class);
        assertThat(rsp).contains("type1");
        info = parseSchemas(rsp);
        assertEquals(1, info.size());
    }

    @Test
    public void testGetPolicySchema() throws Exception {
        addPolicyType("type1", "ric1");
        addPolicyType("type2", "ric2");

        String url = baseUrl() + "/policy_schema?id=type1";
        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
        assertThat(rsp).contains("type1");
        assertThat(rsp).contains("title");
    }

    @Test
    public void testGetPolicyTypes() throws Exception {
        addPolicyType("type1", "ric1");
        addPolicyType("type2", "ric2");

        String url = baseUrl() + "/policy_types";
        String rsp = this.restTemplate.getForObject(url, String.class);
        assertThat(rsp).isEqualTo("[\"type2\",\"type1\"]");

        url = baseUrl() + "/policy_types?ric=ric1";
        rsp = this.restTemplate.getForObject(url, String.class);
        assertThat(rsp).isEqualTo("[\"type1\"]");
    }

    @Test
    public void testGetPolicies() throws Exception {
        String url = baseUrl() + "/policies";
        addPolicy("id1", "type1", "service1");

        String rsp = this.restTemplate.getForObject(url, String.class);
        System.out.println(rsp);
        List<PolicyInfo> info = parseList(rsp, PolicyInfo.class);
        assertThat(info).size().isEqualTo(1);
        PolicyInfo policyInfo = info.get(0);
        assert (policyInfo.validate());
        assertThat(policyInfo.id).isEqualTo("id1");
        assertThat(policyInfo.type).isEqualTo("type1");
        assertThat(policyInfo.service).isEqualTo("service1");
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

    @Test
    public void testPutAndGetService() throws Exception {
        // PUT
        putService("name");

        // GET
        String url = baseUrl() + "/services?name=name";
        String rsp = this.restTemplate.getForObject(url, String.class);
        List<ServiceStatus> info = parseList(rsp, ServiceStatus.class);
        assertThat(info.size() == 1);
        ServiceStatus status = info.iterator().next();
        assertThat(status.keepAliveIntervalSeconds == 1);
        assertThat(status.serviceName.equals("name"));

        // GET (all)
        url = baseUrl() + "/services";
        rsp = this.restTemplate.getForObject(url, String.class);
        assertThat(rsp.contains("name"));
        System.out.println(rsp);

        // Keep alive
        url = baseUrl() + "/services/keepalive?name=name";
        ResponseEntity<String> entity = this.restTemplate.postForEntity(url, null, String.class);
        assertThat(entity.getStatusCode().equals(HttpStatus.OK));

        // DELETE
        assertThat(services.size() == 1);
        url = baseUrl() + "/services?name=name";
        this.restTemplate.delete(url);
        assertThat(services.size() == 0);

        // Keep alive, no registerred service
        setRestErrorhandler();
        url = baseUrl() + "/services/keepalive?name=nameXXX";
        entity = this.restTemplate.postForEntity(url, null, String.class);
        assertThat(entity.getStatusCode().equals(HttpStatus.NOT_FOUND));
    }

    @Test
    public void testGetPolicyStatus() throws Exception {
        addPolicy("id", "typeName", "service1", "ric1");
        assertThat(policies.size()).isEqualTo(1);

        String url = baseUrl() + "/policy_status?instance=id";
        String rsp = this.restTemplate.getForObject(url, String.class);
        assertThat(rsp.equals("OK"));
    }

    private static class ConcurrencyTestRunnable implements Runnable {
        private final RestTemplate restTemplate = new RestTemplate();
        private final String baseUrl;
        static AtomicInteger nextCount = new AtomicInteger(0);
        private final int count;
        private final RepositorySupervision supervision;

        ConcurrencyTestRunnable(String baseUrl, RepositorySupervision supervision) {
            this.baseUrl = baseUrl;
            this.count = nextCount.incrementAndGet();
            this.supervision = supervision;
        }

        public void run() {
            for (int i = 0; i < 100; ++i) {
                if (i % 10 == 0) {
                    this.supervision.checkAllRics();
                }
                String name = "policy:" + count + ":" + i;
                putPolicy(name);
                deletePolicy(name);
            }
        }

        private void putPolicy(String name) {
            String putUrl = baseUrl + "/policy?type=type1&instance=" + name + "&ric=ric1&service=service1";
            this.restTemplate.put(putUrl, createJsonHttpEntity("{}"));
        }

        private void deletePolicy(String name) {
            String deleteUrl = baseUrl + "/policy?instance=" + name;
            this.restTemplate.delete(deleteUrl);
        }
    }

    @Test
    public void testConcurrency() throws Exception {
        final Instant startTime = Instant.now();
        List<Thread> threads = new ArrayList<>();
        addRic("ric1");
        addPolicyType("type1", "ric1");

        for (int i = 0; i < 100; ++i) {
            Thread t = new Thread(new ConcurrencyTestRunnable(baseUrl(), this.supervision), "TestThread_" + i);
            t.start();
            threads.add(t);
        }
        for (Thread t : threads) {
            t.join();
        }
        assertThat(policies.size()).isEqualTo(0);
        System.out.println("Concurrency test took " + Duration.between(startTime, Instant.now()));
    }

    private MockA1Client getA1Client(String ricName) throws ServiceException {
        return a1ClientFactory.getOrCreateA1Client(ricName);
    }

    private PolicyType addPolicyType(String policyTypeName, String ricName) {
        PolicyType type = ImmutablePolicyType.builder() //
            .name(policyTypeName) //
            .schema("{\"title\":\"" + policyTypeName + "\"}") //
            .build();

        policyTypes.put(type);
        addRic(ricName).addSupportedPolicyType(type);
        return type;
    }

    private Ric addRic(String ricName) {
        if (rics.get(ricName) != null) {
            return rics.get(ricName);
        }
        Vector<String> mes = new Vector<>();
        RicConfig conf = ImmutableRicConfig.builder() //
            .name(ricName) //
            .baseUrl(ricName) //
            .managedElementIds(mes) //
            .build();
        Ric ric = new Ric(conf);
        ric.setState(Ric.RicState.IDLE);
        this.rics.put(ric);
        return ric;
    }

    private String createServiceJson(String name) {
        ServiceRegistrationInfo service = new ServiceRegistrationInfo(name, 1, "");

        String json = gson.toJson(service);
        return json;
    }

    private static HttpEntity<String> createJsonHttpEntity(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<String>(content, headers);
    }

    private void putService(String name) {
        String url = baseUrl() + "/service";
        HttpEntity<String> entity = createJsonHttpEntity(createServiceJson(name));
        this.restTemplate.put(url, entity);
    }

    private String jsonString() {
        return "{\n  \"servingCellNrcgi\": \"1\"\n }";
    }

    private ResponseEntity<String> putForEntity(String url, String jsonBody) {
        return restTemplate.exchange(url, HttpMethod.PUT, createJsonHttpEntity(jsonBody), String.class);
    }

    private Policy addPolicy(String id, String typeName, String service, String ric) throws ServiceException {
        addRic(ric);
        Policy p = ImmutablePolicy.builder().id(id) //
            .json(jsonString()) //
            .ownerServiceName(service) //
            .ric(rics.getRic(ric)) //
            .type(addPolicyType(typeName, ric)) //
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

    private static <T> List<T> parseList(String jsonString, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        JsonArray jsonArr = JsonParser.parseString(jsonString).getAsJsonArray();
        for (JsonElement jsonElement : jsonArr) {
            T o = gson.fromJson(jsonElement.toString(), clazz);
            result.add(o);
        }
        return result;
    }

    private static List<String> parseSchemas(String jsonString) {
        JsonArray arrayOfSchema = JsonParser.parseString(jsonString).getAsJsonArray();
        List<String> result = new ArrayList<>();
        for (JsonElement schemaObject : arrayOfSchema) {
            result.add(schemaObject.toString());
        }
        return result;
    }
}
