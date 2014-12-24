package com.holster.etm.processor.parsers;

public class FixedPositionExpressionParser implements ExpressionParser {
	
	private Integer startIx;
	private Integer endIx;
	
	public FixedPositionExpressionParser(Integer startIx, Integer endIx) {
	    this.startIx = startIx;
	    this.endIx = endIx;
    }

	@Override
    public String evaluate(String content) {
		if (content == null) {
			return null;
		}
		if (this.startIx != null && (this.startIx >= content.length() || this.startIx < 0)) {
			return null;
		}
		if (this.endIx != null && (this.endIx < 1 || this.endIx > content.length())) {
			return null;
		}
		if (this.startIx == null && this.endIx == null) {
			return null;
		}
		if (this.startIx != null && this.endIx == null) {
			return content.substring(this.startIx);
		} else if (this.startIx == null && this.endIx != null) {
			return content.substring(0, this.startIx);
		} else {
			return content.substring(this.startIx, this.endIx);
		}
    }

}
