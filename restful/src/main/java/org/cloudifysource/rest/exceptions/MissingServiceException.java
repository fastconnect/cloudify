package org.cloudifysource.rest.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/18/13
 * Time: 6:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class MissingServiceException extends ResourceNotFoundException {

    public MissingServiceException(final String serviceName) {
        super(serviceName);
    }

    public String getServiceName() {
        return super.getResourceName();
    }
}
