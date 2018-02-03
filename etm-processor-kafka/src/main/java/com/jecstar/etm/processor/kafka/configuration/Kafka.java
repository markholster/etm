package com.jecstar.etm.processor.kafka.configuration;

import java.util.ArrayList;
import java.util.List;

public class Kafka {

    public boolean enabled = false;

    public List<Topic> topics = new ArrayList<>();

    public int getNumberOfListeners() {
        if (this.topics.isEmpty()) {
            return 0;
        }
        return this.topics.stream().mapToInt(Topic::getNrOfListeners).sum();
    }

    public List<Topic> getTopics() {
        return this.topics;
    }
}
