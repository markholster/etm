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

package com.jecstar.etm.launcher.http;

import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import io.undertow.security.idm.Account;

import java.util.HashSet;
import java.util.Set;

class EtmAccount implements Account {

    /**
     * The serialVersionUID for this class.
     */
    private static final long serialVersionUID = -7980565495248385591L;

    private final EtmPrincipal principal;
    private final long lastUpdated;
    private final Set<String> roles = new HashSet<>();

    EtmAccount(EtmPrincipal principal) {
        this.principal = principal;
        this.roles.addAll(principal.getRoles());
        for (EtmGroup group : this.principal.getGroups()) {
            this.roles.addAll(group.getRoles());
        }
        this.lastUpdated = System.currentTimeMillis();
    }

    @Override
    public EtmPrincipal getPrincipal() {
        return this.principal;
    }

    @Override
    public Set<String> getRoles() {
        return this.roles;
    }

    public long getLastUpdated() {
        return this.lastUpdated;
    }

}
