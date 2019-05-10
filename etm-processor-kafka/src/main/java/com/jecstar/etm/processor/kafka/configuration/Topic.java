package com.jecstar.etm.processor.kafka.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Topic {

    private String name;
    public List<String> bootstrapServers = new ArrayList<>();
    private int nrOfListeners = 1;
    private String groupId = "Enterprise Telemetry Monitor";
    private String startFrom;
    private String defaultImportProfile;

    private int maxPollRecords = 0;
    private long maxPollInterval = 0;
    private long sessionTimeout = 0;
    private long heartbeatInterval = 0;

    private List<String> cipherSuites = new ArrayList<>();
    private List<String> sslProtocols = new ArrayList<>();
    private File sslKeystoreLocation;
    private String sslKeystoreType = "JKS";
    private String sslKeystorePassword;
    private File sslTruststoreLocation;
    private String sslTruststoreType = "JKS";
    private String sslTruststorePassword;

    public Topic() {
        this.sslProtocols.add("TLSv1.2");
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setNrOfListeners(int nrOfListeners) {
        if (nrOfListeners < 1 || nrOfListeners > 65535) {
            throw new IllegalArgumentException(nrOfListeners + " is an invalid number of listeners");
        }
        this.nrOfListeners = nrOfListeners;
    }

    public int getNrOfListeners() {
        return this.nrOfListeners;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public String getStartFrom() {
        return this.startFrom;
    }

    public void setStartFrom(String startFrom) {
        this.startFrom = startFrom;
    }

    public String getDefaultImportProfile() {
        return this.defaultImportProfile;
    }

    public void setDefaultImportProfile(String defaultImportProfile) {
        this.defaultImportProfile = defaultImportProfile;
    }

    public boolean startFromBeginning() {
        return "beginning".equals(getStartFrom());
    }

    public int getMaxPollRecords() {
        return this.maxPollRecords;
    }

    public void setMaxPollRecords(int maxPollRecords) {
        this.maxPollRecords = maxPollRecords;
    }

    public long getMaxPollInterval() {
        return this.maxPollInterval;
    }

    public void setMaxPollInterval(long maxPollInterval) {
        this.maxPollInterval = maxPollInterval;
    }

    public long getSessionTimeout() {
        return this.sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public long getHeartbeatInterval() {
        return this.heartbeatInterval;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public List<String> getCipherSuites() {
        return this.cipherSuites;
    }

    public void setCipherSuites(List<String> cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public List<String> getSslProtocols() {
        return this.sslProtocols;
    }

    public void setSslProtocols(List<String> sslProtocols) {
        this.sslProtocols = sslProtocols;
    }

    public File getSslKeystoreLocation() {
        return this.sslKeystoreLocation;
    }

    public void setSslKeystoreLocation(File sslKeystoreLocation) {
        this.sslKeystoreLocation = sslKeystoreLocation;
    }

    public String getSslKeystoreType() {
        return this.sslKeystoreType;
    }

    public void setSslKeystoreType(String sslKeystoreType) {
        this.sslKeystoreType = sslKeystoreType;
    }

    public String getSslKeystorePassword() {
        return this.sslKeystorePassword;
    }

    public void setSslKeystorePassword(String sslKeystorePassword) {
        this.sslKeystorePassword = sslKeystorePassword;
    }

    public File getSslTruststoreLocation() {
        return this.sslTruststoreLocation;
    }

    public void setSslTruststoreLocation(File sslTruststoreLocation) {
        this.sslTruststoreLocation = sslTruststoreLocation;
    }

    public String getSslTruststoreType() {
        return this.sslTruststoreType;
    }

    public void setSslTruststoreType(String sslTruststoreType) {
        this.sslTruststoreType = sslTruststoreType;
    }

    public String getSslTruststorePassword() {
        return this.sslTruststorePassword;
    }

    public void setSslTruststorePassword(String sslTruststorePassword) {
        this.sslTruststorePassword = sslTruststorePassword;
    }

}
