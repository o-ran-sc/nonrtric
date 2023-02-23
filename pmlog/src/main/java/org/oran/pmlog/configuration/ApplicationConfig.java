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
    private static final String JOB_DEFINITION_PATH = "./config/jobDefinition.json";

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
    @Value("${server.port}")
    private int localServerHttpsPort;

    @Getter
    @Value("${app.kafka.max-poll-records:300}")
    private int kafkaMaxPollRecords;

    @Getter
    @Value("${app.kafka.group-id}")
    private String kafkaGroupId;

    @Getter
    @Value("${app.kafka.client-id}")
    private String kafkaClientId;

    @Value("${app.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Value("${app.kafka.input-topic}")
    private String kafkaInputTopic;

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
    @Value("${app.consumer-job-id:shouldHaveBeenDefinedInYaml}")
    private String consumerJobId;

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

    public String getConsumerJobInfo() {

        try {
            return Files.readString(java.nio.file.Path.of(JOB_DEFINITION_PATH), Charset.defaultCharset());
        } catch (Exception e) {
            logger.error("Could not load configuration file: {}, reason: {}", JOB_DEFINITION_PATH, e.getMessage());
            return "{}";
        }
    }

    private ConsumerJobInfo.KafkaDeliveryInfo getKafkaDeliveryInfoFromAplicationYaml() {
        return ConsumerJobInfo.KafkaDeliveryInfo.builder() //
                .bootStrapServers(this.kafkaBootstrapServers) //
                .topic(kafkaInputTopic) //
                .build();
    }

    private ConsumerJobInfo.KafkaDeliveryInfo getKafkaDeliveryInfo() {
        try {
            ConsumerJobInfo infoFromFile = gson.fromJson(getConsumerJobInfo(), ConsumerJobInfo.class);
            if (infoFromFile != null && infoFromFile.jobDefinition != null
                    && infoFromFile.jobDefinition.getDeliveryInfo() != null) {
                return infoFromFile.jobDefinition.getDeliveryInfo();
            }

        } catch (Exception e) {
            logger.warn("Could not parse file: {}, reason: {}, falling back to parameters in Application.yaml",
                    JOB_DEFINITION_PATH, e.getMessage());
        }
        return getKafkaDeliveryInfoFromAplicationYaml();

    }

    public String getKafkaInputTopic() {
        return getKafkaDeliveryInfo().getTopic();
    }

    public String getKafkaBootStrapServers() {
        return getKafkaDeliveryInfo().getBootStrapServers();
    }

}
