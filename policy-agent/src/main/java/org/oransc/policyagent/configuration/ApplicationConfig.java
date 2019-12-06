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
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Vector;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.oransc.policyagent.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import reactor.core.publisher.Mono;

@EnableConfigurationProperties
@ConfigurationProperties("app")
public class ApplicationConfig {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    @Value("#{systemEnvironment}")
    Properties systemEnvironment;

    @NotEmpty
    private String filepath;

    private Vector<RicConfig> ricConfigs;

    @Autowired
    public ApplicationConfig() {
    }

    public synchronized void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public Vector<RicConfig> getRicConfigs() {
        return this.ricConfigs;
    }

    public Optional<RicConfig> lookupRicConfigForManagedElement(String managedElementId) {
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
        loadConfigurationFromFile(this.filepath);
    }

    Mono<Environment.Variables> getEnvironment(Properties systemEnvironment) {
        return Environment.readEnvironmentVariables(systemEnvironment);
    }

    /**
     * Reads the configuration from file.
     */
    protected void loadConfigurationFromFile(String filepath) {
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
            this.ricConfigs = appParser.getRicConfigs();
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
