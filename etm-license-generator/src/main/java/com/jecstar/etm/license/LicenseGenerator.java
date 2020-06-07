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

package com.jecstar.etm.license;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Collectors;

class LicenseGenerator {

    private static final char[] HEX_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final int NO_ESCAPE = 0;
    private static final int STANDARD_ESCAPE = -1;
    private static final int[] ESCAPE_TABLE;

    static {
        int[] table = new int[128];
        for (int i = 0; i < 32; ++i) {
            table[i] = STANDARD_ESCAPE;
        }
        table['"'] = '"';
        table['\\'] = '\\';
        table[0x08] = 'b';
        table[0x09] = 't';
        table[0x0C] = 'f';
        table[0x0A] = 'n';
        table[0x0D] = 'r';
        ESCAPE_TABLE = table;
    }


    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, IOException, SignatureException {
        var calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2020);
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999)
        ;
        final var company = "Jecstar Innovation V.O.F.";
        final var maxRequestUnitsPerSecond = -1L;
        final long expiry = calendar.getTimeInMillis();

        final String licenseKey = "{"
                + "\"owner\":" + escapeToJson(company, true)
                + ",\"start_date\":" + Instant.now().toEpochMilli()
                + ",\"expiry_date\":" + expiry
                + ",\"max_request_units_per_second\":" + maxRequestUnitsPerSecond
                + ",\"license_type\":\"ON_PREM\""
                + "}";

        var hash = calculateHash(licenseKey);
        var signature = sign(hash);

        var license = "{"
                + "\"license\":" + licenseKey
                + ",\"hash\":" + escapeToJson(Base64.getEncoder().encodeToString(hash), true)
                + ",\"signature\":" + escapeToJson(Base64.getUrlEncoder().encodeToString(signature), true)
                + "}";

        System.out.println("License key: " + Base64.getEncoder().encodeToString(license.getBytes()));
    }

    private static byte[] sign(byte[] hash) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        String keyData = Files.lines(Path.of("/home/mark/programming/jecstar-workdir/etm_pk_v3.key")).filter(l -> !(l.equals("-----BEGIN EC PRIVATE KEY-----") || l.equals("-----END EC PRIVATE KEY-----"))).collect(Collectors.joining());
        var spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyData));
        var factory = KeyFactory.getInstance("EC");
        var privateKey = factory.generatePrivate(spec);
        var ecdsaVerify = Signature.getInstance("NonewithECDSAinP1363Format");
        ecdsaVerify.initSign(privateKey);
        ecdsaVerify.update(hash);
        return ecdsaVerify.sign();
    }

    private static byte[] calculateHash(String data) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        var mac = Mac.getInstance("HmacSHA512");
        var secretkey = new SecretKeySpec("etm license".getBytes(), "HmacSHA512");
        mac.init(secretkey);
        return mac.doFinal(data.getBytes("UTF-8"));
    }

    private static String escapeToJson(String value, boolean quote) {
        if (value == null) {
            return "null";
        }
        int maxLength = value.length() * 6;
        if (quote) {
            maxLength += 2;
        }
        char[] outputBuffer = new char[maxLength];
        final int escLen = ESCAPE_TABLE.length;
        int outputPointer = 0;
        if (quote) {
            outputBuffer[outputPointer++] = '"';
        }
        conversion_loop:
        for (int i = 0; i < value.length(); i++) {
            while (true) {
                char c = value.charAt(i);
                if (c < escLen && ESCAPE_TABLE[c] != NO_ESCAPE) {
                    break;
                }
                outputBuffer[outputPointer++] = c;
                if (++i >= value.length()) {
                    break conversion_loop;
                }
            }
            char c = value.charAt(i);
            outputPointer = appendCharacterEscape(outputBuffer, outputPointer, c, ESCAPE_TABLE[c]);
        }
        if (quote) {
            outputBuffer[outputPointer++] = '"';
        }
        char[] result = new char[outputPointer];
        System.arraycopy(outputBuffer, 0, result, 0, outputPointer);
        return new String(result);
    }

    private static int appendCharacterEscape(char[] outputBuffer, int outputPointer, char ch, int escCode) {
        if (escCode > NO_ESCAPE) {
            outputBuffer[outputPointer++] = '\\';
            outputBuffer[outputPointer++] = (char) escCode;
            return outputPointer;
        }
        if (escCode == STANDARD_ESCAPE) {
            outputBuffer[outputPointer++] = '\\';
            outputBuffer[outputPointer++] = 'u';
            // We know it's a control char, so only the last 2 chars are non-0
            if (ch > 0xFF) { // beyond 8 bytes
                int hi = (ch >> 8) & 0xFF;
                outputBuffer[outputPointer++] = HEX_CHARS[hi >> 4];
                outputBuffer[outputPointer++] = HEX_CHARS[hi & 0xF];
                ch &= 0xFF;
            } else {
                outputBuffer[outputPointer++] = '0';
                outputBuffer[outputPointer++] = '0';
            }
            outputBuffer[outputPointer++] = HEX_CHARS[ch >> 4];
            outputBuffer[outputPointer++] = HEX_CHARS[ch & 0xF];
            return outputPointer;
        }
        outputBuffer[outputPointer++] = (char) escCode;
        return outputPointer;
    }
}
