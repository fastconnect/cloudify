package org.cloudifysource.rest.events.cache;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/20/13
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class LogEntryMatcherProviderKey {

    private String containerId;
    private EventsCacheKey eventsCacheKey;

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(final String containerId) {
        this.containerId = containerId;
    }

    public EventsCacheKey getEventsCacheKey() {
        return eventsCacheKey;
    }

    public void setEventsCacheKey(final EventsCacheKey eventsCacheKey) {
        this.eventsCacheKey = eventsCacheKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogEntryMatcherProviderKey that = (LogEntryMatcherProviderKey) o;

        if (!containerId.equals(that.containerId)) return false;
        if (!eventsCacheKey.equals(that.eventsCacheKey)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = containerId.hashCode();
        result = 31 * result + eventsCacheKey.hashCode();
        return result;
    }
}
