package com.jecstar.etm.license;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

class LicenseReader {

    private static final String PUBLIC_KEY = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtBCraSZNjqfqnDK/ESEqwZWZiDY6YRe72N8id//B5LHv7eO41cgRrKzAIn+WH10C3jOjGpJjF1RITKTJg1FM4CK+L66hYP3HQVX8ghtQT99TkHuTkTGxbbBMZd4VF77TR5mTa0LjMTGz7+r9q0PAQEGPol/WqaOTxGHiizh7/qmA0hvAA4Ff39T0CsFyWFpI4hmfS5JG/sLsG8WKd125A1VJFk76ZH7kWP1ysrzGzbR1vSQznQpzz7GPpbzFgjDWJpvQzLREv7qSn1z7MGD4YKlLpgaYxoPUsF2kg4N3YzvZw+RfMTFS2v689VmLccZbySXSoqXyssSq6oMlXIwDSus5qFaB1TeYFJWHZh/t6QHHYeyI0RW6pzIAAG/yGF9uX13uiIb9J9+Qu02XAPstl0ZsVfAVdbzV1AKFMPVOCzMHk6T8YcLsFKedigeH4K2vzdQyHC4L0oZ+2xYiDp904Y7A20HfTyBhVJmz7OIKLjJbnuCh8wP1g9VAR9NC468/nhEdCBxT2nHvvJMLzw2xUBYNIoSw5rWd5+nO9kiCWD7OoNpL5nTRRlX3jBpuqEJmszQo3wF0jZEqAi/pYn3c60iEljtx8m8K8EgjylS/C49qBDUfCQnwfNQxGjxzEzeFc9+mJRox87kxYMUsCyT5u46f8P1wfHOWxzubRgcr0hECAwEAAQ==";

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        final String licenseKey = "LVp0yNJH3JvyDlzN1pwWSZnKAkpCGwoHLolwyYjPrW4+aip5DMMB2tOgMPozf3rect+qGie9aU7QRCYIHRv/2JyrrO10Aj+NJilmwPTSpO3LCA3w8a6KH72CEGH+xXaTFTxpSF1XGV1rmENXsFBtf8G1ekwcwmkv61wVdinneR/NeWJ7LEvA2N0SRRE3vwBZyGFBcomrqVlGWNrEHS12lOazkYmZGo5EyYLsZtrJJTsXJZgIfuc6b7jRd2ghA5j2l/hx7M8Lu8TuFTMmZ7i+ChTXukHWT1cdC3dx9XJF90npSi3a0wRYp5KO65DyAMZInh2a6ew/OAAHcQ6VKJVscBh4mFpTDB4hIhxEuH0/80tU3+KQs/Usng1KbDXrzUDk6lIcJf9JpDgH40y0km8R1sfrWtcpGEFiechtdApj1MgKe67TpMn0JKlVEO1C0/lRj9uclGoXNqYvzS0s4tSTZlPF3vy2TwwaZLIornBFNN34oVUby2gd2Lsa6u/3HbdBeqypHVo4M89KhCyYKxdEhkhMUxUoC5ZFxHMhFXknsX6S+MfPIkGBzm4kOQbEZokdRiK/u3/dJvXRuc6xDaz6m4sHv6+dktW9Hh9+pG48ypVsRQVZBz8W7W7vyE0X681isHFRHJdOg7lnOIIU94ZCIcj5l3K0xaikgEebMZSUzT0=";

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
                Base64.getDecoder().decode(PUBLIC_KEY));
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        Cipher decrpyptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decrpyptCipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] decryptedBytes = decrpyptCipher.doFinal(Base64.getDecoder().decode(licenseKey));
        String license = new String(decryptedBytes);
        System.out.println(license);
    }
}
