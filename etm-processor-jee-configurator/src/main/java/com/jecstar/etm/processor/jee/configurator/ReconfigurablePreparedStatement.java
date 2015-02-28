package com.jecstar.etm.processor.jee.configurator;

import java.nio.ByteBuffer;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedId;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.RetryPolicy;

public class ReconfigurablePreparedStatement implements PreparedStatement {
	
	private PreparedStatement preparedStatement;

	public ReconfigurablePreparedStatement(PreparedStatement preparedStatement) {
	    this.preparedStatement = preparedStatement;
    }

	@Override
    public ColumnDefinitions getVariables() {
	    return this.preparedStatement.getVariables();
    }

	@Override
    public BoundStatement bind(Object... values) {
	    return this.preparedStatement.bind(values);
    }

	@Override
    public BoundStatement bind() {
	    return this.preparedStatement.bind();
    }

	@Override
    public PreparedStatement setRoutingKey(ByteBuffer routingKey) {
	    this.preparedStatement.setRoutingKey(routingKey);
	    return this;
    }

	@Override
    public PreparedStatement setRoutingKey(ByteBuffer... routingKeyComponents) {
	    this.preparedStatement.setRoutingKey(routingKeyComponents);
	    return this;
    }

	@Override
    public ByteBuffer getRoutingKey() {
	    return this.preparedStatement.getRoutingKey();
    }

	@Override
    public PreparedStatement setConsistencyLevel(ConsistencyLevel consistency) {
	    this.preparedStatement.setConsistencyLevel(consistency);
	    return this;
    }

	@Override
    public ConsistencyLevel getConsistencyLevel() {
	    return this.preparedStatement.getConsistencyLevel();
    }

	@Override
    public PreparedStatement setSerialConsistencyLevel(ConsistencyLevel serialConsistency) {
	    this.preparedStatement.setSerialConsistencyLevel(serialConsistency);
	    return this;
    }

	@Override
    public ConsistencyLevel getSerialConsistencyLevel() {
	    return this.preparedStatement.getSerialConsistencyLevel();
    }

	@Override
    public String getQueryString() {
	    return this.preparedStatement.getQueryString();
    }

	@Override
    public String getQueryKeyspace() {
	    return this.preparedStatement.getQueryKeyspace();
    }

	@Override
    public PreparedStatement enableTracing() {
	    this.preparedStatement.enableTracing();
	    return this;
    }

	@Override
    public PreparedStatement disableTracing() {
	    this.preparedStatement.disableTracing();
	    return this;
    }

	@Override
    public boolean isTracing() {
	    return this.preparedStatement.isTracing();
    }

	@Override
    public PreparedStatement setRetryPolicy(RetryPolicy policy) {
		this.preparedStatement.setRetryPolicy(policy);
	    return this;
    }

	@Override
    public RetryPolicy getRetryPolicy() {
	    return this.preparedStatement.getRetryPolicy();
    }

	@Override
    public PreparedId getPreparedId() {
	    return this.preparedStatement.getPreparedId();
    }
	
	public void rePrepare(Session session) {
		this.preparedStatement = session.prepare(this.preparedStatement.getQueryString());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ReconfigurablePreparedStatement) {
			// Not checking on equality because PreparedStatement doesn't
			// overwrite equals. But, when preparing the same statement twice
			// the Cluster will return the same PreparedStatement instance every
			// time.
			return ((ReconfigurablePreparedStatement)obj).preparedStatement == this.preparedStatement;
		}
	    return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
	    return this.preparedStatement.hashCode();
	}
}
