package com.jecstar.etm.signaler.domain.converter;

import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.signaler.domain.Signal;

public class SignalConverter extends JsonEntityConverter<Signal> {

    public SignalConverter() {
        super(Signal::new);
    }
}
