/*
 * ============LICENSE_START=======================================================
 * ONAP : SDNC
 * ================================================================================
 * Copyright 2019 AMDOCS
 *=================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.sdnc.oam.datamigrator.migrators;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.onap.sdnc.oam.datamigrator.common.MigratorConfiguration;
import org.onap.sdnc.oam.datamigrator.common.Operation;
import org.onap.sdnc.oam.datamigrator.common.RestconfClient;
import org.onap.sdnc.oam.datamigrator.exceptions.RestconfException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public abstract class Migrator {

    protected RestconfClient sourceClient;
    protected RestconfClient targetClient;
    protected boolean success = true;
    private MigratorConfiguration config;
    private final Logger log = LoggerFactory.getLogger(PreloadInformationMigrator.class);


    public void run(Operation operation){
        {
            JsonObject sourceData;
            if(operation != Operation.RESTORE) {

                try {
                    sourceData = sourceClient.get(getYangModuleName()+":"+ getSourcePath());
                    if(operation == Operation.BACKUP){
                        String fileName = getFileName();
                        try {
                            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
                            writer.write(sourceData.toString());
                            writer.close();
                        } catch (IOException e) {
                            log.error("Error writing data to file : " + fileName, e);
                            success = false;
                            return;
                        }
                        return;
                    }
                } catch (RestconfException e) {
                    if(e.getErrorCode() == 404){
                        log.error("No data available for migration. Returning silent success.", e);
                        success = true;
                    }else {
                        log.error("Error retrieving data from MD-SAL store. Error code: " + e.getErrorCode() + ". Error message:" + e.getErrorMessage(), e);
                        success = false;
                    }
                    return;
                }
            }else {
                String fileName = getFileName();
                try {
                    Gson gson = new Gson();
                    sourceData = gson.fromJson(new BufferedReader(new FileReader(fileName)),JsonObject.class);
                } catch (IOException e) {
                    log.error("Error Reading data from file : " + fileName, e);
                    success = false;
                    return;
                }
            }
            try {
                String targetData = convertData(sourceData);
                targetClient.put(getYangModuleName()+":"+ getTargetPath(),targetData);
            } catch (RestconfException e) {
                log.error("Error loading data to MD-SAL store. Error code: "+e.getErrorCode()+". Error message:"+e.getErrorMessage(),e);
                success=false;
            }
        }
    }

    private String getFileName() {
        return config.getDataPath()+ "/" + getYangModuleName()+ "_"+ getSourcePath()+"_"+ getTargetPath() + ".json";
    }

    protected abstract String convertData(JsonObject sourceData);

    public abstract String getYangModuleName();
    public abstract String getSourcePath();
    public abstract String getTargetPath();

    public void init(MigratorConfiguration config){
        this.config = config;
        sourceClient = new RestconfClient(config.getSourceHost(),config.getSourceUser(),config.getSourcePassword());
        targetClient = new RestconfClient(config.getTargetHost(),config.getTargetUser(),config.getTargetPassword());
    }

    public RestconfClient getSourceClient() {
        return sourceClient;
    }

    public void setSourceClient(RestconfClient sourceClient) {
        this.sourceClient = sourceClient;
    }

    public RestconfClient getTargetClient() {
        return targetClient;
    }

    public void setTargetClient(RestconfClient targetClient) {
        this.targetClient = targetClient;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}

