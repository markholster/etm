package com.jecstar.etm.gui.rest.export;

public enum FileType {

    CSV("text/csv"), XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String contentType;

    FileType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return this.contentType;
    }
}
