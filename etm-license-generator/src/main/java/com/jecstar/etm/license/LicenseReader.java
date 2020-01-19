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

    private static final String PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEf3bVRVqMppp3yPpt2X7W5hojZ47fQhQ7Ii6QteeqOw2hHejVbhVoG0PMPeEYZAViEQ9eAoyc03imDukTkt9Z9g==";

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidParameterSpecException, IOException, SignatureException {
        final String licenseKey = "eyJsaWNlbnNlIjp7Im93bmVyIjoiSmVjc3RhciBJbm5vdmF0aW9uIFYuTy5GLiIsInN0YXJ0X2RhdGUiOjE1Nzk0NzcwMjc3MDQsImV4cGlyeV9kYXRlIjoxNjA5NDU5MTk5OTk5LCJtYXhfcmVxdWVzdF91bml0c19wZXJfc2Vjb25kIjotMSwibGljZW5zZV90eXBlIjoiT05fUFJFTSJ9LCJoYXNoIjoiUmMrcFBzSVF3YklNZHdYa2hmMVEwYkZLeUlUbTQwVHNYKzF2Z0F4RTFuV1BETUNVRGJtYmExeEdlbTZDK0J1aGp2Uzhja1d4UkhHSUdYMWRUMWVIZ1E9PSIsInNpZ25hdHVyZSI6InR6eTd2U2s4dFBIb0JoOFVmZnJ1ODlzU05VcWtweGtDb0p5amExSGhzOHp1alc5NkI2dUc3N1pPdjJVSVp3Tk4wRWQ1UUtLTFY3R0ZubnBVV0RqalF3PT0ifQ==";
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
//        var keySpec = new ECPublicKeySpec(new ECPoint(new BigInteger("57653691665462833728786391541206384446957054885608787839261144528773785991949"), new BigInteger("72875214145998715462716040894238225831315714718726867635759772266221281434102")), ecParameterSpec);
        return keyFactory.generatePublic(keySpec);
    }

    private static String calculateBase64Hash(String data) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        var mac = Mac.getInstance("HmacSHA512");
        var secretkey = new SecretKeySpec("etm license".getBytes(), "HmacSHA512");
        mac.init(secretkey);
        byte[] mac_data = mac.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(mac_data);
    }
}
