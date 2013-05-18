package org.cloudifysource.rest.events.cache.loader;

import com.gigaspaces.log.*;
import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.rest.events.cache.EventsUtils;
import org.openspaces.admin.gsc.GridServiceContainer;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.gigaspaces.log.LogEntryMatchers.regex;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/13/13
 * Time: 10:53 AM
 * To change this template use File | Settings | File Templates.
 */
public class LogEntriesCacheLoader extends CacheLoader<GridServiceContainer, LogEntries> {

    private static final Logger logger = Logger.getLogger(LogEntriesCacheLoader.class.getName());

    private static final String USM_EVENT_LOGGER_NAME = ".*.USMEventLogger.*";

    private static final LogEntryMatcher regexMatcher;

    static {
        regexMatcher = createMatcher();
    }

    @Override
    public LogEntries load(final GridServiceContainer container) throws Exception {

        System.out.println(EventsUtils.getThreadId() + "Loading logs cache for container " + container.getUid());

        // first time loading logs from this container. use a brand new matcher.
        LogEntries logEntries = container.logEntries(regexMatcher);
        System.out.println(EventsUtils.getThreadId() + "Finished loading logs cache for container " + container.getUid()
                + " : \n" + StringUtils.join(toStrings(logEntries.getEntries()), "\n"));
        return logEntries;
    }

    @Override
    public ListenableFuture<LogEntries> reload(final GridServiceContainer container, final LogEntries oldValue) throws Exception {

        AfterEntryLogEntryMatcher afterEntryLogEntryMatcher = new AfterEntryLogEntryMatcher(oldValue,
                oldValue.getEntries().get(oldValue.getEntries().size() - 1),
                regexMatcher);

        System.out.println(EventsUtils.getThreadId() + "Reloading logs cache entry for container " + container.getUid());
        // get latest logs.

        LogEntries logEntries = container.logEntries(afterEntryLogEntryMatcher);
        List<LogEntry> entries = logEntries.getEntries();
        System.out.println(EventsUtils.getThreadId() + "Retrieved latest logs for container " + container.getUid()
                + " : " + StringUtils.join(toStrings(entries), "\n"));

        // remove old logs. they are not needed anymore.
        for (LogEntry entry : new ArrayList<LogEntry>(oldValue.getEntries())) {
            if (entry.isLog()) {
                // only remove logs. not file markers.
                oldValue.getEntries().remove(entry);
            }
        }

        // now add the new ones.
        oldValue.getEntries().addAll(entries);
        System.out.println(EventsUtils.getThreadId() + "New value for container " + container.getUid() + " : "
                + StringUtils.join(toStrings(oldValue.getEntries()), "\n"));
        System.out.println(EventsUtils.getThreadId() + "Finished Reloading logs cache entry for container " + container.getUid());
        return Futures.immediateFuture(oldValue);
    }

    private List<String> toStrings(final List<LogEntry> logEntries) {
        List<String> strings = new ArrayList<String>();
        for (LogEntry logEntry : logEntries) {
            strings.add(logEntry.getText());
        }
        return strings;
    }

    private static LogEntryMatcher createMatcher(){
        final String regex = MessageFormat.format(USM_EVENT_LOGGER_NAME, new Object() {});
        RegexLogEntryMatcher matcher = regex(regex);
        return matcher;
    }
}
