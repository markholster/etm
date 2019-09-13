package com.jecstar.etm.server.core.domain.parser;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.NamePool.NamePoolLimitException;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

public class XsltExpressionParser extends AbstractExpressionParser {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(XsltExpressionParser.class);

    private TransformerFactory transformerFactory;
    private final String template;
    private final ErrorListener errorListener;

    public XsltExpressionParser(final String name, final String template) {
        this(name, template, new XmlErrorListener());
    }

    XsltExpressionParser(final String name, final String template, ErrorListener errorListener) {
        super(name);
        this.errorListener = errorListener;
        this.transformerFactory = createTransformerFactory(this.errorListener);
        this.template = template;
        // Test if the transformer can be created.
        createTransformer(template);
    }

    private TransformerFactory createTransformerFactory(ErrorListener errorListener) {
        Configuration config = Configuration.newConfiguration();
        config.setErrorListener(errorListener);
        config.setErrorListener(new XmlErrorListener());
        config.setSchemaValidationMode(Validation.STRIP);
        config.setValidation(false);
        config.getParseOptions().addParserFeature("http://xml.org/sax/features/validation", false);
        config.getParseOptions().addParserFeature("http://apache.org/xml/features/validation/schema", false);
        config.getParseOptions().addParserFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        config.getParseOptions().addParserFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return new TransformerFactoryImpl(config);
    }

    private Transformer createTransformer(String template) {
        try (StringReader reader = new StringReader(template)) {
            return this.transformerFactory.newTransformer(new StreamSource(reader));
        } catch (TransformerConfigurationException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Error creating xslt template from '" + template + ".", e);
            }
            throw new EtmException(EtmException.INVALID_XSLT_TEMPLATE, e);
        }
    }

    @Override
    public String evaluate(String content) {
        return doTransform(content, true);
    }

    private String doTransform(String content, boolean returnNullOnFailure) {
        StringWriter writer = new StringWriter();
        try {
            createTransformer(getTemplate()).transform(new StreamSource(new StringReader(content)), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("XSLT template '" + this.template + "' could not be applied on content '" + content + "'.", e);
            }
            return returnNullOnFailure ? null : content;
        } catch (NamePoolLimitException e) {
            this.transformerFactory = createTransformerFactory(this.errorListener);
            try {
                createTransformer(getTemplate()).transform(new StreamSource(new StringReader(content)), new StreamResult(writer));
                return writer.toString();
            } catch (TransformerException e2) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("XSLT template '" + this.template + "' could not be applied on content '" + content + "'.", e2);
                }
                return returnNullOnFailure ? null : content;
            }
        }
    }

    public String getTemplate() {
        return this.template;
    }

    @Override
    public boolean isCapableOfReplacing() {
        return true;
    }

    @Override
    public String replace(String content, String replacement, boolean allValues) {
        return doTransform(content, false);
    }
}
