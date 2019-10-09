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

import org.onap.sdnc.oam.datamigrator.common.Description;

import java.util.HashMap;
import java.util.HashSet;

@Description("Migrator for container 'preload-vnf' in GENERIC-RESOURCE-API.yang")
public class PreloadInformationMigrator extends RenameDeleteLeafMigrator {

    private static final String YANG_MODULE = "GENERIC-RESOURCE-API";

    static{
        deletedFields = new HashSet<>();
        deletedFields.add("preload-vnfs.vnf-preload-list.preload-data.vnf-topology-information");
        deletedFields.add("preload-vnfs.vnf-preload-list.preload-data.network-topology-information.network-topology-identifier.service-type");
        deletedFields.add("preload-vnfs.vnf-preload-list.preload-data.oper-status.last-action");
        renamedFields = new HashMap<>();
        renamedFields.put("preload-vnfs","preload-information");
        renamedFields.put("preload-vnfs.vnf-preload-list","preload-list");
        renamedFields.put("preload-vnfs.vnf-preload-list.vnf-type","preload-type");
        renamedFields.put("preload-vnfs.vnf-preload-list.vnf-name","preload-id");
        renamedFields.put("preload-vnfs.vnf-preload-list.preload-data.oper-status","preload-oper-status");
        renamedFields.put("preload-vnfs.vnf-preload-list.preload-data.network-topology-information","preload-network-topology-information");
        renamedFields.put("preload-vnfs.vnf-preload-list.preload-data.network-topology-information.network-topology-identifier","network-topology-identifier-structure");
    }

    @Override
    public String getYangModuleName() {
        return YANG_MODULE;
    }

    @Override
    public String getSourcePath() {
        return "preload-vnfs";
    }

    @Override
    public String getTargetPath() {
        return "preload-information";
    }
}
