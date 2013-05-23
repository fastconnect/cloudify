/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.events.cache;

import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvent;
import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvents;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.zone.Zone;

import java.text.MessageFormat;

import static com.gigaspaces.log.LogEntryMatchers.regex;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/16/13
 * Time: 10:47 AM
 * <br/><br/>
 *
 * Utility class for events related operations.
 * mainly the translation of logs to events.
 */
public final class EventsUtils {

    private EventsUtils() {

    }

    private static final String USM_EVENT_LOGGER_NAME = ".*.USMEventLogger.*";

    /**
     * Given a log entry, translate to event.
     * @param logEntry The log entry.
     * @param hostName The host name.
     * @param hostAddress The host address.
     * @return The event.
     */
    public static ServiceDeploymentEvent logToEvent(final LogEntry logEntry,
                                                     final String hostName,
                                                     final String hostAddress) {
        String text = logEntry.getText();
        String textWithoutLogger = text.split(" - ")[1];
        String actualEvent = textWithoutLogger.substring(textWithoutLogger.indexOf(".") + 1);
        ServiceDeploymentEvent event = new ServiceDeploymentEvent();
        event.setDescription("[" + hostName + "/" + hostAddress + "] - " + actualEvent);
        return event;
    }

    /**
     * Creates a matcher for {@link org.openspaces.admin.gsc.GridServiceContainer#logEntries(com.gigaspaces.log.LogEntryMatcher)}.
     * This matcher will find USM related event only using the {@code USM_EVENT_LOGGER_NAME} regex.
     * @return The log entry matcher.
     */
    public static LogEntryMatcher createMatcher(){
        final String regex = MessageFormat.format(USM_EVENT_LOGGER_NAME, new Object() {
        });
        return regex(regex);
    }

    /**
     * Retrieves containers of a processing unit by its zone.
     * @param pu The processing unit.
     * @return The containers.
     */
    public static GridServiceContainers getContainersForDeployment(final ProcessingUnit pu) {
        Zone zone = pu.getAdmin().getZones().getByName(pu.getName());
        if (zone == null) {
            return null;
        } else {
            return zone.getGridServiceContainers();
        }
    }

    /**
     * Retrieves containers of a processing unit by its deployment id.
     * @param deploymentId The deployment id.
     * @param admin The admin object for admin api access.
     * @return The continers.
     */
    public static GridServiceContainers getContainersForDeployment(final String deploymentId, final Admin admin) {

        for (ProcessingUnit pu : admin.getProcessingUnits()) {
            String puDeploymentId = (String) pu.getBeanLevelProperties().getContextProperties().get(CloudifyConstants.CONTEXT_PROPERTY_DEPLOYMENT_ID);
            if (puDeploymentId == null) {
                throw new IllegalStateException("Service " + pu.getName() + " does not have a deployment id context property");
            }
            if (deploymentId.equals(puDeploymentId)) {
                return getContainersForDeployment(pu);
            }
        }
        return null;
    }

    /**
     * Given a set of events and indices, extract only events who's index is in range.
     * @param events The events set.
     * @param from The start index.
     * @param to The end index.
     * @return The requested events.
     */
    public static ServiceDeploymentEvents extractDesiredEvents(final ServiceDeploymentEvents events,
                                                         final int from,
                                                         final int to) {

        ServiceDeploymentEvents desiredEvents = new ServiceDeploymentEvents();
        for (int i = from; i <= to; i++) {
            ServiceDeploymentEvent serviceDeploymentEvent = events.getEvents().get(i);
            if (serviceDeploymentEvent != null) {
                desiredEvents.getEvents().put(i, serviceDeploymentEvent);
            }
        }
        return desiredEvents;
    }

    /**
     * Given a set of events and indices, check if all index range of events is present.
     * i.e if events contains 1,2,3 and from=1 and to=4, this will return false. you get the idea.
     * @param events The events set.
     * @param from The start index.
     * @param to The end index.
     * @return true if all events in the range are present. false otherwise.
     */
    public static boolean eventsPresent(final ServiceDeploymentEvents events,
                                  final int from,
                                  final int to) {

        for (int i = from; i <= to; i++) {
            if (events.getEvents().get(i) == null) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @return The id of the current thread.
     */
    public static String getThreadId() {
        return "[" + Thread.currentThread().getName() + "][" + Thread.currentThread().getId() + "] - ";
    }

}
