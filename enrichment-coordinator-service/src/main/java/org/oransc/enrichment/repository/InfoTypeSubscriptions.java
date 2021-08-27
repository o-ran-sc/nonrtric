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

package org.oransc.enrichment.repository;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import lombok.Builder;
import lombok.Getter;

import org.oransc.enrichment.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Subscriptions of callbacks for type registrations
 */
@SuppressWarnings("squid:S2629") // Invoke method(s) only conditionally
@Component
public class InfoTypeSubscriptions {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Map<String, SubscriptionInfo> allSubscriptions = new HashMap<>();
    private final MultiMap<SubscriptionInfo> subscriptionsByOwner = new MultiMap<>();

    public interface Callbacks {
        void notifyTypeRegistered(InfoType type, SubscriptionInfo subscriptionInfo);

        void notifyTypeRemoved(InfoType type, SubscriptionInfo subscriptionInfo);
    }

    @Builder
    @Getter
    public static class SubscriptionInfo {
        private String id;

        private String callbackUrl;

        private String owner;

        private Callbacks callback;
    }

    public synchronized void put(SubscriptionInfo subscription) {
        allSubscriptions.put(subscription.getId(), subscription);
        subscriptionsByOwner.put(subscription.owner, subscription.id, subscription);
        logger.debug("Added type status subscription {}", subscription.id);
    }

    public synchronized Collection<SubscriptionInfo> getAllSubscriptions() {
        return new Vector<>(allSubscriptions.values());
    }

    /**
     * Get a subscription and throw if not fond.
     * 
     * @param id the ID of the subscription to get.
     * @return SubscriptionInfo
     * @throws ServiceException if not found
     */
    public synchronized SubscriptionInfo getSubscription(String id) throws ServiceException {
        SubscriptionInfo p = allSubscriptions.get(id);
        if (p == null) {
            throw new ServiceException("Could not find Information subscription: " + id);
        }
        return p;
    }

    /**
     * Get a subscription or return null if not found. Equivalent to get in all java
     * collections.
     * 
     * @param id the ID of the subscription to get.
     * @return SubscriptionInfo
     */
    public synchronized SubscriptionInfo get(String id) {
        return allSubscriptions.get(id);
    }

    public synchronized int size() {
        return allSubscriptions.size();
    }

    public synchronized void clear() {
        allSubscriptions.clear();
        subscriptionsByOwner.clear();
    }

    public void remove(SubscriptionInfo subscription) {
        allSubscriptions.remove(subscription.getId());
        subscriptionsByOwner.remove(subscription.owner, subscription.id);
        logger.debug("Removed type status subscription {}", subscription.id);
    }

    /**
     * returns all subscriptions for an owner. The colllection can contain 0..n
     * subscriptions.
     * 
     * @param owner
     * @return
     */
    public synchronized Collection<SubscriptionInfo> getSubscriptionsForOwner(String owner) {
        return subscriptionsByOwner.get(owner);
    }

    public synchronized void notifyTypeRegistered(InfoType type) {
        allSubscriptions.forEach((id, subscription) -> subscription.callback.notifyTypeRegistered(type, subscription));
    }

    public synchronized void notifyTypeRemoved(InfoType type) {
        allSubscriptions.forEach((id, subscription) -> subscription.callback.notifyTypeRemoved(type, subscription));
    }

}
