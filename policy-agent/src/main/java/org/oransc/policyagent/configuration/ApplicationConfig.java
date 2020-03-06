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
import javax.validation.constraints.NotNull;

import lombok.Getter;

import org.oransc.policyagent.exceptions.ServiceException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import reactor.core.publisher.Flux;

@EnableConfigurationProperties
@ConfigurationProperties("app")
public class ApplicationConfig {
    @NotEmpty
    private String filepath;

    @NotEmpty
    private String a1ControllerBaseUrl;

    @NotEmpty
    private String a1ControllerUsername;

    @NotEmpty
    private String a1ControllerPassword;

    private Map<String, RicConfig> ricConfigs = new HashMap<>();
    @Getter
    private Properties dmaapPublisherConfig;
    @Getter
    private Properties dmaapConsumerConfig;

    public String getLocalConfigurationFilePath() {
        return this.filepath;
    }

    public synchronized String getA1ControllerBaseUrl() {
        return this.a1ControllerBaseUrl;
    }

    public synchronized String getA1ControllerUsername() {
        return this.a1ControllerUsername;
    }

    public synchronized String getA1ControllerPassword() {
        return this.a1ControllerPassword;
    }

    /*
     * Do not remove, used by framework!
     */
    public synchronized void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public synchronized void setA1ControllerBaseUrl(String a1ControllerBaseUrl) {
        this.a1ControllerBaseUrl = a1ControllerBaseUrl;
    }

    public synchronized void setA1ControllerUsername(String a1ControllerUsername) {
        this.a1ControllerUsername = a1ControllerUsername;
    }

    public synchronized void setA1ControllerPassword(String a1ControllerPassword) {
        this.a1ControllerPassword = a1ControllerPassword;
    }

    public synchronized Collection<RicConfig> getRicConfigs() {
        return this.ricConfigs.values();
    }

    public RicConfig getRic(String ricName) throws ServiceException {
        for (RicConfig ricConfig : getRicConfigs()) {
            if (ricConfig.name().equals(ricName)) {
                return ricConfig;
            }
        }
        throw new ServiceException("Could not find ric: " + ricName);
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

    public synchronized Flux<RicConfigUpdate> setConfiguration(@NotNull Collection<RicConfig> ricConfigs,
        Properties dmaapPublisherConfig, Properties dmaapConsumerConfig) {

        Collection<RicConfigUpdate> modifications = new ArrayList<>();
        this.dmaapPublisherConfig = dmaapPublisherConfig;
        this.dmaapConsumerConfig = dmaapConsumerConfig;

        Map<String, RicConfig> newRicConfigs = new HashMap<>();
        for (RicConfig newConfig : ricConfigs) {
            RicConfig oldConfig = this.ricConfigs.get(newConfig.name());
            if (oldConfig == null) {
                newRicConfigs.put(newConfig.name(), newConfig);
                modifications.add(new RicConfigUpdate(newConfig, RicConfigUpdate.Type.ADDED));
                this.ricConfigs.remove(newConfig.name());
            } else if (!newConfig.equals(oldConfig)) {
                modifications.add(new RicConfigUpdate(newConfig, RicConfigUpdate.Type.CHANGED));
                newRicConfigs.put(newConfig.name(), newConfig);
                this.ricConfigs.remove(newConfig.name());
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
