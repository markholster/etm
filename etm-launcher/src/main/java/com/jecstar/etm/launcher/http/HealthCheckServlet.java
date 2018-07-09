package com.jecstar.etm.launcher.http;

import com.jecstar.etm.domain.writer.json.JsonWriter;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
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

    private static Client client;

    private final JsonWriter jsonWriter = new JsonWriter();

    public static void initialize(Client client) {
        HealthCheckServlet.client = client;
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
        writer.write("{ \"status\": \"Up and running\", \"es_status\": " + this.jsonWriter.escapeToJson(getElasticsearchStatus(), true) + " }");
    }

    private String getElasticsearchStatus() {
        try {
            ClusterHealthResponse healths = client.admin().cluster().prepareHealth().setTimeout(TimeValue.timeValueSeconds(10)).get();
            return healths.getStatus().name();
        } catch (ElasticsearchException e) {
            return "DOWN - " + e.getMessage();
        }
    }
}
