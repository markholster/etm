package com.jecstar.etm.server.core.tls;

import com.jecstar.etm.server.core.domain.cluster.certificate.Certificate;
import com.jecstar.etm.server.core.domain.cluster.certificate.Usage;
import org.junit.jupiter.api.Test;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ibm.icu.impl.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertSame;

public class AbstractConfigurableTrustManagerTest {

    private static final String USERTRUST_ECC_ROOT = "-----BEGIN CERTIFICATE-----\n" +
            "MIICjzCCAhWgAwIBAgIQXIuZxVqUxdJxVt7NiYDMJjAKBggqhkjOPQQDAzCBiDEL\n" +
            "MAkGA1UEBhMCVVMxEzARBgNVBAgTCk5ldyBKZXJzZXkxFDASBgNVBAcTC0plcnNl\n" +
            "eSBDaXR5MR4wHAYDVQQKExVUaGUgVVNFUlRSVVNUIE5ldHdvcmsxLjAsBgNVBAMT\n" +
            "JVVTRVJUcnVzdCBFQ0MgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMTAwMjAx\n" +
            "MDAwMDAwWhcNMzgwMTE4MjM1OTU5WjCBiDELMAkGA1UEBhMCVVMxEzARBgNVBAgT\n" +
            "Ck5ldyBKZXJzZXkxFDASBgNVBAcTC0plcnNleSBDaXR5MR4wHAYDVQQKExVUaGUg\n" +
            "VVNFUlRSVVNUIE5ldHdvcmsxLjAsBgNVBAMTJVVTRVJUcnVzdCBFQ0MgQ2VydGlm\n" +
            "aWNhdGlvbiBBdXRob3JpdHkwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAAQarFRaqflo\n" +
            "I+d61SRvU8Za2EurxtW20eZzca7dnNYMYf3boIkDuAUU7FfO7l0/4iGzzvfUinng\n" +
            "o4N+LZfQYcTxmdwlkWOrfzCjtHDix6EznPO/LlxTsV+zfTJ/ijTjeXmjQjBAMB0G\n" +
            "A1UdDgQWBBQ64QmG1M8ZwpZ2dEl23OA1xmNjmjAOBgNVHQ8BAf8EBAMCAQYwDwYD\n" +
            "VR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAwNoADBlAjA2Z6EWCNzklwBBHU6+4WMB\n" +
            "zzuqQhFkoJ2UOQIReVx7Hfpkue4WQrO/isIJxOzksU0CMQDpKmFHjFJKS04YcPbW\n" +
            "RNZu9YO6bVi9JNlWSOrvxKJGgYhqOkbRqZtNyWHa0V1Xahg=\n" +
            "-----END CERTIFICATE-----\n";

    private static final String DIGICERT_GLOBAL_ROOT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDrzCCApegAwIBAgIQCDvgVpBCRrGhdWrJWZHHSjANBgkqhkiG9w0BAQUFADBh\n" +
            "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
            "d3cuZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBD\n" +
            "QTAeFw0wNjExMTAwMDAwMDBaFw0zMTExMTAwMDAwMDBaMGExCzAJBgNVBAYTAlVT\n" +
            "MRUwEwYDVQQKEwxEaWdpQ2VydCBJbmMxGTAXBgNVBAsTEHd3dy5kaWdpY2VydC5j\n" +
            "b20xIDAeBgNVBAMTF0RpZ2lDZXJ0IEdsb2JhbCBSb290IENBMIIBIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4jvhEXLeqKTTo1eqUKKPC3eQyaKl7hLOllsB\n" +
            "CSDMAZOnTjC3U/dDxGkAV53ijSLdhwZAAIEJzs4bg7/fzTtxRuLWZscFs3YnFo97\n" +
            "nh6Vfe63SKMI2tavegw5BmV/Sl0fvBf4q77uKNd0f3p4mVmFaG5cIzJLv07A6Fpt\n" +
            "43C/dxC//AH2hdmoRBBYMql1GNXRor5H4idq9Joz+EkIYIvUX7Q6hL+hqkpMfT7P\n" +
            "T19sdl6gSzeRntwi5m3OFBqOasv+zbMUZBfHWymeMr/y7vrTC0LUq7dBMtoM1O/4\n" +
            "gdW7jVg/tRvoSSiicNoxBN33shbyTApOB6jtSj1etX+jkMOvJwIDAQABo2MwYTAO\n" +
            "BgNVHQ8BAf8EBAMCAYYwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUA95QNVbR\n" +
            "TLtm8KPiGxvDl7I90VUwHwYDVR0jBBgwFoAUA95QNVbRTLtm8KPiGxvDl7I90VUw\n" +
            "DQYJKoZIhvcNAQEFBQADggEBAMucN6pIExIK+t1EnE9SsPTfrgT1eXkIoyQY/Esr\n" +
            "hMAtudXH/vTBH1jLuG2cenTnmCmrEbXjcKChzUyImZOMkXDiqw8cvpOp/2PV5Adg\n" +
            "06O/nVsJ8dWO41P0jmP6P6fbtGbfYmbW0W5BjfIttep3Sp+dWOIrWcBAI+0tKIJF\n" +
            "PnlUkiaY4IBIqDfv8NZ5YBberOgOzW6sRBc4L0na4UU+Krk2U886UAb3LujEV0ls\n" +
            "YSEY1QSteDwsOoBrp+uvFRTp2InBuThs4pFsiv9kuXclVzDAGySj4dzp30d8tbQk\n" +
            "CAUw7C29C79Fv1C5qfPrmAESrciIxpg0X40KPMbp1ZWVbd4=\n" +
            "-----END CERTIFICATE-----\n";

    private static final String ADDTRUST_EXTERNAL_ROOT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIENjCCAx6gAwIBAgIBATANBgkqhkiG9w0BAQUFADBvMQswCQYDVQQGEwJTRTEU\n" +
            "MBIGA1UEChMLQWRkVHJ1c3QgQUIxJjAkBgNVBAsTHUFkZFRydXN0IEV4dGVybmFs\n" +
            "IFRUUCBOZXR3b3JrMSIwIAYDVQQDExlBZGRUcnVzdCBFeHRlcm5hbCBDQSBSb290\n" +
            "MB4XDTAwMDUzMDEwNDgzOFoXDTIwMDUzMDEwNDgzOFowbzELMAkGA1UEBhMCU0Ux\n" +
            "FDASBgNVBAoTC0FkZFRydXN0IEFCMSYwJAYDVQQLEx1BZGRUcnVzdCBFeHRlcm5h\n" +
            "bCBUVFAgTmV0d29yazEiMCAGA1UEAxMZQWRkVHJ1c3QgRXh0ZXJuYWwgQ0EgUm9v\n" +
            "dDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALf3GjPm8gAELTngTlvt\n" +
            "H7xsD821+iO2zt6bETOXpClMfZOfvUq8k+0DGuOPz+VtUFrWlymUWoCwSXrbLpX9\n" +
            "uMq/NzgtHj6RQa1wVsfwTz/oMp50ysiQVOnGXw94nZpAPA6sYapeFI+eh6FqUNzX\n" +
            "mk6vBbOmcZSccbNQYArHE504B4YCqOmoaSYYkKtMsE8jqzpPhNjfzp/haW+710LX\n" +
            "a0Tkx63ubUFfclpxCDezeWWkWaCUN/cALw3CknLa0Dhy2xSoRcRdKn23tNbE7qzN\n" +
            "E0S3ySvdQwAl+mG5aWpYIxG3pzOPVnVZ9c0p10a3CitlttNCbxWyuHv77+ldU9U0\n" +
            "WicCAwEAAaOB3DCB2TAdBgNVHQ4EFgQUrb2YejS0Jvf6xCZU7wO94CTLVBowCwYD\n" +
            "VR0PBAQDAgEGMA8GA1UdEwEB/wQFMAMBAf8wgZkGA1UdIwSBkTCBjoAUrb2YejS0\n" +
            "Jvf6xCZU7wO94CTLVBqhc6RxMG8xCzAJBgNVBAYTAlNFMRQwEgYDVQQKEwtBZGRU\n" +
            "cnVzdCBBQjEmMCQGA1UECxMdQWRkVHJ1c3QgRXh0ZXJuYWwgVFRQIE5ldHdvcmsx\n" +
            "IjAgBgNVBAMTGUFkZFRydXN0IEV4dGVybmFsIENBIFJvb3SCAQEwDQYJKoZIhvcN\n" +
            "AQEFBQADggEBALCb4IUlwtYj4g+WBpKdQZic2YR5gdkeWxQHIzZlj7DYd7usQWxH\n" +
            "YINRsPkyPef89iYTx4AWpb9a/IfPeHmJIZriTAcKhjW88t5RxNKWt9x+Tu5w/Rw5\n" +
            "6wwCURQtjr0W4MHfRnXnJK3s9EK0hZNwEGe6nQY1ShjTK3rMUUKhemPR5ruhxSvC\n" +
            "Nr4TDea9Y355e6cJDUCrat2PisP29owaQgVR1EX1n6diIWgVIEM8med8vSTYqZEX\n" +
            "c4g/VhsxOBi0cQ+azcgOno4uG+GMmIPLHzHxREzGBHNJdmAPx/i9F4BrLunMTA5a\n" +
            "mnkPIAou1Z5jJh5VkpTYghdae9C8x49OhgQ=\n" +
            "-----END CERTIFICATE-----";

    private static final String SECTIGO_INTERMEDIATE = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDyTCCA0+gAwIBAgIRAID1YG06FisUOtwS++jCBm8wCgYIKoZIzj0EAwMwgYgx\n" +
            "CzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpOZXcgSmVyc2V5MRQwEgYDVQQHEwtKZXJz\n" +
            "ZXkgQ2l0eTEeMBwGA1UEChMVVGhlIFVTRVJUUlVTVCBOZXR3b3JrMS4wLAYDVQQD\n" +
            "EyVVU0VSVHJ1c3QgRUNDIENlcnRpZmljYXRpb24gQXV0aG9yaXR5MB4XDTE4MTEw\n" +
            "MjAwMDAwMFoXDTMwMTIzMTIzNTk1OVowgZExCzAJBgNVBAYTAkdCMRswGQYDVQQI\n" +
            "ExJHcmVhdGVyIE1hbmNoZXN0ZXIxEDAOBgNVBAcTB1NhbGZvcmQxGDAWBgNVBAoT\n" +
            "D1NlY3RpZ28gTGltaXRlZDE5MDcGA1UEAxMwU2VjdGlnbyBFQ0MgRXh0ZW5kZWQg\n" +
            "VmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENBMFkwEwYHKoZIzj0CAQYIKoZIzj0D\n" +
            "AQcDQgAEAyJ5Ca9JyXq8bO+krLVWysbtm7fdMSJ54uFD23t0x6JAC4IjxevfQJzW\n" +
            "z4T6yY+FybTBqtOa++ijJFnkB5wKy6OCAY0wggGJMB8GA1UdIwQYMBaAFDrhCYbU\n" +
            "zxnClnZ0SXbc4DXGY2OaMB0GA1UdDgQWBBTvwSqVDDLa+3Mw3IoT2BVL9xPo+DAO\n" +
            "BgNVHQ8BAf8EBAMCAYYwEgYDVR0TAQH/BAgwBgEB/wIBADAdBgNVHSUEFjAUBggr\n" +
            "BgEFBQcDAQYIKwYBBQUHAwIwOgYDVR0gBDMwMTAvBgRVHSAAMCcwJQYIKwYBBQUH\n" +
            "AgEWGWh0dHBzOi8vY3BzLnVzZXJ0cnVzdC5jb20wUAYDVR0fBEkwRzBFoEOgQYY/\n" +
            "aHR0cDovL2NybC51c2VydHJ1c3QuY29tL1VTRVJUcnVzdEVDQ0NlcnRpZmljYXRp\n" +
            "b25BdXRob3JpdHkuY3JsMHYGCCsGAQUFBwEBBGowaDA/BggrBgEFBQcwAoYzaHR0\n" +
            "cDovL2NydC51c2VydHJ1c3QuY29tL1VTRVJUcnVzdEVDQ0FkZFRydXN0Q0EuY3J0\n" +
            "MCUGCCsGAQUFBzABhhlodHRwOi8vb2NzcC51c2VydHJ1c3QuY29tMAoGCCqGSM49\n" +
            "BAMDA2gAMGUCMQCjHztBDL90GCRXHlGqm0H7kzP04hd0MxwakKjWzOmstXNFLONj\n" +
            "RFa0JqI/iKUJMFcCMCbLgyzcFW7DihtY5XE0XCLCw+git0NjxiFB6FaOFIlyDdqT\n" +
            "j+Th+DJ92JLvICVD/g==\n" +
            "-----END CERTIFICATE-----";

    private static final String BADSSL_SELF_SIGNED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDeTCCAmGgAwIBAgIJAPziuikCTox4MA0GCSqGSIb3DQEBCwUAMGIxCzAJBgNV\n" +
            "BAYTAlVTMRMwEQYDVQQIDApDYWxpZm9ybmlhMRYwFAYDVQQHDA1TYW4gRnJhbmNp\n" +
            "c2NvMQ8wDQYDVQQKDAZCYWRTU0wxFTATBgNVBAMMDCouYmFkc3NsLmNvbTAeFw0x\n" +
            "OTEwMDkyMzQxNTJaFw0yMTEwMDgyMzQxNTJaMGIxCzAJBgNVBAYTAlVTMRMwEQYD\n" +
            "VQQIDApDYWxpZm9ybmlhMRYwFAYDVQQHDA1TYW4gRnJhbmNpc2NvMQ8wDQYDVQQK\n" +
            "DAZCYWRTU0wxFTATBgNVBAMMDCouYmFkc3NsLmNvbTCCASIwDQYJKoZIhvcNAQEB\n" +
            "BQADggEPADCCAQoCggEBAMIE7PiM7gTCs9hQ1XBYzJMY61yoaEmwIrX5lZ6xKyx2\n" +
            "PmzAS2BMTOqytMAPgLaw+XLJhgL5XEFdEyt/ccRLvOmULlA3pmccYYz2QULFRtMW\n" +
            "hyefdOsKnRFSJiFzbIRMeVXk0WvoBj1IFVKtsyjbqv9u/2CVSndrOfEk0TG23U3A\n" +
            "xPxTuW1CrbV8/q71FdIzSOciccfCFHpsKOo3St/qbLVytH5aohbcabFXRNsKEqve\n" +
            "ww9HdFxBIuGa+RuT5q0iBikusbpJHAwnnqP7i/dAcgCskgjZjFeEU4EFy+b+a1SY\n" +
            "QCeFxxC7c3DvaRhBB0VVfPlkPz0sw6l865MaTIbRyoUCAwEAAaMyMDAwCQYDVR0T\n" +
            "BAIwADAjBgNVHREEHDAaggwqLmJhZHNzbC5jb22CCmJhZHNzbC5jb20wDQYJKoZI\n" +
            "hvcNAQELBQADggEBAGlwCdbPxflZfYOaukZGCaxYK6gpincX4Lla4Ui2WdeQxE95\n" +
            "w7fChXvP3YkE3UYUE7mupZ0eg4ZILr/A0e7JQDsgIu/SRTUE0domCKgPZ8v99k3A\n" +
            "vka4LpLK51jHJJK7EFgo3ca2nldd97GM0MU41xHFk8qaK1tWJkfrrfcGwDJ4GQPI\n" +
            "iLlm6i0yHq1Qg1RypAXJy5dTlRXlCLd8ufWhhiwW0W75Va5AEnJuqpQrKwl3KQVe\n" +
            "wGj67WWRgLfSr+4QG1mNvCZb2CkjZWmxkGPuoP40/y7Yu5OFqxP5tAjj4YixCYTW\n" +
            "EVA0pmzIzgBg+JIe3PdRy27T0asgQW/F4TY61Yk=\n" +
            "-----END CERTIFICATE-----";

    @Test
    public void testInvalidUsage() throws IOException, CertificateException {
        try (InputStream inStream = new ByteArrayInputStream(USERTRUST_ECC_ROOT.getBytes())) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);

            Certificate certificate = new Certificate(cert);
            certificate.getUsage().add(Usage.LDAP);
            try {
                callEndpoint("www.jecstar.com", createTrustManager(Usage.SMTP, certificate));
                fail("Expected a handshake exception because the certificate usage does not match the usage of the trustmanager");
            } catch (SSLHandshakeException e) {
                assertRootCause(e, CertificateException.class);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                fail(e);
            }
        }
    }

    @Test
    public void testValidChain() throws IOException, CertificateException {
        try (InputStream inStream = new ByteArrayInputStream(USERTRUST_ECC_ROOT.getBytes())) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);

            Certificate certificate = new Certificate(cert);
            certificate.getUsage().add(Usage.SMTP);
            try {
                callEndpoint("www.jecstar.com", createTrustManager(Usage.SMTP, certificate));
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                fail(e);
            }
        }
    }

    @Test
    public void testRevokedCert() throws IOException, CertificateException {
        try (InputStream inStream = new ByteArrayInputStream(DIGICERT_GLOBAL_ROOT.getBytes())) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);

            Certificate certificate = new Certificate(cert);
            certificate.getUsage().add(Usage.SMTP);
            try {
                callEndpoint("revoked.badssl.com", createTrustManager(Usage.SMTP, certificate));
                fail("Expected a handshake exception because the certificate is revoked");
            } catch (SSLHandshakeException e) {
                assertRootCause(e, CertificateRevokedException.class);
                // Expected exception caught
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                fail(e);
            }
        }
    }

    @Test
    public void testExpiredCert() throws IOException, CertificateException {
        try (InputStream inStream = new ByteArrayInputStream(ADDTRUST_EXTERNAL_ROOT.getBytes())) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);

            Certificate certificate = new Certificate(cert);
            certificate.getUsage().add(Usage.SMTP);
            try {
                callEndpoint("expired.badssl.com", createTrustManager(Usage.SMTP, certificate));
                fail("Expected a handshake exception because the certificate is revoked");
            } catch (SSLHandshakeException e) {
                assertRootCause(e, CertificateExpiredException.class);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                fail(e);
            }
        }
    }

//    @Test
//    public void testSha1Intermediate() throws IOException, CertificateException {
//        try (InputStream inStream = new ByteArrayInputStream(ADDTRUST_EXTERNAL_ROOT.getBytes())) {
//            CertificateFactory cf = CertificateFactory.getInstance("X.509");
//            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
//
//            Certificate certificate = new Certificate(cert);
//            certificate.getUsage().add(Usage.SMTP);
//            try {
//                callEndpoint("sha1-intermediate.badssl.com", createTrustManager(Usage.SMTP, certificate));
//                fail("Expected a handshake exception because the intermediate uses SHA1 signing");
//            } catch (SSLHandshakeException e) {
//                assertRootCause(e, CertificateException.class);
//            } catch (NoSuchAlgorithmException | KeyManagementException e) {
//                fail(e);
//            }
//        }
//    }

    @Test
    public void testIntermediateAsTrustAnchor() throws IOException, CertificateException {
        try (InputStream inStream = new ByteArrayInputStream(SECTIGO_INTERMEDIATE.getBytes())) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);

            Certificate certificate = new Certificate(cert);
            certificate.getUsage().add(Usage.SMTP);
            certificate.setTrustAnchor(true);
            try {
                callEndpoint("www.jecstar.com", createTrustManager(Usage.SMTP, certificate));
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                fail(e);
            }
        }
    }

    @Test
    public void testSelfSigned() throws IOException, CertificateException {
        try (InputStream inStream = new ByteArrayInputStream(BADSSL_SELF_SIGNED.getBytes())) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);

            Certificate certificate = new Certificate(cert);
            certificate.getUsage().add(Usage.SMTP);
            try {
                callEndpoint("self-signed.badssl.com", createTrustManager(Usage.SMTP, certificate));
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                fail(e);
            }
        }
    }

    private X509TrustManager createTrustManager(final Usage usage, final Certificate... trustedCerts) {
        return new AbstractConfigurableTrustManager(usage) {
            @Override
            protected Set<Certificate> loadCertificates(Usage usage) {
                return Arrays.stream(trustedCerts).filter(c -> c.getUsage().contains(usage)).collect(Collectors.toSet());
            }
        };
    }

    private void callEndpoint(String host, TrustManager tm) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        SSLSocket socket = null;
        try {
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{tm}, null);
            var socketFactory = sslContext.getSocketFactory();
            socket = (SSLSocket) socketFactory.createSocket(host, 443);
            socket.setSoTimeout(5000);
            socket.startHandshake();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void assertRootCause(SSLHandshakeException e, Class<? extends Exception> expectedRootCause) {
        var causes = new HashSet<Throwable>();
        var cause = e.getCause();
        while (!causes.contains(cause)) {
            causes.add(cause);

            var parentCause = cause.getCause();
            if (parentCause == null || parentCause == cause) {
                break;
            }
            cause = parentCause;
        }
        assertSame(expectedRootCause.getName(), cause.getClass().getName());
    }

}