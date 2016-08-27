package com.jecstar.etm.gui.rest.services.iib;

public class NodeTagsJsonImpl implements NodeTags {

	@Override
	public String getNameTag() {
		return "name";
	}
	
	@Override
	public String getHostTag() {
		return "host";
	}

	@Override
	public String getPortTag() {
		return "port";
	}

	@Override
	public String getQueueManagerTag() {
		return "queue_manager";
	}

	@Override
	public String getChannelTag() {
		return "channel";
	}

}
