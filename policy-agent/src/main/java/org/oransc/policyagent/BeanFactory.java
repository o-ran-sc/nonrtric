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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Rics;
import org.oransc.policyagent.repository.Services;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BeanFactory {
    @Bean
    public Policies getPolicies() {
        return new Policies();
    }

    @Bean
    public PolicyTypes getPolicyTypes() {
        return new PolicyTypes();
    }

    @Bean
    public Rics getRics() {
        return new Rics();
    }

    @Bean
    public ApplicationConfig getApplicationConfig() {
        return new ApplicationConfig();
    }

    @Bean
    Services getServices() {
        return new Services();
    }

    @Bean
    A1ClientFactory getA1ClientFactory() {
        return new A1ClientFactory(getApplicationConfig());
    }

    @Bean
    public ObjectMapper mapper() {
        return new ObjectMapper();
    }

}
