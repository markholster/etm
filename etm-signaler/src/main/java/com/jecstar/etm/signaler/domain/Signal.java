package com.jecstar.etm.signaler.domain;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.signaler.domain.converter.DataConverter;
import com.jecstar.etm.signaler.domain.converter.NotificationsConverter;
import com.jecstar.etm.signaler.domain.converter.ThresholdConverter;

public class Signal {

    public static final String NAME = "name";
    public static final String ENABLED = "enabled";
    public static final String DATA = "data";
    public static final String THRESHOLD = "threshold";
    public static final String NOTIFICATIONS = "notifications";

    @JsonField(NAME)
    private String name;
    @JsonField(ENABLED)
    private boolean enabled = true;
    @JsonField(value = DATA, converterClass = DataConverter.class)
    private Data data;
    @JsonField(value = THRESHOLD, converterClass = ThresholdConverter.class)
    private Threshold threshold;
    @JsonField(value = NOTIFICATIONS, converterClass = NotificationsConverter.class)
    private Notifications notifications;

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

    public String getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public Data getData() {
        return this.data;
    }

    public Threshold getThreshold() {
        return this.threshold;
    }

    public Notifications getNotifications() {
        return this.notifications;
    }

    public Signal setName(String name) {
        this.name = name;
        return this;
    }

    public Signal setData(Data data) {
        this.data = data;
        return this;
    }

    public Signal setThreshold(Threshold threshold) {
        this.threshold = threshold;
        return this;
    }

    public Signal setNotifications(Notifications notifications) {
        this.notifications = notifications;
        return this;
    }
}
