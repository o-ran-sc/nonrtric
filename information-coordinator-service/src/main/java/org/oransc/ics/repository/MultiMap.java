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

package org.oransc.ics.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * A map, where each key can be bound to may values (where each value has an own
 * ID)
 */
public class MultiMap<T> {

    private final Map<String, Map<String, T>> map = new HashMap<>();

    public void put(String key, String id, T value) {
        this.map.computeIfAbsent(key, k -> new HashMap<>()).put(id, value);
    }

    public T remove(String key, String id) {
        Map<String, T> innerMap = this.map.get(key);
        if (innerMap != null) {
            T removedElement = innerMap.remove(id);
            if (innerMap.isEmpty()) {
                this.map.remove(key);
            }
            return removedElement;
        }
        return null;
    }

    public Collection<T> get(String key) {
        Map<String, T> innerMap = this.map.get(key);
        if (innerMap == null) {
            return Collections.emptyList();
        }
        return new Vector<>(innerMap.values());
    }

    public void clear() {
        this.map.clear();
    }

}
