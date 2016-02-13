package com.jecstar.etm.slf4j;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
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

	private static Configuration configuration;
	
	static {
		try {
			@SuppressWarnings("unchecked")
			Class<Configuration> clazz = (Class<Configuration>) Class.forName("com.jecstar.etm.slf4j.ConfigurationImpl");
			configuration = clazz.newInstance();
		} catch (Throwable t) {}
		if (configuration == null) {
			String className = System.getProperty("etm.slf4j.configuration");
			if (className != null) {
				try {
					@SuppressWarnings("unchecked")
					Class<Configuration> clazz = (Class<Configuration>) Class.forName(className);
					configuration = clazz.newInstance();
				} catch (Throwable t) {}				
			}
		}
		if (configuration == null) {
			System.out.println("No etm-slf4j configuration found. Make sure "
					+ "class \"com.jecstar.etm.slf4j.ConfigurationImpl\" is "
					+ "available on the classpath and is implementing the "
					+ "\"com.jecstar.etm.slf4j.Configuration\" interface. "
					+ "Also make sure the class has a public default no-arg "
					+ "constructor. Another option is to provide the system "
					+ "property \"etm.slf4j.configuration\" with a classname "
					+ "that is implementing \"com.jecstar.etm.slf4j.Configuration\". "
					+ "Falling back to defaut configuration which does not log anything!");
		}
		if (configuration == null) {
			configuration = new DefaultConfiguration();
		}
	}

	private int urlIndex = 0;

	private final Queue<String> eventQueue = new ConcurrentLinkedQueue<String>();
	private final TelemetryEventConverter<String, LogTelemetryEvent> converter = new LogTelemetryEventConverterJsonImpl();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(configuration.getNumberOfWorkers(), new DaemonThreadFactory());


	private static final EtmLogForwarder INSTANCE = new EtmLogForwarder();

	private EtmLogForwarder() {
		for (int i=0; i < configuration.getNumberOfWorkers(); i++) { 
			this.scheduler.scheduleAtFixedRate(new QueueDrainer(), configuration.getPushInterval() / 2, configuration.getPushInterval(), TimeUnit.MILLISECONDS);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(new ForwarderCloser(), "ETM-Log-Forwarder-Shutdown-Hook"));
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
			super.run();
		}
	}

	private class QueueDrainer implements Runnable {

		private StringBuilder content = new StringBuilder();
		
		@Override
		public void run() {
			try {
				if (configuration.getEndpointUrls().size() == 0) {
					System.out.println("No ETM endpoints configured. Log messages will be dropped!");
					EtmLogForwarder.this.eventQueue.clear();
					return;
				}
				String element = EtmLogForwarder.this.eventQueue.poll();
				drain:
				while (element != null) {
					this.content.setLength(0);
					this.content.append("[");
					int batchIx = 0;
					while (batchIx < configuration.getMaxRequestsPerBatch() && element != null) {
						if (batchIx != 0) {
							this.content.append(",");
						}
						this.content.append("{\"type\": \"log\", \"data\": " + element + "}");
						batchIx++;
						element = EtmLogForwarder.this.eventQueue.poll();
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
				}
			} catch (Throwable t) {
				System.out.println("Failed to drain logger queue: " + t.getMessage());
				t.printStackTrace();
			}
		}

		private void increaseUrlIndex() {
			EtmLogForwarder.this.urlIndex++;
			if (EtmLogForwarder.this.urlIndex >= configuration.getEndpointUrls().size()) {
				EtmLogForwarder.this.urlIndex = 0;
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
		int nr = 0;
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("ETM-Log-Forwarder-" + this.nr++);
			t.setDaemon(true);
			return t;
		}
	}
}
