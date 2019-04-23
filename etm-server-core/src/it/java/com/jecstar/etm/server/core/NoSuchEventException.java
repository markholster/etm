package com.jecstar.etm.server.core;

class NoSuchEventException extends RuntimeException {

    private static final long serialVersionUID = -5013113610442814312L;

    private final String index;
    private final String id;
    private final Long version;

    public NoSuchEventException(String index, String id, Long version) {
        this.index = index;
        this.id = id;
        this.version = version;
    }

    @Override
    public String getMessage() {
        if (this.version == null) {
            return "Event '" + this.index + " - " + this.id + "' does not exist";
        } else {
            return "Event '" + this.index + " - " + this.id + " - " + this.version + "' does not exist";
        }
    }
}
