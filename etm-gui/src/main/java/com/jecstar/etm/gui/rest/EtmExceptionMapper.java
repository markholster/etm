package com.jecstar.etm.gui.rest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.jecstar.etm.core.EtmException;

@Provider
public class EtmExceptionMapper implements ExceptionMapper<Throwable> {

	@Override
	public Response toResponse(Throwable ex) {
		ErrorMessage errorMessage = new ErrorMessage();
		if (ex instanceof EtmException) {
			EtmException etmException = (EtmException) ex;
			switch(etmException.getErrorCode()) {
				case EtmException.WRAPPED_EXCEPTION:
					errorMessage.setMessage(ex.getCause().getMessage());
					break;
				case EtmException.INVALID_LICENSE_KEY_EXCEPTION:
					errorMessage.setMessage("Invalid license key");
					break;
				case EtmException.LICENSE_EXPIRED_EXCEPTION:
					errorMessage.setMessage("License expired");
					break;
				case EtmException.CONFIGURATION_LOAD_EXCEPTION:
					errorMessage.setMessage("Error loading configuration");
					break;
				case EtmException.UNMARSHALLER_CREATE_EXCEPTION:
					errorMessage.setMessage("Error creating unmarshaller");
					break;
				default:
					break;
			}
			errorMessage.setCode(etmException.getErrorCode());
		} else {
			errorMessage.setCode(EtmException.WRAPPED_EXCEPTION);
			errorMessage.setMessage(ex.getMessage());
		}
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity(errorMessage).type(MediaType.APPLICATION_JSON).build();
	}

}