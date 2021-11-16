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

package org.oran.dmaapadapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oran.dmaapadapter.clients.AsyncRestClient;
import org.oran.dmaapadapter.clients.AsyncRestClientFactory;
import org.oran.dmaapadapter.configuration.ApplicationConfig;
import org.oran.dmaapadapter.configuration.ImmutableHttpProxyConfig;
import org.oran.dmaapadapter.configuration.ImmutableWebClientConfig;
import org.oran.dmaapadapter.configuration.WebClientConfig;
import org.oran.dmaapadapter.configuration.WebClientConfig.HttpProxyConfig;
import org.oran.dmaapadapter.r1.ConsumerJobInfo;
import org.oran.dmaapadapter.repository.InfoType;
import org.oran.dmaapadapter.repository.InfoTypes;
import org.oran.dmaapadapter.repository.Job;
import org.oran.dmaapadapter.repository.Jobs;
import org.oran.dmaapadapter.tasks.KafkaJobDataConsumer;
import org.oran.dmaapadapter.tasks.KafkaTopicConsumers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

@SuppressWarnings("java:S3577") // Rename class
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { //
        "server.ssl.key-store=./config/keystore.jks", //
        "app.webclient.trust-store=./config/truststore.jks", //
        "app.configuration-filepath=./src/test/resources/test_application_configuration_kafka.json"//
})
class IntegrationWithKafka {

    @Autowired
    private ApplicationConfig applicationConfig;

    @Autowired
    private Jobs jobs;

    @Autowired
    private InfoTypes types;

    @Autowired
    private ConsumerController consumerController;

    @Autowired
    private EcsSimulatorController ecsSimulatorController;

    @Autowired
    private KafkaTopicConsumers kafkaTopicConsumers;

    private com.google.gson.Gson gson = new com.google.gson.GsonBuilder().create();

    private static final Logger logger = LoggerFactory.getLogger(IntegrationWithKafka.class);

    @LocalServerPort
    int localServerHttpPort;

    static class TestApplicationConfig extends ApplicationConfig {
        @Override
        public String getEcsBaseUrl() {
            return thisProcessUrl();
        }

        @Override
        public String getDmaapBaseUrl() {
            return thisProcessUrl();
        }

        @Override
        public String getSelfUrl() {
            return thisProcessUrl();
        }

        private String thisProcessUrl() {
            final String url = "https://localhost:" + getLocalServerHttpPort();
            return url;
        }
    }

    /**
     * Overrides the BeanFactory.
     */
    @TestConfiguration
    static class TestBeanFactory extends BeanFactory {

        @Override
        @Bean
        public ServletWebServerFactory servletContainer() {
            return new TomcatServletWebServerFactory();
        }

        @Override
        @Bean
        public ApplicationConfig getApplicationConfig() {
            TestApplicationConfig cfg = new TestApplicationConfig();
            return cfg;
        }
    }

    @AfterEach
    void reset() {
        this.consumerController.testResults.reset();
        this.ecsSimulatorController.testResults.reset();
        this.jobs.clear();
    }

    private AsyncRestClient restClient(boolean useTrustValidation) {
        WebClientConfig config = this.applicationConfig.getWebClientConfig();
        HttpProxyConfig httpProxyConfig = ImmutableHttpProxyConfig.builder() //
                .httpProxyHost("") //
                .httpProxyPort(0) //
                .build();
        config = ImmutableWebClientConfig.builder() //
                .keyStoreType(config.keyStoreType()) //
                .keyStorePassword(config.keyStorePassword()) //
                .keyStore(config.keyStore()) //
                .keyPassword(config.keyPassword()) //
                .isTrustStoreUsed(useTrustValidation) //
                .trustStore(config.trustStore()) //
                .trustStorePassword(config.trustStorePassword()) //
                .httpProxyConfig(httpProxyConfig).build();

        AsyncRestClientFactory restClientFactory = new AsyncRestClientFactory(config);
        return restClientFactory.createRestClientNoHttpProxy(baseUrl());
    }

    private AsyncRestClient restClient() {
        return restClient(false);
    }

    private String baseUrl() {
        return "https://localhost:" + this.applicationConfig.getLocalServerHttpPort();
    }

    private Object jobParametersAsJsonObject(String filter, long maxTimeMiliseconds, int maxSize, int maxConcurrency) {
        Job.Parameters param =
                new Job.Parameters(filter, new Job.BufferTimeout(maxSize, maxTimeMiliseconds), maxConcurrency);
        String str = gson.toJson(param);
        return jsonObject(str);
    }

    private Object jsonObject(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            throw new NullPointerException(e.toString());
        }
    }

    private ConsumerJobInfo consumerJobInfo(String filter, Duration maxTime, int maxSize, int maxConcurrency) {
        try {
            InfoType type = this.types.getAll().iterator().next();
            String typeId = type.getId();
            String targetUri = baseUrl() + ConsumerController.CONSUMER_TARGET_URL;
            return new ConsumerJobInfo(typeId,
                    jobParametersAsJsonObject(filter, maxTime.toMillis(), maxSize, maxConcurrency), "owner", targetUri,
                    "");
        } catch (Exception e) {
            return null;
        }
    }

    private SenderOptions<Integer, String> senderOptions() {
        String bootstrapServers = this.applicationConfig.getKafkaBootStrapServers();

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return SenderOptions.create(props);
    }

    private SenderRecord<Integer, String, Integer> senderRecord(String data, int i) {
        final InfoType infoType = this.types.getAll().iterator().next();
        return SenderRecord.create(new ProducerRecord<>(infoType.getKafkaInputTopic(), i, data + i), i);
    }

    private void sendDataToStream(Flux<SenderRecord<Integer, String, Integer>> dataToSend) {
        final KafkaSender<Integer, String> sender = KafkaSender.create(senderOptions());

        sender.send(dataToSend) //
                .doOnError(e -> logger.error("Send failed", e)) //
                .blockLast();

    }

    private void verifiedReceivedByConsumer(String... strings) {
        ConsumerController.TestResults consumer = this.consumerController.testResults;
        await().untilAsserted(() -> assertThat(consumer.receivedBodies.size()).isEqualTo(strings.length));
        for (String s : strings) {
            assertTrue(consumer.hasReceived(s));
        }
    }

    @Test
    void kafkaIntegrationTest() throws InterruptedException {
        final String JOB_ID1 = "ID1";
        final String JOB_ID2 = "ID2";

        // Register producer, Register types
        await().untilAsserted(() -> assertThat(ecsSimulatorController.testResults.registrationInfo).isNotNull());
        assertThat(ecsSimulatorController.testResults.registrationInfo.supportedTypeIds).hasSize(1);

        // Create two jobs. One buffering and one with a filter
        this.ecsSimulatorController.addJob(consumerJobInfo(null, Duration.ofMillis(400), 1000, 20), JOB_ID1,
                restClient());
        this.ecsSimulatorController.addJob(consumerJobInfo("^Message_1$", Duration.ZERO, 0, 1), JOB_ID2, restClient());

        await().untilAsserted(() -> assertThat(this.jobs.size()).isEqualTo(2));

        var dataToSend = Flux.range(1, 3).map(i -> senderRecord("Message_", i)); // Message_1, Message_2 etc.
        sendDataToStream(dataToSend);

        verifiedReceivedByConsumer("Message_1", "[Message_1, Message_2, Message_3]");

        // Delete the jobs
        this.ecsSimulatorController.deleteJob(JOB_ID1, restClient());
        this.ecsSimulatorController.deleteJob(JOB_ID2, restClient());

        await().untilAsserted(() -> assertThat(this.jobs.size()).isZero());
        await().untilAsserted(() -> assertThat(this.kafkaTopicConsumers.getActiveSubscriptions()).isEmpty());
    }

    @Test
    void kafkaIOverflow() throws InterruptedException {
        // This does not work. After an overflow, the kafka stream does not seem to work
        //
        final String JOB_ID1 = "ID1";
        final String JOB_ID2 = "ID2";

        // Register producer, Register types
        await().untilAsserted(() -> assertThat(ecsSimulatorController.testResults.registrationInfo).isNotNull());
        assertThat(ecsSimulatorController.testResults.registrationInfo.supportedTypeIds).hasSize(1);

        // Create two jobs.
        this.ecsSimulatorController.addJob(consumerJobInfo(null, Duration.ZERO, 0, 1), JOB_ID1, restClient());
        this.ecsSimulatorController.addJob(consumerJobInfo(null, Duration.ZERO, 0, 1), JOB_ID2, restClient());

        await().untilAsserted(() -> assertThat(this.jobs.size()).isEqualTo(2));

        var dataToSend = Flux.range(1, 1000000).map(i -> senderRecord("Message_", i)); // Message_1, Message_2 etc.
        sendDataToStream(dataToSend); // this will overflow

        KafkaJobDataConsumer consumer = kafkaTopicConsumers.getActiveSubscriptions().values().iterator().next();
        await().untilAsserted(() -> assertThat(consumer.isRunning()).isFalse());
        this.consumerController.testResults.reset();

        kafkaTopicConsumers.restartNonRunningTasks();

        dataToSend = Flux.range(1, 3).map(i -> senderRecord("Message__", i)); // Message_1
        sendDataToStream(dataToSend);

        verifiedReceivedByConsumer("Message__1", "Message__1");
    }

}
