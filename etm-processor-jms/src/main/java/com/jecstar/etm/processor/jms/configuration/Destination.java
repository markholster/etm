package com.jecstar.etm.processor.jms.configuration;

public class Destination {

	private String name;
	private String type = "queue";
	private int nrOfListeners = 1;
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
	
	public int getNrOfListeners() {
		if ("topic".equalsIgnoreCase(type)) {
			return 1;
		}
		return this.nrOfListeners;
	}
	
	public void setNrOfListeners(int nrOfListeners) {
		if (nrOfListeners < 1 || nrOfListeners > 65535) {
			throw new IllegalArgumentException(nrOfListeners + " is an invalid number of listeners");
		}
		this.nrOfListeners = nrOfListeners;
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
