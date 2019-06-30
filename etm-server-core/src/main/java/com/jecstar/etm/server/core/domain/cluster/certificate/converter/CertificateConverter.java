package com.jecstar.etm.server.core.domain.cluster.certificate.converter;

import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.domain.cluster.certificate.Certificate;

public class CertificateConverter extends JsonEntityConverter<Certificate> {

    public CertificateConverter() {
        super(f -> new Certificate());
    }

}
