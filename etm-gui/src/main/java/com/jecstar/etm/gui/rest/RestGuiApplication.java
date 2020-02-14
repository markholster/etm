/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.gui.rest;

import com.jecstar.etm.gui.rest.services.dashboard.DashboardService;
import com.jecstar.etm.gui.rest.services.iib.IIBService;
import com.jecstar.etm.gui.rest.services.search.SearchService;
import com.jecstar.etm.gui.rest.services.settings.AuditService;
import com.jecstar.etm.gui.rest.services.settings.SettingsService;
import com.jecstar.etm.gui.rest.services.signal.SignalService;
import com.jecstar.etm.gui.rest.services.user.UserService;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

public class RestGuiApplication extends Application {

    public RestGuiApplication(DataRepository dataRepository, EtmConfiguration etmConfiguration) {
        SearchService.initialize(dataRepository, etmConfiguration);
        UserService.initialize(dataRepository, etmConfiguration);
        SettingsService.initialize(dataRepository, etmConfiguration);
        AuditService.initialize(dataRepository, etmConfiguration);
        DashboardService.initialize(dataRepository, etmConfiguration);
        SignalService.initialize(dataRepository, etmConfiguration);
        if (IIBApi.IIB_PROXY_ON_CLASSPATH) {
            IIBService.initialize(dataRepository, etmConfiguration);
        }
    }


    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();
        classes.add(SearchService.class);
        classes.add(UserService.class);
        classes.add(SettingsService.class);
        classes.add(AuditService.class);
        classes.add(DashboardService.class);
        classes.add(SignalService.class);
        if (IIBApi.IIB_PROXY_ON_CLASSPATH) {
            classes.add(com.jecstar.etm.gui.rest.services.iib.IIBService.class);
        }
        return classes;
    }
}
