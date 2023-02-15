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

package org.oran.pmlog.configuration;

import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@ToString
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static com.google.gson.Gson gson = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();

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

    @Value("${app.webclient.http.proxy-host:}")
    private String httpProxyHost = "";

    @Value("${app.webclient.http.proxy-port:0}")
    private int httpProxyPort = 0;

    @Getter
    @Setter
    @Value("${server.port}")
    private int localServerHttpPort;

    @Getter
    @Value("${app.kafka.max-poll-records:300}")
    private int kafkaMaxPollRecords;

    @Getter
    @Value("${app.kafka.group-id}")
    private String kafkaGroupId;

    @Getter
    @Value("${app.kafka.client-id}")
    private String kafkaClientId;

    @Getter
    @Value("${app.influx.url}")
    private String influxUrl;

    @Getter
    @Value("${app.influx.access-token}")
    private String influxAccessToken;

    @Getter
    @Value("${app.ics-base-url}")
    private String icsBaseUrl;

    @Getter
    @Value("${app.influx.user}")
    private String influxUser;

    @Getter
    @Value("${app.influx.password}")
    private String influxPassword;

    @Getter
    @Value("${app.influx.database}")
    private String influxDatabase;

    @Getter
    @Value("${app.influx.bucket}")
    private String influxBucket;

    @Getter
    @Value("${app.influx.org}")
    private String influxOrg;

    private WebClientConfig webClientConfig = null;

    private ConsumerJobInfo consumerJobInfo = null;

    public WebClientConfig getWebClientConfig() {
        if (this.webClientConfig == null) {
            WebClientConfig.HttpProxyConfig httpProxyConfig = WebClientConfig.HttpProxyConfig.builder() //
                    .httpProxyHost(this.httpProxyHost) //
                    .httpProxyPort(this.httpProxyPort) //
                    .build();

            this.webClientConfig = WebClientConfig.builder() //
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

    public ConsumerJobInfo getConsumerJobInfo() {
        String path = "./config/jobDefinition.json";
        if (this.consumerJobInfo == null) {
            try {
                String str = Files.readString(java.nio.file.Path.of(path), Charset.defaultCharset());
                consumerJobInfo = gson.fromJson(str, ConsumerJobInfo.class);
            } catch (Exception e) {
                logger.error("Could not load configuration file:{}, reason: {}", path, e.getMessage());
            }
        }
        return this.consumerJobInfo;
    }

    public String getKafkaInputTopic() {
        if (getConsumerJobInfo() == null) {
            logger.error("Invalid or missing configuration");
            return "";
        }
        return getConsumerJobInfo().jobDefinition.getDeliveryInfo().getTopic();
    }

    public String getKafkaBootStrapServers() {
        if (getConsumerJobInfo() == null) {
            logger.error("Invalid or missing configuration");
            return "";
        }
        return getConsumerJobInfo().jobDefinition.getDeliveryInfo().getBootStrapServers();
    }
}
