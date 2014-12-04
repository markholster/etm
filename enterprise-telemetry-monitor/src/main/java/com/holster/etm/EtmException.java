package com.holster.etm;

public class EtmException extends RuntimeException {

	private static final long serialVersionUID = 3169443490910515556L;
	
	public static final int WRAPPED_EXCEPTION = 100_000;

	private int errorCode;

	public EtmException(int errorCode) {
		this(errorCode, null);
	}

	public EtmException(int errorCode, Throwable cause) {
		super(cause);
		this.errorCode = errorCode;
	}


	public int getErrorCode() {
		return errorCode;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": Reason " + this.errorCode;
	}
}
