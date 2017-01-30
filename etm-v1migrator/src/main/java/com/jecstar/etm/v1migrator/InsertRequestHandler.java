package com.jecstar.etm.v1migrator;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import com.jecstar.etm.domain.builders.MessagingTelemetryEventBuilder;
import com.jecstar.etm.domain.writers.json.MessagingTelemetryEventWriterJsonImpl;

public class InsertRequestHandler {

	public static final DateTimeFormatter dateTimeFormatterIndexPerDay = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR, 4)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendLiteral("-")
			.appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter().withZone(ZoneId.of("UTC"));
	
	private final URL apiLocation;
	private final StringBuilder buffer = new StringBuilder();
	private final MessagingTelemetryEventWriterJsonImpl writer;

	private final long flushLength = 1024 * 1024 * 2;

	public InsertRequestHandler(String apiLocation) throws MalformedURLException {
		this.writer = new MessagingTelemetryEventWriterJsonImpl();
		this.apiLocation = new URL(apiLocation);
	}

	public boolean addBuilder(MessagingTelemetryEventBuilder builder) {
		if (this.buffer.length() != 0) {
			this.buffer.append("\r\n");
		}
		this.buffer.append("{ \"index\" : { \"_index\" : \"etm_event_" + dateTimeFormatterIndexPerDay.format(ZonedDateTime.now()) + "\", \"_type\" : \"messaging\", \"_id\" : \"" + builder.getId() + "\" } }\r\n");
		this.buffer.append(this.writer.write(builder.build()));
		if (this.buffer.length() > this.flushLength) {
			return flushBuffer();
		}
		return true;
	}

	public boolean flush() {
		if (this.buffer.length() == 0) {
			return true;
		}
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
			this.buffer.setLength(0);
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
