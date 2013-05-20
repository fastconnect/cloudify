package org.cloudifysource.rest.events.cache;

import com.gigaspaces.log.ContinuousLogEntryMatcher;
import com.gigaspaces.log.LogEntryMatcher;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/18/13
 * Time: 8:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class LogEntryMatcherProvider {

    private static final Logger logger = Logger.getLogger(LogEntryMatcherProvider.class.getName());

    private final LoadingCache<LogEntryMatcherProviderKey, ContinuousLogEntryMatcher> matcherCache;

    public LogEntryMatcherProvider() {

        this.matcherCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new CacheLoader<LogEntryMatcherProviderKey, ContinuousLogEntryMatcher>() {

                    @Override
                    public ContinuousLogEntryMatcher load(final LogEntryMatcherProviderKey key) throws Exception {
                        LogEntryMatcher matcher = EventsUtils.createMatcher();
                        logger.fine(EventsUtils.getThreadId() + "Loading matcher cache with matcher for key " + key);
                        return new ContinuousLogEntryMatcher(matcher, matcher);
                    }
                });
    }

    /**
     * Removes all matchers that were dedicated to events with a curtain key.
     * @param key - the specified key.
     */
    public void removeAll(final EventsCacheKey key) {
        System.out.println(EventsUtils.getThreadId() + "Removing matcher for key " + key);

        for (LogEntryMatcherProviderKey logMatcherKey : new HashSet<LogEntryMatcherProviderKey>(matcherCache.asMap().keySet())) {
            if (logMatcherKey.getEventsCacheKey().equals(key)) {
                matcherCache.asMap().remove(logMatcherKey);
            }
        }
    }

    public LogEntryMatcher getOrLoad(final LogEntryMatcherProviderKey key) {
        return matcherCache.getUnchecked(key);
    }
}
