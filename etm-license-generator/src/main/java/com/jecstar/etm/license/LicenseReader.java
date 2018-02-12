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
import java.util.Date;

class LicenseReader {

    private static final String PUBLIC_KEY = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtBCraSZNjqfqnDK/ESEqwZWZiDY6YRe72N8id//B5LHv7eO41cgRrKzAIn+WH10C3jOjGpJjF1RITKTJg1FM4CK+L66hYP3HQVX8ghtQT99TkHuTkTGxbbBMZd4VF77TR5mTa0LjMTGz7+r9q0PAQEGPol/WqaOTxGHiizh7/qmA0hvAA4Ff39T0CsFyWFpI4hmfS5JG/sLsG8WKd125A1VJFk76ZH7kWP1ysrzGzbR1vSQznQpzz7GPpbzFgjDWJpvQzLREv7qSn1z7MGD4YKlLpgaYxoPUsF2kg4N3YzvZw+RfMTFS2v689VmLccZbySXSoqXyssSq6oMlXIwDSus5qFaB1TeYFJWHZh/t6QHHYeyI0RW6pzIAAG/yGF9uX13uiIb9J9+Qu02XAPstl0ZsVfAVdbzV1AKFMPVOCzMHk6T8YcLsFKedigeH4K2vzdQyHC4L0oZ+2xYiDp904Y7A20HfTyBhVJmz7OIKLjJbnuCh8wP1g9VAR9NC468/nhEdCBxT2nHvvJMLzw2xUBYNIoSw5rWd5+nO9kiCWD7OoNpL5nTRRlX3jBpuqEJmszQo3wF0jZEqAi/pYn3c60iEljtx8m8K8EgjylS/C49qBDUfCQnwfNQxGjxzEzeFc9+mJRox87kxYMUsCyT5u46f8P1wfHOWxzubRgcr0hECAwEAAQ==";

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        final String licenseKey = "MSx50HWEoub86CtuBzpvrNyG1DyO00dp+k1TBqcmHzLOKYcvXoRgwlDhTSe/ABOwUgygrHHZow+hyyqa3Y0y7PoUSeGWce6/l0ljtA5ZdpR2M/ff54yej3Kgy6XsJ3JSIIW/3AlSm36nMDvbPH1bhxtcq7R7fPzo9NBLn3MoPq7ArZrVbpUkGEMh1iVqvdgb7OqKdwYK2a70Ku12BHHz8mPybRE73a5bqtc2UadH+H2nn/RYvq1zQ1jiuIbGTmm214dpIltSo569PFS5FYggWUqoJfMrnpVCk/KeQRKt8xx5fwGn3C/iaCGIUD53MY1gn4UPdG/GUXMaz9Zm3GKRwbzy0MvUD1jmd9c4iR68duJelmvYIiIotwLj81K/+s1oG7rh40azVbLs+1IC687eO2BSyEVRMEewIGzVN98nHAzYrek25GBhlbv6AN7QnzfszJkrKmSIwxAHj8UxmisdAGyMr/84t4PDOMQLzgteMWoLWK2smVeCvSbj9YG7IOil0q2dNJMzyWFsusm1NCjMHpHzwOdy4ouowvk9sxOzeYjQu6QpqXxEKslg1XrjC+Vehj1b5ukAh/ZmH+L9iSRchkFCBiUJDQUlbBPRYLtWo/VyQqYJu1ricUrMKftAtKgJ31QbuXWSKaEMDLMJ/E+f/uE9lNKmtlT3CYOKavwNOUI=";

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
                Base64.getDecoder().decode(PUBLIC_KEY));
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        Cipher decrpyptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decrpyptCipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] decryptedBytes = decrpyptCipher.doFinal(Base64.getDecoder().decode(licenseKey));
        String license = new String(decryptedBytes);
        String[] split = license.split(":");
        System.out.println("Licensed to " + split[0] + ", valid till " + new Date(Long.valueOf(split[1])) + ", type " + split[2]);
    }
}
