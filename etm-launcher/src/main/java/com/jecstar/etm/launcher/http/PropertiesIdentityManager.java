package com.jecstar.etm.launcher.http;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.idm.X509CertificateCredential;

public class PropertiesIdentityManager implements IdentityManager {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(PropertiesIdentityManager.class);


	private final Properties users = new Properties();
	private final Properties roles = new Properties();
	private final File usersFile;
	private final File rolesFile;
	
	private long usersFileLastModified;
	private long rolesFileLastModified;
	
	private long lastCheckedForUpdates;

	public PropertiesIdentityManager(File configDir) {
		this.usersFile = new File(configDir, "users.properties");
		this.rolesFile = new File(configDir, "roles.properties");
		loadProperties(this.users, usersFile);
		loadProperties(this.roles, rolesFile);
		this.usersFileLastModified = usersFile.lastModified();
		this.rolesFileLastModified = rolesFile.lastModified();
		this.lastCheckedForUpdates = System.currentTimeMillis();
	}

	private void loadProperties(Properties properties, File file) {
		if (!file.isFile()) {
			properties.clear();
			if (log.isWarningLevelEnabled()) {
				log.logWarningMessage("Unable to read properties from '" + file.getPath() + "' because it isn't a file. Authentication of users may fail.");
			}			
			return;
		}
		if (!file.canRead()) {
			properties.clear();
			if (log.isWarningLevelEnabled()) {
				log.logWarningMessage("Unable to read properties from '" + file.getPath() + "' because it cant't read the file. Authentication of users may fail.");
			}			
			return;
		}
		try (Reader reader = new FileReader(file)) {
			synchronized (properties) {
				properties.clear();
				properties.load(reader);
			}
		} catch (IOException e) {
			if (log.isWarningLevelEnabled()) {
				log.logWarningMessage("Unable to read properties from '" + file.getPath() + "'. Authentication of users may fail.", e);
			}
		}
	}

	@Override
	public Account verify(Account account) {
		checkForUpdates();
		EtmAccount etmAccount = (EtmAccount) account;
		if (etmAccount.getLastUpdated() < this.usersFileLastModified || etmAccount.getLastUpdated() < this.rolesFileLastModified) {
			if (!this.users.containsKey(etmAccount.getPrincipal().getName())) {
				return null;
			}
			setRolesOnAccount(etmAccount);
		}
		return etmAccount;
	}

	@Override
	public Account verify(String id, Credential credential) {
		checkForUpdates();
		if (!this.users.containsKey(id)) {
			return null;
		}
		if (credential instanceof PasswordCredential) {
			boolean valid = BCrypt.checkpw(new String(((PasswordCredential) credential).getPassword()), this.users.getProperty(id));
			if (!valid) {
				return null;
			}
			EtmAccount account = new EtmAccount(new EtmPrincipal(id));
			setRolesOnAccount(account);
			return account;
		} 
		return null;
	}
	
	@Override
	public Account verify(Credential credential) {
		checkForUpdates();
		if (credential instanceof X509CertificateCredential) {
			X509Certificate certificate = ((X509CertificateCredential) credential).getCertificate();
			if (!this.users.containsKey(certificate.getSubjectX500Principal().getName())) {
				return null;
			}
			boolean valid = BCrypt.checkpw(certificate.getSerialNumber().toString(), this.users.getProperty(certificate.getSubjectX500Principal().getName()));
			if (!valid) {
				return null;
			}
			EtmAccount account = new EtmAccount(certificate.getSubjectX500Principal());
			setRolesOnAccount(account);
			return account;
		}
		return null;
	}
	private void setRolesOnAccount(EtmAccount etmAccount) {
		String roles = this.roles.getProperty(etmAccount.getPrincipal().getName());
		if (roles != null) {
			etmAccount.setRoles(Arrays.asList(roles.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1)));
		} else {
			etmAccount.setRoles(Collections.emptyList());
		}
	}

	private void checkForUpdates() {
		if (System.currentTimeMillis() - this.lastCheckedForUpdates > 60000) {
			final long usersFileLastModified = this.usersFile.lastModified();
			final long rolesFileLastModified = this.rolesFile.lastModified();
			if (usersFileLastModified != this.usersFileLastModified) {
				loadProperties(this.users, this.usersFile);
				this.usersFileLastModified = usersFileLastModified;
			}
			if (rolesFileLastModified != this.rolesFileLastModified) {
				loadProperties(this.roles, this.rolesFile);
				this.rolesFileLastModified = rolesFileLastModified;
			}
			this.lastCheckedForUpdates = System.currentTimeMillis();
		}
	}


}
