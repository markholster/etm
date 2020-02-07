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
