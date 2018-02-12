package com.jecstar.etm.server.core.domain.converter;

import com.jecstar.etm.domain.PayloadEncoding;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class for the <code>PayloadDecoder</code> class.
 */
public class PayloadDecoderTest {

    @Test
    public void testNullValues() {
        PayloadDecoder decoder = new PayloadDecoder();
        assertNull(decoder.decode(null, PayloadEncoding.BASE64));
        assertEquals("Data", decoder.decode("Data", null));
    }

    @Test
    public void testBase64Decoding() {
        PayloadDecoder decoder = new PayloadDecoder();
        String data = "This is a test";
        assertEquals(data, decoder.decode(Base64.getEncoder().encodeToString(data.getBytes()), PayloadEncoding.BASE64_CA_API_GATEWAY));
    }

    @Test
    public void testBase64CaApiGatewayDecoding() {
        PayloadDecoder decoder = new PayloadDecoder();
        String data = "This is a test";
        // Create the l7 header.
        byte[] l7Header = createL7Header(data.getBytes());

        // compress the message.
        Deflater deflater = new Deflater();
        deflater.setInput(data.getBytes());
        deflater.finish();
        byte[] compressBuffer = new byte[data.length() * 2];
        int newSize = deflater.deflate(compressBuffer);
        deflater.end();

        // Create a new array with the l7 header and the compressed data;
        byte[] l7Message = new byte[l7Header.length + newSize];
        System.arraycopy(l7Header, 0, l7Message, 0, l7Header.length);
        System.arraycopy(compressBuffer, 0, l7Message, l7Header.length, newSize);

        // Validate the data.
        assertEquals(data, decoder.decode(Base64.getEncoder().encodeToString(l7Message), PayloadEncoding.BASE64_CA_API_GATEWAY));
    }

    private static byte[] createL7Header(byte[] data) {
        int uncompressedLength = data.length;
        byte[] result = new byte[4];
        result[0] = (byte) uncompressedLength;
        result[1] = (byte) (uncompressedLength >>> 8);
        result[2] = (byte) (uncompressedLength >>> 16);
        result[3] = (byte) (uncompressedLength >>> 24);
        return result;
    }
}
