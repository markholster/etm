package com.jecstar.etm.gui.rest.services.dashboard.domain.converter;

import com.jecstar.etm.gui.rest.services.dashboard.domain.Dashboard;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;

public class DashboardConverter extends JsonEntityConverter<Dashboard> {

    public DashboardConverter() {
        super(f -> new Dashboard());
    }
}
