package org.cloudifysource.shell.exceptions.handlers;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 10:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThrowableHandler extends AbstractClientSideExceptionHandler {

    private Throwable t;

    public ThrowableHandler(final Throwable t) {
        this.t = t;
    }

    @Override
    public String getFormattedMessage() {
        return t.getMessage();
    }

    @Override
    public String getVerbose() {
        return ExceptionUtils.getFullStackTrace(t);
    }

    @Override
    public Level getLoggingLevel() {
        return Level.SEVERE;
    }
}
