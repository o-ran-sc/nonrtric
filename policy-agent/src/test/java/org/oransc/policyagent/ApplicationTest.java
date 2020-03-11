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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oransc.policyagent.clients.AsyncRestClient;
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
import org.oransc.policyagent.tasks.RicSupervision;
import org.oransc.policyagent.tasks.ServiceSupervision;
import org.oransc.policyagent.utils.MockA1Client;
import org.oransc.policyagent.utils.MockA1ClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationTest {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationTest.class);

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
    RicSupervision supervision;

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
        private final Services services = new Services();
        private final Policies policies = new Policies();
        MockA1ClientFactory a1ClientFactory = null;

        @Bean
        public ApplicationConfig getApplicationConfig() {
            return new MockApplicationConfig();
        }

        @Bean
        MockA1ClientFactory getA1ClientFactory() {
            if (a1ClientFactory == null) {
                this.a1ClientFactory = new MockA1ClientFactory(this.policyTypes);
            }
            return this.a1ClientFactory;
        }

        @Bean
        public PolicyTypes getPolicyTypes() {
            return this.policyTypes;
        }

        @Bean
        Policies getPolicies() {
            return this.policies;
        }

        @Bean
        Services getServices() {
            return this.services;
        }

        @Bean
        public ServiceSupervision getServiceSupervision() {
            Duration checkInterval = Duration.ofMillis(1);
            return new ServiceSupervision(this.services, this.policies, this.getA1ClientFactory(), checkInterval);
        }
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    public void reset() {
        rics.clear();
        policies.clear();
        policyTypes.clear();
        services.clear();
    }

    @AfterEach
    public void verifyNoRicLocks() {
        for (Ric ric : this.rics.getRics()) {
            ric.getLock().lockBlocking(LockType.EXCLUSIVE);
            ric.getLock().unlockBlocking();
            assertThat(ric.getLock().getLockCounter()).isEqualTo(0);
            assertThat(ric.getState()).isEqualTo(Ric.RicState.IDLE);
        }
    }

    @Test
    public void testGetRics() throws Exception {
        addRic("kista_1");
        this.addPolicyType("type1", "kista_1");
        String url = "/rics?policyType=type1";
        String rsp = restClient().get(url).block();
        assertThat(rsp).contains("kista_1");

        // Non existing policy type
        url = "/rics?policyType=XXXX";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND);
    }

    @Test
    public void testRecovery() throws Exception {
        addRic("ric").setState(Ric.RicState.UNDEFINED);
        String ricName = "ric";
        Policy policy2 = addPolicy("policyId2", "typeName", "service", ricName);

        getA1Client(ricName).putPolicy(policy2); // put it in the RIC
        policies.remove(policy2); // Remove it from the repo -> should be deleted in the RIC

        String policyId = "policyId";
        Policy policy = addPolicy(policyId, "typeName", "service", ricName); // This should be created in the RIC
        supervision.checkAllRics(); // The created policy should be put in the RIC
        await().untilAsserted(() -> RicState.SYNCHRONIZING.equals(rics.getRic(ricName).getState()));
        await().untilAsserted(() -> RicState.IDLE.equals(rics.getRic(ricName).getState()));

        Policies ricPolicies = getA1Client(ricName).getPolicies();
        assertThat(ricPolicies.size()).isEqualTo(1);
        Policy ricPolicy = ricPolicies.get(policyId);
        assertThat(ricPolicy.json()).isEqualTo(policy.json());
    }

    @Test
    public void testGetRicForManagedElement_thenReturnCorrectRic() throws Exception {
        String ricName = "ric1";
        String managedElementId = "kista_1";
        addRic(ricName, managedElementId);

        String url = "/ric?managedElementId=" + managedElementId;
        String rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo(ricName);

        // test GET RIC for ManagedElement that does not exist
        url = "/ric?managedElementId=" + "junk";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND);
    }

    private String putPolicyUrl(String serviceName, String ricName, String policyTypeName, String policyInstanceId) {
        String url = "/policy?type=" + policyTypeName + "&instance=" + policyInstanceId + "&ric=" + ricName
            + "&service=" + serviceName;
        return url;
    }

    @Test
    public void testPutPolicy() throws Exception {
        String serviceName = "service1";
        String ricName = "ric1";
        String policyTypeName = "type1";
        String policyInstanceId = "instance1";

        putService(serviceName);
        addPolicyType(policyTypeName, ricName);

        String url = putPolicyUrl(serviceName, ricName, policyTypeName, policyInstanceId);
        final String policyBody = jsonString();
        this.rics.getRic(ricName).setState(Ric.RicState.IDLE);

        restClient().put(url, policyBody).block();

        Policy policy = policies.getPolicy(policyInstanceId);
        assertThat(policy).isNotNull();
        assertThat(policy.id()).isEqualTo(policyInstanceId);
        assertThat(policy.ownerServiceName()).isEqualTo(serviceName);
        assertThat(policy.ric().name()).isEqualTo("ric1");

        url = "/policies";
        String rsp = restClient().get(url).block();
        assertThat(rsp.contains(policyInstanceId)).isTrue();

        // Test of error codes
        url = putPolicyUrl(serviceName, ricName + "XX", policyTypeName, policyInstanceId);
        testErrorCode(restClient().put(url, policyBody), HttpStatus.NOT_FOUND);

        url = putPolicyUrl(serviceName, ricName, policyTypeName + "XX", policyInstanceId);
        testErrorCode(restClient().put(url, policyBody), HttpStatus.NOT_FOUND);

        url = putPolicyUrl(serviceName, ricName, policyTypeName, policyInstanceId);
        this.rics.getRic(ricName).setState(Ric.RicState.SYNCHRONIZING);
        testErrorCode(restClient().put(url, policyBody), HttpStatus.LOCKED);
        this.rics.getRic(ricName).setState(Ric.RicState.IDLE);
    }

    @Test
    public void testRefuseToUpdatePolicy() throws Exception {
        // Test that only the json can be changed for a already created policy
        // In this case service is attempted to be changed
        this.addRic("ric1");
        this.addRic("ricXXX");
        this.addPolicy("instance1", "type1", "service1", "ric1");

        // Try change ric1 -> ricXXX
        String urlWrongRic = putPolicyUrl("service1", "ricXXX", "type1", "instance1");
        testErrorCode(restClient().put(urlWrongRic, jsonString()), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    public void testGetPolicy() throws Exception {
        String url = "/policy?instance=id";
        Policy policy = addPolicy("id", "typeName", "service1", "ric1");
        {
            String rsp = restClient().get(url).block();
            assertThat(rsp).isEqualTo(policy.json());
        }
        {
            policies.remove(policy);
            testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND);
        }
    }

    @Test
    public void testDeletePolicy() throws Exception {
        addPolicy("id", "typeName", "service1", "ric1");
        assertThat(policies.size()).isEqualTo(1);

        String url = "/policy?instance=id";
        ResponseEntity<String> entity = restClient().deleteForEntity(url).block();

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(policies.size()).isEqualTo(0);

        // Delete a non existing policy
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND);
    }

    @Test
    public void testGetPolicySchemas() throws Exception {
        addPolicyType("type1", "ric1");
        addPolicyType("type2", "ric2");

        String url = "/policy_schemas";
        String rsp = this.restClient().get(url).block();
        assertThat(rsp).contains("type1");
        assertThat(rsp).contains("[{\"title\":\"type2\"}");

        List<String> info = parseSchemas(rsp);
        assertThat(info.size()).isEqualTo(2);

        url = "/policy_schemas?ric=ric1";
        rsp = restClient().get(url).block();
        assertThat(rsp).contains("type1");
        info = parseSchemas(rsp);
        assertThat(info.size()).isEqualTo(1);

        // Get schema for non existing RIC
        url = "/policy_schemas?ric=ric1XXX";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND);
    }

    @Test
    public void testGetPolicySchema() throws Exception {
        addPolicyType("type1", "ric1");
        addPolicyType("type2", "ric2");

        String url = "/policy_schema?id=type1";
        String rsp = restClient().get(url).block();
        logger.info(rsp);
        assertThat(rsp).contains("type1");
        assertThat(rsp).contains("title");

        // Get non existing schema
        url = "/policy_schema?id=type1XX";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND);
    }

    @Test
    public void testGetPolicyTypes() throws Exception {
        addPolicyType("type1", "ric1");
        addPolicyType("type2", "ric2");

        String url = "/policy_types";
        String rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo("[\"type2\",\"type1\"]");

        url = "/policy_types?ric=ric1";
        rsp = restClient().get(url).block();
        assertThat(rsp).isEqualTo("[\"type1\"]");

        // Get policy types for non existing RIC
        url = "/policy_types?ric=ric1XXX";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND);
    }

    @Test
    public void testGetPolicies() throws Exception {
        reset();
        addPolicy("id1", "type1", "service1");

        String url = "/policies";
        String rsp = restClient().get(url).block();
        logger.info(rsp);
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

        String url = "/policies?type=type1";
        String rsp = restClient().get(url).block();
        logger.info(rsp);
        assertThat(rsp).contains("id1");
        assertThat(rsp).contains("id2");
        assertThat(rsp.contains("id3")).isFalse();

        url = "/policies?type=type1&service=service2";
        rsp = restClient().get(url).block();
        logger.info(rsp);
        assertThat(rsp.contains("id1")).isFalse();
        assertThat(rsp).contains("id2");
        assertThat(rsp.contains("id3")).isFalse();

        // Test get policies for non existing type
        url = "/policies?type=type1XXX";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND);

        // Test get policies for non existing RIC
        url = "/policies?ric=XXX";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND);
    }

    @Test
    public void testPutAndGetService() throws Exception {
        // PUT
        putService("name", 0);

        // GET one service
        String url = "/services?name=name";
        String rsp = restClient().get(url).block();
        List<ServiceStatus> info = parseList(rsp, ServiceStatus.class);
        assertThat(info.size()).isEqualTo(1);
        ServiceStatus status = info.iterator().next();
        assertThat(status.keepAliveIntervalSeconds).isEqualTo(0);
        assertThat(status.serviceName).isEqualTo("name");

        // GET (all)
        url = "/services";
        rsp = restClient().get(url).block();
        assertThat(rsp.contains("name")).isTrue();
        logger.info(rsp);

        // Keep alive
        url = "/services/keepalive?name=name";
        ResponseEntity<String> entity = restClient().postForEntity(url, null).block();
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);

        // DELETE service
        assertThat(services.size()).isEqualTo(1);
        url = "/services?name=name";
        restClient().delete(url).block();
        assertThat(services.size()).isEqualTo(0);

        // Keep alive, no registerred service
        testErrorCode(restClient().post("/services/keepalive?name=name", ""), HttpStatus.NOT_FOUND);

        // PUT servive with crap payload
        testErrorCode(restClient().put("/service", "crap"), HttpStatus.BAD_REQUEST);
        testErrorCode(restClient().put("/service", "{}"), HttpStatus.BAD_REQUEST);

        // GET non existing servive
        testErrorCode(restClient().get("/services?name=XXX"), HttpStatus.NOT_FOUND);
    }

    @Test
    public void testServiceSupervision() throws Exception {
        putService("service1", 1);
        addPolicyType("type1", "ric1");

        String url = putPolicyUrl("service1", "ric1", "type1", "instance1");
        final String policyBody = jsonString();
        restClient().put(url, policyBody).block();

        assertThat(policies.size()).isEqualTo(1);
        assertThat(services.size()).isEqualTo(1);

        // Timeout after ~1 second
        await().untilAsserted(() -> assertThat(policies.size()).isEqualTo(0));
        assertThat(services.size()).isEqualTo(0);
    }

    @Test
    public void testGetPolicyStatus() throws Exception {
        addPolicy("id", "typeName", "service1", "ric1");
        assertThat(policies.size()).isEqualTo(1);

        String url = "/policy_status?instance=id";
        String rsp = restClient().get(url).block();
        assertThat(rsp.equals("OK")).isTrue();

        // GET non existing policy status
        url = "/policy_status?instance=XXX";
        testErrorCode(restClient().get(url), HttpStatus.NOT_FOUND);
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

    private String createServiceJson(String name, long keepAliveIntervalSeconds) {
        ServiceRegistrationInfo service = new ServiceRegistrationInfo(name, keepAliveIntervalSeconds, "callbackUrl");

        String json = gson.toJson(service);
        return json;
    }

    private void putService(String name) {
        putService(name, 0);
    }

    private void putService(String name, long keepAliveIntervalSeconds) {
        String url = "/service";
        String body = createServiceJson(name, keepAliveIntervalSeconds);
        restClient().put(url, body).block();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String jsonString() {
        return "{\n  \"servingCellNrcgi\": \"1\"\n }";
    }

    private static class ConcurrencyTestRunnable implements Runnable {
        private final RestTemplate restTemplate = new RestTemplate();
        private final String baseUrl;
        static AtomicInteger nextCount = new AtomicInteger(0);
        private final int count;
        private final RicSupervision supervision;

        ConcurrencyTestRunnable(String baseUrl, RicSupervision supervision) {
            this.baseUrl = baseUrl;
            this.count = nextCount.incrementAndGet();
            this.supervision = supervision;
        }

        @Override
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
            restTemplate.put(putUrl, createJsonHttpEntity("{}"));
        }

        private void deletePolicy(String name) {
            String deleteUrl = baseUrl + "/policy?instance=" + name;
            restTemplate.delete(deleteUrl);
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
        logger.info("Concurrency test took " + Duration.between(startTime, Instant.now()));
    }

    private AsyncRestClient restClient() {
        return new AsyncRestClient(baseUrl());
    }

    private void testErrorCode(Mono<?> request, HttpStatus expStatus) {
        StepVerifier.create(request) //
            .expectSubscription() //
            .expectErrorMatches(t -> checkWebClientError(t, expStatus)) //
            .verify();
    }

    private boolean checkWebClientError(Throwable t, HttpStatus expStatus) {
        assertTrue(t instanceof WebClientResponseException);
        WebClientResponseException e = (WebClientResponseException) t;
        assertThat(e.getStatusCode()).isEqualTo(expStatus);
        return true;
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
        return addRic(ricName, null);
    }

    private Ric addRic(String ricName, String managedElement) {
        if (rics.get(ricName) != null) {
            return rics.get(ricName);
        }
        List<String> mes = new ArrayList<>();
        if (managedElement != null) {
            mes.add(managedElement);
        }
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

    private static HttpEntity<String> createJsonHttpEntity(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<String>(content, headers);
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
