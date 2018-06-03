package com.jecstar.etm.signaler.domain;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;

import java.time.Duration;
import java.util.List;

public class Signal {

    public enum Operation {
        AVERAGE, COUNT, MAX, MEDIAN, MIN, SUM, CARDINALITY;

        public static Operation safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return Operation.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum Comparison {
        LT, LTE, EQ, GTE, GT;

        public static Comparison safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return Comparison.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum TimeUnit {
        MINUTES() {
            @Override
            public Duration toDuration(int duration) {
                return Duration.ofMinutes(duration);
            }
        }, HOURS() {
            @Override
            public Duration toDuration(int duration) {
                return Duration.ofHours(duration);
            }
        }, DAYS() {
            @Override
            public Duration toDuration(int duration) {
                return Duration.ofDays(duration);
            }
        };

        public static TimeUnit safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return TimeUnit.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        public abstract Duration toDuration(int duration);
    }


    public static final String NAME = "name";
    public static final String DATA_SOURCE = "data_source";
    public static final String QUERY = "query";
    public static final String INTERVAL = "interval";
    public static final String INTERVAL_TIMEUNIT = "interval_timeunit";
    public static final String NOTIFIERS = "notifiers";
    public static final String OPERATION = "operation";
    public static final String CARDINALITY = "cardinality";
    public static final String CARDINALITY_TIMEUNIT = "cardinality_timeunit";
    public static final String FIELD = "field";
    public static final String COMPARISON = "comparison";
    public static final String THRESHOLD = "threshold";
    public static final String LIMIT = "limit";
    public static final String TIMESPAN = "timespan";
    public static final String TIMESPAN_TIMEUNIT = "timespan_timeunit";
    public static final String EMAIL_RECIPIENTS = "email_recipients";
    public static final String EMAIL_ALL_ETM_GROUP_MEMBERS = "email_all_etm_group_members";

    // Fields used by this signaler, but not stored in this class.
    public static final String LAST_EXECUTED = "last_executed_timestamp";
    public static final String LAST_FAILED = "last_failed_timestamp";
    public static final String FAILED_SINCE = "failed_since_timestamp";
    public static final String LAST_PASSED = "last_passed_timestamp";

    /**
     * An array of keys that aren't stored by the SignalConverter but need to be copied when the Signal is updated.
     */
    public static String[] METADATA_KEYS = new String[]{
            LAST_EXECUTED, LAST_FAILED, FAILED_SINCE, LAST_PASSED
    };

    // General fields
    @JsonField(NAME)
    private String name;
    @JsonField(DATA_SOURCE)
    private String dataSource;
    @JsonField(QUERY)
    private String query;
    @JsonField(INTERVAL)
    private Integer interval;
    @JsonField(value = INTERVAL_TIMEUNIT, converterClass = EnumConverter.class)
    private TimeUnit intervalTimeunit;
    @JsonField(NOTIFIERS)
    private List<String> notifiers;

    // Threshold fields
    @JsonField(value = OPERATION, converterClass = EnumConverter.class)
    private Operation operation;
    @JsonField(CARDINALITY)
    private Integer cardinality;
    @JsonField(value = CARDINALITY_TIMEUNIT, converterClass = EnumConverter.class)
    private TimeUnit cardinalityTimeunit;
    @JsonField(FIELD)
    private String field;
    @JsonField(value = COMPARISON, converterClass = EnumConverter.class)
    private Comparison comparison;
    @JsonField(THRESHOLD)
    private Integer threshold;

    //Alerting fields
    @JsonField(LIMIT)
    private Integer limit;
    @JsonField(TIMESPAN)
    private Integer timespan;
    @JsonField(value = TIMESPAN_TIMEUNIT, converterClass = EnumConverter.class)
    private TimeUnit timespanTimeunit;
    @JsonField(EMAIL_RECIPIENTS)
    private List<String> emailRecipients;
    @JsonField(EMAIL_ALL_ETM_GROUP_MEMBERS)
    private Boolean emailAllEtmGroupMembers;

    public String getName() {
        return this.name;
    }

    public String getDataSource() {
        return this.dataSource;
    }

    public String getQuery() {
        return this.query;
    }

    public Integer getInterval() {
        return this.interval;
    }

    public TimeUnit getIntervalTimeunit() {
        return this.intervalTimeunit;
    }

    public Duration getIntervalDuration() {
        return getIntervalTimeunit().toDuration(getInterval());
    }

    public List<String> getNotifiers() {
        return this.notifiers;
    }

    public Operation getOperation() {
        return this.operation;
    }

    public Integer getCardinality() {
        return this.cardinality;
    }

    public TimeUnit getCardinalityTimeunit() {
        return this.cardinalityTimeunit;
    }

    public String getCardinalityExpression() {
        return toTimestampExpression(getCardinality(), getCardinalityTimeunit());
    }

    public String getField() {
        return this.field;
    }

    public Comparison getComparison() {
        return this.comparison;
    }

    public Integer getThreshold() {
        return this.threshold;
    }

    public Integer getLimit() {
        return this.limit;
    }

    public Integer getTimespan() {
        return this.timespan;
    }

    public TimeUnit getTimespanTimeunit() {
        return this.timespanTimeunit;
    }

    public String getTimespanExpression() {
        return toTimestampExpression(getTimespan(), getTimespanTimeunit());
    }

    public List<String> getEmailRecipients() {
        return this.emailRecipients;
    }

    public Boolean isEmailAllEtmGroupMembers() {
        return emailAllEtmGroupMembers;
    }

    private String toTimestampExpression(int time, TimeUnit timeUnit) {
        switch (timeUnit) {
            case MINUTES:
                return time + "m";
            case HOURS:
                return time + "h";
            case DAYS:
                return time + "d";
            default:
                throw new IllegalArgumentException(timeUnit.name());
        }
    }

}
