package com.jecstar.etm.v1migrator;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.jecstar.etm.domain.builders.MessagingTelemetryEventBuilder;
import com.jecstar.etm.domain.writers.json.MessagingTelemetryEventWriterJsonImpl;

public class InsertRequestHandler {

	private final URL apiLocation;
	private final StringBuilder buffer = new StringBuilder();
	private final MessagingTelemetryEventWriterJsonImpl writer;

	private final long flushLength = 1024 * 1024 * 2;

	public InsertRequestHandler(String apiLocation) throws MalformedURLException {
		this.writer = new MessagingTelemetryEventWriterJsonImpl();
		this.apiLocation = new URL(apiLocation);
	}

	public boolean addBuilder(MessagingTelemetryEventBuilder builder) {
		if (this.buffer.length() == 0) {
			this.buffer.append("[");
		} else {
			this.buffer.append(",");
		}
		this.buffer.append("{\"type\": \"messaging\", \"data\": " + this.writer.write(builder.build()) + "}");
		if (this.buffer.length() > this.flushLength) {
			this.buffer.append("]");
			return flushBuffer();
		}
		return true;
	}

	public boolean flush() {
		if (this.buffer.length() == 0) {
			return true;
		}
		this.buffer.append("]");
		return flushBuffer();
	}

	private boolean flushBuffer() {
		HttpURLConnection con = null;
		DataOutputStream stream = null;
		try {
			con = (HttpURLConnection) this.apiLocation.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setRequestProperty("Content-Type", "application/json; charset=utf-8");

			// Send post request
			con.setDoOutput(true);
			stream = new DataOutputStream(con.getOutputStream());
			stream.write(this.buffer.toString().getBytes());
			stream.flush();
			stream.close();

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			//  TODO valideren op acknowledged
			in.close();
			con.disconnect();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (con != null) {
				con.disconnect();
			}
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
		}
		return false;
	}
}
