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

package org.oransc.enrichment.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Vector;

import org.oransc.enrichment.configuration.ApplicationConfig;
import org.oransc.enrichment.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

/**
 * Dynamic representation of all EI types in the system.
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
public class EiTypes {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Map<String, EiType> allEiTypes = new HashMap<>();
    private final ApplicationConfig config;
    private final Gson gson;

    public EiTypes(ApplicationConfig config) {
        this.config = config;
        GsonBuilder gsonBuilder = new GsonBuilder();
        ServiceLoader.load(TypeAdapterFactory.class).forEach(gsonBuilder::registerTypeAdapterFactory);
        this.gson = gsonBuilder.create();
    }

    public synchronized void restoreTypesFromDatabase() throws IOException {
        Files.createDirectories(Paths.get(getDatabaseDirectory()));
        File dbDir = new File(getDatabaseDirectory());

        for (File file : dbDir.listFiles()) {
            String json = Files.readString(file.toPath());
            EiType type = gson.fromJson(json, EiType.class);
            allEiTypes.put(type.getId(), type);
        }
    }

    public synchronized void put(EiType type) {
        allEiTypes.put(type.getId(), type);
        storeInFile(type);
    }

    public synchronized Collection<EiType> getAllInfoTypes() {
        return new Vector<>(allEiTypes.values());
    }

    public synchronized EiType getType(String id) throws ServiceException {
        EiType type = allEiTypes.get(id);
        if (type == null) {
            throw new ServiceException("Information type not found: " + id);
        }
        return type;
    }

    public synchronized EiType get(String id) {
        return allEiTypes.get(id);
    }

    public synchronized void remove(EiType type) {
        allEiTypes.remove(type.getId());
        try {
            Files.delete(getPath(type));
        } catch (IOException e) {
            logger.warn("Could not remove file: {} {}", type.getId(), e.getMessage());
        }
    }

    public synchronized int size() {
        return allEiTypes.size();
    }

    public synchronized void clear() {
        this.allEiTypes.clear();
        clearDatabase();
    }

    private void clearDatabase() {
        try {
            FileSystemUtils.deleteRecursively(Path.of(getDatabaseDirectory()));
            Files.createDirectories(Paths.get(getDatabaseDirectory()));
        } catch (IOException e) {
            logger.warn("Could not delete database : {}", e.getMessage());
        }
    }

    private void storeInFile(EiType type) {
        try {
            try (PrintStream out = new PrintStream(new FileOutputStream(getFile(type)))) {
                out.print(gson.toJson(type));
            }
        } catch (Exception e) {
            logger.warn("Could not save job: {} {}", type.getId(), e.getMessage());
        }
    }

    private File getFile(EiType type) {
        return getPath(type).toFile();
    }

    private Path getPath(EiType type) {
        return getPath(type.getId());
    }

    private Path getPath(String typeId) {
        return Path.of(getDatabaseDirectory(), typeId);
    }

    private String getDatabaseDirectory() {
        return config.getVardataDirectory() + "/eitypes";
    }
}
