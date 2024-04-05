/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 *
 * Copyright (C) 2024: OpenInfra Foundation Europe
 *
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

package com.demo.consumer.messages;

import java.util.Properties;
import java.util.Random;
import org.json.simple.JSONObject;

/**
 * The type Message helper.
 */
public class MessageHelper {

    private static Properties props;

    public static String getRandomString() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1).filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }

    @SuppressWarnings("unchecked") // Using only strings
    public static JSONObject getMessageLogEntryJSON(String source, String topic, String key, String message)
            throws Exception {
        JSONObject obj = new JSONObject();
        String bootstrapServers = getProperties().getProperty("bootstrap.servers");
        obj.put("bootstrapServers", bootstrapServers);
        obj.put("source", source);
        obj.put("topic", topic);
        obj.put("key", key);
        obj.put("message", message);

        return obj;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getSimpleJSONObject(String message) {
        JSONObject obj = new JSONObject();
        obj.put("message", message);
        return obj;
    }

    protected static Properties getProperties() throws Exception {
        if (props == null) {
            props = PropertiesHelper.getProperties();
        }
        return props;
    }

}
