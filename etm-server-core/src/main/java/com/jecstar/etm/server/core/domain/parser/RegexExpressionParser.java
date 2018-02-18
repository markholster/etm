package com.jecstar.etm.server.core.domain.parser;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class RegexExpressionParser extends AbstractExpressionParser {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(RegexExpressionParser.class);
    private final Pattern compiledExpression;
    private final String expression;
    private final Integer group;
    private final boolean canonicalEquivalence;
    private final boolean caseInsensitive;
    private final boolean dotall;
    private final boolean literal;
    private final boolean multiline;
    private final boolean unicodeCase;
    private final boolean unicodeCharacterClass;
    private final boolean unixLines;

    public RegexExpressionParser(final String name,
                                 final String expression,
                                 final Integer group,

                                 final boolean canonicalEquivalence,
                                 final boolean caseInsensitive,
                                 final boolean dotall,
                                 final boolean literal,
                                 final boolean multiline,
                                 final boolean unicodeCase,
                                 final boolean unicodeCharacterClass,
                                 final boolean unixLines) {
        super(name);
        this.group = group;
        this.canonicalEquivalence = canonicalEquivalence;
        this.caseInsensitive = caseInsensitive;
        this.dotall = dotall;
        this.literal = literal;
        this.multiline = multiline;
        this.unicodeCase = unicodeCase;
        this.unicodeCharacterClass = unicodeCharacterClass;
        this.unixLines = unixLines;

        this.compiledExpression = createCompiledExpression(expression, getFlags());
        this.expression = expression;
    }

    private int getFlags() {
        int flags = 0;
        if (this.canonicalEquivalence) {
            flags |= Pattern.CANON_EQ;
        }
        if (this.caseInsensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        if (this.dotall) {
            flags |= Pattern.DOTALL;
        }
        if (this.literal) {
            flags |= Pattern.LITERAL;
        }
        if (this.multiline) {
            flags |= Pattern.MULTILINE;
        }
        if (this.unicodeCase) {
            flags |= Pattern.UNICODE_CASE;
        }
        if (this.unicodeCharacterClass) {
            flags |= Pattern.UNICODE_CHARACTER_CLASS;
        }
        if (this.unixLines) {
            flags |= Pattern.UNIX_LINES;
        }
        return flags;
    }

    private Pattern createCompiledExpression(String expression, int flags) {
        try {
            return Pattern.compile(expression, flags);
        } catch (PatternSyntaxException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Error creating regular expression from '" + expression + ".", e);
            }
            throw new EtmException(EtmException.INVALID_REGULAR_EXPRESSION, e);
        }
    }

    @Override
    public String evaluate(String content) {
        if (this.compiledExpression == null) {
            return null;
        }
        final Matcher matcher = this.compiledExpression.matcher(content);
        boolean found = matcher.find();
        if (found && matcher.groupCount() >= this.group) {
            return matcher.group(this.group);
        }
        return null;
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
        final Matcher matcher = this.compiledExpression.matcher(content);
        if (allValues) {
            return matcher.replaceAll(replacement);
        } else {
            return matcher.replaceFirst(replacement);
        }
    }

    public String getExpression() {
        return this.expression;
    }

    public Integer getGroup() {
        return this.group;
    }

    public boolean isCanonicalEquivalence() {
        return this.canonicalEquivalence;
    }

    public boolean isCaseInsensitive() {
        return this.caseInsensitive;
    }

    public boolean isDotall() {
        return this.dotall;
    }

    public boolean isLiteral() {
        return this.literal;
    }

    public boolean isMultiline() {
        return this.multiline;
    }

    public boolean isUnicodeCase() {
        return this.unicodeCase;
    }

    public boolean isUnicodeCharacterClass() {
        return this.unicodeCharacterClass;
    }

    public boolean isUnixLines() {
        return this.unixLines;
    }
}
