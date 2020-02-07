package com.jecstar.etm.launcher.http.session;

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import java.io.*;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ElasticsearchSessionConverterJsonImpl extends JsonConverter implements ElasticsearchSessionConverter<String> {

    private final ElasticsearchSessionTags tags = new ElasticsearchSessionTagsJsonImpl();

    @Override
    public void read(String content, ElasticsearchSession session) {
        Map<String, Object> valueMap = toMap(content);
        valueMap = getObject(ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION, valueMap);
        session.setLastAccessedTime(getLong(this.tags.getLastAccessedTag(), valueMap));
        List<Map<String, Object>> attributes = getArray(this.tags.getAttributesTag(), valueMap);
        if (attributes != null) {
            for (Map<String, Object> attribute : attributes) {
                session.setAttribute(getString(this.tags.getAttributeKeyTag(), attribute), getAttributeValue(attribute));
            }
        }
    }

    private Object getAttributeValue(Map<String, Object> attribute) {
        String type = getString(this.tags.getAttributeValueTypeTag(), attribute);
        if (String.class.getName().equals(type)) {
            return getString(this.tags.getAttributeValueTag(), attribute);
        } else if (Boolean.class.getName().equals(type)) {
            return Boolean.valueOf(getString(this.tags.getAttributeValueTag(), attribute));
        }
        return deserializeAttribute(getString(this.tags.getAttributeValueTag(), attribute));
    }

    @Override
    public String write(ElasticsearchSession session) {
        var builder = new JsonBuilder();
        builder.startObject();
        builder.field(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION);
        builder.startObject(ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION);
        builder.field(this.tags.getLastAccessedTag(), session.getLastAccessedTime());
        builder.field(this.getTags().getIdTag(), session.getId());
        builder.startArray(this.tags.getAttributesTag());
        Set<String> attributeNames = session.getAttributeNames();
        for (String name : attributeNames) {
            builder.startObject();
            builder.field(this.tags.getAttributeKeyTag(), name);
            Object attribute = session.getAttribute(name);
            if (attribute instanceof String) {
                builder.field(this.tags.getAttributeValueTypeTag(), String.class.getName());
                builder.field(this.tags.getAttributeValueTag(), attribute.toString());
            } else if (attribute instanceof Boolean) {
                builder.field(this.tags.getAttributeValueTypeTag(), Boolean.class.getName());
                builder.field(this.tags.getAttributeValueTag(), attribute.toString());
            } else if (attribute instanceof Serializable) {
                builder.field(this.tags.getAttributeValueTypeTag(), attribute.getClass().getName());
                builder.field(this.tags.getAttributeValueTag(), serializeAttribute((Serializable) attribute));
            }
            builder.endObject();
        }
        builder.endArray().endObject().endObject();
        return builder.build();
    }

    private String serializeAttribute(Serializable attribute) {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream(); final ObjectOutputStream objectOutputStream = new ObjectOutputStream(out)) {
            objectOutputStream.writeObject(attribute);
            objectOutputStream.close();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    private Object deserializeAttribute(String attribute) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(attribute)))) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    @Override
    public ElasticsearchSessionTags getTags() {
        return this.tags;
    }

}
