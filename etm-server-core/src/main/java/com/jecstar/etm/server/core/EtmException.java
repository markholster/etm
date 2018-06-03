package com.jecstar.etm.server.core;

public class EtmException extends RuntimeException {

    private static final long serialVersionUID = 3169443490910515556L;

    // 100_* reserved for common errors.
    public static final int WRAPPED_EXCEPTION = 100_000;
    public static final int UNAUTHORIZED = 100_001;

    // 200_* reserved for license errors.
    public static final int INVALID_LICENSE_KEY = 200_000;
    public static final int LICENSE_EXPIRED = 200_001;
    public static final int LICENSE_MESSAGE_COUNT_EXCEEDED = 200_002;
    public static final int LICENSE_MESSAGE_SIZE_EXCEEDED = 200_003;

    // 201_* reserved for configuration errors.
    public static final int CONFIGURATION_LOAD_EXCEPTION = 201_000;
    // 201_010 till 201_029 reserved for expression parsers
    public static final int UNMARSHALLER_CREATE_EXCEPTION = 201_010;
    public static final int INVALID_XPATH_EXPRESSION = 201_011;
    public static final int INVALID_XSLT_TEMPLATE = 201_012;
    public static final int INVALID_JSON_EXPRESSION = 201_013;
    public static final int INVALID_REGULAR_EXPRESSION = 201_014;
    public static final int INVALID_EXPRESSION_PARSER_TYPE = 201_029;
    // 201_030 till ... reserved for users & groups
    public static final int NO_MORE_USER_ADMINS_LEFT = 201_030;
    public static final int INVALID_LDAP_USER = 201_031;
    public static final int INVALID_LDAP_GROUP = 201_032;

    // 202_* reserved for login errors
    public static final int INVALID_PASSWORD = 202_001;
    public static final int PASSWORD_NOT_CHANGED = 202_002;

    // 300_* reserved for IIB errors.
    public static final int IIB_CONNECTION_ERROR = 300_000;
    public static final int IIB_UNKNOWN_OBJECT = 300_001;

    // 400_* reserved for visualization errors.
    public static final int MAX_NR_OF_GRAPHS_REACHED = 400_000;
    public static final int MAX_NR_OF_DASHBOARDS_REACHED = 400_001;

    // 500_* reserved for visualization errors.
    public static final int MAX_NR_OF_SIGNALS_REACHED = 500_000;

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // When adding an code, also add the code to com.jecstar.etm.gui.rest.EtmExceptionMapper and to the user manual!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    private final int errorCode;

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
