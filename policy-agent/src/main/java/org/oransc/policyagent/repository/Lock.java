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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * A resource lock. Exclusive means that the caller takes exclusive ownership of
 * the resurce. Non exclusive lock means that several users can lock the
 * resource (for shared usage).
 */
public class Lock {
    private static final Logger logger = LoggerFactory.getLogger(Lock.class);

    private boolean isExclusive = false;
    private int cnt = 0;

    public static enum LockType {
        EXCLUSIVE, SHARED
    }

    /** The caller thread will be blocked util the lock is granted. */
    public synchronized void lockBlocking(LockType locktype) {
        while (!tryLock(locktype)) {
            this.waitForUnlock();
        }
    }

    /** Reactive version. The Lock will be emitted when the lock is granted */
    public synchronized Mono<Lock> lock(LockType lockType) {
        if (tryLock(lockType)) {
            return Mono.just(this);
        } else {
            return Mono.create(monoSink -> addToQueue(monoSink, lockType));
        }
    }

    public Mono<Lock> unlock() {
        return Mono.create(monoSink -> {
            unlockBlocking();
            monoSink.success(this);
        });
    }

    public void unlockBlocking() {
        synchronized (this) {
            if (cnt <= 0) {
                cnt = -1; // Might as well stop, to make it easier to find the problem
                throw new RuntimeException("Number of unlocks must match the number of locks");
            }
            this.cnt--;
            if (cnt == 0) {
                isExclusive = false;
            }
            this.notifyAll();
        }
        this.processQueuedEntries();

    }

    @Override
    public String toString() {
        return "Lock cnt: " + this.cnt + " exclusive: " + this.isExclusive;
    }

    /** returns the current number of granted locks */
    public synchronized int getLockCounter() {
        return this.cnt;
    }

    private void processQueuedEntries() {
        List<LockRequest> granted = new ArrayList<>();
        synchronized (this) {
            for (Iterator<LockRequest> i = lockRequestQueue.iterator(); i.hasNext();) {
                LockRequest request = i.next();
                if (tryLock(request.lockType)) {
                    i.remove();
                    granted.add(request);
                }
            }
        }
        for (LockRequest request : granted) {
            request.callback.success(this);
        }
    }

    private static class LockRequest {
        final MonoSink<Lock> callback;
        final LockType lockType;

        LockRequest(MonoSink<Lock> callback, LockType lockType) {
            this.callback = callback;
            this.lockType = lockType;
        }
    }

    private final List<LockRequest> lockRequestQueue = new LinkedList<>();

    private synchronized void addToQueue(MonoSink<Lock> callback, LockType lockType) {
        lockRequestQueue.add(new LockRequest(callback, lockType));
    }

    private void waitForUnlock() {
        try {
            this.wait();
        } catch (InterruptedException e) {
            logger.warn("waitForUnlock interrupted", e);
        }
    }

    private boolean tryLock(LockType lockType) {
        if (this.isExclusive) {
            return false;
        }
        if (lockType == LockType.EXCLUSIVE && cnt > 0) {
            return false;
        }
        cnt++;
        this.isExclusive = lockType == LockType.EXCLUSIVE;
        return true;
    }

}
