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

package com.demo.producer.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class InfoTypes {
    private static final Logger logger = LoggerFactory.getLogger(InfoTypes.class);

    private Map<String, InfoType> allTypes = new HashMap<>();

    public InfoTypes(Collection<InfoType> types) {
        for (InfoType type : types) {
            put(type);
        }
    }

    public synchronized InfoType get(String id) {
        return allTypes.get(id);
    }

    public synchronized InfoType getType(String id) throws Exception {
        InfoType type = allTypes.get(id);
        if (type == null) {
            throw new Exception("Could not find type: " + id + HttpStatus.NOT_FOUND.toString());
        }
        return type;
    }

    public static class ConfigFile {
        Collection<InfoType> types;
    }

    public synchronized void put(InfoType type) {
        logger.debug("Put type: {}", type.getId());
        allTypes.put(type.getId(), type);
    }

    public synchronized Iterable<InfoType> getAll() {
        return new Vector<>(allTypes.values());
    }

    public synchronized Collection<String> typeIds() {
        return allTypes.keySet();
    }

    public synchronized int size() {
        return allTypes.size();
    }

    public synchronized void clear() {
        allTypes.clear();
    }
}
