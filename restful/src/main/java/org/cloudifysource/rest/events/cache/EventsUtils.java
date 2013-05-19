package org.cloudifysource.rest.events.cache;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;
import com.gigaspaces.log.RegexLogEntryMatcher;
import org.cloudifysource.dsl.rest.response.InstanceDeploymentEvent;
import org.cloudifysource.dsl.rest.response.InstanceDeploymentEvents;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import static com.gigaspaces.log.LogEntryMatchers.regex;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/16/13
 * Time: 10:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class EventsUtils {

    private static final String USM_EVENT_LOGGER_NAME = ".*.USMEventLogger.*";

    public static InstanceDeploymentEvent logToEvent(final LogEntry logEntry,
                                                     final String hostName,
                                                     final String hostAddress) {
        String text = logEntry.getText();
        String textWithoutLogger = text.split(" - ")[1];
        String actualEvent = textWithoutLogger.substring(textWithoutLogger.indexOf(".") + 1);
        InstanceDeploymentEvent event = new InstanceDeploymentEvent();
        event.setDescirption("[" + hostName + "/" + hostAddress + "] - " + actualEvent);
        return event;
    }

    public static InstanceDeploymentEvents logsToEvents(final LogEntries logEntries,
                                                        final int lastEventIndex,
                                                        final String containerUid) {
        InstanceDeploymentEvents events = new InstanceDeploymentEvents();
        events.setContainerUid(containerUid);
        String hostName = logEntries.getHostName();
        String hostAddress = logEntries.getHostAddress();
        int index = lastEventIndex;
        for (LogEntry logEntry : logEntries) {
            if (logEntry.isLog()) {
                events.getEventPerIndex().put(index++, EventsUtils.logToEvent(logEntry, hostAddress, hostName));
            }
        }
        return events;
    }

    public static LogEntryMatcher createMatcher(){
        final String regex = MessageFormat.format(USM_EVENT_LOGGER_NAME, new Object() {
        });
        RegexLogEntryMatcher matcher = regex(regex);
        return matcher;
    }

    public static Set<GridServiceContainer> getContainersForDeployment(final String fullServiceName, final Admin admin) {
        Set<GridServiceContainer> containers = new HashSet<GridServiceContainer>();
        ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(fullServiceName);
        if (processingUnit == null) {
            return containers;
        }
        for (ProcessingUnitInstance puInstance : processingUnit) {
            containers.add(puInstance.getGridServiceContainer());
        }
        return containers;
    }

    public static String getThreadId() {
        return "[" + Thread.currentThread().getName() + "][" + Thread.currentThread().getId() + "] - ";
    }

}
