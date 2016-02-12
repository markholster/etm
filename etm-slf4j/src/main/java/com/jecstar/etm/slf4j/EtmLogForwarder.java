package com.jecstar.etm.slf4j;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.jecstar.etm.core.domain.LogTelemetryEvent;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverter;
import com.jecstar.etm.core.domain.converter.json.LogTelemetryEventConverterJsonImpl;

public class EtmLogForwarder {

	private static final List<String> endpointUrls = new ArrayList<String>();
	private static int interval;
	private static int max_requests_per_batch;

	private int urlIndex = 0;

	private final Queue<String> eventQueue = new ConcurrentLinkedQueue<String>();
	private final TelemetryEventConverter<String, LogTelemetryEvent> converter = new LogTelemetryEventConverterJsonImpl();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new DaemonThreadFactory());

	static {
		endpointUrls.add("http://127.0.0.1:8080/rest/processor/event/_bulk");
		interval = 5000;
		max_requests_per_batch = 1000;
	}

	private static final EtmLogForwarder INSTANCE = new EtmLogForwarder();

	private EtmLogForwarder() {
		this.scheduler.scheduleAtFixedRate(new QueueDrainer(), interval, interval, TimeUnit.MILLISECONDS);
		Runtime.getRuntime().addShutdownHook(new Thread(new ForwarderCloser()));
	}

	public static EtmLogForwarder getInstance() {
		return INSTANCE;
	}

	public void forwardLog(LogTelemetryEvent event) {
		if (event != null) {
			this.eventQueue.offer(this.converter.convert(event));
		}
	}
	
	private class ForwarderCloser extends QueueDrainer {
		
		@Override
		public void run() {
			EtmLogForwarder.this.scheduler.shutdown();
			try {
				EtmLogForwarder.this.scheduler.awaitTermination(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			super.run();
		}
	}

	private class QueueDrainer implements Runnable {

		private StringBuilder content = new StringBuilder();
		
		@Override
		public void run() {
			try {
				if (endpointUrls.size() == 0) {
					System.out.println("No ETM endpoints configured. Log messages will be dropped!");
					EtmLogForwarder.this.eventQueue.clear();
					return;
				}
				String element = null;
				// Although the queue size is an estimation we don't want to
				// drain forever. This might happen if a log message is added
				// between the last drain (in the inner loop) and the next test
				// in the outer loop.
				drain:
				while ((element = EtmLogForwarder.this.eventQueue.poll()) != null) {
					this.content.setLength(0);
					this.content.append("[");
					int batchIx = 0;
					while (batchIx <= EtmLogForwarder.max_requests_per_batch && element != null) {
						if (batchIx != 0) {
							this.content.append(",");
						}
						this.content.append("{\"type\": \"log\", \"data\": " + element + "}");
						element = EtmLogForwarder.this.eventQueue.poll();
						batchIx++;
					}
					this.content.append("]");
					int startIx = EtmLogForwarder.this.urlIndex;
					while (!drainBatch(this.content, EtmLogForwarder.this.urlIndex)) {
						increaseUrlIndex();
						if (startIx == EtmLogForwarder.this.urlIndex) {
							System.out.println("Failed to find a working ETM endpoint. Log messages will be dropped!");
							// All URLS failed. Empty buffer to prevent
							// OutOfMemoryError.
							EtmLogForwarder.this.eventQueue.clear();
							break drain;
						}
					}
					increaseUrlIndex();
					if (element == null) {
						break drain;
					}
				}
			} catch (Throwable t) {
				System.out.println("Failed to drain logger queue: " + t.getMessage());
				t.printStackTrace();
			}
		}

		private void increaseUrlIndex() {
			EtmLogForwarder.this.urlIndex++;
			if (EtmLogForwarder.this.urlIndex >= endpointUrls.size()) {
				EtmLogForwarder.this.urlIndex = 0;
			}
		}

		private boolean drainBatch(StringBuilder content, int urlIndex) {
			HttpURLConnection con = null;
			DataOutputStream stream = null;
			BufferedReader in = null;
			try {
				URL url = new URL(endpointUrls.get(urlIndex));
				con = (HttpURLConnection) url.openConnection();
				con.setConnectTimeout(1000);
				con.setRequestMethod("POST");
				con.setRequestProperty("Content-Type", "application/json; charset=" + Charset.defaultCharset().name());
				con.setDoOutput(true);
				stream = new DataOutputStream(con.getOutputStream());
				stream.writeBytes(content.toString());
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
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	}
}
