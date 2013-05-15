package org.cloudifysource.rest.events.cache;

import com.gigaspaces.log.LogEntries;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.cloudifysource.rest.events.cache.loader.LogEntriesCacheLoader;
import org.openspaces.admin.gsc.GridServiceContainer;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/13/13
 * Time: 11:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class LogEntriesCache {

    private final LoadingCache<GridServiceContainer, LogEntries> logsLoadingCache;

    public LogEntriesCache() {

        logsLoadingCache = CacheBuilder.newBuilder().build(new LogEntriesCacheLoader());
    }

    public LogEntries get(final GridServiceContainer container) {
        LogEntries logEntries = logsLoadingCache.getIfPresent(container);
        if (logEntries == null) {
            // cache was cleaned for this key, or never existed. in any case, load it again.
            return logsLoadingCache.getUnchecked(container);
        } else {
            // reload cache with latest logs.
            logsLoadingCache.refresh(container);
            return logsLoadingCache.getIfPresent(container);
        }
    }
}
