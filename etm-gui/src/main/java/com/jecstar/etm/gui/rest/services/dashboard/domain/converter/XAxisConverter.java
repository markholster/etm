package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.XAxis;
import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;

public class XAxisConverter extends NestedObjectConverter<XAxis> {

    public XAxisConverter() {
        super(f -> new XAxis());
    }

}
