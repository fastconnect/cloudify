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
    private transient Object mutex;

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
        if (mutex == null) {
            mutex = new Object();
        }
        return mutex;
    }
}
