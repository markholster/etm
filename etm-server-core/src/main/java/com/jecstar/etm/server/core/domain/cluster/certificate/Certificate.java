package com.jecstar.etm.server.core.domain.cluster.certificate;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.JsonNamespace;
import com.jecstar.etm.server.core.domain.cluster.certificate.converter.UsageListConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@JsonNamespace(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_CERTIFICATE)
public class Certificate {


    private final LogWrapper log = LogFactory.getLogger(Certificate.class);

    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----\n";
    public static final String END_CERT = "\n-----END CERTIFICATE-----";

    public static final String SUBJECT_DISTINGUISHED_NAME = "dn";
    public static final String NOT_BEFORE = "not_before";
    public static final String NOT_AFTER = "not_after";
    public static final String ISSUER_DISTINGUISHED_NAME = "issuer_dn";
    public static final String SERIAL = "serial";
    public static final String SELF_SIGNED = "self_signed";
    public static final String FINGERPRINT_MD5 = "fingerprint_md5";
    public static final String FINGERPRINT_SHA1 = "fingerprint_sha1";
    public static final String PEM_DATA = "data";
    public static final String TRUST_ANCHOR = "trust_anchor";
    public static final String USAGE = "usage";

    @JsonField(SUBJECT_DISTINGUISHED_NAME)
    private String distinguishedName;
    @JsonField(NOT_BEFORE)
    private Instant notBefore;
    @JsonField(NOT_AFTER)
    private Instant notAfter;
    @JsonField(ISSUER_DISTINGUISHED_NAME)
    private String issuerDistinguishedName;
    @JsonField(SERIAL)
    private String serial;
    @JsonField(SELF_SIGNED)
    private boolean selfSigned;
    @JsonField(FINGERPRINT_MD5)
    private String fingerprintMd5;
    @JsonField(FINGERPRINT_SHA1)
    private String fingerprintSha1;
    @JsonField(PEM_DATA)
    private String data;
    @JsonField(TRUST_ANCHOR)
    private boolean trustAnchor;
    @JsonField(value = USAGE, converterClass = UsageListConverter.class)
    private List<Usage> usage = new ArrayList<>();

    private CertificateFactory certificateFactory;

    public Certificate() {
    }

    public Certificate(X509Certificate x509Certificate) {
        this();
        this.distinguishedName = x509Certificate.getSubjectX500Principal() != null ? x509Certificate.getSubjectX500Principal().getName() : null;
        this.notBefore = x509Certificate.getNotBefore() != null ? x509Certificate.getNotBefore().toInstant() : null;
        this.notAfter = x509Certificate.getNotAfter() != null ? x509Certificate.getNotAfter().toInstant() : null;
        this.issuerDistinguishedName = x509Certificate.getIssuerX500Principal() != null ? x509Certificate.getIssuerX500Principal().getName() : null;
        this.serial = x509Certificate.getSerialNumber().toString(16);
        this.selfSigned = isSelfSigned(x509Certificate);
        this.trustAnchor = this.selfSigned;

        final var encoder = Base64.getMimeEncoder(64, "\n".getBytes());
        try {
            this.fingerprintMd5 = getThumbprint(x509Certificate, "MD5");
            this.fingerprintSha1 = getThumbprint(x509Certificate, "SHA-1");
            final var encodedCertText = new String(encoder.encode(x509Certificate.getEncoded()));
            this.data = BEGIN_CERT + encodedCertText + END_CERT;
        } catch (CertificateEncodingException | NoSuchAlgorithmException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to construct certificate.", e);
            }
        }
    }

    public String getDistinguishedName() {
        return this.distinguishedName;
    }

    public Instant getNotBefore() {
        return this.notBefore;
    }

    public Instant getNotAfter() {
        return this.notAfter;
    }

    public String getIssuerDistinguishedName() {
        return this.issuerDistinguishedName;
    }

    public String getSerial() {
        return this.serial;
    }

    public boolean isSelfSigned() {
        return this.selfSigned;
    }

    public String getFingerprintMd5() {
        return this.fingerprintMd5;
    }

    public String getFingerprintSha1() {
        return this.fingerprintSha1;
    }

    public String getData() {
        return this.data;
    }

    public boolean isTrustAnchor() {
        return this.trustAnchor;
    }

    public void setTrustAnchor(boolean trustAnchor) {
        this.trustAnchor = trustAnchor;
    }


    public List<Usage> getUsage() {
        return this.usage;
    }

    private boolean isSelfSigned(X509Certificate cert) {
        try {
            PublicKey key = cert.getPublicKey();
            cert.verify(key);
            return true;
        } catch (InvalidKeyException | SignatureException e) {
            return false;
        } catch (CertificateException | NoSuchAlgorithmException | NoSuchProviderException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage(e.getMessage(), e);
            }
            return false;
        }
    }

    public X509Certificate toX509Certificate() {
        if (this.certificateFactory == null) {
            try {
                this.certificateFactory = CertificateFactory.getInstance("X.509");
            } catch (CertificateException e) {
                throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
            }
        }
        try {
            return (X509Certificate) this.certificateFactory.generateCertificate(new ByteArrayInputStream(getData().getBytes()));
        } catch (CertificateException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    private String getThumbprint(X509Certificate cert, String algorithm)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        return StringUtils.byteArrayToHex(digest).toLowerCase();
    }


}
