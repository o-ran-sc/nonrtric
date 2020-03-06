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
    private int lockCounter = 0;
    private final List<LockRequest> lockRequestQueue = new LinkedList<>();
    private static AsynchCallbackExecutor callbackProcessor = new AsynchCallbackExecutor();

    private static class AsynchCallbackExecutor implements Runnable {
        private List<LockRequest> lockRequestQueue = new LinkedList<>();

        public AsynchCallbackExecutor() {
            Thread thread = new Thread(this);
            thread.start();
        }

        public synchronized void addAll(List<LockRequest> requests) {
            this.lockRequestQueue.addAll(requests);
            this.notifyAll();
        }

        @Override
        public void run() {
            while (true) {
                for (LockRequest request : consume()) {
                    request.callback.success(request.lock);
                }
                waitForNewEntries();
            }
        }

        private synchronized List<LockRequest> consume() {
            List<LockRequest> q = this.lockRequestQueue;
            this.lockRequestQueue = new LinkedList<>();
            return q;
        }

        @SuppressWarnings("java:S2274")
        private synchronized void waitForNewEntries() {
            try {
                if (this.lockRequestQueue.isEmpty()) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                logger.warn("waitForUnlock interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    public enum LockType {
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
            if (lockCounter <= 0) {
                lockCounter = -1; // Might as well stop, to make it easier to find the problem
                throw new NullPointerException("Number of unlocks must match the number of locks");
            }
            this.lockCounter--;
            if (lockCounter == 0) {
                isExclusive = false;
            }
            this.notifyAll();
        }
        this.processQueuedEntries();
    }

    @Override
    public String toString() {
        return "Lock cnt: " + this.lockCounter + " exclusive: " + this.isExclusive;
    }

    /** returns the current number of granted locks */
    public synchronized int getLockCounter() {
        return this.lockCounter;
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
        callbackProcessor.addAll(granted);
    }

    private static class LockRequest {
        final MonoSink<Lock> callback;
        final LockType lockType;
        final Lock lock;

        LockRequest(MonoSink<Lock> callback, LockType lockType, Lock lock) {
            this.callback = callback;
            this.lockType = lockType;
            this.lock = lock;
        }
    }

    private synchronized void addToQueue(MonoSink<Lock> callback, LockType lockType) {
        lockRequestQueue.add(new LockRequest(callback, lockType, this));
    }

    @SuppressWarnings("java:S2274")
    private synchronized void waitForUnlock() {
        try {
            this.wait();
        } catch (InterruptedException e) {
            logger.warn("waitForUnlock interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private boolean tryLock(LockType lockType) {
        if (this.isExclusive) {
            return false;
        }
        if (lockType == LockType.EXCLUSIVE && lockCounter > 0) {
            return false;
        }
        lockCounter++;
        this.isExclusive = lockType == LockType.EXCLUSIVE;
        return true;
    }

}
