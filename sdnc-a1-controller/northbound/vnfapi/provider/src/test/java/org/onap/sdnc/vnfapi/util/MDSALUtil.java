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

import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.data.ServiceDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.information.ServiceInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.information.VnfInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.model.infrastructure.VnfListBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.request.information.VnfRequestInformationBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * This uill class provides utility to build yang objects using a recursive syntax that resembles the tree structure
 * when defining the same yang object in json format.
 *
 * For Example
 * <pre>
 * {@code
 * import static org.onap.sdnc.northbound.util.MDSALUtil.*;
 * VnfTopologyOperationInput input = build(vnfTopologyOperationInput()
 *                .setServiceInformation(
 *                        build(serviceInformation()
 *                                .setServiceId("serviceId: xyz")
 *                                .setServiceInstanceId("serviceInstanceId: xyz")
 *                                .setServiceType("serviceType: xyz")
 *                                .setSubscriberName("subscriberName: xyz")
 *                        )
 *                )
 *                .setVnfRequestInformation(
 *                        build(vnfRequestInformation()
 *                                .setVnfId("vnfId: xyz")
 *                                .setVnfName("vnfName: xyz")//defect if missing
 *                                .setVnfType("vnfType: xyz")//defect if missing
 *                        )
 *                )
 *                .setSdncRequestHeader(
 *                        build(sdncRequestHeader()
 *                          .setSvcAction(SvcAction.Delete)
 *                        )
 *                )
 *        );
 * );
 * }
 * </pre>
 */
public class MDSALUtil {

    public static VnfTopologyOperationInputBuilder vnfTopologyOperationInput(){return new VnfTopologyOperationInputBuilder();}
    public static VnfTopologyOperationOutputBuilder vnfTopologyOperationOutput(){return new VnfTopologyOperationOutputBuilder();}

    public static ServiceInformationBuilder serviceInformation(){return new ServiceInformationBuilder();}
    public static VnfRequestInformationBuilder vnfRequestInformation(){return new VnfRequestInformationBuilder();}
    public static VnfListBuilder vnfList(){return new VnfListBuilder();}
    public static ServiceDataBuilder serviceData() { return new ServiceDataBuilder();}
    public static SdncRequestHeaderBuilder sdncRequestHeader(){return new SdncRequestHeaderBuilder();}
    public static VnfInformationBuilder vnfInformation(){return new VnfInformationBuilder();}


    public static <P> P build(Builder<P> b) {
        return b == null? null :b.build();
    }

    public static <P,B extends Builder<P>> P build(Function<P,B> builderConstructor,P sourceDataObject){
        if(sourceDataObject == null){
            return null;
        }
        B bp = builderConstructor.apply(sourceDataObject);
        return bp.build();
    }

    public static <P,B extends Builder<P>> P build(Function<P,B> builderConstructor,P sourceDataObject,Consumer<B> builder){
        if(sourceDataObject == null){
            return null;
        }
        B bp = builderConstructor.apply(sourceDataObject);
        builder.accept(bp);
        return bp.build();
    }

    public static <I,O> O exec(Function<I,Future<RpcResult<O>>> rpc,I rpcParameter,Function<RpcResult<O>,O> rpcResult)  throws Exception {
        Future<RpcResult<O>> future = rpc.apply(rpcParameter);
        return rpcResult.apply(future.get());
    }

}
