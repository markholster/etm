package com.jecstar.etm.core;

public class EtmException extends RuntimeException {

	private static final long serialVersionUID = 3169443490910515556L;
	
	
	public static final int WRAPPED_EXCEPTION 				= 100_000;
	public static final int INVALID_LICENSE_KEY_EXCEPTION 	= 100_001;
	public static final int LICENSE_EXPIRED_EXCEPTION 		= 100_002;
	public static final int CONFIGURATION_LOAD_EXCEPTION 	= 100_003;
	public static final int UNMARSHALLER_CREATE_EXCEPTION 	= 100_004;
	public static final int INVALID_XPATH_EXPRESSION 		= 100_005;
	public static final int INVALID_XSLT_TEMPLATE 			= 100_006;
	public static final int INVALID_JSON_EXPRESSION			= 100_007;
	public static final int INVALID_EXPRESSION_PARSER_TYPE 	= 100_008;
	// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	// When adding an code, also add the code to com.jecstar.etm.gui.rest.EtmExceptionMapper and to the user manual!!!!!
	// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	
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
