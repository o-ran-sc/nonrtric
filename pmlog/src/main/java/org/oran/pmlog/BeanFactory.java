/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2023 Nordix Foundation
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

package org.oran.pmlog;

import org.apache.catalina.connector.Connector;
import org.oran.pmlog.configuration.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanFactory {

    @Value("${server.http-port}")
    private int httpPort = 0;

    @Bean
    public ApplicationConfig getApplicationConfig() {
        return new ApplicationConfig();
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        if (httpPort > 0) {
            tomcat.addAdditionalTomcatConnectors(getHttpConnector(httpPort));
        }
        return tomcat;
    }

    private static Connector getHttpConnector(int httpPort) {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(httpPort);
        connector.setSecure(false);
        return connector;
    }

    @Bean
    public KafkaTopicListener getKafkaTopicListener(@Autowired ApplicationConfig appConfig) {
        return new KafkaTopicListener(appConfig);
    }

    @Bean
    public InfluxStore getInfluxStore(@Autowired ApplicationConfig appConfig, @Autowired KafkaTopicListener listener) {

        InfluxStore distr = new InfluxStore(appConfig);
        distr.start(listener.getFlux());
        return distr;
    }

}
