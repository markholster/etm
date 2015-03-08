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
		final String licenseKey = "ktvdc73PSvx7Wx/7QveysrsR3s3INESrAuhbsFq3n9tdQ7LRzIzjCievkEjQKMzPbET8s8r3laDQ8Fw+d4rqTVA+c3zCX3THEobxyUc+tPjgys3Ln7E+tg0VIhOzmGQklfR0E9SKW8WexG3NEUeO3fRe94rMAZdFkwnuWkn76daYzt6NFOqE9R9breX3KmYub2U+DeJuhhZP0olQ1Xb/G0HA0j47ZBuTawev0pGixRgxU6jggjh0TsRpsXmosu1fdELoSfiMGMCh5Qrh3LYXh8Uj7osoNCJulcUDNIDv4jigOHTivyK6AK1do3y8tsMoRHfqFekNX6UHI7bgplAfQ3HfeP9uAoiDTW3qlgGAxoqfoXEM+NwShEDyMti1QZkirE3dcR7xDNWY7HaGHOnembSIaD8XThssZFwoPVqB0yIqQqV8jb/b3lV/4d0m7pXu2i3i1X0PMlc9S5CGHwU3dVtfXvdcukRnAFJGW9ri1FuUlB8AtV/jHR4wKpqPgWVp+ipx6U80R54XeKmcg97DnHLusZkXBka2Rf3WB5NQEcu/EqO8HwSVamuO2nk9D838lOxIGOcyxNjVUC+W2pm5gU36dqvhIiqMTOgzLs8Y8GlufmuvJpVlsx0hrNKEqHl3CgRKSr2nbvolHYaGkovE8/WD0MVmssIg7ZbNWX0PFao=";
		
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
