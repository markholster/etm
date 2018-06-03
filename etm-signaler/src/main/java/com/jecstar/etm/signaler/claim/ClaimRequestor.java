package com.jecstar.etm.signaler.claim;

import org.elasticsearch.client.Client;

/**
 * The <code>ClaimRequestor</code> claims to be the master node for alerting users when certain thresholds are met.
 * When the claim is valid this class will keep it up to date, and makes sure all scheduled triggers are executed.
 */
public class ClaimRequestor {

    /**
     * The elasticsearch <code>Client</code>.
     */
    private final Client client;
    private final String nodeName;

    public ClaimRequestor(Client client, String nodeName) {
        this.client = client;
        this.nodeName = nodeName;
    }

    /**
     * Method that request a <code>ClusterClaim</code>.
     *
     * @return <code>true</code> when the claim is accepted by the cluster, <code>false</code> otherwise.
     */
    public boolean requestClusterClaim() {
        // load current claim
        // if none found, try to insert a claim, if success, we're the master. if failure try again later
        // if found and not expired, try again later.
        // if found and expired insert claim with version + 1. if success, we're the master. if failure try again later
        return false;
    }

    public void extendClaim() {

    }
}
