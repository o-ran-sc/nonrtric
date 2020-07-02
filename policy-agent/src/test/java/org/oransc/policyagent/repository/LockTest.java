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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.repository.Lock.LockType;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LockTest {

    @SuppressWarnings("squid:S2925") // "Thread.sleep" should not be used in tests.
    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Do nothing.
        }
    }

    private void asynchUnlock(Lock lock) {
        Thread thread = new Thread(() -> {
            sleep();
            lock.unlockBlocking();
        });
        thread.start();
    }

    @Test
    void testLock() throws IOException, ServiceException {
        Lock lock = new Lock();
        lock.lockBlocking(LockType.SHARED);
        lock.unlockBlocking();

        lock.lockBlocking(LockType.EXCLUSIVE);
        asynchUnlock(lock);

        lock.lockBlocking(LockType.SHARED);
        lock.unlockBlocking();

        assertThat(lock.getLockCounter()).isZero();
    }

    @Test
    void testReactiveLock() {
        Lock lock = new Lock();

        Mono<Lock> seq = lock.lock(LockType.EXCLUSIVE) //
            .flatMap(l -> lock.lock(LockType.EXCLUSIVE)) //
            .flatMap(l -> lock.unlock());

        asynchUnlock(lock);
        StepVerifier.create(seq) //
            .expectSubscription() //
            .expectNext(lock) //
            .verifyComplete();

        assertThat(lock.getLockCounter()).isZero();

    }

}
