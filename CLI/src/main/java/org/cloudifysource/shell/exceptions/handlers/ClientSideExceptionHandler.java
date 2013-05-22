package org.cloudifysource.shell.exceptions.handlers;

import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 8:03 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ClientSideExceptionHandler {

    String getMessage(boolean verbose);

    Level getLoggingLevel();
}
