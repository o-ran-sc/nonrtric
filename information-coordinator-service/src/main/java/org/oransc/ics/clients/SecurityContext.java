/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2022 Nordix Foundation
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

package org.oransc.ics.clients;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@EnableConfigurationProperties
@ConfigurationProperties()
@Component
public class SecurityContext {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private long tokenTimestamp = 0;

    private String authToken = "";

    @Setter
    private Path authTokenFilePath;

    public SecurityContext(@Value("${app.auth-token-file:\"\"}") String authTokenFilename) {
        if (!authTokenFilename.isEmpty()) {
            this.authTokenFilePath = Path.of(authTokenFilename);
        }
    }

    public boolean isConfigured() {
        return authTokenFilePath != null;
    }

    public synchronized String getBearerAuthToken() {
        if (!isConfigured()) {
            return "";
        }
        try {
            long lastModified = authTokenFilePath.toFile().lastModified();
            if (lastModified != this.tokenTimestamp) {
                this.authToken = Files.readString(authTokenFilePath);
                this.tokenTimestamp = lastModified;
            }
        } catch (Exception e) {
            logger.warn("Could not read auth token file: {}, reason: {}", authTokenFilePath, e.getMessage());
        }
        return this.authToken;
    }

}
