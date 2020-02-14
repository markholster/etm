/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.tls;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.cluster.certificate.Certificate;
import com.jecstar.etm.server.core.domain.cluster.certificate.Usage;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.EnumSet;
import java.util.Set;


/**
 * Abstract superclass for all <code>X509TrustManager</code> instances can be assigned a specific usage scenario.
 */
public abstract class AbstractConfigurableTrustManager extends X509ExtendedTrustManager {

    private final LogWrapper log = LogFactory.getLogger(getClass());

    private final Usage usage;
    private final TrustManagerFactory trustManagerFactory;

    protected AbstractConfigurableTrustManager(Usage usage) {
        this.usage = usage;
        try {
            this.trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
        } catch (NoSuchAlgorithmException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        X509ExtendedTrustManager trustManager = getTrustManager();
        if (trustManager == null) {
            throw new CertificateException("Certificate '" + chain[0].getSubjectDN().getName() + "' not trusted because no certificates found in truststore.");
        }
        trustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        X509ExtendedTrustManager trustManager = getTrustManager();
        if (trustManager == null) {
            throw new CertificateException("Certificate '" + chain[0].getSubjectDN().getName() + "' not trusted because no certificates found in truststore.");
        }
        trustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        X509ExtendedTrustManager trustManager = getTrustManager();
        if (trustManager == null) {
            throw new CertificateException("Certificate '" + chain[0].getSubjectDN().getName() + "' not trusted because no certificates found in truststore.");
        }
        trustManager.checkClientTrusted(chain, authType, socket);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        X509ExtendedTrustManager trustManager = getTrustManager();
        if (trustManager == null) {
            throw new CertificateException("Certificate '" + chain[0].getSubjectDN().getName() + "' not trusted because no certificates found in truststore.");
        }
        trustManager.checkServerTrusted(chain, authType, socket);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        X509ExtendedTrustManager trustManager = getTrustManager();
        if (trustManager == null) {
            throw new CertificateException("Certificate '" + chain[0].getSubjectDN().getName() + "' not trusted because no certificates found in truststore.");
        }
        trustManager.checkClientTrusted(chain, authType, engine);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        X509ExtendedTrustManager trustManager = getTrustManager();
        if (trustManager == null) {
            throw new CertificateException("Certificate '" + chain[0].getSubjectDN().getName() + "' not trusted because no certificates found in truststore.");
        }
        trustManager.checkServerTrusted(chain, authType, engine);

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        X509ExtendedTrustManager trustManager = getTrustManager();
        if (trustManager == null) {
            return new X509Certificate[0];
        }
        return trustManager.getAcceptedIssuers();
    }

    private KeyStore getKeystore() {
        try {
            var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            var certificates = loadCertificates(this.usage);
            for (Certificate certificate : certificates) {
                keyStore.setCertificateEntry(certificate.getFingerprintSha1(), certificate.toX509Certificate());
            }
            return keyStore;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    private X509ExtendedTrustManager getTrustManager() {
        try {
            var keystore = getKeystore();
            if (keystore.size() == 0) {
                return null;
            }
            var builder = CertPathBuilder.getInstance("PKIX");
            var revocationChecker = (PKIXRevocationChecker) builder.getRevocationChecker();
            revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.SOFT_FAIL));
            var pkixParams = new PKIXBuilderParameters(keystore, new X509CertSelector());
            pkixParams.addCertPathChecker(revocationChecker);
            this.trustManagerFactory.init(new CertPathTrustManagerParameters(pkixParams));
            return (X509ExtendedTrustManager) this.trustManagerFactory.getTrustManagers()[0];
        } catch (KeyStoreException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    /**
     * Load all <code>Certificate</code> instances from the certificate store.
     *
     * @param usage The context in which the certificates should be loaded.
     * @return A <code>Set</code> with all <code>Certificate</code> instances. The <code>Set</code> will be empty if no certificates are present in the store.
     */
    protected abstract Set<Certificate> loadCertificates(Usage usage);

}
