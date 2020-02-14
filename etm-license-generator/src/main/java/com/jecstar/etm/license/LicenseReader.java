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

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

class LicenseReader {

    private static final String PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEOJ1GA0fdjjCZKxaUwHidhu52fy3F9V+Ow3PxbVDmbk2EYtP2VBJNXZ/YRlW1gAid8v9GNexTRes4FU47L+kk8A==";

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidParameterSpecException, IOException, SignatureException {
        final String licenseKey = "<ENTER_LICENSE_HERE>";
        byte[] decodedLicense = Base64.getDecoder().decode(licenseKey);

        final ObjectMapper objectMapper = new ObjectMapper();
        var fullLicenseMap = objectMapper.readValue(new String(decodedLicense), HashMap.class);

        var licenseMap = (Map<String, Object>) fullLicenseMap.get("license");
        var hash = (String) fullLicenseMap.get("hash");
        var signature = Base64.getUrlDecoder().decode((String) fullLicenseMap.get("signature"));

        var calculatedHash = calculateBase64Hash(objectMapper.writeValueAsString(licenseMap));
        if (!calculatedHash.equals(hash)) {
            throw new RuntimeException("Invalid hash");
        }

        Signature ecdsaVerify = Signature.getInstance("NonewithECDSAinP1363Format");
        PublicKey publicKey = createPublicKey();
        ecdsaVerify.initVerify(publicKey);
        ecdsaVerify.update(Base64.getDecoder().decode(hash.getBytes()));
        if (!ecdsaVerify.verify(signature)) {
            throw new RuntimeException("Invalid signature");
        }
        System.out.println(objectMapper.writeValueAsString(fullLicenseMap));
    }

    private static PublicKey createPublicKey() throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException {
        var keyFactory = KeyFactory.getInstance("EC");
        var keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY));

        // Build a key from scratch.
//        var parameters = AlgorithmParameters.getInstance("EC");
//        var paramSpec = new ECGenParameterSpec("secp256r1");
//        parameters.init(paramSpec);
//        var ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);
//        var keySpec = new ECPublicKeySpec(new ECPoint(new BigInteger("25607397720525554755086171574123621152492826433167876032144549549701908033101"), new BigInteger("59879909934819259450180492625915173288861052532058099988322409938911501034736")), ecParameterSpec);
        var publicKey = keyFactory.generatePublic(keySpec);
//        System.out.println(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        return publicKey;
    }

    private static String calculateBase64Hash(String data) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        var mac = Mac.getInstance("HmacSHA512");
        var secretkey = new SecretKeySpec("etm license".getBytes(), "HmacSHA512");
        mac.init(secretkey);
        byte[] mac_data = mac.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(mac_data);
    }
}
