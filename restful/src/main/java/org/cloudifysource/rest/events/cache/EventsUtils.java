package org.cloudifysource.rest.events.cache;

import com.gigaspaces.log.LogEntry;
import org.cloudifysource.dsl.rest.response.InstanceDeploymentEvent;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/16/13
 * Time: 10:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class EventsUtils {

    public static InstanceDeploymentEvent logToEvent(final LogEntry logEntry,
                                                     final String hostName,
                                                     final String hostAddress) {
        String text = logEntry.getText();
        String textWithoutLogger = text.split(" - ")[1];
        // remove the application name - TODO elip - why not leave this?
        String actualEvent = textWithoutLogger.substring(textWithoutLogger.indexOf(".") + 1);
        InstanceDeploymentEvent event = new InstanceDeploymentEvent();
        event.setDescirption("[" + hostName + "/" + hostAddress + "] - " + actualEvent);
        return event;
    }

    public static String getThreadId() {
        return "[" + Thread.currentThread().getName() + "][" + Thread.currentThread().getId() + "] - ";
    }

}
