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

package com.jecstar.etm.server.core.domain.aggregator;

import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public enum DateInterval {

    SECONDS("yyyy-MM-dd'T'HH:mm:ss", DateHistogramInterval.SECOND, 1000L),
    MINUTES("yyyy-MM-dd'T'HH:mm", DateHistogramInterval.MINUTE, 60 * 1000L),
    HOURS("yyyy-MM-dd'T'HH:mm", DateHistogramInterval.HOUR, 60 * 60 * 1000L),
    DAYS("yyyy-MM-dd", DateHistogramInterval.DAY, 24 * 60 * 60 * 1000L),
    WEEKS("yyyy-ww", DateHistogramInterval.WEEK, 7 * 24 * 60 * 60 * 1000L),
    MONTHS("yyyy-MM", DateHistogramInterval.MONTH, 30 * 24 * 60 * 60 * 1000L),
    QUARTERS(null, DateHistogramInterval.QUARTER, 13 * 7 * 24 * 60 * 60 * 1000L),
    YEARS("yyyy", DateHistogramInterval.YEAR, 365 * 24 * 60 * 60 * 1000L);

    private final DateHistogramInterval dateHistogramInterval;
    private final String simpleDateFormat;
    private final long milliseconds;

    DateInterval(String simpleDateFormat, DateHistogramInterval dateHistogramInterval, long milliseconds) {
        this.simpleDateFormat = simpleDateFormat;
        this.dateHistogramInterval = dateHistogramInterval;
        this.milliseconds = milliseconds;
    }

    public DateHistogramInterval getDateHistogramInterval() {
        return this.dateHistogramInterval;
    }

    public DateFormat getDateFormat(Locale locale, TimeZone timeZone) {
        if (DateInterval.QUARTERS.equals(this)) {
            return new QuarterDateFormat(locale, timeZone);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(this.simpleDateFormat, locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat;
    }

    public static DateInterval safeValueOf(String interval) {
        if ("seconds".equalsIgnoreCase(interval)) {
            return DateInterval.SECONDS;
        } else if ("minutes".equalsIgnoreCase(interval)) {
            return DateInterval.MINUTES;
        } else if ("hours".equalsIgnoreCase(interval)) {
            return DateInterval.HOURS;
        } else if ("days".equalsIgnoreCase(interval)) {
            return DateInterval.DAYS;
        } else if ("weeks".equalsIgnoreCase(interval)) {
            return DateInterval.WEEKS;
        } else if ("months".equalsIgnoreCase(interval)) {
            return DateInterval.MONTHS;
        } else if ("quarters".equalsIgnoreCase(interval)) {
            return DateInterval.QUARTERS;
        } else if ("years".equalsIgnoreCase(interval)) {
            return DateInterval.YEARS;
        }
        return null;
    }

    private class QuarterDateFormat extends SimpleDateFormat {

        private static final long serialVersionUID = 7698888710576662511L;

        private final Calendar calendar;

        private QuarterDateFormat(Locale locale, TimeZone timeZone) {
            super("yyyy");
            this.calendar = Calendar.getInstance(locale);
            this.calendar.setTimeZone(timeZone);
        }

        @Override
        public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
            this.calendar.setTime(date);
            return super.format(date, toAppendTo, pos).append(" Q").append((this.calendar.get(Calendar.MONTH) / 3) + 1);
        }
    }

}
