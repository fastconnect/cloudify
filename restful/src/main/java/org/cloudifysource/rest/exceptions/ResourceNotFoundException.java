package org.cloudifysource.rest.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/18/13
 * Time: 6:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResourceNotFoundException extends Exception {

    private String resourceDescription;

    public ResourceNotFoundException(final String resourceName) {
        this.resourceDescription = resourceName;
    }

    public String getResourceDescription() {
        return resourceDescription;
    }
}
