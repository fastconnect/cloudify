package org.cloudifysource.rest.events.cache.loader;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.cloudifysource.dsl.rest.response.InstanceDeploymentEvent;
import org.cloudifysource.dsl.rest.response.InstanceDeploymentEvents;
import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvents;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.events.cache.EventsCacheKey;
import org.cloudifysource.rest.events.cache.EventsUtils;
import org.cloudifysource.rest.events.cache.LogEntriesCache;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/13/13
 * Time: 11:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class EventsCacheLoader extends CacheLoader<EventsCacheKey, ServiceDeploymentEvents> {

    private final LogEntriesCache logEntriesCache;
    private final Admin admin;

    public EventsCacheLoader(final Admin admin) {
        this.admin = admin;
        this.logEntriesCache = new LogEntriesCache();
    }

    @Override
    public ServiceDeploymentEvents load(final EventsCacheKey key) throws Exception {

        System.out.println(EventsUtils.getThreadId() + "Loading events cache for entry with key : " + key);

        ServiceDeploymentEvents events = new ServiceDeploymentEvents();

        // initial load. no events are present in the cache for this deployment.
        // iterate over all container and retrieve logs from logs cache.
        final String fullServiceName = ServiceUtils.getAbsolutePUName(key.getAppName(), key.getServiceName());
        Set<GridServiceContainer> containersForDeployment = getContainersForDeployment(fullServiceName);
        for (GridServiceContainer container : containersForDeployment) {
            LogEntries logEntries = logEntriesCache.get(container);
            InstanceDeploymentEvents instanceDeploymentEvents = toEvents(logEntries);
            events.getEventsPerInstance().put(container.getProcessingUnitInstances()[0].getProcessingUnitInstanceName(),
                    instanceDeploymentEvents);

        }
        events.setLastRefreshedTimestamp(System.currentTimeMillis());
        System.out.println(EventsUtils.getThreadId() + "Finished loading events cache for entry with key : " + key);
        return events;
    }

    @Override
    public ListenableFuture<ServiceDeploymentEvents> reload(final EventsCacheKey key, final ServiceDeploymentEvents oldValue) throws Exception {

        System.out.println(EventsUtils.getThreadId() + "Reloading events cache entry for key " + key);

        // pickup any new containers along with the old ones
        Set<GridServiceContainer> containersForDeployment = getContainersForDeployment(ServiceUtils.getAbsolutePUName(key.getAppName(), key.getServiceName()));
        for (GridServiceContainer container : containersForDeployment) {
            // refresh each container logs
            logEntriesCache.refresh(container);
            LogEntries logEntries = logEntriesCache.get(container);
            InstanceDeploymentEvents instanceDeploymentEvents = toEvents(logEntries);
            oldValue.add(container.getProcessingUnitInstances()[0].getProcessingUnitInstanceName(), instanceDeploymentEvents);
        }

        checkNotNull(key);
        checkNotNull(oldValue);
        oldValue.setLastRefreshedTimestamp(System.currentTimeMillis());
        System.out.println(EventsUtils.getThreadId() + "Finished Reloading events cache for entry with key : " + key);
        return Futures.immediateFuture(oldValue);
    }

    private Set<GridServiceContainer> getContainersForDeployment(final String fullServiceName) {
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

    private InstanceDeploymentEvents toEvents(final LogEntries logEntries) {
        InstanceDeploymentEvents events = new InstanceDeploymentEvents();
        String hostName = logEntries.getHostName();
        String hostAddress = logEntries.getHostAddress();
        int index = 0;
        for (LogEntry logEntry : logEntries) {
            if (logEntry.isLog()) {
                events.getEventPerIndex().put(index++, toEvent(logEntry, hostAddress, hostName));
            }
        }
        return events;
    }

    private InstanceDeploymentEvent toEvent(final LogEntry logEntry,
                                            final String hostAddress,
                                            final String hostName) {
        return EventsUtils.logToEvent(logEntry, hostName, hostAddress);
    }
}
