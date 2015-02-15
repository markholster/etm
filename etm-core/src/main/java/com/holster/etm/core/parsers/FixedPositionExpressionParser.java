package com.holster.etm.core.parsers;

public class FixedPositionExpressionParser implements ExpressionParser {
	
	private Integer startIx;
	private Integer endIx;
	private Integer lineIx;
	
	public FixedPositionExpressionParser(Integer lineIx, Integer startIx, Integer endIx) {
		this.lineIx = lineIx;
	    this.startIx = startIx;
	    this.endIx = endIx;
    }

	@Override
    public String evaluate(String content) {
		if (content == null) {
			return null;
		}
		String line = null;
		if (this.lineIx != null) {
			String lines[] = content.split("\\r?\\n");
			if (lines.length >= this.lineIx) {
				return null;
			}
			line = lines[this.lineIx];
		} else {
			line = content;
		}
		if (this.startIx != null && (this.startIx >= line.length() || this.startIx < 0)) {
			return null;
		}
		if (this.endIx != null && (this.endIx < 1 || this.endIx > line.length())) {
			return null;
		}
		if (this.startIx == null && this.endIx == null) {
			return null;
		}
		if (this.startIx != null && this.endIx == null) {
			return line.substring(this.startIx);
		} else if (this.startIx == null && this.endIx != null) {
			return line.substring(0, this.startIx);
		} else {
			return line.substring(this.startIx, this.endIx);
		}
    }
	
	public Integer getStartIx() {
	    return this.startIx;
    }
	
	public Integer getEndIx() {
	    return this.endIx;
    }
	
	public Integer getLineIx() {
	    return this.lineIx;
    }

}
