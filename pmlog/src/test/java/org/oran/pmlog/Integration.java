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

package org.oran.pmlog;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oran.pmlog.configuration.ApplicationConfig;
import org.oran.pmlog.configuration.ConsumerJobInfo;
import org.oran.pmlog.configuration.ConsumerJobInfo.PmFilterData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

@SuppressWarnings("java:S3577") // Rename class
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { //
        "server.ssl.key-store=./config/keystore.jks", //
        "app.webclient.trust-store=./config/truststore.jks", //
        "app.configuration-filepath=./src/test/resources/test_application_configuration.json", //
        "app.pm-files-path=./src/test/resources/" //
}) //
class Integration {

    @Autowired
    private ApplicationConfig applicationConfig;

    private static com.google.gson.Gson gson = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();

    private final Logger logger = LoggerFactory.getLogger(Integration.class);

    @LocalServerPort
    int localServerHttpPort;

    static class TestApplicationConfig extends ApplicationConfig {
        String thisProcessUrl() {
            final String url = "https://localhost:" + getLocalServerHttpsPort();
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

    @BeforeEach
    void init() {}

    @AfterEach
    void reset() {}

    private SenderOptions<byte[], byte[]> kafkaSenderOptions() {
        String bootstrapServers = this.applicationConfig.getKafkaBootStrapServers();

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // props.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producerx");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        return SenderOptions.create(props);
    }

    private SenderRecord<byte[], byte[], Integer> kafkaSenderRecord(String data, String key) {
        String topic = this.applicationConfig.getKafkaInputTopic();
        int correlationMetadata = 2;
        return SenderRecord.create(new ProducerRecord<>(topic, key.getBytes(), data.getBytes()), correlationMetadata);
    }

    private void sendDataToKafka(Flux<SenderRecord<byte[], byte[], Integer>> dataToSend) {
        final KafkaSender<byte[], byte[]> sender = KafkaSender.create(kafkaSenderOptions());

        sender.send(dataToSend) //
                .doOnError(e -> logger.error("Send failed", e)) //
                .blockLast();

        sender.close();
    }

    @SuppressWarnings("squid:S2925") // "Thread.sleep" should not be used in tests.
    private static void waitForKafkaListener() throws InterruptedException {
        Thread.sleep(4000);
    }

    private String generateCounterValue(int sequenceValue, int noOfObjects, String counterName, String resourceFdn) {
        long value = (random.nextInt() % 100) * sequenceValue + (counterName.hashCode() % 5000);
        return Long.toString(value);
    }

    static java.util.Random random = new java.util.Random(System.currentTimeMillis());

    private long currentEpochMicroSeconds() {
        return java.util.concurrent.TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());
    }

    private String measType(PmReport.MeasResult measResult, PmReport.MeasInfoList measInfoList) {
        return measInfoList.getMeasTypes().getMeasType(measResult.getP());
    }

    // Set end time. Now - (GP * sequenceValue)
    private void setEndTime(PmReport report, int sequenceValue, int noOfObjects) {
        long gpMicro = report.event.getPerf3gppFields().getMeasDataCollection().getGranularityPeriod() * 1000 * 1000;
        long endTime = currentEpochMicroSeconds() - ((noOfObjects - sequenceValue - 1) * gpMicro);
        report.event.getCommonEventHeader().setLastEpochMicrosec(endTime);

    }

    final String PM_REPORT_FILE_BIG = "./src/test/resources/A20000626.2315+0200-2330+0200_HTTPS-6-73.json";
    final String PM_REPORT_FILE = "./src/test/resources/pm_report.json";

    String pmReport(int sequenceValue, int noOfObjects) {
        try {
            String str = Files.readString(Path.of(PM_REPORT_FILE), Charset.defaultCharset());
            PmReport report = gson.fromJson(str, PmReport.class);
            PmReport.MeasDataCollection measDataCollection = report.event.getPerf3gppFields().getMeasDataCollection();

            setEndTime(report, sequenceValue, noOfObjects);

            // Fill it with generated values
            for (PmReport.MeasInfoList measInfoList : measDataCollection.getMeasInfoList()) {
                for (PmReport.MeasValuesList measValueList : measInfoList.getMeasValuesList()) {
                    for (PmReport.MeasResult measResult : measValueList.getMeasResults()) {
                        String value = this.generateCounterValue(sequenceValue, noOfObjects,
                                measType(measResult, measInfoList), report.fullDistinguishedName(measValueList));
                        measResult.setSValue(value);
                    }
                }
            }
            return gson.toJson(report);
        } catch (Exception e) {
            logger.error("Could not loadPM report {}", e.getMessage(), e);
            return null;
        }

    }

    @Test
    void testStoreReportsInflux() throws Exception {
        final int NO_OF_OBJECTS = 24 * 4;
        InfluxStore influxStore = new InfluxStore(this.applicationConfig);

        Flux<DataFromKafkaTopic> input = Flux.range(0, NO_OF_OBJECTS) //
                .map(i -> pmReport(i, NO_OF_OBJECTS)) //
                .map(str -> new DataFromKafkaTopic(null, null, str.getBytes()));

        influxStore.start(input);

    }

    @SuppressWarnings("squid:S2925") // "Thread.sleep" should not be used in tests.
    @Test
    void sendPmReportsThroughKafka() throws Exception {
        waitForKafkaListener();

        final int NO_OF_OBJECTS = 20;

        var dataToSend = Flux.range(0, NO_OF_OBJECTS).map(i -> kafkaSenderRecord(pmReport(i, NO_OF_OBJECTS), "key"));
        sendDataToKafka(dataToSend);

        Thread.sleep(1000 * 1000);
    }

    @Test
    void printConfiguration() {
        PmFilterData f = new PmFilterData();
        f.getMeasObjInstIds().add("measObj");
        PmFilterData.MeasTypeSpec spec = new PmFilterData.MeasTypeSpec();
        spec.setMeasuredObjClass("measuredObjClass");
        spec.getMeasTypes().add("measType");
        f.getMeasTypeSpecs().add(spec);
        f.getSourceNames().add("sourceName");
        ConsumerJobInfo.KafkaDeliveryInfo deliveryInfo = ConsumerJobInfo.KafkaDeliveryInfo.builder() //
                .topic("topic").bootStrapServers("bootStrapServers") //
                .build();
        ConsumerJobInfo.PmJobParameters params = ConsumerJobInfo.PmJobParameters.builder() //
                .filter(f) //
                .deliveryInfo(deliveryInfo).build();

        ConsumerJobInfo info = new ConsumerJobInfo("type", params, "owner");
        String str = gson.toJson(info);
        System.out.print(str);
    }

    @SuppressWarnings("squid:S2925") // "Thread.sleep" should not be used in tests.
    @Test
    void tet() throws Exception {
        Thread.sleep(1000 * 1000);
    }

}
