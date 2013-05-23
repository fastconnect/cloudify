package org.cloudifysource.rest.events.cache;

import java.util.logging.Logger;

import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvent;
import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvents;
import org.cloudifysource.rest.exceptions.ResourceNotFoundException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;
import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/20/13
 * Time: 2:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class EventsCacheLoader extends CacheLoader<EventsCacheKey, EventsCacheValue> {

    private static final Logger logger = Logger.getLogger(EventsCacheLoader.class.getName());

    private final LogEntryMatcherProvider matcherProvider;
    private Admin admin;

    public EventsCacheLoader(final Admin admin) {

        this.admin = admin;
        this.matcherProvider = new LogEntryMatcherProvider();
    }

    @Override
    public EventsCacheValue load(final EventsCacheKey key) throws Exception {

        logger.fine(EventsUtils.getThreadId() + "Could not find events for key " + key + " in cache. Loading from container logs...");

        ServiceDeploymentEvents events = new ServiceDeploymentEvents();

        // initial load. no events are present in the cache for this deployment.
        // iterate over all container and retrieve logs from logs cache.
        GridServiceContainers containersForDeployment = EventsUtils.getContainersForDeployment(key.getDeploymentId(), admin);

        if (containersForDeployment == null) {
            throw new ResourceNotFoundException("Deployment with id " + key.getDeploymentId());
        }

        int index = 0;
        for (GridServiceContainer container : containersForDeployment) {

            LogEntryMatcherProviderKey logEntryMatcherProviderKey = new LogEntryMatcherProviderKey();
            logEntryMatcherProviderKey.setContainerId(container.getUid());
            logEntryMatcherProviderKey.setEventsCacheKey(key);
            LogEntries logEntries = container.logEntries(matcherProvider.getOrLoad(logEntryMatcherProviderKey));
            for (LogEntry logEntry : logEntries) {
                if (logEntry.isLog()) {
                    ServiceDeploymentEvent event = EventsUtils.logToEvent(logEntry, logEntries.getHostName(), logEntries.getHostAddress());
                    events.getEvents().put(index++, event);
                }
            }

        }

        EventsCacheValue value = new EventsCacheValue();
        value.setLastEventIndex(index);
        value.setEvents(events);
        value.setLastRefreshedTimestamp(System.currentTimeMillis());
        return value;
    }


    @Override
    public ListenableFuture<EventsCacheValue> reload(final EventsCacheKey key, final EventsCacheValue oldValue) throws Exception {

        logger.fine(EventsUtils.getThreadId() + "Reloading events cache entry for key " + key);

        // pickup any new containers along with the old ones
        GridServiceContainers containersForDeployment = EventsUtils.getContainersForDeployment(oldValue.getProcessingUnit());
        if (containersForDeployment != null) {
            int index = oldValue.getLastEventIndex();
            for (GridServiceContainer container : containersForDeployment) {

                // this will give us just the new logs.
                LogEntryMatcherProviderKey logEntryMatcherProviderKey = new LogEntryMatcherProviderKey();
                logEntryMatcherProviderKey.setContainerId(container.getUid());
                logEntryMatcherProviderKey.setEventsCacheKey(key);
                LogEntryMatcher matcher = matcherProvider.getOrLoad(logEntryMatcherProviderKey);
                LogEntries logEntries = container.logEntries(matcher);

                for (LogEntry logEntry : logEntries) {
                    if (logEntry.isLog()) {
                        ServiceDeploymentEvent event = EventsUtils.logToEvent(logEntry, logEntries.getHostName(), logEntries.getHostAddress());
                        oldValue.getEvents().getEvents().put(index++, event);
                    }
                }
            }

            // update refresh time.
            oldValue.setLastRefreshedTimestamp(System.currentTimeMillis());
            oldValue.setLastEventIndex(index);
        }
        return Futures.immediateFuture(oldValue);
    }

    public LogEntryMatcherProvider getMatcherProvider() {
        return matcherProvider;
    }
}
