package com.jecstar.etm.gui.rest.services;

public class Keyword {
	
	public final static Keyword EXISTS = new Keyword("_exists_", null);
	public final static Keyword MISSING = new Keyword("_missing_", null);
	public final static Keyword TYPE = new Keyword("_type", null);

	private String name;
	private String type;

	public Keyword(String name, String type) {
		this.name = name;
		this.type = type;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getType() {
		return this.type;
	}
	
	public boolean isNumber() {
		return "long".equals(this.type) || "float".equals(this.type);
	}
	
	public boolean isDate() {
		return "date".equals(this.type);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Keyword) {
			return ((Keyword)obj).getName().equals(this.name);
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
}
