package com.jecstar.etm.license;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Calendar;
import java.util.TimeZone;

class LicenseGenerator {

	private static final String PRIVATE_KEY = "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";
	
	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.set(Calendar.YEAR, 2018);
		calendar.set(Calendar.MONTH, Calendar.DECEMBER);
		calendar.set(Calendar.DAY_OF_MONTH, 31);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
	    final String company = "Jecstar Innovation V.O.F.";
	    final Long maxEventsPerDay = -1L;
	    final Long maxSizePerDay = -1L;
	    final long expiry = calendar.getTimeInMillis();
	    
		final String licenseKey = company + "_:_" + expiry + "_:_" + maxEventsPerDay + "_:_" + maxSizePerDay;
		
		
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
				Base64.getDecoder().decode(PRIVATE_KEY));
		PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
		
	    Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
	    encryptCipher.init(Cipher.ENCRYPT_MODE, privateKey);
	    
	    byte[] encryptedBytes = encryptCipher.doFinal(licenseKey.getBytes());
	    System.out.println("License key: " + Base64.getEncoder().encodeToString(encryptedBytes));
    }
}
