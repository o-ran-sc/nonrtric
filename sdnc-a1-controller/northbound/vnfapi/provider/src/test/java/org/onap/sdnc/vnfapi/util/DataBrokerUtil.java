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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.Vnfs;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.model.infrastructure.VnfList;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.model.infrastructure.VnfListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * This util class provides utility to read and write {@link VnfList} data objects from the {@link DataBroker}
 *
 */
public class DataBrokerUtil {


    private final DataBroker dataBroker;

    public DataBrokerUtil(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /** @return VnfList - the VnfList object read from the DataBroker or null if none was found */
    public VnfList read(String VnfListKey, LogicalDatastoreType logicalDatastoreType) throws Exception {
        InstanceIdentifier VnfListInstanceIdentifier = InstanceIdentifier.<Vnfs>builder(Vnfs.class)
                .child(VnfList.class, new VnfListKey(VnfListKey)).build();
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        Optional<VnfList> data = (Optional<VnfList>) readTx.read(logicalDatastoreType, VnfListInstanceIdentifier).get();
        if(!data.isPresent()){
            return null;
        }
        return data.get();
    }


    /**
     * Write the {@link VnfList} object to the {@link DataBroker}
     * @param isReplace - false specifies the new data is to be merged into existing data, where as true cause the
     *                  existing data to be replaced.
     * @param VnfList - the {@link VnfList} data object to be presisted in the db.
     * @param logicalDatastoreType - The logicalDatastoreType
     */
    public void write(boolean isReplace,VnfList VnfList, LogicalDatastoreType logicalDatastoreType) throws Exception {
        // Each entry will be identifiable by a unique key, we have to create that
        // identifier
        InstanceIdentifier.InstanceIdentifierBuilder<VnfList> VnfListBuilder = InstanceIdentifier
                .<Vnfs>builder(Vnfs.class).child(VnfList.class, VnfList.key());
        InstanceIdentifier<VnfList> path = VnfListBuilder.build();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        if (!isReplace) {
            tx.merge(logicalDatastoreType, path, VnfList);
        } else {
            tx.put(logicalDatastoreType, path, VnfList);
        }
        CheckedFuture<Void,TransactionCommitFailedException> cf = tx.submit();
        cf.checkedGet();

    }







}
