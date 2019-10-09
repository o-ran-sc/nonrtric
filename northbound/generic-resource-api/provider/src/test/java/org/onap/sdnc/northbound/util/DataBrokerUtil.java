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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.Services;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.ServiceDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.level.oper.status.ServiceLevelOperStatusBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.model.infrastructure.Service;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.model.infrastructure.ServiceBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.model.infrastructure.ServiceKey;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.status.ServiceStatusBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.onap.sdnc.northbound.util.MDSALUtil.build;


/**
 * This util class provides utility to read and write {@link Service} data objects from the {@link DataBroker}
 *
 */
public class DataBrokerUtil {


    private final DataBroker dataBroker;

    public DataBrokerUtil(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /** @return Service - the Service object read from the DataBroker or null if none was found */
    public Service read(String serviceKey, LogicalDatastoreType logicalDatastoreType) throws Exception {
        InstanceIdentifier serviceInstanceIdentifier = InstanceIdentifier.<Services>builder(Services.class)
                .child(Service.class, new ServiceKey(serviceKey)).build();
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        Optional<Service> data = (Optional<Service>) readTx.read(logicalDatastoreType, serviceInstanceIdentifier).get();
        if(!data.isPresent()){
            return null;
        }


        //The toString() value from a Service object returned form data.get() is different than the toString() value
        //from a Service Object constructed from a Builder. This makes it difficult to compare deltas when doing a
        // assertEquals.  That why we rebuild it her to solve that problem.
        return build(ServiceBuilder::new,data.get(),(service) -> service
                .setServiceStatus(build(ServiceStatusBuilder::new,service.getServiceStatus()))
                .setServiceData(build(ServiceDataBuilder::new,service.getServiceData(),(serviceStatus)->serviceStatus
                                .setServiceLevelOperStatus(build(ServiceLevelOperStatusBuilder::new,serviceStatus.getServiceLevelOperStatus()))
                                ))
        );
    }


    /**
     * Write the {@link Service} object to the {@link DataBroker}
     * @param isReplace - false specifies the new data is to be merged into existing data, where as true cause the
     *                  existing data to be replaced.
     * @param service - the {@link Service} data object to be presisted in the db.
     * @param logicalDatastoreType - The logicalDatastoreType
     */
    public void write(boolean isReplace,Service service, LogicalDatastoreType logicalDatastoreType) throws Exception {
        // Each entry will be identifiable by a unique key, we have to create that
        // identifier
        InstanceIdentifier.InstanceIdentifierBuilder<Service> serviceBuilder = InstanceIdentifier
                .<Services>builder(Services.class).child(Service.class, service.key());
        InstanceIdentifier<Service> path = serviceBuilder.build();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        if (!isReplace) {
            tx.merge(logicalDatastoreType, path, service);
        } else {
            tx.put(logicalDatastoreType, path, service);
        }
        CheckedFuture<Void,TransactionCommitFailedException> cf = tx.submit();
        cf.checkedGet();

    }







}
