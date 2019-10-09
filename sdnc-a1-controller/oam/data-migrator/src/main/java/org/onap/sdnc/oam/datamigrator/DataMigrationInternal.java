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
package org.onap.sdnc.oam.datamigrator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Strings;
import org.onap.sdnc.oam.datamigrator.common.Description;
import org.onap.sdnc.oam.datamigrator.common.MigratorConfiguration;
import org.onap.sdnc.oam.datamigrator.common.Operation;
import org.onap.sdnc.oam.datamigrator.migrators.Migrator;
import org.reflections.Reflections;
import org.slf4j.Logger;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DataMigrationInternal {

    private final Logger log;

    public DataMigrationInternal(Logger log) {
        this.log = log;
    }

    private void logAndPrint(String msg) {
        System.out.println(msg);
        log.info(msg);
    }

    public void run(String[] args){
        CommandLineArgs cArgs = new CommandLineArgs();
        JCommander jCommander = new JCommander(cArgs, args);
        jCommander.setProgramName(DataMigration.class.getSimpleName());

        if (cArgs.help) {
            jCommander.usage();
            return;
        }

        Set<Class<? extends Migrator>> migratorList = getMigratorList();
        if(cArgs.scripts.size() > 0){
            migratorList = migratorList.stream().filter(aClass -> cArgs.scripts.contains(aClass.getSimpleName())).collect(Collectors.toSet());
        }
        if(cArgs.excludeClasses.size() > 0){
            migratorList = migratorList.stream().filter(aClass -> !cArgs.excludeClasses.contains(aClass.getSimpleName())).collect(Collectors.toSet());
        }

        if(migratorList.size()>0) {
            logAndPrint("Total number of available migrations: " + migratorList.size());
            if(cArgs.list) {
                logAndPrint("List of available migrations:");
                for (Class<? extends Migrator> migrator : migratorList) {
                    if(migrator.getAnnotation(Description.class) != null && !migrator.getAnnotation(Description.class).value().isEmpty()) {
                        logAndPrint(migrator.getSimpleName()+ ": " + migrator.getAnnotation(Description.class).value() );
                    }else {
                        logAndPrint(migrator.getSimpleName());
                    }
                }
            }else {
                Operation operation;
                try {
                    operation = Operation.valueOf(cArgs.operation.toUpperCase());
                    logAndPrint("Starting operation: " + operation.name());
                }catch (IllegalArgumentException e) {
                    logAndPrint("Invalid operation: " + cArgs.operation +". Supported operations are: Migrate, Backup, Restore.");
                    return;
                }
                boolean success = true;
                MigratorConfiguration config;
                if(!Strings.isStringEmpty(cArgs.config)){
                    config = new MigratorConfiguration(cArgs.config);
                }else {
                    logAndPrint("No external configuration provided. Initializing Default configuration.");
                    config = new MigratorConfiguration();
                }
                for (Class<? extends Migrator> migratorClass : migratorList) {
                    logAndPrint("Started executing migrator: "+ migratorClass.getSimpleName());
                    try {
                        Migrator migrator =  migratorClass.newInstance();
                        migrator.init(config);
                        migrator.run(operation);
                        success = success && migrator.isSuccess();
                    } catch (InstantiationException | IllegalAccessException e) {
                        logAndPrint("Error instantiating migrator: " + migratorClass);
                        success=false;
                    }
                    logAndPrint("Completed execution for migrator "+ migratorClass.getSimpleName() +" with status: " + success);
                }
                if(success){
                    logAndPrint(operation.name()+ " operation completed Successfully.");
                }else{
                    logAndPrint("Error during "+ operation.name() +" operation. Check logs for details.");
                }
            }
        }else{
            logAndPrint("No migrations available.");
        }
    }

    private Set<Class<? extends Migrator>> getMigratorList() {
        Reflections reflections = new Reflections("org.onap.sdnc.oam.datamigrator.migrators");
        return reflections.getSubTypesOf(Migrator.class).stream().filter(aClass -> !Modifier.isAbstract(aClass.getModifiers())).collect(Collectors.toSet());
    }

    class CommandLineArgs {

        @Parameter(names = "--h", help = true)
        public boolean help;

        @Parameter(names = "-o", description = "Operation to be performed. Default is Migrate. Supported operations: Migrate , Backup , Restore.")
        public String operation = "Migrate";

        @Parameter(names = "-c", description = "Configuration File path / directory")
        public String config;

        @Parameter(names = "-m", description = "Names of migration scripts to run")
        public List<String> scripts = new ArrayList<>();

        @Parameter(names = "-l", description = "List the available of migrations")
        public boolean list = false;

        @Parameter(names = "-e", description = "Exclude list of migrator classes")
        public List<String> excludeClasses = new ArrayList<>();
    }


}
