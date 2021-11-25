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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Value("${app.configuration-filepath}")
    private String localConfigurationFilePath;

    private long configFileLastModification = 0;
    private static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        applicationContext = SpringApplication.run(Application.class);
    }

    @Scheduled(fixedRate = 10 * 1000)
    public void checkConfigFileChanges() {
        long timestamp = new File(localConfigurationFilePath).lastModified();
        if (configFileLastModification != 0 && timestamp != configFileLastModification) {
            logger.info("Restarting due to change in the file {}", localConfigurationFilePath);
            restartApplication();
        }
        configFileLastModification = timestamp;
    }

    private static void restartApplication() {
        if (applicationContext == null) {
            logger.info("Cannot restart in unittest");
            return;
        }
        ApplicationArguments args = applicationContext.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            applicationContext.close();
            applicationContext = SpringApplication.run(Application.class, args.getSourceArgs());
        });

        thread.setDaemon(false);
        thread.start();
    }
}
