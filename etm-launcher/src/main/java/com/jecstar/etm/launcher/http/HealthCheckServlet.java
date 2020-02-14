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

import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.ClusterHealthRequestBuilder;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.common.unit.TimeValue;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

public class HealthCheckServlet extends HttpServlet {


    /**
     * The serialVersionUID for this class.
     */
    private static final long serialVersionUID = -7829850779479859389L;

    private static DataRepository dataRepository;

    public static void initialize(DataRepository dataRepository) {
        HealthCheckServlet.dataRepository = dataRepository;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        resp.setCharacterEncoding(Charset.defaultCharset().name());
        resp.setContentType("application/json");
        PrintWriter writer = resp.getWriter();
        writer.write("{ \"status\": \"Up and running\", \"es_status\": " + JsonBuilder.escapeToJson(getElasticsearchStatus(), true) + " }");
    }

    private String getElasticsearchStatus() {
        try {
            ClusterHealthResponse healths = dataRepository.clusterHealth(new ClusterHealthRequestBuilder().setTimeout(TimeValue.timeValueSeconds(10)));
            return healths.getStatus().name();
        } catch (ElasticsearchException e) {
            return "DOWN - " + e.getMessage();
        }
    }
}
