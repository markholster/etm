package com.jecstar.etm.gui.rest.services.search;

public class TransactionEvent {

	public String index;
	public String type;
	public String id;
	public String name;
	public Long handlingTime;
	public String direction;
	public String payload;
	public String subType;

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TransactionEvent) {
			TransactionEvent other = (TransactionEvent) obj;
			return this.index.equals(other.index) && this.type.equals(other.type) && this.id.equals(other.id);
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return (this.index + this.type + this.id).hashCode();
	}
}
