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
import org.oransc.enrichment.controllers.r1producer.ProducerCallbacks;
import org.oransc.enrichment.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

/**
 * Dynamic representation of all existing Information Jobs.
 */
public class InfoJobs {
    private Map<String, InfoJob> allEiJobs = new HashMap<>();

    private MultiMap<InfoJob> jobsByType = new MultiMap<>();
    private MultiMap<InfoJob> jobsByOwner = new MultiMap<>();
    private final Gson gson;

    private final ApplicationConfig config;
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ProducerCallbacks producerCallbacks;

    public InfoJobs(ApplicationConfig config, ProducerCallbacks producerCallbacks) {
        this.config = config;
        var gsonBuilder = new GsonBuilder();
        ServiceLoader.load(TypeAdapterFactory.class).forEach(gsonBuilder::registerTypeAdapterFactory);
        this.gson = gsonBuilder.create();
        this.producerCallbacks = producerCallbacks;
    }

    public synchronized void restoreJobsFromDatabase() throws IOException {
        Files.createDirectories(Paths.get(getDatabaseDirectory()));
        var dbDir = new File(getDatabaseDirectory());

        for (File file : dbDir.listFiles()) {
            var json = Files.readString(file.toPath());
            var job = gson.fromJson(json, InfoJob.class);
            this.doPut(job);
        }
    }

    public synchronized void put(InfoJob job) {
        this.doPut(job);
        storeJobInFile(job);
    }

    public synchronized Collection<InfoJob> getJobs() {
        return new Vector<>(allEiJobs.values());
    }

    public synchronized InfoJob getJob(String id) throws ServiceException {
        var ric = allEiJobs.get(id);
        if (ric == null) {
            throw new ServiceException("Could not find Information job: " + id);
        }
        return ric;
    }

    public synchronized Collection<InfoJob> getJobsForType(String typeId) {
        return jobsByType.get(typeId);
    }

    public synchronized Collection<InfoJob> getJobsForType(InfoType type) {
        return jobsByType.get(type.getId());
    }

    public synchronized Collection<InfoJob> getJobsForOwner(String owner) {
        return jobsByOwner.get(owner);
    }

    public synchronized InfoJob get(String id) {
        return allEiJobs.get(id);
    }

    public synchronized InfoJob remove(String id, InfoProducers infoProducers) {
        var job = allEiJobs.get(id);
        if (job != null) {
            remove(job, infoProducers);
        }
        return job;
    }

    public synchronized void remove(InfoJob job, InfoProducers infoProducers) {
        this.allEiJobs.remove(job.getId());
        jobsByType.remove(job.getTypeId(), job.getId());
        jobsByOwner.remove(job.getOwner(), job.getId());

        try {
            Files.delete(getPath(job));
        } catch (IOException e) {
            logger.warn("Could not remove file: {}", e.getMessage());
        }
        this.producerCallbacks.stopInfoJob(job, infoProducers);
    }

    public synchronized int size() {
        return allEiJobs.size();
    }

    public synchronized void clear() {
        this.allEiJobs.clear();
        this.jobsByType.clear();
        jobsByOwner.clear();
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

    private void doPut(InfoJob job) {
        allEiJobs.put(job.getId(), job);
        jobsByType.put(job.getTypeId(), job.getId(), job);
        jobsByOwner.put(job.getOwner(), job.getId(), job);
    }

    private void storeJobInFile(InfoJob job) {
        try {
            try (var out = new PrintStream(new FileOutputStream(getFile(job)))) {
                out.print(gson.toJson(job));
            }
        } catch (Exception e) {
            logger.warn("Could not store job: {} {}", job.getId(), e.getMessage());
        }
    }

    private File getFile(InfoJob job) {
        return getPath(job).toFile();
    }

    private Path getPath(InfoJob job) {
        return Path.of(getDatabaseDirectory(), job.getId());
    }

    private String getDatabaseDirectory() {
        return config.getVardataDirectory() + "/database/eijobs";
    }

}
