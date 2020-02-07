package com.jecstar.etm.gui.rest.services.iib;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import java.util.Map;

public class NodeConverterJsonImpl implements NodeConverter<String> {

    private final NodeTags tags = new NodeTagsJsonImpl();
    private final JsonConverter converter = new JsonConverter();


    @Override
    public Node read(String content) {
        Map<String, Object> valueMap = this.converter.toMap(content);
        valueMap = this.converter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE, valueMap);
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
        final var builder = new JsonBuilder();
        builder.startObject();
        builder.field(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE);
        builder.startObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IIB_NODE);
        builder.field(this.tags.getNameTag(), node.getName());
        builder.field(this.tags.getHostTag(), node.getHost());
        builder.field(this.tags.getPortTag(), node.getPort());
        builder.field(this.tags.getUsernameTag(), node.getUsername());
        builder.field(this.tags.getPasswordTag(), this.converter.encodeBase64(node.getPassword(), 7));
        builder.field(this.tags.getQueueManagerTag(), node.getQueueManager());
        builder.field(this.tags.getChannelTag(), node.getChannel());
        builder.endObject().endObject();
        return builder.build();
    }

    @Override
    public NodeTags getTags() {
        return this.tags;
    }

}
