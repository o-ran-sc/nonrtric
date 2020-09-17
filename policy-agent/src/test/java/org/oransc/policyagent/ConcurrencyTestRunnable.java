/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2020 Nordix Foundation
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

import java.util.concurrent.atomic.AtomicInteger;

import org.oransc.policyagent.clients.AsyncRestClient;
import org.oransc.policyagent.repository.ImmutablePolicy;
import org.oransc.policyagent.repository.Policy;
import org.oransc.policyagent.repository.PolicyType;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;
import org.oransc.policyagent.tasks.RicSupervision;
import org.oransc.policyagent.utils.MockA1Client;
import org.oransc.policyagent.utils.MockA1ClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

/**
 * Invoke operations over the NBI and start synchronizations in a separate
 * thread. For test of robustness using concurrent clients.
 */
class ConcurrencyTestRunnable implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyTestRunnable.class);
    private final AsyncRestClient webClient;
    static AtomicInteger nextCount = new AtomicInteger(0);
    private final int count;
    private final RicSupervision supervision;
    private final MockA1ClientFactory a1ClientFactory;
    private final Rics rics;
    private final PolicyTypes types;
    private boolean failed = false;

    ConcurrencyTestRunnable(AsyncRestClient webClient, RicSupervision supervision, MockA1ClientFactory a1ClientFactory,
        Rics rics, PolicyTypes types) {
        this.count = nextCount.incrementAndGet();
        this.supervision = supervision;
        this.a1ClientFactory = a1ClientFactory;
        this.rics = rics;
        this.types = types;
        this.webClient = webClient;
    }

    private void printStatusInfo() {
        try {
            String url = "/actuator/metrics/jvm.threads.live";
            ResponseEntity<String> result = webClient.getForEntity(url).block();
            System.out.println(Thread.currentThread() + result.getBody());

            url = "/rics";
            result = webClient.getForEntity(url).block();
            System.out.println(Thread.currentThread() + result.getBody());

        } catch (Exception e) {
            logger.error(Thread.currentThread() + "Concurrency test printStatusInfo exception " + e.toString());
        }
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < 500; ++i) {
                if (i % 100 == 0) {
                    createInconsistency();
                    this.supervision.checkAllRics();
                }
                String name = "policy:" + count + ":" + i;
                putPolicy(name);
                putPolicy(name + "-");
                listPolicies();
                listTypes();
                deletePolicy(name);
                deletePolicy(name + "-");
            }
        } catch (Exception e) {
            logger.error("Concurrency test exception " + e.toString());
            printStatusInfo();
            failed = true;
        }
    }

    public boolean isFailed() {
        return this.failed;
    }

    private Policy createPolicyObject(String id) {
        Ric ric = this.rics.get("ric");
        PolicyType type = this.types.get("type1");
        return ImmutablePolicy.builder() //
            .id(id) //
            .json("{}") //
            .type(type) //
            .ric(ric) //
            .ownerServiceName("") //
            .lastModified("") //
            .isTransient(false) //
            .build();
    }

    private void createInconsistency() {
        MockA1Client client = a1ClientFactory.getOrCreateA1Client("ric");
        Policy policy = createPolicyObject("junk");
        client.putPolicy(policy).block();

    }

    private void listPolicies() {
        String uri = "/policies";
        webClient.getForEntity(uri).block();
    }

    private void listTypes() {
        String uri = "/policy_types";
        webClient.getForEntity(uri).block();
    }

    private void putPolicy(String name) {
        String putUrl = "/policy?type=type1&id=" + name + "&ric=ric&service=service1";
        webClient.putForEntity(putUrl, "{}").block();
    }

    private void deletePolicy(String name) {
        String deleteUrl = "/policy?id=" + name;
        webClient.delete(deleteUrl).block();
    }
}
