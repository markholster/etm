package com.jecstar.etm.gui.rest.export;

import java.util.Comparator;
import java.util.List;

public enum MultiSelect {

    LOWEST() {
        @Override
        Object select(List<Object> values) {
            if (values == null || values.isEmpty()) {
                return null;
            }
            values.sort(this.objectComparator);
            return values.get(0);
        }
    }, HIGHEST() {
        @Override
        Object select(List<Object> values) {
            if (values == null || values.isEmpty()) {
                return null;
            }
            values.sort(this.objectComparator);
            return values.get(values.size() - 1);
        }
    }, FIRST() {
        @Override
        Object select(List<Object> values) {
            if (values == null || values.isEmpty()) {
                return null;
            }
            return values.get(0);
        }
    }, LAST() {
        @Override
        Object select(List<Object> values) {
            if (values == null || values.isEmpty()) {
                return null;
            }
            return values.get(values.size() - 1);
        }
    };

    abstract Object select(List<Object> values);

    private static final Comparator<Object> objectComparator = (o1, o2) -> o1.toString().compareTo(o2.toString());
}
