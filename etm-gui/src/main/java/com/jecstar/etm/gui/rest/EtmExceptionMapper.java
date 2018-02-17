package com.jecstar.etm.gui.rest;


import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

@Provider
public class EtmExceptionMapper implements ExceptionMapper<Throwable> {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(EtmExceptionMapper.class);

    @Override
    public Response toResponse(Throwable ex) {
        ErrorMessage errorMessage = new ErrorMessage();
        if (ex instanceof EtmException) {
            EtmException etmException = (EtmException) ex;
            switch (etmException.getErrorCode()) {
                case EtmException.WRAPPED_EXCEPTION:
                    errorMessage.setMessage(getRootCauseMessage(ex));
                    break;
                case EtmException.UNAUTHORIZED:
                    errorMessage.setMessage("You are not authorized for this action");
                    break;
                case EtmException.INVALID_LICENSE_KEY:
                    errorMessage.setMessage("Invalid license key");
                    break;
                case EtmException.LICENSE_EXPIRED:
                    errorMessage.setMessage("License expired");
                    break;
                case EtmException.LICENSE_MESSAGE_COUNT_EXCEEDED:
                    errorMessage.setMessage("Your license does not permit more messages to be stored");
                    break;
                case EtmException.LICENSE_MESSAGE_SIZE_EXCEEDED:
                    errorMessage.setMessage("Your license does not permit more messages to be stored");
                    break;
                case EtmException.CONFIGURATION_LOAD_EXCEPTION:
                    errorMessage.setMessage("Error loading configuration");
                    break;
                case EtmException.UNMARSHALLER_CREATE_EXCEPTION:
                    errorMessage.setMessage("Error creating XML unmarshaller");
                    break;
                case EtmException.INVALID_XPATH_EXPRESSION:
                    errorMessage.setMessage("Invalid xpath expression");
                    break;
                case EtmException.INVALID_XSLT_TEMPLATE:
                    errorMessage.setMessage("Invalid xslt template");
                    break;
                case EtmException.INVALID_JSON_EXPRESSION:
                    errorMessage.setMessage("Invalid json path expression");
                    break;
                case EtmException.INVALID_EXPRESSION_PARSER_TYPE:
                    errorMessage.setMessage("Invalid expression parser type");
                    break;
                case EtmException.NO_MORE_USER_ADMINS_LEFT:
                    errorMessage.setMessage("No users with the '" + SecurityRoles.USER_SETTINGS_READ_WRITE + "' role left");
                    break;
                case EtmException.INVALID_LDAP_USER:
                    errorMessage.setMessage("Invalid LDAP user");
                    break;
                case EtmException.INVALID_LDAP_GROUP:
                    errorMessage.setMessage("Invalid LDAP group");
                    break;
                case EtmException.INVALID_PASSWORD:
                    errorMessage.setMessage("Invalid password");
                    break;
                case EtmException.PASSWORD_NOT_CHANGED:
                    errorMessage.setMessage("The new password may not be the same as the old password");
                    break;
                case EtmException.IIB_CONNECTION_ERROR:
                    errorMessage.setMessage("Unable to connect to IIB node");
                    break;
                case EtmException.IIB_UNKNOWN_OBJECT:
                    errorMessage.setMessage("Unknown IIB object");
                    break;
                case EtmException.MAX_NR_OF_GRAPHS_REACHED:
                    errorMessage.setMessage("Maximum number of graphs reached");
                case EtmException.MAX_NR_OF_DASHBOARDS_REACHED:
                    errorMessage.setMessage("Maximum number of dashboards reached");
                default:
                    break;
            }
            errorMessage.setCode(etmException.getErrorCode());
        } else {
            errorMessage.setCode(EtmException.WRAPPED_EXCEPTION);
            errorMessage.setMessage(getRootCauseMessage(ex));
        }
        if (log.isErrorLevelEnabled()) {
            log.logErrorMessage(errorMessage.getCode() + ": " + errorMessage.getMessage(), ex);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity(errorMessage).type(MediaType.APPLICATION_JSON).build();
    }

    private String getRootCauseMessage(Throwable ex) {
        List<Throwable> stack = new ArrayList<>();
        return getRootCauseMessage(ex, stack);
    }

    private String getRootCauseMessage(Throwable ex, List<Throwable> stack) {
        stack.add(ex);
        if (ex.getCause() != null && !stack.contains(ex.getCause())) {
            return getRootCauseMessage(ex.getCause(), stack);
        }
        return ex.getMessage();
    }


}