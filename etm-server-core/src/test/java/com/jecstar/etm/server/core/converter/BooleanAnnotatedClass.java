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

package com.jecstar.etm.server.core.converter;

import java.util.Objects;

class BooleanAnnotatedClass {

    @JsonField("first_value")
    private Boolean firstValue;

    @JsonField(value = "second_value", writeWhenNull = true)
    private Boolean secondValue;

    @JsonField("third_value")
    private boolean thirdValue;

    private Boolean getFirstValue() {
        return this.firstValue;
    }

    void setFirstValue(Boolean firstValue) {
        this.firstValue = firstValue;
    }

    private Boolean getSecondValue() {
        return this.secondValue;
    }

    void setSecondValue(Boolean secondValue) {
        this.secondValue = secondValue;
    }

    private boolean getThirdValue() {
        return this.thirdValue;
    }

    void setThirdValue(boolean thirdValue) {
        this.thirdValue = thirdValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BooleanAnnotatedClass) {
            BooleanAnnotatedClass other = (BooleanAnnotatedClass) obj;
            return Objects.equals(getFirstValue(), other.getFirstValue())
                    && Objects.equals(getSecondValue(), other.getSecondValue())
                    && getThirdValue() == other.getThirdValue();
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFirstValue(), getSecondValue(), getThirdValue());
    }
}
