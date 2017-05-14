package com.jecstar.etm.launcher.http.session;

public class ElasticsearchSessionTagsJsonImpl implements ElasticsearchSessionTags {

	@Override
	public String getIdTag() {
		return "id";
	}
	
	@Override
	public String getLastAccessedTag() {
		return "last_accessed";
	}

	@Override
	public String getAttributesTag() {
		return "attributes";
	}

	@Override
	public String getAttributeKeyTag() {
		return "key";
	}
	
	@Override
	public String getAttributeValueTag() {
		return "value";
	}
	
	@Override
	public String getAttributeValueTypeTag() {
		return "value_type";
	}

}
