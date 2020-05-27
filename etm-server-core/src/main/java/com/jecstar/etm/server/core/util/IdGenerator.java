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

package com.jecstar.etm.server.core.util;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Id generator that is heavily based on the Elasticsearch ID generator.
 */
public class IdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AtomicInteger sequenceNumber = new AtomicInteger(SECURE_RANDOM.nextInt());

    private final AtomicLong lastTimestamp = new AtomicLong(0);
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    private static final byte[] SECURE_MUNGED_ADDRESS = getSecureMungedAddress();

    static {
        assert SECURE_MUNGED_ADDRESS.length == 6;
    }

    private static byte[] getMacAddress() throws SocketException {
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        if (en != null) {
            while (en.hasMoreElements()) {
                NetworkInterface networkInterface = en.nextElement();
                if (!networkInterface.isLoopback()) {
                    // Pick the first valid non loopback address we find
                    byte[] address = networkInterface.getHardwareAddress();
                    if (isValidAddress(address)) {
                        return address;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isValidAddress(byte[] address) {
        if (address == null || address.length != 6) {
            return false;
        }
        for (byte b : address) {
            if (b != 0x00) {
                return true; // If any of the bytes are non zero assume a good address
            }
        }
        return false;
    }

    private static byte[] getSecureMungedAddress() {
        byte[] address = null;
        try {
            address = getMacAddress();
        } catch (SocketException e) {
            // Dummy address created later on.
        }

        if (!isValidAddress(address)) {
            address = constructDummyMulticastAddress();
        }

        byte[] mungedBytes = new byte[6];
        SECURE_RANDOM.nextBytes(mungedBytes);
        for (int i = 0; i < 6; ++i) {
            mungedBytes[i] ^= address[i];
        }

        return mungedBytes;
    }

    private static byte[] constructDummyMulticastAddress() {
        byte[] dummy = new byte[6];
        SECURE_RANDOM.nextBytes(dummy);
        dummy[0] |= (byte) 0x01;
        return dummy;
    }

    public String createId() {
        final int sequenceId = sequenceNumber.incrementAndGet() & 0xffffff;
        long currentTimeMillis = System.currentTimeMillis();

        long timestamp = this.lastTimestamp.updateAndGet(lastTimestamp -> {
            long nonBackwardsTimestamp = Math.max(lastTimestamp, currentTimeMillis);

            if (sequenceId == 0) {
                nonBackwardsTimestamp++;
            }

            return nonBackwardsTimestamp;
        });

        final byte[] uuidBytes = new byte[15];
        int i = 0;


        uuidBytes[i++] = (byte) sequenceId;
        uuidBytes[i++] = (byte) (sequenceId >>> 16);
        uuidBytes[i++] = (byte) (timestamp >>> 16);
        uuidBytes[i++] = (byte) (timestamp >>> 24);
        uuidBytes[i++] = (byte) (timestamp >>> 32);
        uuidBytes[i++] = (byte) (timestamp >>> 40);
        System.arraycopy(SECURE_MUNGED_ADDRESS, 0, uuidBytes, i, SECURE_MUNGED_ADDRESS.length);
        i += SECURE_MUNGED_ADDRESS.length;

        uuidBytes[i++] = (byte) (timestamp >>> 8);
        uuidBytes[i++] = (byte) (sequenceId >>> 8);
        uuidBytes[i++] = (byte) timestamp;

        return this.encoder.encodeToString(uuidBytes);
    }
}
