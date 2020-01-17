package com.jecstar.etm.server.core.persisting.elastic;

import com.jecstar.etm.server.core.domain.configuration.License;
import org.elasticsearch.common.io.stream.StreamOutput;

/**
 * <code>StreamOutput</code> instance that does nothing except counting the number of bytes that are passed through this instance.
 */
public class RequestUnitCalculatingStreamOutput extends StreamOutput {

    private long length = 0;

    @Override
    public void writeByte(byte b) {
        this.length++;
    }

    @Override
    public void writeBytes(byte[] b, int offset, int length) {
        this.length += length;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    public void reset() {
        this.length = 0;
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
