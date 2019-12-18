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

package org.oransc.policyagent.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.oransc.policyagent.configuration.RicConfig;

/**
 * Represents the dynamic information about a NearRealtime-RIC.
 */
public class Ric {
    private final RicConfig ricConfig;
    private RicState state = RicState.NOT_INITIATED;
    private Map<String, PolicyType> supportedPolicyTypes = new HashMap<>();

    /**
     * Creates the Ric. Initial state is {@link RicState.NOT_INITIATED}.
     *
     * @param ricConfig The {@link RicConfig} for this Ric.
     */
    public Ric(RicConfig ricConfig) {
        this.ricConfig = ricConfig;
    }

    public String name() {
        return ricConfig.name();
    }

    public RicState state() {
        return state;
    }

    public void setState(RicState newState) {
        state = newState;
    }

    public RicConfig getConfig() {
        return this.ricConfig;
    }

    /**
     * Gets the nodes managed by this Ric.
     *
     * @return a vector containing the nodes managed by this Ric.
     */
    public Vector<String> getManagedNodes() {
        return ricConfig.managedElementIds();
    }

    /**
     * Determines if the given node is managed by this Ric.
     *
     * @param nodeName the node name to check.
     * @return true if the given node is managed by this Ric.
     */
    public boolean isManaging(String nodeName) {
        return ricConfig.managedElementIds().contains(nodeName);
    }

    /**
     * Adds the given node as managed by this Ric.
     *
     * @param nodeName the node to add.
     */
    public void addManagedNode(String nodeName) {
        if (!ricConfig.managedElementIds().contains(nodeName)) {
            ricConfig.managedElementIds().add(nodeName);
        }
    }

    /**
     * Removes the given node as managed by this Ric.
     *
     * @param nodeName the node to remove.
     */
    public void removeManagedNode(String nodeName) {
        ricConfig.managedElementIds().remove(nodeName);
    }

    /**
     * Gets the policy types supported by this Ric.
     *
     * @return the policy types supported by this Ric in an unmodifiable list.
     */
    public Collection<PolicyType> getSupportedPolicyTypes() {
        return supportedPolicyTypes.values();
    }

    public Collection<String> getSupportedPolicyTypeNames() {
        return supportedPolicyTypes.keySet();
    }

    /**
     * Adds a policy type as supported by this Ric.
     *
     * @param type the policy type to support.
     */
    public void addSupportedPolicyType(PolicyType type) {
        supportedPolicyTypes.put(type.name(), type);
    }

    /**
     * Adds policy types as supported by this Ric.
     *
     * @param types the policy types to support.
     */
    public void addSupportedPolicyTypes(Collection<PolicyType> types) {
        for (PolicyType type : types) {
            addSupportedPolicyType(type);
        }
    }

    /**
     * Removes a policy type as supported by this Ric.
     *
     * @param type the policy type to remove as supported by this Ric.
     */
    public void removeSupportedPolicyType(PolicyType type) {
        supportedPolicyTypes.remove(type.name());
    }

    /**
     * Checks if a type is supported by this Ric.
     *
     * @param type the type to check if it is supported.
     *
     * @return true if the given type issupported by this Ric, false otherwise.
     */
    public boolean isSupportingType(String typeName) {
        return supportedPolicyTypes.containsKey(typeName);
    }

    /**
     * Represents the states possible for a Ric.
     */
    public static enum RicState {
        /**
         * The Ric has not been initiated yet.
         */
        NOT_INITIATED,
        /**
         * The Ric is working fine.
         */
        ACTIVE,
        /**
         * Something is wrong with the Ric.
         */
        FAULTY,
        /**
         * The node is unreachable at the moment.
         */
        UNREACHABLE
    }
}
