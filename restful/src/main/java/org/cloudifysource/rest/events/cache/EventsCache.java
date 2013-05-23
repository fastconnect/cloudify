package org.cloudifysource.rest.events.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.openspaces.admin.Admin;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/13/13
 * Time: 8:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class EventsCache {

    private static final Logger logger = Logger.getLogger(EventsCache.class.getName());

    private final LoadingCache<EventsCacheKey, EventsCacheValue> eventsLoadingCache;
    private final LogEntryMatcherProvider matcherProvider;

    public EventsCache(final Admin admin) {

        final EventsCacheLoader loader = new EventsCacheLoader(admin);

        this.matcherProvider = loader.getMatcherProvider();
        this.eventsLoadingCache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<Object, Object>() {

                    @Override
                    public void onRemoval(final RemovalNotification<Object, Object> notification) {

                        // the onRemoval will also be triggered on reload (RemovalCause.REPLACED)
                        // in that case we don't want to remove the matcher.
                        if (notification.wasEvicted()) {

                            System.out.println("Entry with key " + notification.getKey() + " was removed from cache.");
                            final EventsCacheKey key = (EventsCacheKey) notification.getKey();
                            matcherProvider.removeAll(key);
                        }

                    }
                })
                .build(loader);
    }

    public void refresh(final EventsCacheKey key) {
        eventsLoadingCache.refresh(key);
    }

    public EventsCacheValue get(final EventsCacheKey key) throws ExecutionException {
        return eventsLoadingCache.get(key);
    }

    public void put(final EventsCacheKey key, final EventsCacheValue value) {
        if (!eventsLoadingCache.asMap().containsKey(key)) {
            eventsLoadingCache.asMap().put(key, value);
        }
    }
}
