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

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;


public class SSLContextBuilder {

    public SSLContext createSslContext(String sslProtocol, File keystore, String keystoreType, char[] keystorePassword, File truststore, String truststoreType, char[] truststorePassword) throws KeyManagementException, NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, UnrecoverableKeyException {
        KeyStore keyStore = loadKeyStore(keystore, keystoreType, keystorePassword);
        KeyStore trustStore = loadKeyStore(truststore, truststoreType, truststorePassword);
        KeyManager[] keyManagers = buildKeyManagers(keyStore, keystorePassword);
        TrustManager[] trustManagers = buildTrustManagers(trustStore, truststorePassword);
        if (keyManagers == null || trustManagers == null) {
            return null;
        }
        SSLContext sslContext = SSLContext.getInstance(sslProtocol);
        sslContext.init(keyManagers, trustManagers, null);
        return sslContext;
    }

    private KeyStore loadKeyStore(final File location, String type, final char[] storePassword) throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
        try (final InputStream stream = location == null ? null : new FileInputStream(location)) {
            if (stream == null) {
                return null;
            }
            KeyStore loadedKeystore = KeyStore.getInstance(type);
            loadedKeystore.load(stream, storePassword);
            return loadedKeystore;
        }
    }

    private KeyManager[] buildKeyManagers(final KeyStore keyStore, final char[] storePassword) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, storePassword);
        return keyManagerFactory.getKeyManagers();
    }

    private TrustManager[] buildTrustManagers(final KeyStore trustStore, final char[] storePassword) throws KeyStoreException, NoSuchAlgorithmException {
        if (trustStore == null) {
            return new TrustManager[]{new TrustAllTrustManager()};
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        return trustManagerFactory.getTrustManagers();
    }
}
