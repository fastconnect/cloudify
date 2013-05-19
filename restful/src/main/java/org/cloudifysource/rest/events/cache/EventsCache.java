package org.cloudifysource.rest.events.cache;

import com.gigaspaces.log.LogEntries;
import com.google.common.cache.*;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import edu.emory.mathcs.backport.java.util.Collections;
import org.cloudifysource.dsl.rest.response.InstanceDeploymentEvents;
import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvents;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/13/13
 * Time: 8:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class EventsCache {

    private final LoadingCache<EventsCacheKey, ServiceDeploymentEvents> eventsLoadingCache;
    private final LogEntryMatcherProvider matcherProvider;

    private final Admin admin;

    public EventsCache(final Admin admin) {
        this.admin = admin;
        this.matcherProvider = new LogEntryMatcherProvider();
        this.eventsLoadingCache = createEventsCache();
    }

    public ServiceDeploymentEvents getIfPresent(final EventsCacheKey key) {
        return eventsLoadingCache.getIfPresent(key);
    }

    public void refresh(final EventsCacheKey key) {
        eventsLoadingCache.refresh(key);
    }

    public ServiceDeploymentEvents get(final EventsCacheKey key) throws ExecutionException {
        return eventsLoadingCache.get(key);
    }

    private LoadingCache<EventsCacheKey, ServiceDeploymentEvents> createEventsCache() {
        return CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<Object, Object>() {
                    @Override
                    public void onRemoval(final RemovalNotification<Object, Object> notification) {

                        final EventsCacheKey key = (EventsCacheKey) notification.getKey();
                        final ServiceDeploymentEvents events = (ServiceDeploymentEvents) notification.getValue();

                        // lets remove the corresponding matchers.
                        for (InstanceDeploymentEvents instanceDeploymentEvents : events.getEventsPerInstance().values()) {
                            matcherProvider.remove(instanceDeploymentEvents.getContainerUid());
                        }
                    }
                }).build(new CacheLoader<EventsCacheKey, ServiceDeploymentEvents>() {

                    @Override
                    public ServiceDeploymentEvents load(final EventsCacheKey key) throws Exception {

                        System.out.println(EventsUtils.getThreadId() + "Loading events cache for entry with key : " + key);

                        ServiceDeploymentEvents events = new ServiceDeploymentEvents();

                        // initial load. no events are present in the cache for this deployment.
                        // iterate over all container and retrieve logs from logs cache.
                        final String fullServiceName = ServiceUtils.getAbsolutePUName(key.getAppName(), key.getServiceName());
                        Set<GridServiceContainer> containersForDeployment = EventsUtils.getContainersForDeployment(fullServiceName, admin);
                        for (GridServiceContainer container : containersForDeployment) {
                            LogEntries logEntries = container.logEntries(matcherProvider.getOrLoad(container.getUid()));
                            InstanceDeploymentEvents instanceDeploymentEvents = EventsUtils.logsToEvents(logEntries, 0, container.getUid());
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
                        String absolutePUName = ServiceUtils.getAbsolutePUName(key.getAppName(), key.getServiceName());
                        Set<GridServiceContainer> containersForDeployment = EventsUtils.getContainersForDeployment(absolutePUName, admin);
                        for (GridServiceContainer container : containersForDeployment) {

                            // this will give us just the new logs.
                            LogEntries logEntries = container.logEntries(matcherProvider.get(container.getUid()));

                            // retrieve the latest event for this instance
                            String processingUnitInstanceName = container.getProcessingUnitInstances()[0].getProcessingUnitInstanceName();
                            int lastEventForInstance = (Integer) Collections.max(oldValue.getEventsPerInstance()
                                    .get(processingUnitInstanceName).getEventPerIndex().keySet());
                            InstanceDeploymentEvents instanceDeploymentEvents = EventsUtils.logsToEvents(logEntries,
                                    lastEventForInstance, container.getUid());

                            // add new events the cache.
                            oldValue.add(processingUnitInstanceName, instanceDeploymentEvents);
                        }

                        // update refresh time.
                        oldValue.setLastRefreshedTimestamp(System.currentTimeMillis());
                        System.out.println(EventsUtils.getThreadId() + "Finished Reloading events cache for entry with key : " + key);
                        return Futures.immediateFuture(oldValue);
                    }
                });
    }

}
