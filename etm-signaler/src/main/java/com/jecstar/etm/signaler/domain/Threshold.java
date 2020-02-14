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

package com.jecstar.etm.signaler.domain;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.converter.AggregatorListConverter;

import java.util.List;

public class Threshold {

    public static final String COMPARISON = "comparison";
    public static final String VALUE = "value";
    public static final String CARDINALITY = "cardinality";
    public static final String CARDINALITY_TIMEUNIT = "cardinality_timeunit";
    private static final String AGGREGATORS = "aggregators";

    public enum Comparison {
        LT() {
            @Override
            public boolean isExceeded(double limit, double valueToTest) {
                return Double.compare(valueToTest, limit) < 0;
            }
        },
        LTE() {
            @Override
            public boolean isExceeded(double limit, double valueToTest) {
                return Double.compare(valueToTest, limit) <= 0;
            }
        }, EQ() {
            @Override
            public boolean isExceeded(double limit, double valueToTest) {
                return Double.compare(valueToTest, limit) == 0;
            }
        }, GTE() {
            @Override
            public boolean isExceeded(double limit, double valueToTest) {
                return Double.compare(valueToTest, limit) >= 0;
            }
        }, GT() {
            @Override
            public boolean isExceeded(double limit, double valueToTest) {
                return Double.compare(valueToTest, limit) > 0;
            }
        };

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

        public abstract boolean isExceeded(double limit, double valueToTest);
    }

    @JsonField(value = COMPARISON, converterClass = EnumConverter.class)
    private Comparison comparison;
    @JsonField(VALUE)
    private double value;
    @JsonField(CARDINALITY)
    private int cardinality;
    @JsonField(value = CARDINALITY_TIMEUNIT, converterClass = EnumConverter.class)
    private TimeUnit cardinalityUnit;
    @JsonField(value = AGGREGATORS, converterClass = AggregatorListConverter.class)
    private List<Aggregator> aggregators;

    public Comparison getComparison() {
        return this.comparison;
    }

    public double getValue() {
        return this.value;
    }

    public TimeUnit getCardinalityUnit() {
        return this.cardinalityUnit;
    }

    public int getCardinality() {
        return this.cardinality;
    }

    public List<Aggregator> getAggregators() {
        return this.aggregators;
    }

    public Threshold setComparison(Comparison comparison) {
        this.comparison = comparison;
        return this;
    }

    public Threshold setValue(double value) {
        this.value = value;
        return this;
    }

    public Threshold setCardinality(int cardinality) {
        this.cardinality = cardinality;
        return this;
    }

    public Threshold setCardinalityUnit(TimeUnit cardinalityUnit) {
        this.cardinalityUnit = cardinalityUnit;
        return this;
    }

    public Threshold setAggregators(List<Aggregator> aggregators) {
        this.aggregators = aggregators;
        return this;
    }
}
