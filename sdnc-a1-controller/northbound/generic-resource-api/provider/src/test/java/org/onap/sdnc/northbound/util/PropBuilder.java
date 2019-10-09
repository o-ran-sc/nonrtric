/*-
 * ============LICENSE_START=======================================================
 * openECOMP : SDN-C
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
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
 * ============LICENSE_END=========================================================
 */

package org.onap.sdnc.northbound.util;

import org.opendaylight.yangtools.concepts.Builder;

import java.util.Properties;

/**
 * A Util class that adds method chaining to the {@link #set(String, String)} to reducing the syntax needed to populate
 * {@link Properties}
 */
public class PropBuilder implements Builder<Properties> {


    final Properties prop;

    public PropBuilder(Properties prop) {
        this.prop = prop;
    }

    public PropBuilder() {
        this.prop = new Properties();
    }

    public Properties build(){
        return prop;
    }

    public PropBuilder set(String key, String value) {
        prop.setProperty(key, value);
        return this;
    }

    public String get(String key) {
        return prop.getProperty(key);
    }


    public static PropBuilder propBuilder(){
        return (new PropBuilder());
    }
}