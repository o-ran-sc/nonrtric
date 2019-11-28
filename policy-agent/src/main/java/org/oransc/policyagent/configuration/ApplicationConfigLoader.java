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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.swagger.annotations.ApiOperation;
import reactor.core.publisher.Mono;

@Configuration
@EnableScheduling
public class ApplicationConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfigLoader.class);
    private static List<ScheduledFuture<?>> scheduledFutureList = new ArrayList<>();
    private static final Duration CONFIGURATION_REFRESH_INTERVAL = Duration.ofSeconds(15);

    private final TaskScheduler taskScheduler;
    private final ApplicationConfig configuration;

    @Autowired
    public ApplicationConfigLoader(TaskScheduler taskScheduler, ApplicationConfig configuration) {
        this.taskScheduler = taskScheduler;
        this.configuration = configuration;
    }

    /**
     * Function which have to stop tasks execution.
     *
     * @return response entity about status of cancellation operation
     */
    @ApiOperation(value = "Get response on stopping task execution")
    public synchronized Mono<ResponseEntity<String>> getResponseFromCancellationOfTasks() {
        scheduledFutureList.forEach(x -> x.cancel(false));
        scheduledFutureList.clear();
        logger.info("Stopped");
        return Mono.just(new ResponseEntity<>("Service has already been stopped!", HttpStatus.CREATED));
    }

    @PostConstruct
    @ApiOperation(value = "Start task if possible")
    public synchronized boolean start() {
        logger.info("Start scheduling Datafile workflow");
        configuration.initialize();

        if (scheduledFutureList.isEmpty()) {
            scheduledFutureList.add(
                    taskScheduler.scheduleWithFixedDelay(this::refreshConfiguration, CONFIGURATION_REFRESH_INTERVAL));
            return true;
        } else {
            return false;
        }
    }

    private void refreshConfiguration() {
        // TBD

    }

}
