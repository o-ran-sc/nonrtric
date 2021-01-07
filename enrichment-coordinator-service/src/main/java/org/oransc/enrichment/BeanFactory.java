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

package org.oransc.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.invoke.MethodHandles;

import org.apache.catalina.connector.Connector;
import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.controllers.producer.ProducerCallbacks;
import org.oransc.enrichment.repository.EiJobs;
import org.oransc.enrichment.repository.EiProducers;
import org.oransc.enrichment.repository.EiTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BeanFactory {

    @Value("${server.http-port}")
    private int httpPort = 0;

    private final ApplicationConfig applicationConfig = new ApplicationConfig();
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ProducerCallbacks producerCallbacks;
    private EiTypes eiTypes;
    private EiJobs eiJobs;
    private EiProducers eiProducers;

    @Bean
    public ObjectMapper mapper() {
        return new ObjectMapper();
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        if (httpPort > 0) {
            tomcat.addAdditionalTomcatConnectors(getHttpConnector(httpPort));
        }
        return tomcat;
    }

    @Bean
    public EiJobs eiJobs() {
        if (eiJobs == null) {
            eiJobs = new EiJobs(getApplicationConfig(), producerCallbacks());
            try {
                eiJobs.restoreJobsFromDatabase();
            } catch (Exception e) {
                logger.error("Could not restore jobs from database: {}", e.getMessage());
            }
        }
        return eiJobs;
    }

    @Bean
    public EiTypes eiTypes() {
        if (this.eiTypes == null) {
            eiTypes = new EiTypes(getApplicationConfig());
            try {
                eiTypes.restoreTypesFromDatabase();
            } catch (Exception e) {
                logger.error("Could not restore EI types from database: {}", e.getMessage());
            }
        }
        return eiTypes;
    }

    @Bean
    public ProducerCallbacks producerCallbacks() {
        if (this.producerCallbacks == null) {
            producerCallbacks = new ProducerCallbacks(getApplicationConfig());
        }
        return this.producerCallbacks;
    }

    @Bean
    public ApplicationConfig getApplicationConfig() {
        return this.applicationConfig;
    }

    private static Connector getHttpConnector(int httpPort) {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(httpPort);
        connector.setSecure(false);
        return connector;
    }

}
