package org.cloudifysource.dsl.rest.response;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/8/13
 * Time: 4:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceDeploymentEvents {

    private Map<String, InstanceDeploymentEvents> eventsPerInstance = new HashMap<String, InstanceDeploymentEvents>();
    private long lastRefreshedTimestamp;
    private volatile Object mutex = new Object();

    public long getLastRefreshedTimestamp() {
        return lastRefreshedTimestamp;
    }

    public void setLastRefreshedTimestamp(final long lastRefreshedTimestamp) {
        this.lastRefreshedTimestamp = lastRefreshedTimestamp;
    }

    public Map<String, InstanceDeploymentEvents> getEventsPerInstance() {
        return eventsPerInstance;
    }

    public void setEventsPerInstance(final Map<String, InstanceDeploymentEvents> eventsPerInstance) {
        this.eventsPerInstance = eventsPerInstance;
    }

    public Object mutex() {
        return mutex;
    }

    public void add(final String processingUnitInstanceName, InstanceDeploymentEvents instanceDeploymentEvents) {
        eventsPerInstance.get(processingUnitInstanceName).add(instanceDeploymentEvents);
    }

    @Override
    public String toString() {
        return "ServiceDeploymentEvents{"
                + "eventsPerInstance=" + eventsPerInstance
                + ", lastRefreshedTimestamp=" + lastRefreshedTimestamp + '}';
    }
}
