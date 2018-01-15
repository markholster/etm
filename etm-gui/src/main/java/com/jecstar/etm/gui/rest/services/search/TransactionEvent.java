package com.jecstar.etm.gui.rest.services.search;

class TransactionEvent {

	public String index;
	public String type;
	public String objectType;
	public String id;
	public String name;
	public Long handlingTime;
	public String direction;
	public String payload;
	public String subType;
	public String endpoint;

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
