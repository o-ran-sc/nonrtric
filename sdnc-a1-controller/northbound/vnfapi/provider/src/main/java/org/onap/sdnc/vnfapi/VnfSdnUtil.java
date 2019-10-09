/*-
 * ============LICENSE_START=======================================================
 * openECOMP : SDN-C
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 * 							reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.onap.ccsdk.sli.core.sli.provider.MdsalHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VnfSdnUtil extends MdsalHelper {
    private static final Logger LOG = LoggerFactory.getLogger(VnfSdnUtil.class);

    private static File odlHomePath = null;
    private static Properties properties;

    public static void loadProperties() {
        if (odlHomePath == null) {
            odlHomePath = new File("/opt/opendaylight/current");

            if (!odlHomePath.isDirectory()) {
                odlHomePath = new File("/opt/bvc/controller");
            }
        }

        File propFile = new File(odlHomePath.getAbsolutePath() + "/configuration/vnfapi.properties");
        String propFileName = propFile.getAbsolutePath();
        properties = new Properties();
        if (propFile.isFile() && propFile.canRead()) {
            try (InputStream input = new FileInputStream(propFile)) {
                properties.load(input);
                LOG.info("Loaded properties from " + propFileName);
                setYangMappingProperties(properties);
            } catch (IOException e) {
                LOG.error("Failed to close properties file " + propFileName + "\n", e);
            } catch (Exception e) {
                LOG.error("Failed to load properties " + propFileName + "\n", e);
            }
        }
    }

    static {
        // Trick class loader into loading builders. Some of
        // these will be needed later by Reflection classes, but need
        // to explicitly "new" them here to get class loader to load them.
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.information.NetworkInformationBuilder
            u1 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.information.NetworkInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.policy.NetworkPolicyBuilder
            u2 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.policy.NetworkPolicyBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.request.information.NetworkRequestInformationBuilder
            u3 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.request.information.NetworkRequestInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.topology.identifier.NetworkTopologyIdentifierBuilder
            u4 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.topology.identifier.NetworkTopologyIdentifierBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.topology.information.NetworkTopologyInformationBuilder
            u5 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.topology.information.NetworkTopologyInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.NetworkTopologyOperationInputBuilder
            u6 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.NetworkTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.NetworkTopologyOperationOutputBuilder
            u7 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.NetworkTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.oper.status.OperStatusBuilder
            u8 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.oper.status.OperStatusBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.data.PreloadDataBuilder
            u9 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.data.PreloadDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.model.information.VnfPreloadListBuilder
            u10 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.model.information.VnfPreloadListBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadNetworkTopologyOperationInputBuilder
            u11 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadNetworkTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadNetworkTopologyOperationOutputBuilder
            u12 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadNetworkTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.vf.module.model.information.VfModulePreloadListBuilder
            u13 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.vf.module.model.information.VfModulePreloadListBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVfModulesBuilder
            u14 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVfModulesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVfModuleTopologyOperationInputBuilder
            u15 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVfModuleTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVfModuleTopologyOperationOutputBuilder
            u16 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVfModuleTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.vnf.instance.model.information.VnfInstancePreloadListBuilder
            u17 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.vnf.instance.model.information.VnfInstancePreloadListBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfInstancesBuilder
            u18 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfInstancesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfInstanceTopologyOperationInputBuilder
            u19 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfInstanceTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfInstanceTopologyOperationOutputBuilder
            u20 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfInstanceTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfsBuilder
            u21 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfTopologyOperationInputBuilder
            u22 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfTopologyOperationOutputBuilder
            u23 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.provider.network.information.ProviderNetworkInformationBuilder
            u24 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.provider.network.information.ProviderNetworkInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.request.information.RequestInformationBuilder
            u25 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.request.information.RequestInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.route.table.reference.RouteTableReferenceBuilder
            u26 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.route.table.reference.RouteTableReferenceBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.sdnc.request.header.SdncRequestHeaderBuilder
            u27 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.sdnc.request.header.SdncRequestHeaderBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.data.ServiceDataBuilder
            u28 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.data.ServiceDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.information.ServiceInformationBuilder
            u29 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.information.ServiceInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.status.ServiceStatusBuilder
            u30 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.status.ServiceStatusBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.sriov.vlan.filter.list.SriovVlanFilterListBuilder
            u31 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.sriov.vlan.filter.list.SriovVlanFilterListBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.subnets.SubnetsBuilder
            u32 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.subnets.SubnetsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.identifiers.VfModuleIdentifiersBuilder
            u33 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.identifiers.VfModuleIdentifiersBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.information.VfModuleInformationBuilder
            u34 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.information.VfModuleInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.model.infrastructure.VfModuleListBuilder
            u35 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.model.infrastructure.VfModuleListBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.preload.data.VfModulePreloadDataBuilder
            u36 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.preload.data.VfModulePreloadDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.relationship.list.VfModuleRelationshipListBuilder
            u37 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.relationship.list.VfModuleRelationshipListBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.request.information.VfModuleRequestInformationBuilder
            u38 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.request.information.VfModuleRequestInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModulesBuilder
            u39 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModulesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.service.data.VfModuleServiceDataBuilder
            u40 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.service.data.VfModuleServiceDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.topology.information.VfModuleTopologyInformationBuilder
            u41 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.topology.information.VfModuleTopologyInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModuleTopologyOperationInputBuilder
            u42 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModuleTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModuleTopologyOperationOutputBuilder
            u43 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModuleTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.network.InterfaceRoutePrefixesBuilder
            u44 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.network.InterfaceRoutePrefixesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.network.NetworkIpsBuilder
            u45 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.network.NetworkIpsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.network.NetworkIpsV6Builder
            u46 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.network.NetworkIpsV6Builder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.network.NetworkMacsBuilder
            u47 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.network.NetworkMacsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.topology.VnfVmsBuilder
            u48 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.topology.VnfVmsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.topology.vnf.vms.VmNamesBuilder
            u49 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.topology.vnf.vms.VmNamesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.topology.vnf.vms.VmNetworksBuilder
            u50 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vm.topology.vnf.vms.VmNetworksBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.assignments.vnf.assignments.AvailabilityZonesBuilder
            u51 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.assignments.vnf.assignments.AvailabilityZonesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.assignments.VnfAssignmentsBuilder
            u52 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.assignments.VnfAssignmentsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.assignments.vnf.assignments.VnfNetworksBuilder
            u53 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.assignments.vnf.assignments.VnfNetworksBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.information.VnfInformationBuilder
            u54 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.information.VnfInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.identifiers.VnfInstanceIdentifiersBuilder
            u55 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.identifiers.VnfInstanceIdentifiersBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.information.VnfInstanceInformationBuilder
            u56 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.information.VnfInstanceInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.model.infrastructure.VnfInstanceListBuilder
            u57 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.model.infrastructure.VnfInstanceListBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.preload.data.VnfInstancePreloadDataBuilder
            u58 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.preload.data.VnfInstancePreloadDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.request.information.VnfInstanceRequestInformationBuilder
            u59 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.request.information.VnfInstanceRequestInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.request.information.vnf.instance.request.information.VnfNetworksBuilder
            u60 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.request.information.vnf.instance.request.information.VnfNetworksBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfInstancesBuilder
            u61 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfInstancesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.service.data.VnfInstanceServiceDataBuilder
            u62 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.service.data.VnfInstanceServiceDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.topology.information.VnfInstanceTopologyInformationBuilder
            u63 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.topology.information.VnfInstanceTopologyInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfInstanceTopologyOperationInputBuilder
            u64 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfInstanceTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfInstanceTopologyOperationOutputBuilder
            u65 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfInstanceTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.model.infrastructure.VnfListBuilder
            u66 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.model.infrastructure.VnfListBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.parameters.VnfParametersBuilder
            u67 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.parameters.VnfParametersBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.request.information.VnfRequestInformationBuilder
            u68 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.request.information.VnfRequestInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.request.information.vnf.request.information.VnfNetworksBuilder
            u69 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.request.information.vnf.request.information.VnfNetworksBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfsBuilder
            u70 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.topology.identifier.VnfTopologyIdentifierBuilder
            u71 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.topology.identifier.VnfTopologyIdentifierBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.topology.information.VnfTopologyInformationBuilder
            u72 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.topology.information.VnfTopologyInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfTopologyOperationInputBuilder
            u73 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfTopologyOperationOutputBuilder
            u74 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vpn.bindings.VpnBindingsBuilder
            u75 =
            new org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vpn.bindings.VpnBindingsBuilder();
    }
}
