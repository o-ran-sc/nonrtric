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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Getter;

import org.oransc.policyagent.exceptions.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@ConfigurationProperties("app")
public class ApplicationConfig {
    @NotEmpty
    private String filepath;

    private Collection<Observer> observers = new Vector<>();
    private Map<String, RicConfig> ricConfigs = new HashMap<>();
    @Getter
    private Properties dmaapPublisherConfig;
    @Getter
    private Properties dmaapConsumerConfig;

    @Autowired
    public ApplicationConfig() {
    }

    public String getLocalConfigurationFilePath() {
        return this.filepath;
    }

    /*
     * Do not remove, used by framework!
     */
    public synchronized void setFilepath(String filepath) {
        this.filepath = filepath;
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

    public static enum RicConfigUpdate {
        ADDED, CHANGED, REMOVED
    }

    public interface Observer {
        void onRicConfigUpdate(RicConfig ric, RicConfigUpdate event);
    }

    public void addObserver(Observer o) {
        this.observers.add(o);
    }

    private class Notification {
        final RicConfig ric;
        final RicConfigUpdate event;

        Notification(RicConfig ric, RicConfigUpdate event) {
            this.ric = ric;
            this.event = event;
        }
    }

    public void setConfiguration(@NotNull Collection<RicConfig> ricConfigs, Properties dmaapPublisherConfig,
        Properties dmaapConsumerConfig) {

        Collection<Notification> notifications = new Vector<>();
        synchronized (this) {
            Map<String, RicConfig> newRicConfigs = new HashMap<>();
            for (RicConfig newConfig : ricConfigs) {
                RicConfig oldConfig = this.ricConfigs.get(newConfig.name());
                if (oldConfig == null) {
                    newRicConfigs.put(newConfig.name(), newConfig);
                    notifications.add(new Notification(newConfig, RicConfigUpdate.ADDED));
                    this.ricConfigs.remove(newConfig.name());
                } else if (!newConfig.equals(oldConfig)) {
                    notifications.add(new Notification(newConfig, RicConfigUpdate.CHANGED));
                    newRicConfigs.put(newConfig.name(), newConfig);
                    this.ricConfigs.remove(newConfig.name());
                } else {
                    newRicConfigs.put(oldConfig.name(), oldConfig);
                }
            }
            for (RicConfig deletedConfig : this.ricConfigs.values()) {
                notifications.add(new Notification(deletedConfig, RicConfigUpdate.REMOVED));
            }
            this.ricConfigs = newRicConfigs;
        }

        notifyObservers(notifications);
        this.dmaapPublisherConfig = dmaapPublisherConfig;
        this.dmaapConsumerConfig = dmaapConsumerConfig;

    }

    private void notifyObservers(Collection<Notification> notifications) {
        for (Observer observer : this.observers) {
            for (Notification notif : notifications) {
                observer.onRicConfigUpdate(notif.ric, notif.event);
            }
        }
    }
}
