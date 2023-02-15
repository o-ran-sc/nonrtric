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

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import reactor.netty.transport.ProxyProvider;

@Builder
@Getter
@ToString
public class WebClientConfig {
    private String keyStoreType;

    private String keyStorePassword;

    private String keyStore;

    @ToString.Exclude
    private String keyPassword;

    private boolean isTrustStoreUsed;

    @ToString.Exclude
    private String trustStorePassword;

    private String trustStore;

    @ToString
    @Builder
    @Getter
    public static class HttpProxyConfig {
        private String httpProxyHost;

        private int httpProxyPort;

        private ProxyProvider.Proxy httpProxyType;
    }

    private HttpProxyConfig httpProxyConfig;
}
