package com.holster.etm.processor.jee.configurator;

import com.datastax.driver.core.CloseFuture;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.util.concurrent.ListenableFuture;

public class ReconfigurableSession implements Session {

	private Session session;

	public ReconfigurableSession(Session session) {
		this.session = session;
	}

	@Override
    public String getLoggedKeyspace() {
	    return this.session.getLoggedKeyspace();
    }

	@Override
    public Session init() {
	    return this.session.init();
    }

	@Override
    public ResultSet execute(String query) {
	    return this.session.execute(query);
    }

	@Override
    public ResultSet execute(String query, Object... values) {
	    return this.session.execute(query, values);
    }

	@Override
    public ResultSet execute(Statement statement) {
	    return this.session.execute(statement);
    }

	@Override
    public ResultSetFuture executeAsync(String query) {
	    return this.session.executeAsync(query);
    }

	@Override
    public ResultSetFuture executeAsync(String query, Object... values) {
	    return this.session.executeAsync(query, values);
    }

	@Override
    public ResultSetFuture executeAsync(Statement statement) {
	    return this.session.executeAsync(statement);
    }

	@Override
    public PreparedStatement prepare(String query) {
	    return this.session.prepare(query);
    }

	@Override
    public PreparedStatement prepare(RegularStatement statement) {
	    return this.session.prepare(statement);
    }

	@Override
    public ListenableFuture<PreparedStatement> prepareAsync(String query) {
	    return this.session.prepareAsync(query);
    }

	@Override
    public ListenableFuture<PreparedStatement> prepareAsync(RegularStatement statement) {
	    return this.session.prepareAsync(statement);
    }

	@Override
    public CloseFuture closeAsync() {
	    return this.session.closeAsync();
    }

	@Override
    public void close() {
	    this.session.close();
    }

	@Override
    public boolean isClosed() {
	    return this.session.isClosed();
    }

	@Override
    public Cluster getCluster() {
	    return this.session.getCluster();
    }

	@Override
    public State getState() {
	    return this.session.getState();
    }

	public void switchToSession(Session newSession) {
		Session oldSession = this.session;
		this.session = newSession;
		oldSession.close();
    }
}
