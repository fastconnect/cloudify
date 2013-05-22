package org.cloudifysource.shell.exceptions.handlers;

import org.cloudifysource.shell.ShellUtils;

import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 10:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class InterruptedExceptionHandler extends AbstractClientSideExceptionHandler {

    @Override
    public String getFormattedMessage() {
        return ShellUtils.getFormattedMessage("command_interrupted", new Object[] {});
    }

    @Override
    public String getVerbose() {
        return null;
    }

    @Override
    public Level getLoggingLevel() {
        return Level.SEVERE;
    }
}
