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

package org.onap.sdnc.vnfapi.util;

import org.onap.sdnc.vnfapi.VNFSDNSvcLogicServiceClient;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.data.ServiceDataBuilder;

import java.util.Properties;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.when;
import static org.onap.sdnc.vnfapi.util.MDSALUtil.build;
import static org.onap.sdnc.vnfapi.util.PropBuilder.propBuilder;


/**
 * VNFSDNSvcLogicServiceClientMockUtil provides a set of util methods for quickly configuring method
 * behaviour on the Mock VNFSDNSvcLogicServiceClient
 */
public class VNFSDNSvcLogicServiceClientMockUtil {


    private final String MODULE = "VNF-API";
    private final String MODE = "sync";
    private final String VERSION = null;
    private String scvOperation = null;


    public final String errorCode = "error-code";
    public final String errorMessage = "error-message";
    public final String ackFinal = "ack-final";
    public final String serviceObjectPath = "service-object-path";
    public final String networkObjectPath = "network-object-path";
    public final String networkId = "networkId";


    private final VNFSDNSvcLogicServiceClient mockVNFSDNSvcLogicServiceClient;



    public VNFSDNSvcLogicServiceClientMockUtil(VNFSDNSvcLogicServiceClient mockVNFSDNSvcLogicServiceClient) {
        this.mockVNFSDNSvcLogicServiceClient = mockVNFSDNSvcLogicServiceClient;
    }


    /** @param scvOperation -  The scvOperation parameter to use on the {@link VNFSDNSvcLogicServiceClient} methods */
    public void setScvOperation(String scvOperation) {
        this.scvOperation = scvOperation;
    }

    /**
     * Configure {@link VNFSDNSvcLogicServiceClient#hasGraph(String, String, String, String)}
     * to return the specified value when when invoked with the parameters
     * {@link #MODULE}, {@link #MODE}, {@link #VERSION} and {@link #scvOperation}
     */
    public void mockHasGraph(Boolean isHasGraph) throws Exception {
        when(
                mockVNFSDNSvcLogicServiceClient
                        .hasGraph(
                                eq(MODULE),
                                eq(scvOperation),
                                eq(VERSION),
                                eq(MODE)
                        )
        )
                .thenReturn(isHasGraph);
    }


    /**
     * @return
     * PropBuilder - A PropBuilder populated with the expected properties returned from
     * {@link VNFSDNSvcLogicServiceClient#execute(String, String, String, String, ServiceDataBuilder, Properties)}
     */
    public PropBuilder createExecuteOKResult(){
        return propBuilder()
                .set(errorCode,"200")
                .set(errorMessage,"OK")
                .set(ackFinal,"Y")
                .set(serviceObjectPath,"serviceObjectPath: XYZ")
                .set(networkObjectPath,"networkObjectPath: XYZ")
                .set(networkId,"networkId: XYZ");

    }


    /**
     * Configure
     * {@link VNFSDNSvcLogicServiceClient#execute(String, String, String, String, ServiceDataBuilder, Properties)}
     * to return the specified svcResultProp when when invoked with the parameters
     * {@link #MODULE}, {@link #MODE}, {@link #VERSION} and {@link #scvOperation}
     */
    public void mockExecute(PropBuilder svcResultProp) throws Exception{
        when(
                mockVNFSDNSvcLogicServiceClient
                        .execute(
                                eq(MODULE),
                                eq(scvOperation),
                                eq(VERSION),
                                eq(MODE),
                                isA(ServiceDataBuilder.class),
                                isA(Properties.class)
                        )
        )
                .thenReturn(build(
                        svcResultProp
                ));
    }

}
