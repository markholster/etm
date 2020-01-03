package com.jecstar.etm.server.core.domain.parser;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;

/**
 * <code>ExpressionParser</code> that uses the GraalVM for executing a javascript function.
 */
public class JavascriptExpressionParser extends AbstractExpressionParser {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(JavascriptExpressionParser.class);
    private final String script;
    private final String mainFunction;
    private Context context;
    private Value function;

    public JavascriptExpressionParser(final String name, String script, String mainFunction) {
        super(name);
        testValues(name, script, mainFunction);
        this.script = script;
        this.mainFunction = mainFunction;
    }

    private void testValues(String name, String script, String mainFunction) {
        try (var context = Context.create("js")) {
            context.eval(Source.newBuilder("js", script, name + ".js").build());
            var function = context.getBindings("js").getMember("main");
            if (function == null) {
                throw new EtmException(EtmException.INVALID_JAVASCRIPT_EXPRESSION, new IllegalArgumentException("Function '" + mainFunction + "' not found."));
            }
            function.execute("test", "test");
        } catch (IOException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Error creating Javascript Expression Parser with name '" + name + ".", e);
            }
            throw new EtmException(EtmException.INVALID_JAVASCRIPT_EXPRESSION, e);
        }
    }

    public String getScript() {
        return this.script;
    }

    public String getMainFunction() {
        return this.mainFunction;
    }

    @Override
    public synchronized String evaluate(String content) {
        initialize();
        Value value = this.function.execute(content);
        return value == null ? null : value.asString();
    }

    @Override
    public boolean isCapableOfReplacing() {
        return true;
    }

    @Override
    public String replace(String content, String replacement, boolean allValues) {
        if (content == null) {
            return null;
        }
        if (replacement == null) {
            replacement = "";
        }
        initialize();
        Value value = this.function.execute(content, replacement);
        return value == null ? null : value.asString();
    }

    @Override
    public void close() {
        if (this.context != null) {
            this.context.close();
        }
        super.close();
    }

    /**
     * Lazy load the GraalVM polyglot context.
     */
    private void initialize() {
        if (this.context != null) {
            return;
        }
        this.context = Context.create("js");
        try {
            this.context.eval(Source.newBuilder("js", script, getName() + ".js").build());
        } catch (IOException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Error creating Javascript Expression Parser with name '" + getName() + ".", e);
            }
            throw new EtmException(EtmException.INVALID_JAVASCRIPT_EXPRESSION, e);
        }
        this.function = context.getBindings("js").getMember("main");
        if (this.function == null) {
            throw new EtmException(EtmException.INVALID_JAVASCRIPT_EXPRESSION, new IllegalArgumentException("Function '" + mainFunction + "' not found."));
        }
    }
}