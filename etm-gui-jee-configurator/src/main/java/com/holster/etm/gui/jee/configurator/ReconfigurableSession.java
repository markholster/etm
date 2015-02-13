package com.holster.etm.gui.jee.configurator;

import java.util.ArrayList;
import java.util.List;

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
	private List<ReconfigurablePreparedStatement> preparedStatements = new ArrayList<ReconfigurablePreparedStatement>();

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
		ReconfigurablePreparedStatement statement = new ReconfigurablePreparedStatement(this.session.prepare(query));
		int ix = this.preparedStatements.indexOf(statement);
		if (ix == -1) {
			this.preparedStatements.add(statement);
			return statement;
		} 
		return this.preparedStatements.get(ix);	    
    }

	@Override
    public PreparedStatement prepare(RegularStatement statement) {
		ReconfigurablePreparedStatement preparedStatement = new ReconfigurablePreparedStatement(this.session.prepare(statement));
		int ix = this.preparedStatements.indexOf(preparedStatement);
		if (ix == -1) {
			this.preparedStatements.add(preparedStatement);
			return preparedStatement;
		} 
		return this.preparedStatements.get(ix);	    
    }

	@Override
    public ListenableFuture<PreparedStatement> prepareAsync(String query) {
	    throw new UnsupportedOperationException();
    }

	@Override
    public ListenableFuture<PreparedStatement> prepareAsync(RegularStatement statement) {
		throw new UnsupportedOperationException();
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
		this.preparedStatements.forEach(c -> c.rePrepare(newSession));
		oldSession.close();
    }
}
