package com.jecstar.etm.launcher.http.session;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import io.undertow.server.session.SessionConfig;

public class ElasticsearchSessionConverterJsonImpl extends JsonConverter implements ElasticsearchSessionConverter<String> {

	private final ElasticsearchSessionTags tags = new ElasticsearchSessionTagsJsonImpl();
	
	@Override
	public ElasticsearchSession read(String content, EtmConfiguration etmConfiguration, ElasticsearchSessionManager sessionManager, SessionConfig sessionConfig) {
		Map<String, Object> valueMap = toMap(content);
		ElasticsearchSession session = new ElasticsearchSession(getString(this.tags.getIdTag(), valueMap), etmConfiguration, sessionManager, sessionConfig);
		List<Map<String, Object>>  attributes = getArray(this.tags.getAttributesTag(), valueMap);
		if (attributes != null) {
			for (Map<String, Object> attribute : attributes) {
				session.setAttribute(getString(this.tags.getAttributeKeyTag(), attribute), getAttributeValue(attribute));
			}
		}
		return session;
	}
	
	private Object getAttributeValue(Map<String, Object> attribute) {
		String type = getString(this.tags.getAttributeValueTypeTag(), attribute);
		if ("string".equals(type)) {
			return getString(this.tags.getAttributeValueTag(), attribute);
		} else if ("boolean".equals(type)) {
			return new Boolean(getString(this.tags.getAttributeValueTag(), attribute));
		}
		return null;
	}

	@Override
	public String write(ElasticsearchSession session) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		addStringElementToJsonBuffer(this.tags.getIdTag(), session.getId(), sb, true);
		addLongElementToJsonBuffer(this.tags.getLastAccessedTag(), session.getLastAccessedTime(), sb, false);
		sb.append(", " + escapeToJson(this.tags.getAttributesTag(), true) + ": [");
		Set<String> attributeNames = session.getAttributeNames();
		boolean first = true;
		for (String name : attributeNames) {
			if (!first) {
				sb.append(",");
			}
			first = false;
			sb.append("{");
			addStringElementToJsonBuffer(this.tags.getAttributeKeyTag(), name, sb, true);
			Object attribute = session.getAttribute(name);
			if (attribute instanceof String) {
				addStringElementToJsonBuffer(this.tags.getAttributeValueTypeTag(), "string", sb, false);
				addStringElementToJsonBuffer(this.tags.getAttributeValueTag(), attribute.toString(), sb, false);
			} else if (attribute instanceof Boolean) {
				addStringElementToJsonBuffer(this.tags.getAttributeValueTypeTag(), "boolean", sb, false);
				addStringElementToJsonBuffer(this.tags.getAttributeValueTag(), attribute.toString(), sb, false);
			}
			sb.append("}");
		}
		sb.append("]}");
		return sb.toString();
	}

	@Override
	public ElasticsearchSessionTags getTags() {
		return this.tags;
	}


}
