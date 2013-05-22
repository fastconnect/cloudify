package org.cloudifysource.shell.exceptions.handlers;

import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIStatusException;

import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 7:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class CLIStatusExceptionHandler extends AbstractClientSideExceptionHandler {

    private CLIStatusException e;

    public CLIStatusExceptionHandler(final CLIStatusException e) {
        this.e = e;
    }

    @Override
    public String getFormattedMessage() {
        String message = ShellUtils.getFormattedMessage(e.getReasonCode(), e.getArgs());
        if (message == null) {
            message = e.getReasonCode();
        }
        return message;
    }

    @Override
    public String getVerbose() {
       return e.getVerboseData();
    }

    @Override
    public Level getLoggingLevel() {
        return Level.WARNING;
    }
}
