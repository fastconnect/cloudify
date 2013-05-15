package org.cloudifysource.rest.events.cache;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;
import com.google.common.cache.*;
import com.google.common.util.concurrent.ListenableFuture;
import org.cloudifysource.dsl.rest.response.InstanceDeploymentEvent;
import org.cloudifysource.dsl.rest.response.InstanceDeploymentEvents;
import org.cloudifysource.dsl.rest.response.ServiceDeploymentEvents;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.events.cache.loader.EventsCacheLoader;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.gigaspaces.log.LogEntryMatchers.regex;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/13/13
 * Time: 8:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class EventsCache {

    private final LoadingCache<EventsCacheKey, ServiceDeploymentEvents> eventsLoadingCache;

    public EventsCache(final Admin admin) {
        this.eventsLoadingCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new EventsCacheLoader(admin));
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
}
