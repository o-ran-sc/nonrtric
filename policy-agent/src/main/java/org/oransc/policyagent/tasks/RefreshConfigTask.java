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

package org.oransc.policyagent.tasks;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapterFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;

import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClientFactory;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsRequests;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsRequest;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.EnvProperties;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.RequestDiagnosticContext;
import org.oransc.policyagent.clients.A1ClientFactory;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.ApplicationConfig.RicConfigUpdate;
import org.oransc.policyagent.configuration.ApplicationConfigParser;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.Policies;
import org.oransc.policyagent.repository.PolicyTypes;
import org.oransc.policyagent.repository.Ric;
import org.oransc.policyagent.repository.Rics;
import org.oransc.policyagent.repository.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Regularly refreshes the configuration from Consul or from a local
 * configuration file.
 */
@Component
public class RefreshConfigTask {

    private static final Logger logger = LoggerFactory.getLogger(RefreshConfigTask.class);

    @Value("#{systemEnvironment}")
    public Properties systemEnvironment;

    final ApplicationConfig appConfig;
    @Getter(AccessLevel.PROTECTED)
    private Disposable refreshTask = null;
    private boolean isConsulUsed = false;

    private final Rics rics;
    private final A1ClientFactory a1ClientFactory;
    private final Policies policies;
    private final Services services;
    private final PolicyTypes policyTypes;
    private static final Duration FILE_CONFIG_REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final Duration CONSUL_CONFIG_REFRESH_INTERVAL = Duration.ofMinutes(1);

    @Autowired
    public RefreshConfigTask(ApplicationConfig appConfig, Rics rics, Policies policies, Services services,
        PolicyTypes policyTypes, A1ClientFactory a1ClientFactory) {
        this.appConfig = appConfig;
        this.rics = rics;
        this.policies = policies;
        this.services = services;
        this.policyTypes = policyTypes;
        this.a1ClientFactory = a1ClientFactory;
    }

    public void start() {
        logger.debug("Starting refreshConfigTask");
        stop();
        refreshTask = createRefreshTask() //
            .subscribe(notUsed -> logger.debug("Refreshed configuration data"),
                throwable -> logger.error("Configuration refresh terminated due to exception", throwable),
                () -> logger.error("Configuration refresh terminated"));
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.dispose();
        }
    }

    Flux<RicConfigUpdate.Type> createRefreshTask() {
        Flux<JsonObject> loadFromFile = Flux.interval(Duration.ZERO, FILE_CONFIG_REFRESH_INTERVAL) //
            .filter(notUsed -> configFileExists()) //
            .filter(notUsed -> !this.isConsulUsed) //
            .flatMap(notUsed -> loadConfigurationFromFile()) //
            .onErrorResume(this::ignoreError) //
            .doOnNext(json -> logger.debug("loadFromFile")) //
            .doOnTerminate(() -> logger.error("loadFromFile Terminate"));

        Flux<JsonObject> loadFromConsul = getEnvironment(systemEnvironment) //
            .flatMap(this::createCbsClient) //
            .flatMapMany(this::periodicConfigurationUpdates) //
            .onErrorResume(this::ignoreError) //
            .doOnNext(json -> logger.debug("loadFromConsul")) //
            .doOnNext(json -> this.isConsulUsed = true) //
            .doOnTerminate(() -> logger.error("loadFromConsul Terminate"));

        return Flux.merge(loadFromFile, loadFromConsul) //
            .flatMap(this::parseConfiguration) //
            .flatMap(this::updateConfig) //
            .doOnNext(this::handleUpdatedRicConfig) //
            .flatMap(configUpdate -> Flux.just(configUpdate.getType())) //
            .doOnTerminate(() -> logger.error("Configuration refresh task is terminated"));
    }

    Mono<EnvProperties> getEnvironment(Properties systemEnvironment) {
        return EnvironmentProcessor.readEnvironmentVariables(systemEnvironment);
    }

    Mono<CbsClient> createCbsClient(EnvProperties env) {
        return CbsClientFactory.createCbsClient(env);
    }

    private Flux<JsonObject> periodicConfigurationUpdates(CbsClient cbsClient) {
        final Duration initialDelay = Duration.ZERO;
        final CbsRequest getConfigRequest = CbsRequests.getAll(RequestDiagnosticContext.create());
        return cbsClient.updates(getConfigRequest, initialDelay, CONSUL_CONFIG_REFRESH_INTERVAL);
    }

    private <R> Mono<R> ignoreError(Throwable throwable) {
        String errMsg = throwable.toString();
        logger.warn("Could not refresh application configuration. {}", errMsg);
        return Mono.empty();
    }

    private Mono<ApplicationConfigParser.ConfigParserResult> parseConfiguration(JsonObject jsonObject) {
        try {
            ApplicationConfigParser parser = new ApplicationConfigParser();
            return Mono.just(parser.parse(jsonObject));
        } catch (ServiceException e) {
            logger.error("Could not parse configuration {}", e.toString(), e);
            return Mono.error(e);
        }
    }

    private Flux<RicConfigUpdate> updateConfig(ApplicationConfigParser.ConfigParserResult config) {
        return this.appConfig.setConfiguration(config);
    }

    boolean configFileExists() {
        String filepath = appConfig.getLocalConfigurationFilePath();
        return (filepath != null && (new File(filepath).exists()));
    }

    private void handleUpdatedRicConfig(RicConfigUpdate updatedInfo) {
        synchronized (this.rics) {
            String ricName = updatedInfo.getRicConfig().name();
            RicConfigUpdate.Type event = updatedInfo.getType();
            if (event == RicConfigUpdate.Type.ADDED) {
                addRic(updatedInfo.getRicConfig());
            } else if (event == RicConfigUpdate.Type.REMOVED) {
                rics.remove(ricName);
                this.policies.removePoliciesForRic(ricName);
            } else if (event == RicConfigUpdate.Type.CHANGED) {
                Ric ric = this.rics.get(ricName);
                if (ric == null) {
                    // Should not happen,just for robustness
                    addRic(updatedInfo.getRicConfig());
                } else {
                    ric.setRicConfig(updatedInfo.getRicConfig());
                }
            }
        }
    }

    private void addRic(RicConfig config) {
        Ric ric = new Ric(config);
        this.rics.put(ric);
        runRicSynchronization(ric);
    }

    void runRicSynchronization(Ric ric) {
        RicSynchronizationTask synchronizationTask =
            new RicSynchronizationTask(a1ClientFactory, policyTypes, policies, services);
        synchronizationTask.run(ric);
    }

    /**
     * Reads the configuration from file.
     */
    Flux<JsonObject> loadConfigurationFromFile() {
        String filepath = appConfig.getLocalConfigurationFilePath();
        GsonBuilder gsonBuilder = new GsonBuilder();
        ServiceLoader.load(TypeAdapterFactory.class).forEach(gsonBuilder::registerTypeAdapterFactory);

        try (InputStream inputStream = createInputStream(filepath)) {
            JsonObject rootObject = getJsonElement(inputStream).getAsJsonObject();
            ApplicationConfigParser appParser = new ApplicationConfigParser();
            appParser.parse(rootObject);
            logger.debug("Local configuration file loaded: {}", filepath);
            return Flux.just(rootObject);
        } catch (JsonSyntaxException | ServiceException | IOException e) {
            logger.debug("Local configuration file not loaded: {}", filepath, e);
            return Flux.empty();
        }
    }

    JsonElement getJsonElement(InputStream inputStream) {
        return JsonParser.parseReader(new InputStreamReader(inputStream));
    }

    InputStream createInputStream(@NotNull String filepath) throws IOException {
        return new BufferedInputStream(new FileInputStream(filepath));
    }
}
