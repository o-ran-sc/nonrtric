/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2020 Nordix Foundation
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

package org.oransc.enrichment.configuration;

import javax.validation.constraints.NotEmpty;

import lombok.Getter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@ConfigurationProperties()
public class ApplicationConfig {
    @NotEmpty
    @Getter
    @Value("${app.filepath}")
    private String localConfigurationFilePath;

    @Getter
    @Value("${app.vardata-directory}")
    private String vardataDirectory;

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

    public WebClientConfig getWebClientConfig() {
        return ImmutableWebClientConfig.builder() //
            .keyStoreType(this.sslKeyStoreType) //
            .keyStorePassword(this.sslKeyStorePassword) //
            .keyStore(this.sslKeyStore) //
            .keyPassword(this.sslKeyPassword) //
            .isTrustStoreUsed(this.sslTrustStoreUsed) //
            .trustStore(this.sslTrustStore) //
            .trustStorePassword(this.sslTrustStorePassword) //
            .build();
    }

}
