package com.jecstar.etm.server.core.domain.configuration;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;

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
import java.time.Instant;
import java.util.Base64;

public class License {

    private static final String PUBLIC_KEY = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtBCraSZNjqfqnDK/ESEqwZWZiDY6YRe72N8id//B5LHv7eO41cgRrKzAIn+WH10C3jOjGpJjF1RITKTJg1FM4CK+L66hYP3HQVX8ghtQT99TkHuTkTGxbbBMZd4VF77TR5mTa0LjMTGz7+r9q0PAQEGPol/WqaOTxGHiizh7/qmA0hvAA4Ff39T0CsFyWFpI4hmfS5JG/sLsG8WKd125A1VJFk76ZH7kWP1ysrzGzbR1vSQznQpzz7GPpbzFgjDWJpvQzLREv7qSn1z7MGD4YKlLpgaYxoPUsF2kg4N3YzvZw+RfMTFS2v689VmLccZbySXSoqXyssSq6oMlXIwDSus5qFaB1TeYFJWHZh/t6QHHYeyI0RW6pzIAAG/yGF9uX13uiIb9J9+Qu02XAPstl0ZsVfAVdbzV1AKFMPVOCzMHk6T8YcLsFKedigeH4K2vzdQyHC4L0oZ+2xYiDp904Y7A20HfTyBhVJmz7OIKLjJbnuCh8wP1g9VAR9NC468/nhEdCBxT2nHvvJMLzw2xUBYNIoSw5rWd5+nO9kiCWD7OoNpL5nTRRlX3jBpuqEJmszQo3wF0jZEqAi/pYn3c60iEljtx8m8K8EgjylS/C49qBDUfCQnwfNQxGjxzEzeFc9+mJRox87kxYMUsCyT5u46f8P1wfHOWxzubRgcr0hECAwEAAQ==";

    public static final String OWNER = "owner";
    public static final String EXPIRY_DATE = "expiry_date";
    public static final String MAX_EVENTS_PER_DAY = "max_events_per_day";
    public static final String MAX_SIZE_PER_DAY = "max_size_per_day";
    public static final String LICENSE_TYPE = "license_type";

    public enum LicenseType {
        CLOUD, ON_PREM;

        public static LicenseType safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return LicenseType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    @JsonField(OWNER)
    private String owner;
    @JsonField(EXPIRY_DATE)
    private Instant expiryDate;
    @JsonField(MAX_EVENTS_PER_DAY)
    private Long maxEventsPerDay;
    @JsonField(MAX_SIZE_PER_DAY)
    private Long maxSizePerDay;
    @JsonField(value = LICENSE_TYPE, converterClass = EnumConverter.class)
    private LicenseType licenseType = LicenseType.ON_PREM;

    public License(String licenseKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY));
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            Cipher decrpyptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decrpyptCipher.init(Cipher.DECRYPT_MODE, publicKey);

            byte[] decryptedBytes = decrpyptCipher.doFinal(Base64.getDecoder().decode(licenseKey.getBytes()));
            String license = new String(decryptedBytes);
            if (license.startsWith("{") && license.endsWith("}")) {
                JsonEntityConverter<License> converter = new JsonEntityConverter<License>(f -> this) {};
                converter.read(license);
            } else {
                handleLicenseOldFormat(license);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException
                | IllegalBlockSizeException | BadPaddingException | IllegalArgumentException e) {
            throw new EtmException(EtmException.INVALID_LICENSE_KEY, e);
        }
    }

    private void handleLicenseOldFormat(String license) {
        String[] split = license.split("_:_");
        if (split.length != 4) {
            throw new EtmException(EtmException.INVALID_LICENSE_KEY);
        }
        this.owner = split[0];
        this.expiryDate = Instant.ofEpochMilli(Long.valueOf(split[1]));
        this.maxEventsPerDay = Long.valueOf(split[2]);
        this.maxSizePerDay = Long.valueOf(split[3]);
    }

    public String getOwner() {
        return this.owner;
    }

    public Instant getExpiryDate() {
        return this.expiryDate;
    }

    public Long getMaxEventsPerDay() {
        return this.maxEventsPerDay;
    }

    public Long getMaxSizePerDay() {
        return this.maxSizePerDay;
    }

    public LicenseType getLicenseType() {
        return this.licenseType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof License) {
            License other = (License) obj;
            return other.owner.equals(this.owner)
                    && other.expiryDate.equals(this.expiryDate)
                    && other.maxEventsPerDay.equals(this.maxEventsPerDay)
                    && other.maxSizePerDay.equals(this.maxSizePerDay);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (this.owner + this.expiryDate.hashCode() + this.maxEventsPerDay + this.maxSizePerDay).hashCode();
    }
}


