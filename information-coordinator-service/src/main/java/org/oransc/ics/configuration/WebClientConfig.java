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

package org.oransc.ics.configuration;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(redactedMask = "####")
public interface WebClientConfig {
    public String keyStoreType();

    @Value.Redacted
    public String keyStorePassword();

    public String keyStore();

    @Value.Redacted
    public String keyPassword();

    public boolean isTrustStoreUsed();

    @Value.Redacted
    public String trustStorePassword();

    public String trustStore();

    @Value.Immutable
    public interface HttpProxyConfig {
        public String httpProxyHost();

        public int httpProxyPort();
    }

    public HttpProxyConfig httpProxyConfig();

}
