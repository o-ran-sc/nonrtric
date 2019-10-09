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

package org.onap.sdnc.vnfapi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.sdnc.vnfapi.util.PropBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.sdnc.request.header.SdncRequestHeader.SvcAction;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.model.infrastructure.VnfList;
import org.opendaylight.yangtools.yang.common.RpcResult;

import static org.onap.sdnc.vnfapi.util.MDSALUtil.build;
import static org.onap.sdnc.vnfapi.util.MDSALUtil.exec;
import static org.onap.sdnc.vnfapi.util.MDSALUtil.sdncRequestHeader;
import static org.onap.sdnc.vnfapi.util.MDSALUtil.serviceData;
import static org.onap.sdnc.vnfapi.util.MDSALUtil.serviceInformation;
import static org.onap.sdnc.vnfapi.util.MDSALUtil.vnfInformation;
import static org.onap.sdnc.vnfapi.util.MDSALUtil.vnfList;
import static org.onap.sdnc.vnfapi.util.MDSALUtil.vnfRequestInformation;
import static org.onap.sdnc.vnfapi.util.MDSALUtil.vnfTopologyOperationInput;
import static org.onap.sdnc.vnfapi.util.MDSALUtil.vnfTopologyOperationOutput;


/**
 * This class test the VnfTopologyOperation mdsal RPC.
 */
@RunWith(MockitoJUnitRunner.class)
public class VnfTopologyOperationRPCTest extends VnfApiProviderTest {


    final String SVC_OPERATION = "vnf-topology-operation";


    @Before
    public void setUp() throws Exception {
        super.setUp();
        svcClient.setScvOperation(SVC_OPERATION);
    }


    /**
     * Verify  VnfTopologyOperation RPC executes Delete VNFList entry
     * {@link VnfTopologyOperationOutput} and persisted the expected {@link VnfList} in the {@link DataBroker}
     */
    @Test
    public void testVnfTopologyOperationRPC_Delete_VNFList_Entry_Success() throws Exception {


        //mock svcClient to perform a successful execution with the expected parameters
        svcClient.mockHasGraph(true);
        PropBuilder svcResultProp = svcClient.createExecuteOKResult();
        svcClient.mockExecute(svcResultProp);

        // create the VnfTopologyOperationInput from the template
        VnfTopologyOperationInput vnfTopologyOperationInput = createVTOI();

        //persist a vnfList entry in the dataBroker
        persistVnfListBroker(vnfTopologyOperationInput);

        //execute the mdsal exec
        VnfTopologyOperationOutput actualVnfTopologyOperationOutput = exec(
                vnfapiProvider::vnfTopologyOperation
                , vnfTopologyOperationInput
                , RpcResult::getResult
        );


        //verify the returned VnfTopologyOperationOutput
        VnfTopologyOperationOutput expectedVnfTopologyOperationOutput = createExpectedVTOO(svcResultProp,vnfTopologyOperationInput);
        Assert.assertEquals(expectedVnfTopologyOperationOutput,actualVnfTopologyOperationOutput);


        //verify the persisted VnfList
        VnfList actualVnfList = db.read(vnfTopologyOperationInput.getVnfRequestInformation().getVnfId(), LogicalDatastoreType.CONFIGURATION);
        VnfList expectedVnfList = null;
        Assert.assertEquals(expectedVnfList,actualVnfList);

        LOG.debug("done");
    }


    public VnfTopologyOperationInput createVTOI(){
        return build(vnfTopologyOperationInput()
                .setServiceInformation(
                        build(serviceInformation()
                                .setServiceId("serviceId: xyz")
                                .setServiceInstanceId("serviceInstanceId: xyz")
                                .setServiceType("serviceType: xyz")
                                .setSubscriberName("subscriberName: xyz")
                        )
                )
                .setVnfRequestInformation(
                        build(vnfRequestInformation()
                                .setVnfId("vnfId: xyz")
                                .setVnfName("vnfName: xyz")//defect if missing
                                .setVnfType("vnfType: xyz")//defect if missing


                        )
                )
                .setSdncRequestHeader(
                        build(sdncRequestHeader()
                          .setSvcAction(SvcAction.Delete)
                        )
                )
        );

    }



    private VnfList persistVnfListBroker(
            VnfTopologyOperationInput vnfTopologyOperationInput
    ) throws Exception{
        VnfList service = build(
                vnfList()
                        .setVnfId(vnfTopologyOperationInput.getVnfRequestInformation().getVnfId())
                        .setServiceData(
                                build(serviceData()
                                  .setVnfId(vnfTopologyOperationInput.getVnfRequestInformation().getVnfId())
                                )
                        )
        );
        db.write(true,service, LogicalDatastoreType.CONFIGURATION);
        return service;
    }





    private VnfTopologyOperationOutput createExpectedVTOO(PropBuilder expectedSvcResultProp,VnfTopologyOperationInput expectedVnfTopologyOperationInput){
        return build(
                vnfTopologyOperationOutput()
                        .setSvcRequestId(expectedVnfTopologyOperationInput.getSdncRequestHeader().getSvcRequestId())
                        .setResponseCode(expectedSvcResultProp.get(svcClient.errorCode))
                        .setAckFinalIndicator(expectedSvcResultProp.get(svcClient.ackFinal))
                        .setResponseMessage(expectedSvcResultProp.get(svcClient.errorMessage))
                        .setVnfInformation(build(vnfInformation()
                                .setVnfId(expectedVnfTopologyOperationInput.getVnfRequestInformation().getVnfId())
                        ))
        );
    }




}
