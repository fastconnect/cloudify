package org.cloudifysource.shell.exceptions.handlers;

import org.cloudifysource.restclient.exceptions.RestClientException;

import java.util.logging.Level;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/21/13
 * Time: 7:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestClientExceptionHandler extends AbstractClientSideExceptionHandler {

    private RestClientException e;

    public RestClientExceptionHandler(final RestClientException e) {
        this.e = e;
    }

    @Override
    public String getFormattedMessage() {
        return e.getMessageFormattedText();
    }

    @Override
    public String getVerbose() {
        return e.getVerbose();
    }

    @Override
    public Level getLoggingLevel() {
        return Level.WARNING;
    }
}
