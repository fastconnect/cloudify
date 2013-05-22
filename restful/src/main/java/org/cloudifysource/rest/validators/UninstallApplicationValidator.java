package org.cloudifysource.rest.validators;

import org.cloudifysource.rest.controllers.RestErrorException;

/**
 * uninstall-application validator interface.
 * 
 * @author adaml
 *
 */
public interface UninstallApplicationValidator {

	/**
	 * 
	 * @param validationContext .
	 * @throws RestErrorException .
	 */
	void validate(final UninstallApplicationValidationContext validationContext) throws RestErrorException;
}
