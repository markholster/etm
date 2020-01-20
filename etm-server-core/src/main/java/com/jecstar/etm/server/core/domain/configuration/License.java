package com.jecstar.etm.server.core.domain.configuration;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public class License {

    private static final String PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEOJ1GA0fdjjCZKxaUwHidhu52fy3F9V+Ow3PxbVDmbk2EYtP2VBJNXZ/YRlW1gAid8v9GNexTRes4FU47L+kk8A==";

    public static final String OWNER = "owner";
    public static final String START_DATE = "start_date";
    public static final String EXPIRY_DATE = "expiry_date";
    public static final String MAX_REQUEST_UNITS_PER_SECOND = "max_request_units_per_second";
    public static final String LICENSE_TYPE = "license_type";
    public static final Long UNLIMITED = -1L;
    /**
     * The number of bytes that make up 1 RU.
     */
    public static final long BYTES_PER_RU = 1024;
    /**
     * The number of request units per second you may consume when no license is installed.
     */
    public static final Long UNLICENSED_REQUEST_UNITS_PER_SECOND = 10L;
    /**
     * The number of bytes storage based on the license. For every request unit 10 MiB is assigned.
     */
    private static final long STORAGE_BYTES_PER_RU = 1024 * 1024 * 10;

    /**
     * A <code>JsonConverter</code> for decoding a license string.
     */
    private JsonConverter jsonConverter = new JsonConverter();

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
    @JsonField(START_DATE)
    private Instant startDate;
    @JsonField(EXPIRY_DATE)
    private Instant expiryDate;
    @JsonField(MAX_REQUEST_UNITS_PER_SECOND)
    private Long maxRequestUnitsPerSecond;
    @JsonField(value = LICENSE_TYPE, converterClass = EnumConverter.class)
    private LicenseType licenseType = LicenseType.ON_PREM;

    public License(String licenseKey) {
        try {
            byte[] decodedLicense = Base64.getDecoder().decode(licenseKey);

            var fullLicenseMap = this.jsonConverter.toMap(new String(decodedLicense));
            var licenseMap = this.jsonConverter.getObject("license", fullLicenseMap);
            var hash = this.jsonConverter.getString("hash", fullLicenseMap);
            var signature = Base64.getUrlDecoder().decode(this.jsonConverter.getString("signature", fullLicenseMap));

            var license = this.jsonConverter.toString(licenseMap);
            var calculatedHash = calculateBase64Hash(license);
            if (!calculatedHash.equals(hash)) {
                throw new EtmException(EtmException.INVALID_LICENSE_KEY);
            }

            Signature ecdsaVerify = Signature.getInstance("NonewithECDSAinP1363Format");
            PublicKey publicKey = createPublicKey();
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(Base64.getDecoder().decode(hash.getBytes()));
            if (!ecdsaVerify.verify(signature)) {
                throw new EtmException(EtmException.INVALID_LICENSE_KEY);
            }
            JsonEntityConverter<License> converter = new JsonEntityConverter<>(f -> this) {
            };
            converter.read(license);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | IllegalArgumentException | UnsupportedEncodingException | SignatureException e) {
            throw new EtmException(EtmException.INVALID_LICENSE_KEY, e);
        }
    }

    public String getOwner() {
        return this.owner;
    }

    public Instant getStartDate() {
        return this.startDate;
    }

    public Instant getExpiryDate() {
        return this.expiryDate;
    }

    public Long getMaxRequestUnitsPerSecond() {
        return this.maxRequestUnitsPerSecond;
    }

    public Long getMaxDatabaseSize() {
        if (getMaxRequestUnitsPerSecond().equals(UNLIMITED)) {
            return UNLIMITED;
        }
        return this.maxRequestUnitsPerSecond * STORAGE_BYTES_PER_RU;
    }

    public LicenseType getLicenseType() {
        return this.licenseType;
    }

    public boolean isExpiredAt(Instant moment) {
        return !moment.isAfter(getExpiryDate());
    }

    public boolean isValidAt(Instant moment) {
        var valid = !moment.isAfter(getExpiryDate());
        if (getStartDate() != null) {
            valid = valid && !moment.isBefore(getStartDate());
        }
        return valid;
    }

    private PublicKey createPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        var keyFactory = KeyFactory.getInstance("EC");
        var keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY));
        return keyFactory.generatePublic(keySpec);
    }

    private String calculateBase64Hash(String data) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        var mac = Mac.getInstance("HmacSHA512");
        var secretkey = new SecretKeySpec("etm license".getBytes(), "HmacSHA512");
        mac.init(secretkey);
        byte[] mac_data = mac.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(mac_data);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof License) {
            var other = (License) obj;
            return Objects.equals(other.owner, this.owner)
                    && Objects.equals(other.startDate, this.startDate)
                    && Objects.equals(other.expiryDate, this.expiryDate)
                    && Objects.equals(other.maxRequestUnitsPerSecond, this.maxRequestUnitsPerSecond);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.owner, this.startDate, this.expiryDate, this.maxRequestUnitsPerSecond);
    }
}


