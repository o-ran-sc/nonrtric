/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2021 Nordix Foundation
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

package org.oran.dmaapadapter.configuration;

import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import lombok.Getter;
import lombok.Setter;

import org.oran.dmaapadapter.configuration.WebClientConfig.HttpProxyConfig;
import org.oran.dmaapadapter.repository.InfoType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter
    @Value("${app.configuration-filepath}")
    private String localConfigurationFilePath;

    @Value("${server.ssl.key-store-type}")
    private String sslKeyStoreType = "";

    @Value("${server.ssl.key-store-password}")
    private String sslKeyStorePassword = "";

    @Value("${server.ssl.key-store}")
    private String sslKeyStore = "";

    @Value("${server.ssl.key-password}")
    private String sslKeyPassword = "";

    @Value("${app.webclient.trust-store-used}")
    private boolean sslTrustStoreUsed = false;

    @Value("${app.webclient.trust-store-password}")
    private String sslTrustStorePassword = "";

    @Value("${app.webclient.trust-store}")
    private String sslTrustStore = "";

    @Value("${app.webclient.http.proxy-host:\"\"}")
    private String httpProxyHost = "";

    @Value("${app.webclient.http.proxy-port:0}")
    private int httpProxyPort = 0;

    @Getter
    @Setter
    @Value("${server.port}")
    private int localServerHttpPort;

    @Getter
    @Value("${app.ics-base-url}")
    private String icsBaseUrl;

    @Getter
    @Value("${app.dmaap-adapter-base-url}")
    private String selfUrl;

    @Getter
    @Value("${app.dmaap-base-url}")
    private String dmaapBaseUrl;

    @Getter
    @Value("${app.kafka.bootstrap-servers:}")
    private String kafkaBootStrapServers;

    private WebClientConfig webClientConfig = null;

    public WebClientConfig getWebClientConfig() {
        if (this.webClientConfig == null) {
            HttpProxyConfig httpProxyConfig = ImmutableHttpProxyConfig.builder() //
                    .httpProxyHost(this.httpProxyHost) //
                    .httpProxyPort(this.httpProxyPort) //
                    .build();

            this.webClientConfig = ImmutableWebClientConfig.builder() //
                    .keyStoreType(this.sslKeyStoreType) //
                    .keyStorePassword(this.sslKeyStorePassword) //
                    .keyStore(this.sslKeyStore) //
                    .keyPassword(this.sslKeyPassword) //
                    .isTrustStoreUsed(this.sslTrustStoreUsed) //
                    .trustStore(this.sslTrustStore) //
                    .trustStorePassword(this.sslTrustStorePassword) //
                    .httpProxyConfig(httpProxyConfig) //
                    .build();
        }
        return this.webClientConfig;
    }

    // Adapter to parse the json format of the configuration file.
    static class ConfigFile {
        Collection<InfoType> types;
    }

    public Collection<InfoType> getTypes() {
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().create();

        try {
            String configJson = Files.readString(Path.of(getLocalConfigurationFilePath()), Charset.defaultCharset());
            ConfigFile configData = gson.fromJson(configJson, ConfigFile.class);
            return configData.types;
        } catch (Exception e) {
            logger.error("Could not load configuration file {}", getLocalConfigurationFilePath());
            return Collections.emptyList();
        }

    }

}
