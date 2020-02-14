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

package com.jecstar.etm.slf4j;

import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.writer.TelemetryEventWriter;
import com.jecstar.etm.domain.writer.json.LogTelemetryEventWriterJsonImpl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.*;

public class RemoteEtmLogForwarder implements EtmLogForwarder {

    static Configuration configuration;

    private int urlIndex = 0;

    private final Queue<String> eventQueue = new ConcurrentLinkedQueue<>();
    private final TelemetryEventWriter<String, LogTelemetryEvent> writer = new LogTelemetryEventWriterJsonImpl();
    private final ScheduledExecutorService scheduler;

    private static final RemoteEtmLogForwarder INSTANCE = new RemoteEtmLogForwarder();

    private RemoteEtmLogForwarder() {
        scheduler = Executors.newScheduledThreadPool(configuration.getNumberOfWorkers(), new DaemonThreadFactory());
        for (int i = 0; i < configuration.getNumberOfWorkers(); i++) {
            this.scheduler.scheduleAtFixedRate(new QueueDrainer(), configuration.getPushInterval() / 2, configuration.getPushInterval(), TimeUnit.MILLISECONDS);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new ForwarderCloser(), "ETM-Log-Forwarder-Shutdown-Hook"));
    }

    public static RemoteEtmLogForwarder getInstance() {
        return INSTANCE;
    }

    public void forwardLog(LogTelemetryEvent event) {
        if (event != null) {
            this.eventQueue.offer(this.writer.write(event));
        }
    }

    private class ForwarderCloser extends QueueDrainer {

        @Override
        public void run() {
            RemoteEtmLogForwarder.this.scheduler.shutdown();
            super.run();
        }
    }

    private class QueueDrainer implements Runnable {

        private final StringBuilder content = new StringBuilder();

        @Override
        public void run() {
            try {
                if (configuration.getEndpointUrls().size() == 0) {
                    System.out.println("No ETM endpoints configured. Log messages will be dropped!");
                    RemoteEtmLogForwarder.this.eventQueue.clear();
                    return;
                }
                String element = RemoteEtmLogForwarder.this.eventQueue.poll();
                drain:
                while (element != null) {
                    this.content.setLength(0);
                    this.content.append("[");
                    int batchIx = 0;
                    while (batchIx < configuration.getMaxRequestsPerBatch() && element != null) {
                        if (batchIx != 0) {
                            this.content.append(",");
                        }
                        this.content.append("{\"type\": \"log\", \"data\": ").append(element).append("}");
                        batchIx++;
                        element = RemoteEtmLogForwarder.this.eventQueue.poll();
                    }
                    this.content.append("]");
                    int startIx = RemoteEtmLogForwarder.this.urlIndex;
                    while (!drainBatch(this.content, RemoteEtmLogForwarder.this.urlIndex)) {
                        increaseUrlIndex();
                        if (startIx == RemoteEtmLogForwarder.this.urlIndex) {
                            System.out.println("Failed to find a working ETM endpoint. Log messages will be dropped!");
                            // All URLS failed. Empty buffer to prevent
                            // OutOfMemoryError.
                            RemoteEtmLogForwarder.this.eventQueue.clear();
                            break drain;
                        }
                    }
                    increaseUrlIndex();
                }
            } catch (Throwable t) {
                System.out.println("Failed to drain logger queue: " + t.getMessage());
                t.printStackTrace();
            }
        }

        private void increaseUrlIndex() {
            RemoteEtmLogForwarder.this.urlIndex++;
            if (RemoteEtmLogForwarder.this.urlIndex >= configuration.getEndpointUrls().size()) {
                RemoteEtmLogForwarder.this.urlIndex = 0;
            }
        }

        private boolean drainBatch(StringBuilder content, int urlIndex) {
            HttpURLConnection con = null;
            DataOutputStream stream = null;
            BufferedReader in = null;
            try {
                URL url = new URL(configuration.getEndpointUrls().get(urlIndex));
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(1000);
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                con.setDoOutput(true);
                stream = new DataOutputStream(con.getOutputStream());
                stream.write(content.toString().getBytes(Charset.forName("utf-8")));
                stream.flush();
                stream.close();

                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                response.append("");
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                con.disconnect();
                return "{ \"status\": \"acknowledged\" }".equals(response.toString().trim());
            } catch (Throwable t) {
                return false;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                }
                if (con != null) {
                    con.disconnect();
                }
            }
        }
    }

    private class DaemonThreadFactory implements ThreadFactory {
        int nr = 0;

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("ETM-Log-Forwarder-" + this.nr++);
            t.setDaemon(true);
            return t;
        }
    }
}
