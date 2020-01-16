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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapterFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Vector;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClientFactory;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsRequests;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsRequest;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.EnvProperties;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.RequestDiagnosticContext;
import org.oransc.policyagent.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@EnableConfigurationProperties
@ConfigurationProperties("app")
public class ApplicationConfig {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    @Value("#{systemEnvironment}")
    Properties systemEnvironment;

    private Disposable refreshConfigTask = null;
    private Collection<Observer> observers = new Vector<>();

    private Map<String, RicConfig> ricConfigs = new HashMap<>();

    @Autowired
    public ApplicationConfig() {
    }

    protected String getLocalConfigurationFilePath() {
        return null;
    }

    public synchronized Collection<RicConfig> getRicConfigs() {
        return this.ricConfigs.values();
    }

    public synchronized Optional<RicConfig> lookupRicConfigForManagedElement(String managedElementId) {
        for (RicConfig ricConfig : getRicConfigs()) {
            if (ricConfig.managedElementIds().contains(managedElementId)) {
                return Optional.of(ricConfig);
            }
        }
        return Optional.empty();
    }

    public RicConfig getRic(String ricName) throws ServiceException {
        for (RicConfig ricConfig : getRicConfigs()) {
            if (ricConfig.name().equals(ricName)) {
                return ricConfig;
            }
        }
        throw new ServiceException("Could not find ric: " + ricName);
    }

    public void initialize() {
        stop();
        loadConfigurationFromFile();

        refreshConfigTask = createRefreshTask() //
            .subscribe(notUsed -> logger.info("Refreshed configuration data"),
                throwable -> logger.error("Configuration refresh terminated due to exception", throwable),
                () -> logger.error("Configuration refresh terminated"));
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

    Mono<EnvProperties> getEnvironment(Properties systemEnvironment) {
        return EnvironmentProcessor.readEnvironmentVariables(systemEnvironment);
    }

    Flux<ApplicationConfig> createRefreshTask() {
        return getEnvironment(systemEnvironment) //
            .flatMap(this::createCbsClient) //
            .flatMapMany(this::periodicConfigurationUpdates) //
            .map(this::parseRicConfigurationfromConsul) //
            .onErrorResume(this::onErrorResume);
    }

    Mono<CbsClient> createCbsClient(EnvProperties env) {
        return CbsClientFactory.createCbsClient(env);
    }

    private Flux<JsonObject> periodicConfigurationUpdates(CbsClient cbsClient) {
        final Duration initialDelay = Duration.ZERO;
        final Duration refreshPeriod = Duration.ofMinutes(1);
        final CbsRequest getConfigRequest = CbsRequests.getAll(RequestDiagnosticContext.create());
        return cbsClient.updates(getConfigRequest, initialDelay, refreshPeriod);
    }

    private <R> Mono<R> onErrorResume(Throwable trowable) {
        logger.error("Could not refresh application configuration {}", trowable.toString());
        return Mono.empty();
    }

    private ApplicationConfig parseRicConfigurationfromConsul(JsonObject jsonObject) {
        try {
            ApplicationConfigParser parser = new ApplicationConfigParser();
            parser.parse(jsonObject);
            setConfiguration(parser.getRicConfigs());

        } catch (ServiceException e) {
            logger.error("Could not parse configuration {}", e.toString(), e);
        }
        return this;
    }

    private class Notification {
        final RicConfig ric;
        final RicConfigUpdate event;

        Notification(RicConfig ric, RicConfigUpdate event) {
            this.ric = ric;
            this.event = event;
        }
    }

    private void setConfiguration(@NotNull Collection<RicConfig> ricConfigs) {
        Collection<Notification> notifications = new Vector<>();
        synchronized (this) {
            Map<String, RicConfig> newRicConfigs = new HashMap<>();
            for (RicConfig newConfig : ricConfigs) {
                RicConfig oldConfig = this.ricConfigs.get(newConfig.name());
                if (oldConfig == null) {
                    newRicConfigs.put(newConfig.name(), newConfig);
                    notifications.add(new Notification(newConfig, RicConfigUpdate.ADDED));
                    this.ricConfigs.remove(newConfig.name());
                } else if (!newConfig.equals(newConfig)) {
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
    }

    private void notifyObservers(Collection<Notification> notifications) {
        for (Observer observer : this.observers) {
            for (Notification notif : notifications) {
                observer.onRicConfigUpdate(notif.ric, notif.event);
            }
        }
    }

    public void stop() {
        if (refreshConfigTask != null) {
            refreshConfigTask.dispose();
            refreshConfigTask = null;
        }
    }

    /**
     * Reads the configuration from file.
     */
    public void loadConfigurationFromFile() {
        String filepath = getLocalConfigurationFilePath();
        if (filepath == null) {
            logger.debug("No localconfiguration file used");
            return;
        }
        GsonBuilder gsonBuilder = new GsonBuilder();
        ServiceLoader.load(TypeAdapterFactory.class).forEach(gsonBuilder::registerTypeAdapterFactory);

        try (InputStream inputStream = createInputStream(filepath)) {
            JsonParser parser = new JsonParser();
            JsonObject rootObject = getJsonElement(parser, inputStream).getAsJsonObject();
            if (rootObject == null) {
                throw new JsonSyntaxException("Root is not a json object");
            }
            ApplicationConfigParser appParser = new ApplicationConfigParser();
            appParser.parse(rootObject);
            setConfiguration(appParser.getRicConfigs());
            logger.info("Local configuration file loaded: {}", filepath);
        } catch (JsonSyntaxException | ServiceException | IOException e) {
            logger.trace("Local configuration file not loaded: {}", filepath, e);
        }
    }

    JsonElement getJsonElement(JsonParser parser, InputStream inputStream) {
        return parser.parse(new InputStreamReader(inputStream));
    }

    InputStream createInputStream(@NotNull String filepath) throws IOException {
        return new BufferedInputStream(new FileInputStream(filepath));
    }
}
