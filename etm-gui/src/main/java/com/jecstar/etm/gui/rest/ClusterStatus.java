package com.jecstar.etm.gui.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClusterStatus {

	public String clusterName;
	public StatusCode statusCode;
	public int numberOfNodes;
	public int numberOfDataNodes;
	public int numberOfActivePrimaryShards;
	public int numberOfActiveShards;
	public int numberOfRelocatingShards;
	public int numberOfInitializingShards;
	public int numberOfUnassignedShards;
	public int numberOfDelayedUnassignedShards;
	public int numberOfPendingTasks;
	public int numberOfInFlightFethch;
	public List<IndexStatus> indexStatuses = new ArrayList<IndexStatus>();	
	
	public void addShardStatus(String index, ShardStatus shardStatus) {
		Optional<IndexStatus> optional = this.indexStatuses.stream().filter(p -> p.indexName.equals(index)).findFirst();
		if (optional.isPresent()) {
			optional.get().shardStatuses.add(shardStatus);
		} else {
			IndexStatus indexStatus = new IndexStatus();
			indexStatus.indexName = index;
			indexStatus.shardStatuses.add(shardStatus);
			indexStatuses.add(indexStatus);
		}
	}
	
	public class IndexStatus {
		public String indexName;
		public List<ShardStatus> shardStatuses = new ArrayList<ShardStatus>();
	}
	
	public class ShardStatus {
		public int id;
		public boolean active;
		public boolean assigned;
		public boolean primary;
		public boolean initializing;
		public boolean relocating;
		public String node;
	}
}
