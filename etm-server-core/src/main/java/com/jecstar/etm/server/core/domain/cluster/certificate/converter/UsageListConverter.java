package com.jecstar.etm.server.core.domain.cluster.certificate.converter;

import com.jecstar.etm.server.core.converter.custom.NestedListEnumConverter;
import com.jecstar.etm.server.core.domain.cluster.certificate.Usage;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class UsageListConverter extends NestedListEnumConverter<Usage, List<Usage>> {

    private static final LogWrapper log = LogFactory.getLogger(UsageListConverter.class);

    public UsageListConverter() {
        super(s -> {
            try {
                Method safeValueOf = Usage.class.getDeclaredMethod("safeValueOf", String.class);
                return (Usage) safeValueOf.invoke(null, s);
            } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                log.logErrorMessage("Unable to convert value '" + s + "'.", e);
                return null;
            }
        });
    }
}
