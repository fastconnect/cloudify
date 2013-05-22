package org.cloudifysource.shell.exceptions.handlers;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.shell.exceptions.CLIException;

import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 7:47 PM
 */
public class CLIExceptionHandler extends AbstractClientSideExceptionHandler {

    private CLIException e;

    public CLIExceptionHandler(final CLIException e) {
        this.e = e;
    }

    @Override
    public String getFormattedMessage() {
        return e.getMessage();
    }

    @Override
    public String getVerbose() {
        return ExceptionUtils.getFullStackTrace(e);
    }

    @Override
    public Level getLoggingLevel() {
        return Level.WARNING;
    }
}
