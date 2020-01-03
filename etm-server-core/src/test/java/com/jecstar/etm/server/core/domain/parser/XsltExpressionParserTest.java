package com.jecstar.etm.server.core.domain.parser;

import org.junit.jupiter.api.Test;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

public class XsltExpressionParserTest {

    @Test
    public void testMultiThreading() throws InterruptedException {
        ErrorListener errorListener = new ErrorListener() {
            @Override
            public void warning(TransformerException exception) {
                fail(exception);
            }

            @Override
            public void error(TransformerException exception) {
                fail(exception);
            }

            @Override
            public void fatalError(TransformerException exception) {
                fail(exception);
            }
        };
        XsltExpressionParser xsltExpressionParser = new XsltExpressionParser("test", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:fo=\"http://www.w3.org/1999/XSL/Format\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:fn=\"http://www.w3.org/2005/xpath-functions\" xmlns:xdt=\"http://www.w3.org/2005/xpath-datatypes\">" +
                "<xsl:template match=\"/\">\n" +
                "<xsl:element name=\"world\">\n" +
                "<xsl:for-each-group select=\"//country\" group-by=\"@continent\">\n" +
                "<xsl:sort select=\"@continent\" data-type=\"text\" order=\"ascending\"/>\n" +
                "<xsl:variable name=\"continent\" select=\"@continent\"/>\n" +
                "<xsl:apply-templates select=\"//country[@continent = $continent]\" mode=\"group\">\n" +
                "<xsl:sort select=\"@name\" data-type=\"text\" order=\"ascending\"/>\n" +
                "</xsl:apply-templates>\n" +
                "</xsl:for-each-group>\n" +
                "</xsl:element>\n" +
                "</xsl:template>\n" +
                "<xsl:template match=\"*\" mode=\"group\">\n" +
                "<xsl:copy-of select=\".\"/>\n" +
                "</xsl:template>\n" +
                "</xsl:stylesheet>",
                errorListener);
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 100000; i++) {
            executorService.submit(() -> {
                xsltExpressionParser.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<world>\n" +
                        "<country name=\"Canada\" continent=\"North America\">\n" +
                        "<city>Toronto</city>\n" +
                        "<city>Vancouver</city>\n" +
                        "</country>\n" +
                        "<country name=\"Jamaica\" continent=\"North America\">\n" +
                        "<city>Kingston</city>\n" +
                        "<city>Ocho Rios</city>\n" +
                        "</country>\n" +
                        "<country name=\"United States\" continent=\"North America\">\n" +
                        "<city>Allentown</city>\n" +
                        "<city>Mobile</city>\n" +
                        "</country>\n" +
                        "<country name=\"United Kingdom\" continent=\"Europe\">\n" +
                        "<city>London</city>\n" +
                        "<city>Dundee</city>\n" +
                        "</country>\n" +
                        "<country name=\"France\" continent=\"Europe\">\n" +
                        "<city>Paris</city>\n" +
                        "<city>Nice</city>\n" +
                        "</country>\n" +
                        "<country name=\"Japan\" continent=\"Asia\">\n" +
                        "<city>Tokyo</city>\n" +
                        "<city>Osaka</city>\n" +
                        "</country>\n" +
                        "</world>", null, false);
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }
}
