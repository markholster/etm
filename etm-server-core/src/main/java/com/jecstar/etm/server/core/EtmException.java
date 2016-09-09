package com.jecstar.etm.server.core;

public class EtmException extends RuntimeException {

	private static final long serialVersionUID = 3169443490910515556L;
	
	// 100_* reserved for common errors.
	public static final int WRAPPED_EXCEPTION 				= 100_000;
	
	// 200_* reserved for license errors.
	public static final int INVALID_LICENSE_KEY_EXCEPTION 	= 200_000;
	public static final int LICENSE_EXPIRED_EXCEPTION 		= 200_001;
	
	// 201_* reserved for confguration errors.
	public static final int CONFIGURATION_LOAD_EXCEPTION 	= 201_000;
	public static final int UNMARSHALLER_CREATE_EXCEPTION 	= 201_001;
	public static final int INVALID_XPATH_EXPRESSION 		= 201_002;
	public static final int INVALID_XSLT_TEMPLATE 			= 201_003;
	public static final int INVALID_JSON_EXPRESSION			= 201_004;
	public static final int INVALID_EXPRESSION_PARSER_TYPE 	= 201_005;
	public static final int INVALID_PASSWORD 				= 201_006;
	public static final int NO_MORE_ADMINS_LEFT 			= 201_007;
	
	// 300_* reserved for IIB errors.
	public static final int IIB_CONNECTION_ERROR 			= 300_000;
	public static final int IIB_UNKNOWN_OBJECT 				= 300_001;
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
