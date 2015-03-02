package com.jecstar.etm.license;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class LicenseReader {
	
	private static final String PUBLIC_KEY = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtBCraSZNjqfqnDK/ESEqwZWZiDY6YRe72N8id//B5LHv7eO41cgRrKzAIn+WH10C3jOjGpJjF1RITKTJg1FM4CK+L66hYP3HQVX8ghtQT99TkHuTkTGxbbBMZd4VF77TR5mTa0LjMTGz7+r9q0PAQEGPol/WqaOTxGHiizh7/qmA0hvAA4Ff39T0CsFyWFpI4hmfS5JG/sLsG8WKd125A1VJFk76ZH7kWP1ysrzGzbR1vSQznQpzz7GPpbzFgjDWJpvQzLREv7qSn1z7MGD4YKlLpgaYxoPUsF2kg4N3YzvZw+RfMTFS2v689VmLccZbySXSoqXyssSq6oMlXIwDSus5qFaB1TeYFJWHZh/t6QHHYeyI0RW6pzIAAG/yGF9uX13uiIb9J9+Qu02XAPstl0ZsVfAVdbzV1AKFMPVOCzMHk6T8YcLsFKedigeH4K2vzdQyHC4L0oZ+2xYiDp904Y7A20HfTyBhVJmz7OIKLjJbnuCh8wP1g9VAR9NC468/nhEdCBxT2nHvvJMLzw2xUBYNIoSw5rWd5+nO9kiCWD7OoNpL5nTRRlX3jBpuqEJmszQo3wF0jZEqAi/pYn3c60iEljtx8m8K8EgjylS/C49qBDUfCQnwfNQxGjxzEzeFc9+mJRox87kxYMUsCyT5u46f8P1wfHOWxzubRgcr0hECAwEAAQ==";

	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		final String licenseKey = "bD6NT9EsZmrg5uyDuVHL1R80sYh9b94ewJ6LdFHPh5RME6HDESUVaHrzYXMsldxgLAplW3Ku+uTNC38duGc9pc9aBgn4WZJk5m2tfB3uXsT8drS2vpdCpriyd/aUv5ZFI7bGPNLeVkCCkieDly4e41M0jzSUfzfiWXRAR+GnPjzoay4FxdFv1VbB/aLFIygAf4ynNNqAih+rrM90DNOT4PXs4yjihmdNX9WJTwQ+LN5MNv+vWB/aJZqkgHMlSReSQGPYoK6953isg1bXsos0NIv119yrf5koD7NrkeAVreswYTvb2+oUWPBngmcYfq2TSbQVqSl1kY4tv4mmLXjCqSGp9798NPa5eM1nuUr8ODhruhKaHRz65sJn3JHJaorVHZpk+xP3xWslhZksYbEPSyRAkTQpgiUSwOl7Ks8ZMJtcjApk3QFWyggZcRznX1rZN54iV2RCcLNmPm09diLEzGTvBQoa/pNcnKSzztgby/Ddu0UwfR8XlgDvF/8tmcZPRrJE/QQTy/Y6Fcyf1EAj8soEXUN2unhKHCzcRMlstpdbB3Ywjb/2jwMlx30MfPBaKZXCpuppPHzzh3i4sHZvyDFuTBCWY2FEGR6K5eCuHsAjZmFltPNMCm48DXFWmGaCsKHHTkpHLf3RCo2O7sc4PKUMEzzWqujhZxgZGCqK0k8=";
		
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
				Base64.getDecoder().decode(PUBLIC_KEY));
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
		
	    Cipher decrpyptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
	    decrpyptCipher.init(Cipher.DECRYPT_MODE, publicKey);
	    
	    byte[] decryptedBytes = decrpyptCipher.doFinal(Base64.getDecoder().decode(licenseKey));
	    String license = new String(decryptedBytes);
	    String[] split = license.split(":");
	    System.out.println("Licensed to " + split[0] + ", valid till " + new Date(Long.valueOf(split[1])));

	    
    }
}
