package org.cloudifysource.rest.events.cache.loader;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.ListenableFuture;
import org.cloudifysource.dsl.rest.response.InstanceDeploymentEvent;
import org.cloudifysource.dsl.rest.response.InstanceDeploymentEvents;
import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvents;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.events.cache.EventsCacheKey;
import org.cloudifysource.rest.events.cache.LogEntriesCache;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.HashSet;
import java.util.Set;

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
        return events;
    }

    @Override
    public ListenableFuture<ServiceDeploymentEvents> reload(final EventsCacheKey key, final ServiceDeploymentEvents oldValue) throws Exception {
        return super.reload(key, oldValue);    //To change body of overridden methods use File | Settings | File Templates.
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
            events.getEventPerIndex().put(index++, toEvent(logEntry, hostAddress, hostName));
        }
        return events;
    }

    private InstanceDeploymentEvent toEvent(final LogEntry logEntry,
                                            final String hostAddress,
                                            final String hostName) {
        String text = logEntry.getText();
        String textWithoutLogger = text.split(" - ")[1];
        String instanceName = textWithoutLogger.split(" ")[0].split("\\.")[1];
        String eventText = textWithoutLogger.split(" ")[1];
        String actualEvent = instanceName + " " + eventText;
        InstanceDeploymentEvent event = new InstanceDeploymentEvent();
        event.setDescirption("[" + hostName + "/" + hostAddress + "] - " + actualEvent);
        return event;
    }

}
