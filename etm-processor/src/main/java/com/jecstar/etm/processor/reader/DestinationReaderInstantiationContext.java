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

package com.jecstar.etm.processor.reader;

/**
 * A context class that holds the context under which a new <code>DestinationReader</code> is created.
 *
 * @param <T>
 */
public class DestinationReaderInstantiationContext<T extends DestinationReader> {

    private final DestinationReaderPool<T> destinationReaderPool;
    private final int indexInPool;

    DestinationReaderInstantiationContext(DestinationReaderPool<T> destinationReaderPool, int indexInPool) {
        this.destinationReaderPool = destinationReaderPool;
        this.indexInPool = indexInPool;
    }

    public DestinationReaderPool<T> getDestinationReaderPool() {
        return this.destinationReaderPool;
    }

    public int getIndexInPool() {
        return this.indexInPool;
    }
}
