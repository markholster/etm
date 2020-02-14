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

package com.jecstar.etm.server.core.domain.converter;

import com.jecstar.etm.domain.PayloadEncoding;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.InflaterInputStream;

/**
 * Class that decodes the encoded payload of an event.
 */
public class PayloadDecoder {

    private static final LogWrapper log = LogFactory.getLogger(PayloadDecoder.class);


    public String decode(String encodedData, PayloadEncoding encoding) {
        if (encodedData == null) {
            return null;
        }
        if (encoding == null) {
            return encodedData;
        }

        try {
            if (PayloadEncoding.BASE64.equals(encoding)) {
                return new String(Base64.getDecoder().decode(encodedData));
            } else if (PayloadEncoding.BASE64_CA_API_GATEWAY.equals(encoding)) {
                final int l7HeaderLength = 4;
                byte[] base64DecodedData = Base64.getDecoder().decode(encodedData);
                byte[] l7Header = new byte[l7HeaderLength];
                try (ByteArrayInputStream bis = new ByteArrayInputStream(base64DecodedData); InflaterInputStream iis = new InflaterInputStream(bis)) {
                    int read = bis.read(l7Header, 0, l7HeaderLength);
                    if (read != l7HeaderLength) {
                        if (log.isErrorLevelEnabled()) {
                            log.logErrorMessage("Unable to decode payload. Probably not an CA Api Gateway encoding.");
                        }
                        return null;
                    }
                    long uncompressedLength = calculateUncompressedLength(l7Header);
                    byte[] result = new byte[(int) uncompressedLength];
                    int offset = 0;
                    byte[] buffer = new byte[2048];
                    while ((read = iis.read(buffer, 0, buffer.length)) != -1) {
                        System.arraycopy(buffer, 0, result, offset, read);
                        offset += read;
                    }
                    return new String(result, "UTF-8");
                } catch (IOException e) {
                    if (log.isDebugLevelEnabled()) {
                        log.logDebugMessage("Unable to decode payload.", e);
                    }
                }
            }
        } catch (Exception e) {
            log.logDebugMessage("Unable to decode payload with encoding '" + encoding + "'.", e);
        }
        return encodedData;
    }


    /**
     * Calculate the length of the data when uncompressed.
     *
     * @param l7Header The layer7 (ca api gateway) header.
     * @return The length of the uncompressed data.
     */
    private static long calculateUncompressedLength(byte[] l7Header) {
        long uncompressedLength = 0;
        if (l7Header.length == 4) {
            uncompressedLength |= ((long) l7Header[0] & 255);
            uncompressedLength |= ((long) l7Header[1] & 255) << 8;
            uncompressedLength |= ((long) l7Header[2] & 255) << 16;
            uncompressedLength |= ((long) l7Header[3] & 255) << 24;
        }
        return uncompressedLength;
    }

}
