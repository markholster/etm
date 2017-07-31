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

import com.jecstar.etm.domain.writer.json.ConversionMessagingTelemetryEventWriterJsonImpl;

class InsertRequestHandler {

	private static final DateTimeFormatter dateTimeFormatterIndexPerDay = new DateTimeFormatterBuilder()
			.appendValue(ChronoField.YEAR, 4)
			.appendLiteral("-")
			.appendValue(ChronoField.MONTH_OF_YEAR, 2)
			.appendLiteral("-")
			.appendValue(ChronoField.DAY_OF_MONTH, 2).toFormatter().withZone(ZoneId.of("UTC"));
	
	private final URL apiLocation;
	private final StringBuilder buffer = new StringBuilder();
	private final ConversionMessagingTelemetryEventWriterJsonImpl writer;

    private int callCount;

	public InsertRequestHandler(String apiLocation) throws MalformedURLException {
		this.writer = new ConversionMessagingTelemetryEventWriterJsonImpl();
		this.apiLocation = new URL(apiLocation);
	}

	public void addBuilder(ConversionMessagingTelemetryEventBuilder builder, ZonedDateTime timestamp) {
		this.buffer.append("{ \"index\" : { \"_index\" : \"etm_event_").append(dateTimeFormatterIndexPerDay.format(timestamp)).append("\", \"_type\" : \"messaging\", \"_id\" : \"").append(builder.getId()).append("\" } }\n");
		this.buffer.append(this.writer.write(builder)).append("\n");
		this.callCount++;
	}

	public boolean shouldFlush() {
        int flushMaxCount = 100;
        long flushLength = 1024 * 1024 * 2;
        return this.buffer.length() > flushLength || this.callCount >= flushMaxCount;
	}
	
	public boolean flush() {
        return this.buffer.length() == 0 || flushBuffer();
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
			stream.write(this.buffer.toString().getBytes("UTF-8"));
			stream.flush();
			stream.close();

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			//  TODO valideren op acknowledged
			in.close();
			con.disconnect();
			this.buffer.setLength(0);
			this.callCount = 0;
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(this.buffer);
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
