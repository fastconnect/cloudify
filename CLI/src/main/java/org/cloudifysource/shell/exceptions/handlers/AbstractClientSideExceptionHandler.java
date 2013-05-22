package org.cloudifysource.shell.exceptions.handlers;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 10:37 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractClientSideExceptionHandler implements ClientSideExceptionHandler {

    public abstract String getFormattedMessage();
    public abstract String getVerbose();

    @Override
    public String getMessage(final boolean verbose) {

        if (verbose) {
            // display the stack trace if present
            final String stackTrace = getVerbose();
            if (stackTrace != null) {
                return getFormattedMessage() + " : " + stackTrace;
            } else {
                return getFormattedMessage();
            }
        } else {
            return getFormattedMessage();
        }
    }
}
