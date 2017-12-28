package com.jecstar.etm.processor.jms.configuration;

public class Destination {

	private String name;
	private String type = "queue";
	private int minNrOfListeners = 1;
	private int maxNrOfListeners = 5;

	private String messagesType = "auto"; // etmevent, clone

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public String getType() {
		return this.type;
	}
	
	public void setType(String type) {
		if (!"queue".equalsIgnoreCase(type) && !"topic".equalsIgnoreCase(type)) {
			throw new IllegalArgumentException("'" + type + "' is an invalid destination type.");
		}
		this.type = type;
	}

    public int getMinNrOfListeners() {
        if ("topic".equalsIgnoreCase(type)) {
            return 1;
        }
        return this.minNrOfListeners;
    }

    public void setMinNrOfListeners(int minNrOfListeners) {
        if (minNrOfListeners < 1 || minNrOfListeners > 65535) {
            throw new IllegalArgumentException(minNrOfListeners + " is an invalid minimum number of listeners");
        }
        this.minNrOfListeners = minNrOfListeners;
    }

    public int getMaxNrOfListeners() {
        if ("topic".equalsIgnoreCase(type)) {
            return 1;
        }
        return this.maxNrOfListeners;
    }

    public void setMaxNrOfListeners(int maxNrOfListeners) {
        if (maxNrOfListeners < 1 || maxNrOfListeners > 65535) {
            throw new IllegalArgumentException(maxNrOfListeners + " is an invalid maximum number of listeners");
        }
        this.maxNrOfListeners = maxNrOfListeners;
    }

    public String getMessagesType() {
        return this.messagesType;
    }

    public void setMessagesType(String messagesType) {
        if (!"auto".equalsIgnoreCase(messagesType)
                && !"etmevent".equalsIgnoreCase(messagesType)
                && !"clone".equalsIgnoreCase(messagesType)) {
            throw new IllegalArgumentException("'" + messagesType + "' is an invalid messages type.");
        }
        this.messagesType = messagesType;
    }
}
