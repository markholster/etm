package com.jecstar.etm.server.core.persisting.elastic;

import com.jecstar.etm.server.core.domain.configuration.License;

import java.io.OutputStream;


public class RequestUnitCalculatingOutputStream extends OutputStream {

    private long length = 0;

    @Override
    public void write(int b) {
        this.length++;
    }

    /**
     * Gives the number of request units that are passed through this instance.
     *
     * @return The request unit amount that has passed through.
     */
    public double getRequestUnits() {
        return (double) this.length / License.BYTES_PER_RU;
    }
}
