package org.cloudifysource.rest.events.cache;

import com.gigaspaces.log.ContinuousLogEntryMatcher;
import com.gigaspaces.log.LogEntryMatcher;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/18/13
 * Time: 8:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class LogEntryMatcherProvider {

    private final LoadingCache<String, ContinuousLogEntryMatcher> matchersCache;

    public LogEntryMatcherProvider() {

        this.matchersCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, ContinuousLogEntryMatcher>() {

                    @Override
                    public ContinuousLogEntryMatcher load(final String key) throws Exception {
                        LogEntryMatcher matcher = EventsUtils.createMatcher();
                        return new ContinuousLogEntryMatcher(matcher, matcher);
                    }
                });
    }

    public void remove(final String containerUid) {
        matchersCache.asMap().remove(containerUid);
    }

    public LogEntryMatcher getOrLoad(final String containerUid) {
        return matchersCache.getUnchecked(containerUid);
    }

    public LogEntryMatcher get(final String containerUid) {
        ContinuousLogEntryMatcher matcher = matchersCache.getIfPresent(containerUid);
        if (matcher == null) {
            throw new IllegalStateException("matcher cannot be null!");
        }
        return matcher;
    }
}
