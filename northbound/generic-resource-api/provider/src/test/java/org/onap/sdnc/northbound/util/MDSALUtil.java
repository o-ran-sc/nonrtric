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

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.BrgTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.BrgTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ConnectionAttachmentTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ConnectionAttachmentTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ContrailRouteTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ContrailRouteTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.NetworkTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.NetworkTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadNetworkTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadNetworkTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadVfModuleTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadVfModuleTopologyOperationOutputBuilder;

import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.GenericConfigurationTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.GenericConfigurationTopologyOperationOutputBuilder;

import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.GenericConfigurationNotificationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.GenericConfigurationNotificationOutputBuilder;

import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.GetpathsegmentTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.GetpathsegmentTopologyOperationOutputBuilder;

import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PolicyUpdateNotifyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PolicyUpdateNotifyOperationOutputBuilder;

import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PortMirrorTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PortMirrorTopologyOperationOutputBuilder;

import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.SecurityZoneTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.SecurityZoneTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ServiceTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ServiceTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.TunnelxconnTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.TunnelxconnTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VfModuleTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VfModuleTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VnfTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VnfTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PnfTopologyOperationInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PnfTopologyOperationOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.brg.response.information.BrgResponseInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.connection.attachment.response.information.ConnectionAttachmentResponseInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.contrail.route.response.information.ContrailRouteResponseInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.information.NetworkInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.response.information.NetworkResponseInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.topology.identifier.structure.NetworkTopologyIdentifierStructureBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.preload.network.topology.information.PreloadNetworkTopologyInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.preload.vf.module.topology.information.PreloadVfModuleTopologyInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.request.information.RequestInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.security.zone.response.information.SecurityZoneResponseInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.ServiceDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.information.ServiceInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.level.oper.status.ServiceLevelOperStatusBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.model.infrastructure.ServiceBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.response.information.ServiceResponseInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.status.ServiceStatusBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.tunnelxconn.response.information.TunnelxconnResponseInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.information.VfModuleInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.response.information.VfModuleResponseInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.topology.VfModuleTopologyBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.topology.identifier.VfModuleTopologyIdentifierBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.information.VnfInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.response.information.VnfResponseInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.topology.identifier.structure.VnfTopologyIdentifierStructureBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.pnf.details.PnfDetailsBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.pnf.response.information.PnfResponseInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.pnf.topology.identifier.structure.PnfTopologyIdentifierStructureBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.common.RpcResult;


/**
 * This uill class provides utility to build yang objects using a recursive syntax that resembles the tree structure
 * when defining the same yang object in json format.
 *
 * For Example
 * <pre>
 * {@code
 * import static org.onap.sdnc.northbound.util.MDSALUtil.*;
 * ServiceTopologyOperationInput input = build(
 *         serviceTopologyOperationInput()
 *                 .setSdncRequestHeader(build(sdncRequestHeader()
 *                         .setSvcRequestId("svc-request-id: xyz")
 *                         .setSvcAction(SvcAction.Assign)
 *                 ))
 *                 .setRequestInformation(build(requestInformation()
 *                         .setRequestId("request-id: xyz")
 *                        .setRequestAction(RequestInformation.RequestAction.CreateServiceInstance)
 *                 ))
 *                .setServiceInformation(build(serviceInformationBuilder()
 *                         .setServiceInstanceId("service-instance-id: xyz")
 *                ))
 * );
 * }
 * </pre>
 */
public class MDSALUtil {

    public static PreloadVfModuleTopologyOperationInputBuilder preloadVfModuleTopologyOperationInput() {
        return new PreloadVfModuleTopologyOperationInputBuilder();
    }

    public static GenericConfigurationTopologyOperationInputBuilder GenericConfigurationTopologyOperationInput() {
        return new GenericConfigurationTopologyOperationInputBuilder();
    }

    public static GenericConfigurationNotificationInputBuilder GenericConfigurationNotificationInput() {
        return new GenericConfigurationNotificationInputBuilder();
    }

    public static GetpathsegmentTopologyOperationInputBuilder GetpathsegmentTopologyOperationInput() {
        return new GetpathsegmentTopologyOperationInputBuilder();
    }

    public static PolicyUpdateNotifyOperationInputBuilder PolicyUpdateNotifyOperationInput() {
        return new PolicyUpdateNotifyOperationInputBuilder();
    }

    public static PortMirrorTopologyOperationInputBuilder PortMirrorTopologyOperationInput() {
        return new PortMirrorTopologyOperationInputBuilder();
    }

    public static PreloadVfModuleTopologyOperationOutputBuilder preloadVfModuleTopologyOperationOutput() {
        return new PreloadVfModuleTopologyOperationOutputBuilder();
    }

    public static PreloadNetworkTopologyOperationInputBuilder preloadNetworkTopologyOperationInput() {
        return new PreloadNetworkTopologyOperationInputBuilder();
    }

    public static PreloadNetworkTopologyOperationOutputBuilder preloadNetworkTopologyOperationOutput() {
        return new PreloadNetworkTopologyOperationOutputBuilder();
    }

    public static BrgTopologyOperationInputBuilder brgTopologyOperationInput() {
        return new BrgTopologyOperationInputBuilder();
    }

    public static BrgTopologyOperationOutputBuilder brgTopologyOperationOutput() {
        return new BrgTopologyOperationOutputBuilder();
    }

    public static TunnelxconnTopologyOperationInputBuilder tunnelxconnTopologyOperationInput() {
        return new TunnelxconnTopologyOperationInputBuilder();
    }

    public static TunnelxconnTopologyOperationOutputBuilder tunnelxconnTopologyOperationOutput() {
        return new TunnelxconnTopologyOperationOutputBuilder();
    }

    public static SecurityZoneTopologyOperationInputBuilder securityZoneTopologyOperationInput() {
        return new SecurityZoneTopologyOperationInputBuilder();
    }

    public static SecurityZoneTopologyOperationOutputBuilder securityZoneTopologyOperationOutput() {
        return new SecurityZoneTopologyOperationOutputBuilder();
    }

    public static ContrailRouteTopologyOperationInputBuilder contrailRouteTopologyOperationInput() {
        return new ContrailRouteTopologyOperationInputBuilder();
    }

    public static ContrailRouteTopologyOperationOutputBuilder contrailRouteTopologyOperationOutput() {
        return new ContrailRouteTopologyOperationOutputBuilder();
    }

    public static VfModuleTopologyOperationInputBuilder vfModuleTopologyOperationInput() {
        return new VfModuleTopologyOperationInputBuilder();
    }

    public static VfModuleTopologyOperationOutputBuilder vfModuleTopologyOperationOutput() {
        return new VfModuleTopologyOperationOutputBuilder();
    }

    public static PnfTopologyOperationInputBuilder pnfTopologyOperationInput() {
        return new PnfTopologyOperationInputBuilder();
    }

    public static PnfTopologyOperationOutputBuilder pnfTopologyOperationOutput() {
        return new PnfTopologyOperationOutputBuilder();
    }

    public static VnfTopologyOperationInputBuilder vnfTopologyOperationInput() {
        return new VnfTopologyOperationInputBuilder();
    }

    public static VnfTopologyOperationOutputBuilder vnfTopologyOperationOutput() {
        return new VnfTopologyOperationOutputBuilder();
    }

    public static ServiceTopologyOperationInputBuilder serviceTopologyOperationInput() {
        return new ServiceTopologyOperationInputBuilder();
    }

    public static ServiceTopologyOperationOutputBuilder serviceTopologyOperationOutput() {
        return new ServiceTopologyOperationOutputBuilder();
    }


    public static SdncRequestHeaderBuilder sdncRequestHeader() {
        return new SdncRequestHeaderBuilder();
    }


    public static RequestInformationBuilder requestInformation() {
        return new RequestInformationBuilder();
    }

    public static ServiceResponseInformationBuilder serviceResponseInformation() {
        return new ServiceResponseInformationBuilder();
    }
    
    public static SecurityZoneResponseInformationBuilder securityZoneResponseInformation() {
        return new SecurityZoneResponseInformationBuilder();
    }

    public static TunnelxconnResponseInformationBuilder tunnelxconnResponseInformation() {
        return new TunnelxconnResponseInformationBuilder();
    }

    public static BrgResponseInformationBuilder brgResponseInformation() {
        return new BrgResponseInformationBuilder();
    }

    public static ContrailRouteResponseInformationBuilder contrailRouteResponseInformation() {
        return new ContrailRouteResponseInformationBuilder();
    }

    public static PnfResponseInformationBuilder pnfResponseInformation() {
        return new PnfResponseInformationBuilder();
    }

    public static VnfResponseInformationBuilder vnfResponseInformation() {
        return new VnfResponseInformationBuilder();
    }

    public static VfModuleResponseInformationBuilder vfModuleResponseInformation() {
        return new VfModuleResponseInformationBuilder();
    }

    public static ServiceInformationBuilder serviceInformationBuilder() {
        return new ServiceInformationBuilder();
    }

    public static PreloadNetworkTopologyInformationBuilder preloadNetworkTopologyInformationBuilder() {
        return new PreloadNetworkTopologyInformationBuilder();
    }

    public static NetworkTopologyIdentifierStructureBuilder networkTopologyIdentifierStructureBuilder(){
        return new NetworkTopologyIdentifierStructureBuilder();
    }

    public static PnfDetailsBuilder pnfDetailsBuilder() {
        return new PnfDetailsBuilder();
    }

    public static VnfInformationBuilder vnfInformationBuilder() {
        return new VnfInformationBuilder();
    }

    public static VfModuleInformationBuilder vfModuleInformationBuilder() {
        return new VfModuleInformationBuilder();
    }

    public static ServiceBuilder service() {
        return new ServiceBuilder();
    }

    public static ServiceDataBuilder serviceData() {
        return new ServiceDataBuilder();
    }

    public static ServiceStatusBuilder serviceStatus() {
        return new ServiceStatusBuilder();
    }

    public static NetworkInformationBuilder networkInformation() {
        return new NetworkInformationBuilder();
    }

    public static NetworkTopologyOperationInputBuilder networkTopologyOperationInput() {
        return new NetworkTopologyOperationInputBuilder();
    }

    public static NetworkTopologyOperationOutputBuilder networkTopologyOperationOutput() {
        return new NetworkTopologyOperationOutputBuilder();
    }

    public static PnfTopologyIdentifierStructureBuilder pnfTopologyIdentifierStructureBuilder() {
        return new PnfTopologyIdentifierStructureBuilder();
    }

    public static VnfTopologyIdentifierStructureBuilder vnfTopologyIdentifierStructureBuilder() {
        return new VnfTopologyIdentifierStructureBuilder();
    }

    public static PreloadVfModuleTopologyInformationBuilder preloadVfModuleTopologyInformationBuilder() {
        return new PreloadVfModuleTopologyInformationBuilder();
    }

    public static VfModuleTopologyBuilder vfModuleTopologyBuilder() {
        return new VfModuleTopologyBuilder();
    }
    
    public static VfModuleTopologyIdentifierBuilder vfModuleTopologyIdentifierBuilder() {
        return new VfModuleTopologyIdentifierBuilder();
    }
    
    public static NetworkResponseInformationBuilder networkResponseInformation() {
        return new NetworkResponseInformationBuilder();
    }

    public static ConnectionAttachmentTopologyOperationInputBuilder connectionAttachmentTopologyOperationInput() {
        return new ConnectionAttachmentTopologyOperationInputBuilder();
    }

    public static ConnectionAttachmentTopologyOperationOutputBuilder connectionAttachmentTopologyOperationOutput() {
        return new ConnectionAttachmentTopologyOperationOutputBuilder();
    }

    public static ConnectionAttachmentResponseInformationBuilder connectionAttachmentResponseInformation() {
        return new ConnectionAttachmentResponseInformationBuilder();
    }

    public static ServiceLevelOperStatusBuilder serviceLevelOperStatus() {
        return new ServiceLevelOperStatusBuilder();
    }

    public static <P> P build(Builder<P> b) {
        return b == null ? null : b.build();
    }

    public static <P, B extends Builder<P>> P build(Function<P, B> builderConstructor, P sourceDataObject) {
        if (sourceDataObject == null) {
            return null;
        }
        B bp = builderConstructor.apply(sourceDataObject);
        return bp.build();
    }

    public static <P, B extends Builder<P>> P build(Function<P, B> builderConstructor, P sourceDataObject,
        Consumer<B> builder) {
        if (sourceDataObject == null) {
            return null;
        }
        B bp = builderConstructor.apply(sourceDataObject);
        builder.accept(bp);
        return bp.build();
    }

    public static <I, O> O exec(Function<I, Future<RpcResult<O>>> rpc, I rpcParameter,
        Function<RpcResult<O>, O> rpcResult) throws Exception {
        Future<RpcResult<O>> future = rpc.apply(rpcParameter);
        return rpcResult.apply(future.get());
    }

}
