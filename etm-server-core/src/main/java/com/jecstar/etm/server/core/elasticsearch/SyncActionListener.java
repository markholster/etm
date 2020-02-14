/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.elasticsearch;


import org.elasticsearch.action.ActionListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SyncActionListener<Response> implements ActionListener<Response> {

    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<Response> response = new AtomicReference<>();
    private final AtomicReference<Exception> exception = new AtomicReference<>();

    private final long timeout;

    public SyncActionListener(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void onResponse(Response response) {
        boolean wasResponseNull = this.response.compareAndSet(null, response);
        if (!wasResponseNull) {
            throw new IllegalStateException("response is already set");
        }

        this.latch.countDown();
    }

    @Override
    public void onFailure(Exception exception) {
        boolean wasExceptionNull = this.exception.compareAndSet(null, exception);
        if (!wasExceptionNull) {
            throw new IllegalStateException("exception is already set");
        }
        latch.countDown();
    }

    /**
     * Waits (up to a timeout) for some result of the request: either a response, or an exception.
     */
    public Response get() {
        try {
            //providing timeout is just a safety measure to prevent everlasting waits
            //the different client timeouts should already do their jobs
            if (!this.latch.await(this.timeout, TimeUnit.MILLISECONDS)) {
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("thread waiting for the response was interrupted", e);
        }
        return this.response.get();
    }
}
