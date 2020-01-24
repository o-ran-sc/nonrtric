
/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
 * %%
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
 * ========================LICENSE_END===================================
 */

package org.oransc.policyagent.dmaap;

import java.util.Properties;

/**
 * The Dmaap consumer which has the base methods to be implemented by any class which implements this interface
 *
 */
public interface DmaapMessageConsumer {

    /**
     * The init method creates the MRConsumer with the properties passed from the Application Config
     *
     * @param properties
     */
    public void init(Properties properties);

    /**
     * This method process the message and call the respective Controller
     *
     * @param msg
     * @throws Exception
     */
    public abstract void processMsg(String msg) throws Exception;

    /**
     * To check whether the DMAAP Listner is alive
     *
     * @return boolean
     */
    public boolean isAlive();

    /**
     * To Stop the DMAAP Listener
     */
    public void stopConsumer();

    /**
     * It's a infinite loop run every configured seconds to fetch the message from DMAAP. This method can be stop by
     * setting the alive flag to false
     */
    public void run();

}
