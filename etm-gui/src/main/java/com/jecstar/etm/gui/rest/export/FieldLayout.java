package com.jecstar.etm.gui.rest.export;

public class FieldLayout {

    private String name;

    private String field;

    private FieldType type;

    private MultiSelect multiSelect;

    public FieldLayout(String name, String field, FieldType type, MultiSelect multiSelect) {
        this.name = name;
        this.field = field;
        this.type = type;
        this.multiSelect = multiSelect;
    }

    public String getName() {
        return this.name;
    }

    public String getField() {
        return this.field;
    }

    public FieldType getType() {
        return this.type;
    }

    public MultiSelect getMultiSelect() {
        return this.multiSelect;
    }
}
