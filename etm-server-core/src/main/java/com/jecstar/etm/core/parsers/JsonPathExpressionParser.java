package com.jecstar.etm.core.parsers;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;

public class JsonPathExpressionParser implements ExpressionParser {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(JsonPathExpressionParser.class);
	
	private final JsonPath path;
	
	static {
		Configuration.setDefaults(new JsonPathDefaults());
	}
	
	
	public JsonPathExpressionParser(String path) {
		this.path = JsonPath.compile(path);
		if (!this.path.isDefinite()) {
			throw new EtmException(EtmException.INVALID_JSON_EXPRESSION);
		}
	}
	
	@Override
    public String evaluate(String content) {
		if (this.path == null) {
			return null;
		}
		try {
			return JsonPath.parse(content).read(this.path, String.class);
		} catch (Exception e) {
        	if (log.isDebugLevelEnabled()) {
        		log.logDebugMessage("Json path '" + this.path.getPath() + "' could not be evaluated against content '" + content + "'.", e);
        	}
	        return null;
		}
    }
	
	public String getPath() {
	    return this.path.getPath();
    }

}
