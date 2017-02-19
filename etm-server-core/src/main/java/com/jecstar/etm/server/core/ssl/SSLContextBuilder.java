package com.jecstar.etm.server.core.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;


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
		try (final InputStream stream = location == null ? null : new FileInputStream(location);) {
			if (stream == null) {
				return null;
			}
			KeyStore loadedKeystore = KeyStore.getInstance(type);
			loadedKeystore.load(stream, storePassword == null ? null : storePassword);
			return loadedKeystore;
		}
	}

	private KeyManager[] buildKeyManagers(final KeyStore keyStore, final char[] storePassword) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, storePassword == null ? null : storePassword);
		return keyManagerFactory.getKeyManagers();
	}

	private TrustManager[] buildTrustManagers(final KeyStore trustStore, final char[] storePassword) throws KeyStoreException, NoSuchAlgorithmException {
		if (trustStore == null) {
			return new TrustManager[] { new TrustAllTrustManager() };
		}
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);
		return trustManagerFactory.getTrustManagers();
	}}
