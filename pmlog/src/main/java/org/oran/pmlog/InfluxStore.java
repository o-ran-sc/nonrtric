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

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.Ready;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import org.oran.pmlog.configuration.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The class receives PM reports and stores these in an Influx DB
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class InfluxStore {
    private static final Logger logger = LoggerFactory.getLogger(InfluxStore.class);

    @Getter
    private Disposable subscription;

    private static com.google.gson.Gson gson = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();
    private final ApplicationConfig applConfig;

    private final InfluxDBClient influxClient;

    public InfluxStore(ApplicationConfig applConfig) {
        this.applConfig = applConfig;
        this.influxClient = createInfluxClient();

        pingDb();
    }

    private void pingDb() {
        try {
            String version = this.influxClient.version();
            logger.info("Influx version {} ", version);
            Ready ready = this.influxClient.ready();
            logger.info("Ready {}", ready);
            logger.info("Onboarding {}", this.influxClient.isOnboardingAllowed());
        } catch (Exception e) {
            logger.error("Could not connect to influx DB, reason: {}", e.getMessage());
        }
    }

    private InfluxDBClient createInfluxClient() {
        try {
            // org = est bucket = pm_data
            String token = applConfig.getInfluxAccessToken();
            if (!token.isBlank()) {
                return InfluxDBClientFactory.create(applConfig.getInfluxUrl(), token.toCharArray());
            } else {
                return InfluxDBClientFactory.createV1(applConfig.getInfluxUrl(), //
                        applConfig.getInfluxUser(), applConfig.getInfluxPassword().toCharArray(), //
                        applConfig.getInfluxDatabase(), //
                        null);
            }
        } catch (Exception e) {
            logger.error("Exception,could not create influx client {}", e.getMessage());
            return null;
        }
    }

    public void start(Flux<DataFromKafkaTopic> input) {
        this.subscription = input.flatMap(this::storeInInflux) //
                .subscribe(this::handleSentOk, //
                        this::handleExceptionInStream, //
                        () -> stop());

    }

    private String measType(PmReport.MeasResult measResult, PmReport.MeasInfoList measInfoList) {
        return measInfoList.getMeasTypes().getMeasType(measResult.getP());
    }

    private void addCounterFieldToPoint(Point point, PmReport.MeasInfoList measInfoList,
            PmReport.MeasValuesList measValueList, PmReport.MeasResult measResult) {
        String measType = measType(measResult, measInfoList);

        try {
            Long value = Long.valueOf(measResult.getSValue());
            point.addField(measType, value);
        } catch (Exception e) {
            point.addField(measType, measResult.getSValue());
        }
    }

    private Instant endTime(PmReport report) {
        return Instant.ofEpochMilli(report.lastTimeEpochMili());
    }

    private Mono<String> storeInInflux(DataFromKafkaTopic data) {
        PmReport report = PmReport.parse(data);

        List<Point> points = new ArrayList<>();
        PmReport.MeasDataCollection measDataCollection = report.event.getPerf3gppFields().getMeasDataCollection();
        for (PmReport.MeasInfoList measInfoList : measDataCollection.getMeasInfoList()) {

            for (PmReport.MeasValuesList measValueList : measInfoList.getMeasValuesList()) {
                if (measValueList.getSuspectFlag().equals("true")) {
                    continue;
                }
                Point point = Point.measurement(report.fullDistinguishedName(measValueList)).time(endTime(report),
                        WritePrecision.MS);

                point.addField("GranularityPeriod", measDataCollection.getGranularityPeriod());

                for (PmReport.MeasResult measResult : measValueList.getMeasResults()) {
                    addCounterFieldToPoint(point, measInfoList, measValueList, measResult);
                }
                points.add(point);
            }
        }

        store(points, report);

        logger.info("Processed file from: {}", report.event.getCommonEventHeader().getSourceName());

        return Mono.just("ok");

    }

    public void store(List<Point> points, PmReport report) {
        try {
            WriteApiBlocking writeApi = influxClient.getWriteApiBlocking();
            writeApi.writePoints(applConfig.getInfluxBucket(), applConfig.getInfluxOrg(), points);
        } catch (Exception e) {
            logger.error("Could not write points from {}, reason: {}",
                    report.event.getCommonEventHeader().getSourceName(), e.getMessage());
        }
    }

    public synchronized void stop() {
        if (this.subscription != null) {
            this.subscription.dispose();
            this.subscription = null;
        }
        logger.info("InfluxStore stopped");
    }

    private void handleExceptionInStream(Throwable t) {
        logger.warn(" exception: {}", t.getMessage());
        stop();
    }

    public synchronized boolean isRunning() {
        return this.subscription != null;
    }

    private void handleSentOk(String data) {
        logger.debug("Stored data");
    }

}
