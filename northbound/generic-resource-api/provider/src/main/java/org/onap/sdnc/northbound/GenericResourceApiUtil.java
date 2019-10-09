package org.onap.sdnc.northbound;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.onap.ccsdk.sli.core.sli.provider.MdsalHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericResourceApiUtil extends MdsalHelper {

    private static final Logger LOG = LoggerFactory.getLogger(GenericResourceApiUtil.class);

    public static final String PROPERTIES_FILE = "/opt/opendaylight/current/configuration/generic-resource-api.properties";
    private static Properties properties;

    public static void loadProperties() {

        File file = new File(PROPERTIES_FILE);
        properties = new Properties();
        if (file.isFile() && file.canRead()) {
            try (InputStream input = new FileInputStream(file)) {
                properties.load(input);
                LOG.info("Loaded properties from " + PROPERTIES_FILE);
                setYangMappingProperties(properties);
            } catch (IOException e) {
                LOG.error("Failed to close properties file " + PROPERTIES_FILE + "\n", e);
            } catch (Exception e) {
                LOG.error("Failed to load properties " + PROPERTIES_FILE + "\n", e);
            }
        }
    }

    static {

        // Trick class loader into loading builders. Some of
        // these will be needed later by Reflection classes, but need
        // to explicitly "new" them here to get class loader to load them.
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.onap.model.information.OnapModelInformationBuilder u1 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.onap.model.information.OnapModelInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.assignments.NetworkAssignmentsBuilder u2 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.assignments.NetworkAssignmentsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.information.NetworkInformationBuilder u3 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.information.NetworkInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.level.oper.status.NetworkLevelOperStatusBuilder u4 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.level.oper.status.NetworkLevelOperStatusBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.parameters.NetworkParametersBuilder u5 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.parameters.NetworkParametersBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.parameters.network.parameters.NetworkParameterBuilder u6 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.parameters.network.parameters.NetworkParameterBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.policy.NetworkPolicyBuilder u7 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.policy.NetworkPolicyBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.request.input.NetworkRequestInputBuilder u8 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.request.input.NetworkRequestInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.request.input.network.request.input.NetworkInputParametersBuilder u9 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.request.input.network.request.input.NetworkInputParametersBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.response.information.NetworkResponseInformationBuilder u10 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.response.information.NetworkResponseInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.topology.NetworkTopologyBuilder u14 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.network.topology.NetworkTopologyBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.param.ParamBuilder u18 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.param.ParamBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.preload.data.PreloadDataBuilder u19 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.preload.data.PreloadDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadNetworkTopologyOperationOutputBuilder u22 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadNetworkTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadInformationBuilder u23 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadVfModuleTopologyOperationInputBuilder u24 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadVfModuleTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadVfModuleTopologyOperationOutputBuilder u25 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadVfModuleTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.request.information.RequestInformationBuilder u26 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.request.information.RequestInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.route.table.reference.RouteTableReferenceBuilder u27 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.route.table.reference.RouteTableReferenceBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.sdnc.request.header.SdncRequestHeaderBuilder u28 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.sdnc.request.header.SdncRequestHeaderBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.assignments.ServiceAssignmentsBuilder u29 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.assignments.ServiceAssignmentsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.ServiceDataBuilder u30 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.ServiceDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.NetworksBuilder u31 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.NetworksBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.networks.NetworkBuilder u32 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.networks.NetworkBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.networks.network.NetworkDataBuilder u33 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.networks.network.NetworkDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.VnfsBuilder u34 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.VnfsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.vnfs.VnfBuilder u35 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.vnfs.VnfBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.vnfs.vnf.VnfDataBuilder u36 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.vnfs.vnf.VnfDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.vnfs.vnf.vnf.data.VfModulesBuilder u37 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.vnfs.vnf.vnf.data.VfModulesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.vnfs.vnf.vnf.data.vf.modules.VfModuleBuilder u38 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.vnfs.vnf.vnf.data.vf.modules.VfModuleBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.vnfs.vnf.vnf.data.vf.modules.vf.module.VfModuleDataBuilder u39 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.vnfs.vnf.vnf.data.vf.modules.vf.module.VfModuleDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.information.ServiceInformationBuilder u40 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.information.ServiceInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.level.oper.status.ServiceLevelOperStatusBuilder u41 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.level.oper.status.ServiceLevelOperStatusBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.model.infrastructure.ServiceBuilder u42 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.model.infrastructure.ServiceBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.parameters.ServiceParametersBuilder u43 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.parameters.ServiceParametersBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.parameters.service.parameters.ServiceParameterBuilder u44 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.parameters.service.parameters.ServiceParameterBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.request.input.ServiceRequestInputBuilder u45 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.request.input.ServiceRequestInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.request.input.service.request.input.ServiceInputParametersBuilder u46 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.request.input.service.request.input.ServiceInputParametersBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.response.information.ServiceResponseInformationBuilder u47 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.response.information.ServiceResponseInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ServicesBuilder u48 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ServicesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.status.ServiceStatusBuilder u49 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.status.ServiceStatusBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.topology.identifier.ServiceTopologyIdentifierBuilder u50 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.topology.identifier.ServiceTopologyIdentifierBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ServiceTopologyOperationInputBuilder u51 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ServiceTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ServiceTopologyOperationOutputBuilder u52 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ServiceTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.topology.ServiceTopologyBuilder u53 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.topology.ServiceTopologyBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.subnets.SubnetsBuilder u55 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.subnets.SubnetsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.assignments.VfModuleAssignmentsBuilder u56 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.assignments.VfModuleAssignmentsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.assignments.vf.module.assignments.VmsBuilder u57 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.assignments.vf.module.assignments.VmsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.information.VfModuleInformationBuilder u59 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.information.VfModuleInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.level.oper.status.VfModuleLevelOperStatusBuilder u60 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.level.oper.status.VfModuleLevelOperStatusBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.request.input.VfModuleRequestInputBuilder u61 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.request.input.VfModuleRequestInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.request.input.vf.module.request.input.VfModuleInputParametersBuilder u62 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.request.input.vf.module.request.input.VfModuleInputParametersBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.response.information.VfModuleResponseInformationBuilder u63 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.response.information.VfModuleResponseInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.topology.identifier.VfModuleTopologyIdentifierBuilder u64 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.topology.identifier.VfModuleTopologyIdentifierBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VfModuleTopologyOperationInputBuilder u65 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VfModuleTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VfModuleTopologyOperationOutputBuilder u66 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VfModuleTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.topology.VfModuleTopologyBuilder u67 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.topology.VfModuleTopologyBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.topology.vf.module.topology.VfModuleParametersBuilder u68 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vf.module.topology.vf.module.topology.VfModuleParametersBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.FloatingIpsBuilder u69 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.FloatingIpsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.InterfaceRoutePrefixesBuilder u70 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.InterfaceRoutePrefixesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.MacAddressesBuilder u71 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.MacAddressesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.NetworkInformationItemsBuilder u72 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.NetworkInformationItemsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.network.information.items.NetworkInformationItemBuilder u73 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.network.information.items.NetworkInformationItemBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.network.information.items.network.information.item.NetworkIpsBuilder u74 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vm.network.data.network.information.items.network.information.item.NetworkIpsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.request.input.VnfRequestInputBuilder u98 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.request.input.VnfRequestInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.request.input.vnf.request.input.VnfInputParametersBuilder u99 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.request.input.vnf.request.input.VnfInputParametersBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.request.input.vnf.request.input.VnfNetworksBuilder u100 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.request.input.vnf.request.input.VnfNetworksBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.resource.assignments.vnf.resource.assignments.AvailabilityZonesBuilder u102 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.resource.assignments.vnf.resource.assignments.AvailabilityZonesBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.resource.assignments.VnfResourceAssignmentsBuilder u103 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.resource.assignments.VnfResourceAssignmentsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.resource.assignments.vnf.resource.assignments.VnfNetworksBuilder u104 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.resource.assignments.vnf.resource.assignments.VnfNetworksBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.resource.assignments.vnf.resource.assignments.vnf.networks.VnfNetworkBuilder u105 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.resource.assignments.vnf.resource.assignments.vnf.networks.VnfNetworkBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.response.information.VnfResponseInformationBuilder u106 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.response.information.VnfResponseInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.topology.identifier.structure.VnfTopologyIdentifierStructureBuilder u107 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.vnf.topology.identifier.structure.VnfTopologyIdentifierStructureBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VnfTopologyOperationInputBuilder u108 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VnfTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VnfTopologyOperationOutputBuilder u109 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VnfTopologyOperationOutputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.PnfsBuilder u110 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.PnfsBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.pnfs.PnfBuilder u111 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.pnfs.PnfBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.pnfs.pnf.PnfDataBuilder u112 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.service.data.pnfs.pnf.PnfDataBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.pnf.request.input.PnfRequestInputBuilder u113 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.pnf.request.input.PnfRequestInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.pnf.request.input.pnf.request.input.PnfInputParametersBuilder u114 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.pnf.request.input.pnf.request.input.PnfInputParametersBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.pnf.response.information.PnfResponseInformationBuilder u115 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.pnf.response.information.PnfResponseInformationBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.pnf.topology.identifier.structure.PnfTopologyIdentifierStructureBuilder u116 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.pnf.topology.identifier.structure.PnfTopologyIdentifierStructureBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PnfTopologyOperationInputBuilder u117 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PnfTopologyOperationInputBuilder();
        org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PnfTopologyOperationOutputBuilder u118 = new org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PnfTopologyOperationOutputBuilder();
    }
}
