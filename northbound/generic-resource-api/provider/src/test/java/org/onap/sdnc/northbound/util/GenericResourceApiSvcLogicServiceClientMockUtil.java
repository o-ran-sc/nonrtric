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

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.when;
import static org.onap.sdnc.northbound.util.MDSALUtil.build;
import static org.onap.sdnc.northbound.util.PropBuilder.propBuilder;

import java.util.Properties;
import org.onap.sdnc.northbound.GenericResourceApiSvcLogicServiceClient;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.preload.data.PreloadDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.ServiceDataBuilder;


/**
 * GenericResourceApiSvcLogicServiceClientMockUtil provides a set of util methods for quickly configuring method
 * behaviour on the Mock GenericResourceApiSvcLogicServiceClient
 */
public class GenericResourceApiSvcLogicServiceClientMockUtil {


    private final String MODULE = "generic-resource-api";
    private final String MODE = "sync";
    private final String VERSION = null;
    private String scvOperation = null;

    public final String errorCode = "error-code";
    public final String errorMessage = "error-message";
    public final String ackFinal = "ack-final";
    public final String serviceObjectPath = "service-object-path";
    public final String networkObjectPath = "network-object-path";
    public final String pnfObjectPath = "pnf-object-path";
    public final String vnfObjectPath = "vnf-object-path";
    public final String vfModuleObjectPath = "vf-module-object-path";
    public final String networkId = "networkId";

    private final GenericResourceApiSvcLogicServiceClient mockGenericResourceApiSvcLogicServiceClient;

    public GenericResourceApiSvcLogicServiceClientMockUtil(
        GenericResourceApiSvcLogicServiceClient mockGenericResourceApiSvcLogicServiceClient) {
        this.mockGenericResourceApiSvcLogicServiceClient = mockGenericResourceApiSvcLogicServiceClient;
    }


    /**
     * @param scvOperation -  The scvOperation parameter to use on the {@link GenericResourceApiSvcLogicServiceClient}
     * methods
     */
    public void setScvOperation(String scvOperation) {
        this.scvOperation = scvOperation;
    }

    /**
     * Configure {@link GenericResourceApiSvcLogicServiceClient#hasGraph(String, String, String, String)} to return the
     * specified value when when invoked with the parameters {@link #MODULE}, {@link #MODE}, {@link #VERSION} and {@link
     * #scvOperation}
     */
    public void mockHasGraph(Boolean isHasGraph) throws Exception {
        when(
            mockGenericResourceApiSvcLogicServiceClient
                .hasGraph(
                    eq(MODULE),
                    eq(scvOperation),
                    eq(VERSION),
                    eq(MODE))
        ).thenReturn(isHasGraph);
    }


    /**
     * @return PropBuilder - A PropBuilder populated with the expected properties returned from {@link
     * GenericResourceApiSvcLogicServiceClient#execute(String, String, String, String, ServiceDataBuilder, Properties)}
     */
    public PropBuilder createExecuteOKResult() {
        return propBuilder()
            .set(errorCode, "200")
            .set(errorMessage, "OK")
            .set(ackFinal, "Y")
            .set(serviceObjectPath, "serviceObjectPath: XYZ")
            .set(networkObjectPath, "networkObjectPath: XYZ")
            .set(pnfObjectPath,  "pnfObjectPath: XYZ")
            .set(vnfObjectPath,  "vnfObjectPath: XYZ")
            .set(vfModuleObjectPath,  "vfModuleObjectPath: XYZ")
            .set(networkId, "networkId: XYZ");
    }


    /**
     * Configure {@link GenericResourceApiSvcLogicServiceClient#execute(String, String, String, String,
     * ServiceDataBuilder, Properties)} to return the specified svcResultProp when when invoked with the parameters
     * {@link #MODULE}, {@link #MODE}, {@link #VERSION} and {@link #scvOperation}
     */
    public void mockExecute(PropBuilder svcResultProp) throws Exception {
        when(
            mockGenericResourceApiSvcLogicServiceClient
                .execute(
                    eq(MODULE),
                    eq(scvOperation),
                    eq(VERSION),
                    eq(MODE),
                    isA(ServiceDataBuilder.class),
                    isA(Properties.class))
        ).thenReturn(build(svcResultProp));
    }

    public void mockExecute(RuntimeException exception) throws Exception {
        when(
            mockGenericResourceApiSvcLogicServiceClient
                .execute(
                    eq(MODULE),
                    eq(scvOperation),
                    eq(VERSION),
                    eq(MODE),
                    isA(ServiceDataBuilder.class),
                    isA(Properties.class)
                )
        ).thenThrow(exception);
    }


    public void mockExecuteWoServiceData(PropBuilder svcResultProp) throws Exception {
        when(
            mockGenericResourceApiSvcLogicServiceClient
                .execute(
                    eq(MODULE),
                    eq(scvOperation),
                    eq(VERSION),
                    eq(MODE),
                    isA(Properties.class))
        ).thenReturn(build(svcResultProp));
    }

    public void mockExecuteWoServiceData(RuntimeException exception) throws Exception {
        when(
            mockGenericResourceApiSvcLogicServiceClient
                .execute(
                    eq(MODULE),
                    eq(scvOperation),
                    eq(VERSION),
                    eq(MODE),
                    isA(Properties.class)
                )
        ).thenThrow(exception);
    }

    public void mockExecuteWoServiceDataPreload(RuntimeException exception) throws Exception {
        when(
            mockGenericResourceApiSvcLogicServiceClient
                .execute(
                    eq(MODULE),
                    eq(scvOperation),
                    eq(VERSION),
                    eq(MODE),
                    isA(PreloadDataBuilder.class),
                    isA(Properties.class)
                )
        ).thenThrow(exception);
    }


}
