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
		final String licenseKey = "Fh/7W/Y77Y6xqO6aBWY4ouC3DljJk0rA7/DYMgB6BOtXZ8uRVyovWzdfB9gSiSpIJdPkuNxLKTH9j90fPsgMV8gkG2kX8u56zLN2u7hYBXAhX0+s3TEEobCei+MlB+nzLYtgomu62RdmL18uoFL27JR9rV8vFM3bN1L3YigmtytkKJ4Ton9nugqsBW6Me55qHSsGXqQw0D+veCoOG9xijMyQ+1pDwycFBTMGsFsgdLGzXy/qtBK4caAm7BfXkj5iutD6oD9Ynt2Y72RBqsdC0prmOk6V05OngTY/7onKQhiQ186f8zjl5/d37eT8DnT9AQWjOgPTuJVZhkzlmk9KLdBxOccbUKkuAplRFC4IFcs+mYeF6sBX2R2nkyI6hdtdRJNpZ2p6jxbfh9t+cflKz3I+FdrLqHJYt9XfCC6TVVb3ZzjgnRlSFrLwhtGbK5mHeotYzYkmlv5tsnV8bcBH/Io2IbYcUdFS+Z+nHAfTRFkzO2hcViy4V2Ss1IK5WRDDGVeimNCTEwa6UTai8zcOPHgutz0GqVQNTVtiiT1RWEKvS8gchssdXBJQx7sFEVJ5V5A/IaDXnzIAmcD2K4ttrCNWlE49hho/PfY8SeETvzswkowiKzyUleKsnXuXZQl2uQzvU1OEVH9u8Q7YxIEXCoDSoA75xQWbyGgNa9vKPZQ=";
		
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
				Base64.getDecoder().decode(PUBLIC_KEY));
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
		
	    Cipher decrpyptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
	    decrpyptCipher.init(Cipher.DECRYPT_MODE, publicKey);
	    
	    byte[] decryptedBytes = decrpyptCipher.doFinal(Base64.getDecoder().decode(licenseKey));
	    String license = new String(decryptedBytes);
	    String[] split = license.split("_:_");
	    System.out.println("Licensed to " + split[0] + ", valid till " + new Date(Long.valueOf(split[1])) + ", type " + split[2]);

	    
    }
}
