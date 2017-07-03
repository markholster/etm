package com.jecstar.etm.gui.rest.services.iib;

import java.util.Map;

import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

public class NodeConverterJsonImpl implements NodeConverter<String> {

	private final NodeTags tags = new NodeTagsJsonImpl();
	private final JsonConverter converter = new JsonConverter();

	
	@Override
	public Node read(String content) {
		Map<String, Object> valueMap = this.converter.toMap(content);
		String name = this.converter.getString(this.tags.getNameTag(), valueMap);
		String host = this.converter.getString(this.tags.getHostTag(), valueMap);
		int port = this.converter.getInteger(this.tags.getPortTag(), valueMap);
		Node node = new Node(name, host, port);
		node.setUsername(this.converter.getString(this.tags.getUsernameTag(), valueMap));
		node.setPassword(this.converter.decodeBase64(this.converter.getString(this.tags.getPasswordTag(), valueMap), 7));
		node.setChannel(this.converter.getString(this.tags.getChannelTag(), valueMap));
		node.setQueueManager(this.converter.getString(this.tags.getQueueManagerTag(), valueMap));
		return node;
	}

	@Override
	public String write(Node node) {
		final StringBuilder sb = new StringBuilder();
		boolean added = false;
		sb.append("{");
		added = this.converter.addStringElementToJsonBuffer(this.tags.getNameTag(), node.getName(), sb, !added)  || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getHostTag(), node.getHost(), sb, !added)  || added;
		added = this.converter.addIntegerElementToJsonBuffer(this.tags.getPortTag(), node.getPort(), sb, !added)  || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getUsernameTag(), node.getUsername(), sb, !added)  || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getPasswordTag(),this.converter.encodeBase64( node.getPassword(), 7), sb, !added)  || added;
		added = this.converter.addIntegerElementToJsonBuffer(this.tags.getPortTag(), node.getPort(), sb, !added)  || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getQueueManagerTag(), node.getQueueManager(), sb, !added)  || added;
		added = this.converter.addStringElementToJsonBuffer(this.tags.getChannelTag(), node.getChannel(), sb, !added)  || added;
		sb.append("}");
		return sb.toString();
	}

	@Override
	public NodeTags getTags() {
		return this.tags;
	}

}
