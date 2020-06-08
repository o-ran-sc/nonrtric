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

package org.oransc.policyagent.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.validation.constraints.NotEmpty;

import lombok.Getter;

import org.oransc.policyagent.exceptions.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import reactor.core.publisher.Flux;

@EnableConfigurationProperties
@ConfigurationProperties()
public class ApplicationConfig {
    @NotEmpty
    @Getter
    @Value("${app.filepath}")
    private String localConfigurationFilePath;

    @Value("${app.webclient.trust-store-used}")
    private boolean sslTrustStoreUsed = false;

    @Value("${app.webclient.trust-store-password}")
    private String sslTrustStorePassword = "";

    @Value("${app.webclient.trust-store}")
    private String sslTrustStore = "";

    private Map<String, RicConfig> ricConfigs = new HashMap<>();

    private Properties dmaapPublisherConfig;

    private Properties dmaapConsumerConfig;

    private Map<String, ControllerConfig> controllerConfigs = new HashMap<>();

    public synchronized Properties getDmaapPublisherConfig(){
        return this.dmaapPublisherConfig;
    }

    public synchronized Properties getDmaapConsumerConfig(){
        return this.dmaapConsumerConfig;
    }

    public synchronized Collection<RicConfig> getRicConfigs() {
        return this.ricConfigs.values();
    }

    public WebClientConfig getWebClientConfig() {
        return ImmutableWebClientConfig.builder() //
            .isTrustStoreUsed(this.sslTrustStoreUsed) //
            .trustStore(this.sslTrustStore) //
            .trustStorePassword(this.sslTrustStorePassword) //
            .build();
    }

    public synchronized ControllerConfig getControllerConfig(String name) throws ServiceException {
        ControllerConfig controllerConfig = this.controllerConfigs.get(name);
        if (controllerConfig == null) {
            throw new ServiceException("Could not find controller config: " + name);
        }
        return controllerConfig;
    }

    public synchronized RicConfig getRic(String ricName) throws ServiceException {
        RicConfig ricConfig = this.ricConfigs.get(ricName);
        if (ricConfig == null) {
            throw new ServiceException("Could not find ric configuration: " + ricName);
        }
        return ricConfig;
    }

    public static class RicConfigUpdate {
        public enum Type {
            ADDED, CHANGED, REMOVED
        }

        @Getter
        private final RicConfig ricConfig;
        @Getter
        private final Type type;

        RicConfigUpdate(RicConfig ric, Type event) {
            this.ricConfig = ric;
            this.type = event;
        }
    }

    public synchronized Flux<RicConfigUpdate> setConfiguration(
        ApplicationConfigParser.ConfigParserResult parserResult) {

        Collection<RicConfigUpdate> modifications = new ArrayList<>();
        this.dmaapPublisherConfig = parserResult.dmaapPublisherConfig();
        this.dmaapConsumerConfig = parserResult.dmaapConsumerConfig();
        this.controllerConfigs = parserResult.controllerConfigs();

        Map<String, RicConfig> newRicConfigs = new HashMap<>();
        for (RicConfig newConfig : parserResult.ricConfigs()) {
            RicConfig oldConfig = this.ricConfigs.get(newConfig.name());
            this.ricConfigs.remove(newConfig.name());
            if (oldConfig == null) {
                newRicConfigs.put(newConfig.name(), newConfig);
                modifications.add(new RicConfigUpdate(newConfig, RicConfigUpdate.Type.ADDED));
            } else if (!newConfig.equals(oldConfig)) {
                modifications.add(new RicConfigUpdate(newConfig, RicConfigUpdate.Type.CHANGED));
                newRicConfigs.put(newConfig.name(), newConfig);
            } else {
                newRicConfigs.put(oldConfig.name(), oldConfig);
            }
        }
        for (RicConfig deletedConfig : this.ricConfigs.values()) {
            modifications.add(new RicConfigUpdate(deletedConfig, RicConfigUpdate.Type.REMOVED));
        }
        this.ricConfigs = newRicConfigs;

        return Flux.fromIterable(modifications);
    }
}
