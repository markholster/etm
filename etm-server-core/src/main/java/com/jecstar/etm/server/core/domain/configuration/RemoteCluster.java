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

package com.jecstar.etm.server.core.domain.configuration;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RemoteCluster {

    private String name;
    private boolean clusterWide;
    private Set<Seed> seeds = new HashSet<>();

    public RemoteCluster setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public RemoteCluster setClusterWide(boolean clusterWide) {
        this.clusterWide = clusterWide;
        return this;
    }

    public boolean isClusterWide() {
        return this.clusterWide;
    }

    public RemoteCluster addSeed(Seed seed) {
        this.seeds.add(seed);
        return this;
    }

    public Set<Seed> getSeeds() {
        return this.seeds;
    }

    public static class Seed {
        String host;
        int port;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Seed seed = (Seed) o;
            return port == seed.port &&
                    host.equals(seed.host);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, port);
        }

        public Seed setHost(String host) {
            this.host = host;
            return this;
        }

        public String getHost() {
            return this.host;
        }

        public Seed setPort(int port) {
            this.port = port;
            return this;
        }

        public int getPort() {
            return this.port;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteCluster that = (RemoteCluster) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
