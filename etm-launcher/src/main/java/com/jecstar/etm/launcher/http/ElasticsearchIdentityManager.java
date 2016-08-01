package com.jecstar.etm.launcher.http;

import java.security.cert.X509Certificate;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.BCrypt;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.idm.X509CertificateCredential;

public class ElasticsearchIdentityManager implements IdentityManager {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ElasticsearchIdentityManager.class);
	
	private final Client elasticClient;
	private final EtmPrincipalConverter<String> etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
	
	public ElasticsearchIdentityManager(Client elasticClient) {
		this.elasticClient = elasticClient;
	}

	@Override
	public Account verify(Account account) {
		EtmAccount etmAccount = (EtmAccount) account;
		if (System.currentTimeMillis() - etmAccount.getLastUpdated() > 60000 || etmAccount.getPrincipal().forceReload) {
			GetResponse getResponse = this.elasticClient.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, etmAccount.getPrincipal().getId()).get();
			if (!getResponse.isExists()) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Account with id '" + etmAccount.getPrincipal().getId() + "' not found. Account will be invalidated.");
				}
				return null;
			}
			EtmPrincipal principal = this.etmPrincipalConverter.read(getResponse.getSourceAsString());
			etmAccount = new EtmAccount(principal);
		}
		return etmAccount;
	}

	@Override
	public Account verify(String id, Credential credential) {
		GetResponse getResponse = this.elasticClient.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, id).get();
		if (!getResponse.isExists()) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Account with id '" + id + "' not found.");
			}
			return null;
		}
		if (credential instanceof PasswordCredential) {
			EtmPrincipal principal = this.etmPrincipalConverter.read(getResponse.getSourceAsString());
			boolean valid = BCrypt.checkpw(new String(((PasswordCredential) credential).getPassword()), principal.getPasswordHash());
			if (!valid) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Invalid password for account with id '" + id + "'.");
				}
				return null;
			}
			EtmAccount etmAccount = new EtmAccount(principal);
			return etmAccount;
		}
		return null;
	}

	@Override
	public Account verify(Credential credential) {
		if (credential instanceof X509CertificateCredential) {
			X509Certificate certificate = ((X509CertificateCredential) credential).getCertificate();
			GetResponse getResponse = this.elasticClient.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, certificate.getSerialNumber().toString()).get();
			if (!getResponse.isExists()) {
				return null;
			}
			// TODO, de public key zou hier uitgelezen moeten worden, en vergeleken met hetgeen in de DB.
			EtmPrincipal principal = this.etmPrincipalConverter.read(getResponse.getSourceAsString());
			boolean valid = BCrypt.checkpw(certificate.getIssuerX500Principal().getName(), principal.getPasswordHash());
			if (!valid) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Invalid password (issuer name) for certificate with serial number '" + certificate.getSerialNumber().toString() + "'.");
				}
				return null;
			}
			EtmAccount etmAccount = new EtmAccount(principal);
			return etmAccount;
		}
		return null;
	}

}
